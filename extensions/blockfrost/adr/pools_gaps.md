# Pools Module — Gaps & Open Items

**PR:** #847 | **Status:** 🔄 Open PR | **Endpoints:** 12 / 12 (36 / 60 full field matches)

## Endpoint Status

| Endpoint | Match | Category | Notes |
|----------|-------|----------|-------|
| `GET /pools` | ✅ | — | |
| `GET /pools/extended` | ⚠️ | Architecture | Stake semantics differ; off-chain metadata fields null |
| `GET /pools/retired` | ⚠️ | Ordering | Same-epoch tie-breaking drift |
| `GET /pools/retiring` | ✅ | — | |
| `GET /pools/{pool_id}` | ⚠️ | Architecture | `calidus_key` not populated; live/active size semantics differ |
| `GET /pools/{pool_id}/history` | ⚠️ | Formatting | Tiny floating-point serialization tail drift |
| `GET /pools/{pool_id}/metadata` | ⚠️ | Architecture | Off-chain metadata (ticker, name, description, homepage) unsupported |
| `GET /pools/{pool_id}/relays` | ⚠️ | Upstream | IPv6 byte-order decode bug in `yaci 0.4.0` |
| `GET /pools/{pool_id}/delegators` | ✅ | — | |
| `GET /pools/{pool_id}/blocks` | ✅ | — | |
| `GET /pools/{pool_id}/updates` | ✅ | — | |
| `GET /pools/{pool_id}/votes` | ⚠️ | Upstream | Blockfrost preprod returns `404` for all pools |

## Open Gaps

### Category A — Architecture / yaci-store Scope

- **Off-chain metadata unavailable:** `GET /pools/{pool_id}/metadata` and the `name`, `ticker`,
  `description`, `homepage` fields in `GET /pools/extended` and `GET /pools/{pool_id}` are always
  `null`. Only on-chain URL and hash are indexed. Requires a separate off-chain metadata crawler.

- **Stake semantics:** `live_stake` and `active_stake` use epoch-snapshot values from AdaPot
  tables rather than Blockfrost's UTxO-derived methodology. Small value differences expected.

- **`calidus_key` not populated:** Always `null` in `GET /pools/{pool_id}`.
  Requires dedicated indexing support for the Calidus key certificate.

- **Live/active size semantics:** `live_size` and `active_size` are fractions computed from
  snapshots, not live UTxO data.

### Category B — Upstream / Source-of-Truth Gaps

- **`/votes` returns 404 on preprod:** Blockfrost preprod returns `404` for all pool vote queries.
  Our implementation returns `[]`. Re-validate once Blockfrost indexes governance pool votes.

- **IPv6 relay decode bug:** IPv6 addresses decoded incorrectly due to a byte-order bug in
  `yaci 0.4.0`. Will auto-fix on library upgrade.

### Category C — Formatting / Non-Functional

- **Retired pool tie-breaking:** `GET /pools/retired` has ordering drift for same-epoch retirements.
  Blockfrost uses an undocumented secondary sort key.

- **History floating-point drift:** `GET /pools/{pool_id}/history` shows a tiny tail difference
  in floating-point serialization. Values are functionally identical.

## Indexes

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pool_registration_pool_id_slot
    ON pool_registration (pool_id, slot);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pool_retirement_pool_id_slot
    ON pool_retirement (pool_id, slot);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pool_status_pool_id
    ON pool_status (pool_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_epoch_stake_pool_epoch
    ON epoch_stake (pool_id, epoch);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_block_slot_leader_epoch
    ON block (slot_leader, epoch);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_gov_action_pool_id
    ON voting_procedure (pool_hash);
```

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.pools.enabled=true` | All pool endpoints |
| `store.adapot.enabled=true` | Stake fields |
| `store.account.stake-address-balance-enabled=true` | `live_stake` / delegator amounts |
| `store.governance.enabled=true` | `/votes` endpoint |

## Release Notes

All known mismatches are: accepted architecture limitations, an upstream yaci bug (auto-resolves
on library upgrade), or a Blockfrost preprod data gap. No blockers prevent merging.
Off-chain metadata is the most visible user-facing gap — document prominently in release notes.
