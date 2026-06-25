# Epoch Module — Gaps & Open Items

**PR:** #780 | **Status:** ✅ Merged | **Endpoints:** 10 / 10

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /epochs/latest` | ✅ | |
| `GET /epochs/{number}` | ⚠️ | `total_fees` mismatch (see #781) |
| `GET /epochs/{number}/parameters` | ✅ | |
| `GET /epochs/{number}/next` | ✅ | `active_stake` query optimized — see below |
| `GET /epochs/{number}/previous` | ✅ | `active_stake` query optimized — see below |
| `GET /epochs/{number}/stakes` | ✅ | Requires `store.adapot.enabled=true`; returns `null` when disabled |
| `GET /epochs/{number}/stakes/{pool_id}` | ✅ | Same adapot dependency |
| `GET /epochs/{number}/blocks` | ✅ | Fast with composite index (see below) |
| `GET /epochs/{number}/blocks/{pool_id}` | ✅ | Same composite index |

## Open Gaps

### Configuration Notes

- **`active_stake` dependency on AdaPot job:** `active_stake` fields in `/stakes` and
  `/stakes/{pool_id}` return `null` when `store.adapot.enabled=false`. This is not a gap —
  it is expected behavior when the AdaPot aggregation job is not running. Enable
  `store.adapot.enabled=true` to populate these fields.

### Tracked Issues

- **`total_fees` mismatch (#781):** `GET /epochs/{number}` returns incorrect `total_fees`.
  Correct approach: use `adapot` table for past epochs; for current epoch, `SUM` transaction fees
  from the `block` table. Caching recommended for performance.

- **`/stakes` and `/stakes/{pool_id}` ordering mismatch:**
  `GET /epochs/{number}/stakes` and `GET /epochs/{number}/stakes/{pool_id}` in Blockfrost
  return results ordered according to cardano-db-sync's internal row ordering. Yaci Store
  does not replicate this ordering, so paginated responses may return rows in a different
  sequence than Blockfrost.

- **`first_block_time` / `last_block_time` not the true first/last block times
  ([#973](https://github.com/bloxbean/yaci-store/issues/973)):**
  `epoch.start_time` / `epoch.end_time` (which back these fields) are computed from an
  **unordered** block query in `EpochService`, so they come from arbitrary blocks rather than
  the chronologically first/last block — off by seconds to hours vs Blockfrost. Code currently
  keeps reading directly from the `epoch` columns;
  
- **Block query performance (#791) — ✅ resolved by `idx_block_epoch_slot`:**
  `GET /epochs/{number}/blocks` was slow specifically for `order=asc` on epochs far from
  the chain tip. Without a composite index, the JDBC prepared-statement plan walks
  `idx_block_slot` ascending and filters by epoch, traversing millions of older blocks
  before reaching the target epoch. Adding `block (epoch, slot)` gives the planner a direct
  access path. Measured on preprod: `order=asc` dropped from **~3.2 s → ~70 ms**; `order=desc`
  was always fast (the descending scan finds recent epochs immediately).

  > Note: a manual `EXPLAIN` with a literal `epoch = N` (or `force_generic_plan`) picks
  > `idx_block_epoch` and runs in ~16 ms, masking the problem. The regression only appears
  > through the application's actual prepared-statement plan — verify via the live endpoint,
  > not just `psql`.

## Performance

### `active_stake` query — partition-key pruning

`getActiveStakesByEpochs` (backing `active_stake` in `/epochs/{number}`, `/next`, `/previous`)
originally filtered `epoch_stake` on `active_epoch`. Because `epoch_stake` is **partitioned by
`epoch`**, that filter scanned every partition. Since `active_epoch = epoch + 2` is an invariant
(set by `StakeSnapshotService`), the query now filters on the partition key `epoch` (= `active_epoch − 2`)
and remaps the grouped result back to `active_epoch`. This enables partition pruning and reuse of the
PK `(epoch, address)` — no schema change. Measured on preprod: `/next` **~1016 ms → ~132 ms**,
`/previous` **~1066 ms → ~529 ms**.

## Indexes

```sql
-- Required for /epochs/{number}/blocks and /blocks/{pool_id} performance.
-- Verified: order=asc on far-from-tip epochs ~3.2 s -> ~70 ms (preprod).
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_block_epoch_slot
    ON block (epoch, slot);
```

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.epoch.enabled=true` | All epoch endpoints |
| `store.adapot.enabled=true` | `/stakes`, `/stakes/{pool_id}` — otherwise returns null |

## Release Notes

`total_fees` discrepancy (#781) is the only functional gap preventing full parity.
All other gaps are either accepted limitations or performance-only.
