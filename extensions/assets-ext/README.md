# Assets Extension

Indexes and serves Cardano fungible token metadata from multiple CIP standards. Replaces the need for a standalone
[cf-token-metadata-registry](https://github.com/cardano-foundation/cf-token-metadata-registry) deployment — consumers
query a single yaci-store instance and get merged metadata transparently.

## Supported Standards

| Standard | Source | Description |
|----------|--------|-------------|
| CIP-26 | Off-chain (GitHub [cardano-token-registry](https://github.com/cardano-foundation/cardano-token-registry)) | Fungible token metadata synced periodically from Git |
| CIP-68 | On-chain (reference NFT inline datums; fungible tokens, label 333, and rich fungible tokens, label 444) | Token metadata parsed from blockchain. NFTs (label 222) are indexed but not served. |

## REST API

All endpoints use the configurable `${apiPrefix}` (default: `/api/v1`).

### Merged Metadata (main API)

The primary API — merges CIP-26 and CIP-68 metadata by configurable priority. Users don't need to know which
standard provides the data. Response-compatible with cf-token-metadata-registry's subjects API.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/subject/{subject}` | Query merged metadata for a single subject |
| `POST` | `/subject/query` | Batch query merged metadata (max 100 subjects) |

**Query parameters:**

- `property` (optional, repeatable) — filter which properties to return.
- `query_priority` (optional, repeatable) — CIP priority order, e.g. `?query_priority=CIP_68&query_priority=CIP_26`. Default: `CIP_68,CIP_26` (on-chain preferred).
- `show_cips_details` (optional, default `false`) — include raw per-standard metadata in the response.

**Example:**

```
GET /api/v1/subject/577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454

{
  "subject": {
    "subject": "577f0b...464c4454",
    "metadata": {
      "name": { "value": "FLDT", "source": "CIP_68" },
      "description": { "value": "The official token of FluidTokens", "source": "CIP_68" },
      "ticker": { "value": "FLDT", "source": "CIP_26" },
      "decimals": { "value": 6, "source": "CIP_68" },
      "url": { "value": "https://fluidtokens.com", "source": "CIP_26" }
    },
    "validation_errors": []
  },
  "queryPriority": ["CIP_68", "CIP_26"]
}
```

### Validation errors

A token is **returned as it is indexed**, whatever is wrong with its metadata: nothing is dropped for breaking a CIP. Instead, every
response has a `validation_errors` array (empty when the token follows its CIP) that says how it departs from it, so that the
owner can fix the datum or the registry entry, and a wallet can decide whether to accept the token (for example, reject any token with a
`MANDATORY` error).

```
GET /api/v1/subject/<policyId>0014df10<name>      # a 333 token with no description

{
  "subject": {
    "subject": "…",
    "metadata": { "name": { "value": "Coin", "source": "CIP_68" } },
    "validation_errors": [
      { "cip": "CIP-68", "sub_standard": "333", "field_name": "description", "field_category": "MANDATORY",
        "code": "missing_value", "error": "description field is missing a mandatory value" }
    ]
  },
  "queryPriority": ["CIP_68", "CIP_26"]
}
```

| Field | Meaning |
|---|---|
| `cip` | `CIP-26` or `CIP-68`. The errors of every standard that has the token are listed, in query priority order. |
| `sub_standard` | For CIP-68, the label the rule was checked for (`222`, `333`, `444`). A reference NFT with user tokens of several labels is checked against each of them. Absent for CIP-26 and for a rule that does not depend on a label. |
| `field_name` | The metadata field, for example `description`, `image`, `properties.owner`, `files`. |
| `field_category` | `MANDATORY` if the CIP requires the field, `OPTIONAL` otherwise. |
| `code` | A stable reason: `missing_value`, `wrong_type`, `invalid_uri_scheme`, `invalid_length`, `out_of_range`, `undefined_version`, `plutus_constructor`, `not_utf8`, `invalid_files_entry`, `no_nested_entry`, `too_deep`, `unsupported_key`, `key_collision`, `invalid_base64`, `too_large`. |
| `error` | A human-readable message. |

`validation_errors` is also on `standards.cip26` and `standards.cip68` (with `show_cips_details=true`), and on `/cip68/ft/...` and `/cip26/...`.
It is the one thing a client has to adapt to: a token that was `404` because its `name` or `description` was missing, or because
its version, `image` or `logo` did not follow CIP-68, is now `200` with errors, so `name` and `description` are no longer
guaranteed to be there. The `metadata.valid` flag the response always had stays. It is `true` when `validation_errors` is empty, that is when the token follows its CIP completely, optional fields included. It used to mean "name and description are both there" and was the gate of the API, so it was `true` for every token returned; now it can be `false`, and `validation_errors` has the detail. Only an obvious attack is still dropped (a datum or property nested so deep that it cannot be decoded, a CIP-26
entry whose subject cannot be a key, a logo far beyond any real size), with a warning in the log.

**Rich fungible tokens (CIP-68 label 444)** are served like fungible tokens, by their own `001bc280…` subject. They have no `logo`: their
`image` is served as `metadata.logo`, as cf-token-metadata-registry does, and `description` is optional for them.

Upgrading needs a **full resync**: the tokens that used to be dropped exist nowhere, and a row written before `validation_errors` existed has
none. A database is never upgraded in place; there is no backfill.

**Batch request:**

```
POST /api/v1/subject/query

{
  "subjects": ["subject1", "subject2"],
  "properties": ["name", "description", "ticker"]
}
```

### CIP-Specific Endpoints (direct access)

For advanced users who want to query a specific standard directly.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/cip26/{subject}` | CIP-26 off-chain metadata only |
| `GET` | `/cip68/ft/{policyId}/{assetName}` | CIP-68 fungible and rich fungible token metadata only |


## Configuration

```yaml
store:
  assets:
    enabled: true

    cip26:
      enabled: true                    # Enable GitHub registry sync
      git-organization: cardano-foundation
      git-project-name: cardano-token-registry
      sync-interval-minutes: 60

    cip68:
      enabled: true                    # Fungible (333) and rich fungible (444) token support

    query:
      priority: "CIP_68,CIP_26"       # Default merge priority
```

## Database Tables

| Table | Description |
|-------|-------------|
| `cip26_metadata` | CIP-26 token metadata, logo included (subject as PK), with `validation_errors` |
| `cip26_sync_state` | GitHub sync progress tracking |
| `cip68_metadata` | CIP-68 on-chain reference NFT metadata (composite PK: policy_id, asset_name, slot, tx_hash). `label` is 222, 333 or 444; `validation_errors` says what the datum breaks. |

All tables include a `last_synced_at` timestamp for monitoring. See [`db/store/README.md`](src/main/resources/db/store/README.md).

## Health Indicators

Spring Boot Actuator health indicators are provided:

- **Offchain sync** — reports CIP-26 GitHub sync status and last sync time
- **On-chain connection** — reports blockchain indexer connectivity
- **On-chain readiness** — reports whether on-chain sync has reached chain tip

Configure Kubernetes probe groups:

```yaml
management:
  endpoint:
    health:
      group:
        readiness:
          include: onchainReadiness, offchainSync
        liveness:
          include: onchainConnection
```
