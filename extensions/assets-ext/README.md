# Assets Extension

Indexes and serves Cardano fungible token metadata from multiple CIP standards. Replaces the need for a standalone
[cf-token-metadata-registry](https://github.com/cardano-foundation/cf-token-metadata-registry) deployment — consumers
query a single yaci-store instance and get merged metadata transparently.

## Supported Standards

| Standard | Source | Description |
|----------|--------|-------------|
| CIP-26 | Off-chain (GitHub [cardano-token-registry](https://github.com/cardano-foundation/cardano-token-registry)) | Fungible token metadata synced periodically from Git |
| CIP-68 | On-chain (reference NFT inline datums; fungible tokens, label 333, and rich fungible tokens, label 444) | Token metadata parsed from blockchain. NFTs (label 222) are not served, and a datum minted with the NFT alone is not stored. |
| CIP-113 | On-chain (the nodes of the programmable-token registry) | Marks a token whose policy is registered as `PROGRAMMABLE` and returns the scripts that govern it. Adds to CIP-26 or CIP-68 metadata, which still describe the token. |

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

Every subject has a `type`: `NATIVE`, or `PROGRAMMABLE` when its policy is registered in CIP-113 (see below); the example leaves it out.

### CIP-113 programmable tokens

A programmable token is a native asset whose every transfer is checked by a smart contract, so it cannot leave its issuer's rules (for
example a freeze-and-seize list). A token is programmable when its policy is registered in the on-chain CIP-113 registry. The registry says
which scripts govern the token; its name, ticker, decimals and logo still come from CIP-26 or CIP-68. As in cf-token-metadata-registry's V2 API:

- `type` is `PROGRAMMABLE`, and `extensions.cip113` holds the registry node of the token's policy: `minting_logic_script`,
  `transfer_logic_script`, `third_party_transfer_logic_script`, `unfracking_logic_script` (`null` means unfracking is forbidden) and
  `global_state_policy_id`. Each is `null` when the node has none. A native token has `type: NATIVE` and no `extensions`.
- A programmable token **without CIP-26 or CIP-68 metadata** is served, without metadata and with a `MANDATORY` error: `cip` `CIP-113`,
  `field_name` `companion_metadata`, `code` `missing_value`. `?validation=MANDATORY_ONLY` and `STRICT` leave it out.

```
"subject": {
  "subject": "01c24df7...5257414d696e74696e6754657374",
  "type": "PROGRAMMABLE",
  "metadata": { "valid": false },
  "extensions": {
    "cip113": {
      "minting_logic_script": "4e9ba4785eebbeb77fe694c8451f1abec411f7774c09bbacfa3ba1ac",
      "transfer_logic_script": "ddf0363f0d73f54778e792e16bad802df5fa50061319b1f01d280a37",
      "third_party_transfer_logic_script": "446fc9584df8316cfab440c60d140e2007f236cdb65180917a374fb8",
      "unfracking_logic_script": null,
      "global_state_policy_id": "218ba0c5b7156b13c51e41096e34f0310823adf0e0d3fc70a741f3c2"
    }
  },
  "validation_errors": [
    { "cip": "CIP-113", "field_name": "companion_metadata", "field_category": "MANDATORY", "code": "missing_value", "error": "..." }
  ]
}
```

The module keeps every version of every registry node in its own table, `cip113_registry_node`, filled from the blocks it indexes: it does not
read the utxo store's tables, so it works whether or not the utxo store prunes (ADR 0011). The registries deployed on mainnet, preprod and preview
are bundled and picked by `store.cardano.protocol-magic`. **A node is only seen if the sync covers the block that created it**: start the sync
before the registry was deployed (mainnet slot 199,384,339), or resync, otherwise a registered token is served as `NATIVE`. An output that holds
a registry NFT but whose datum is not a registry node, or that is not at the registry's address with the NFT of its own key, is not stored and is
recorded in `invalid_token` (`registry_node_unparseable`, `registry_node_spoofed`).

Each node records the registry it belongs to (the policy of its NFT), and only the configured registries are read. When a new registry is
deployed, list it together with the old one: the old registrations keep being served. A policy registered in more than one configured registry
is served from its most recent node. A registry that is no longer configured stops being served, though its rows stay.

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
| `sub_standard` | For CIP-68, the label the rule was checked for: the label of the subject asked for (`333` or `444`). A 333 and a 444 token of the same reference NFT are each checked for their own label. Absent for CIP-26 and for a rule that does not depend on a label. |
| `field_name` | The metadata field, for example `description`, `image`, `properties.owner`, `files`. |
| `field_category` | `MANDATORY` if the CIP requires the field, `OPTIONAL` otherwise. |
| `code` | A stable reason: `missing_value`, `wrong_type`, `invalid_uri_scheme`, `invalid_length`, `out_of_range`, `invalid_version`, `plutus_constructor`, `not_utf8`, `invalid_files_entry`, `no_nested_entry`, `too_deep`, `unsupported_key`, `key_collision`, `invalid_base64`, `too_large`, `invalid_constructor`. A CIP-68 text given as a list of byte strings is joined and read as the text, with no error. |
| `error` | A human-readable message. |

Every `code` is described in the OpenAPI documentation of `validation_errors` (Swagger UI), and listed as the allowed values of the `code` field.

`validation_errors` is also on `standards.cip26` and `standards.cip68` (with `show_cips_details=true`), and on `/cip68/ft/...` and `/cip26/...`.
It is the one thing a client has to adapt to: a token that was `404` because its `name` or `description` was missing, or because
its version, `image` or `logo` did not follow CIP-68, is now `200` with errors, so `name` and `description` are no longer
guaranteed to be there. The `metadata.valid` flag the response always had stays. It is `true` when `validation_errors` is empty, that is when the token follows its CIP completely, optional fields included. It used to mean "name and description are both there" and was the gate of the API, so it was `true` for every token returned; now it can be `false`, and `validation_errors` has the detail. Only an obvious attack is still dropped (a datum or property nested so deep that it cannot be decoded, a CIP-26
entry whose subject cannot be a key, a logo far beyond any real size), with a warning in the log.

**Rich fungible tokens (CIP-68 label 444)** are served like fungible tokens, by their own `001bc280…` subject. They have no `logo`: their
`image` is served as `metadata.logo`, as cf-token-metadata-registry does, and `description` is optional for them.

**A `200` does not say that the token exists.** The metadata of a CIP-68 token is in its reference NFT, so a reference NFT also answers for its 333 and
444 subjects (the 333 and 444 subject of an NFT, or of a token that was never minted, answer `200`), and it keeps answering after the token is burned, so that
explorers and wallets can still show historical transactions. To know whether a token exists, use the chain data (ADR 0008, #1292).

**Choosing which tokens are returned** (`?validation=`, on `GET /tokens/subject/{subject}`, `POST /tokens/subject/query`, `/tokens/cip68/ft/...` and
`/tokens/cip26/...`):

| `validation` | Returned when |
|---|---|
| `ALL` (default) | always: every indexed token, as before |
| `MANDATORY_ONLY` | the token has no `MANDATORY` validation error (optional problems are tolerated) |
| `STRICT` | the token has no validation error at all (`metadata.valid` is `true`) |

**What `STRICT` does not promise.** No validation error means no error from the checks the module runs; it is not a guarantee that the metadata
follows every rule of its CIP. Some departures are tolerated on purpose, and reported by no error:

- **A CIP-68 text given as a list of byte strings** (`name`, `description`, `ticker`, `url`). CIP-68 defines these as one byte string (only a
  `uri` may be a list), but minting tools split long texts into 64-byte chunks the way CIP-25 does: on mainnet about 570 descriptions, 43 urls and
  12 names. They are joined and served as the text; reporting them would mark these tokens invalid and hide them under `STRICT`.
- **A URI is checked for its scheme only** (`https`, `ipfs`, `ar`, `data`), not for its full syntax. No mainnet value with an allowed scheme
  breaks the syntax, and a strict parser would risk rejecting real `data:` URIs and IPFS paths.
- **A CIP-26 `logo` is checked for base64 and size, not for being a PNG.** CIP-26 asks for an `image/png`; one mainnet logo is a JPEG ("Volume"),
  which wallets show as well, so flagging it would only hide it under `STRICT`.

A token that is not accepted is a `404` for a single subject and is left out of a batch; the `validation_errors` of the tokens that are returned
are never filtered. A token indexed before validation existed (no `validation_errors`) is judged by the rule it had before: name and description
present (under `?property=`, only those of the two that are asked for).

**`?property=` limits the errors too.** When a request asks for some properties (`?property=ticker`), `validation_errors`, `metadata.valid` and the
`?validation=` gate are about those properties only: an error on the `image` is not reported to a client that asked for the `ticker`. An `image` error
counts for `logo` (a rich fungible token's image is served as its logo), the error that says a nested datum has no entry for the token is always kept,
and errors about what the merged endpoint cannot return (`files`, extra properties, keys) are not reported under a filter. Without `property`, all
errors are returned. The filter runs on the rows the request already reads, so it adds no query.

What is dropped is recorded in the `invalid_token` table (CIP-26 entries, CIP-68 datums and CIP-113 registry nodes that are unusable or look like an
attack: the reason, the token, the transaction and output or the registry file). There is no API for it; read it with SQL, for example
`SELECT standard, reason_code, count(*) FROM invalid_token GROUP BY 1, 2`. See `db/store/README.md` and ADR 0007. The `yaci.store.assets.invalid_tokens` counter
(`standard`, `reason_code`) counts the rows written.

**A dropped update leaves the previous metadata in the API.** The API serves the latest stored row of a token, and a dropped datum or registry
entry writes no row: when an update of a token that is already indexed is dropped (a CIP-68 datum that cannot be decoded, or a CIP-26 file that
becomes unusable), the API keeps serving the metadata of the earlier row, with its errors, and says nothing about the update. The `invalid_token`
row of the update is where it shows (its subject, and the transaction or the file). A CIP-26 logo far beyond any real size is dropped on its own:
the entry is stored without a logo, and the logo is an `invalid_token` row (`logo_too_large`).

Upgrading is **in place and needs no resync to keep working**: the migrations only add. A row written before `validation_errors` existed has
`NULL` (never checked, not the same as "no errors"): the API leaves `validation_errors` out for it, and `metadata.valid` falls back to the rule
it had before, name and description both present. A **full resync** fills the errors in and brings back the tokens that used to be dropped, which
exist nowhere until then; there is no backfill. It also removes the rows an earlier build stored that are no longer stored (the datums
of an NFT, `nft_only`, and the datums that are not token metadata, `not_token_metadata`): until then they stay, and a 333 or 444 subject
answered from such a row is served with its errors.

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

    cip113:
      enabled: true                    # Programmable tokens; the registry is bundled per network
      # registry-nft-policy-ids: <id>,<id>  # Replaces the bundled registries; list every registry to keep serving

    query:
      priority: "CIP_68,CIP_26"       # Default merge priority
```

## Database Tables

| Table | Description |
|-------|-------------|
| `cip26_metadata` | CIP-26 token metadata, logo included (subject as PK), with `validation_errors` |
| `cip26_sync_state` | GitHub sync progress tracking |
| `cip68_metadata` | CIP-68 on-chain reference NFT metadata (composite PK: policy_id, asset_name, slot, tx_hash). `label` is 222, 333 or 444; `validation_errors` says what the datum breaks. |
| `cip113_registry_node` | Every version of every CIP-113 registry node (composite PK: key, slot, tx_hash); the current node of a policy is its latest row. |
| `invalid_token` | What is dropped, with the reason (ADR 0007). |

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
