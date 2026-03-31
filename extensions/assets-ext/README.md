# Assets Extension

Indexes and serves Cardano fungible token metadata from multiple CIP standards. Replaces the need for a standalone
[cf-token-metadata-registry](https://github.com/cardano-foundation/cf-token-metadata-registry) deployment — consumers
query a single yaci-store instance and get merged metadata transparently.

## Supported Standards

| Standard | Source | Description |
|----------|--------|-------------|
| CIP-26 | Off-chain (GitHub [cardano-token-registry](https://github.com/cardano-foundation/cardano-token-registry)) | Fungible token metadata synced periodically from Git |
| CIP-68 | On-chain (reference NFT inline datums, label 333) | Fungible token metadata parsed from blockchain |
| CIP-113 | On-chain (programmable token registry NFTs) | Transfer logic scripts (disabled by default) |

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

- `property` (optional, repeatable) — filter which properties to return. When used, `name` and `description` are required.
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
    }
  },
  "queryPriority": ["CIP_68", "CIP_26"]
}
```

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
| `GET` | `/cip68/ft/{policyId}/{assetName}` | CIP-68 fungible token metadata only |


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
      enabled: true
      fungible-enabled: true           # Index fungible tokens (label 333)
      nft-enabled: false               # NFT support (label 222) — not yet implemented

    cip113:
      enabled: false                   # Disabled by default
      registry-nft-policy-ids: []      # Override auto-detected policy IDs

    query:
      priority: "CIP_68,CIP_26"       # Default merge priority
```

## Database Tables

| Table | Description |
|-------|-------------|
| `ft_offchain_metadata` | CIP-26 fungible token metadata (subject as PK) |
| `ft_offchain_logo` | CIP-26 token logos (separated for performance) |
| `off_chain_sync_state` | GitHub sync progress tracking |
| `metadata_reference_nft` | CIP-68 on-chain reference NFT metadata (composite PK: policy_id, asset_name, slot). `label` column discriminates FT (333) vs NFT (222) for future use. |
| `cip113_registry_node` | CIP-113 programmable token registry nodes |

All tables include a `last_synced_at` timestamp for monitoring.

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
