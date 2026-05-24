# Network Module — Gaps & Open Items

**PR:** #866 | **Status:** 🔄 Open PR | **Endpoints:** 4 / 4 (76 / 82 field matches)

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /` (root) | ⚠️ | `url`/`version` reflect local service; root hardcodes mainnet URL |
| `GET /genesis` | ✅ | Exact match on all 10 fields; sourced from bundled config |
| `GET /network` | ⚠️ | `supply.locked` = 0 (stub); `stake.live` = 0 (structural gap) |
| `GET /network/eras` | ⚠️ | Conway era end is `null` until next hardfork fires |

## Open Gaps

### Accepted Limitations

- **Root endpoint `url`:** yaci-store serves its own configured base URL — correct and
  intentional for a self-hosted deployment. The tracked issue below covers the specific
  case where the URL was previously hardcoded to a mainnet value instead of reading from
  configuration; once that fix lands, the `url` field will reflect the actual configured
  network endpoint.

- **`supply.locked` = 0:** Computing true locked supply requires scanning all script-locked
  UTxOs — a heavy query with no pre-aggregated table. Marked as permanent limitation unless
  a materialized summary is introduced.

- **`stake.live` = 0:** Live stake requires the Cardano node's in-memory mark snapshot via the
  Local State Query (LSQ) protocol. Not persisted to database tables. Structural gap — cannot
  be resolved without LSQ integration.

- **Conway era end null:** `GET /network/eras` returns `null` for the Conway era's `end`
  boundary because Conway is the current era — no successor era exists yet. This is expected
  behavior: the `end` field will auto-populate once the next hardfork event fires and the
  new era begins. No action required.

- **Circulating supply sync lag:** ~4 ADA difference vs. Blockfrost due to AdaPot
  materialization timing. Accepted.

### Tracked Issues

- **Root endpoint hardcodes mainnet URL:** Raised by a reviewer in PR #866.
  Should read network identifier from configuration. Needs a fix before merge.

## Indexes

No additional indexes required. Supply and stake values are computed from existing AdaPot
tables already indexed by epoch.

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.network.enabled=true` | All network endpoints |
| `store.adapot.enabled=true` | `supply.*` and `stake.active` fields |
| `store.adapot.api-enabled=true` | `NetworkInfoApiService` dependency |

## Release Notes

Two permanent structural gaps (`supply.locked`, `stake.live`) — document clearly.
One actionable tracked item: root endpoint hardcodes mainnet URL regardless of network.
Remaining gaps are accepted limitations.
