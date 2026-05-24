# Epoch Module — Gaps & Open Items

**PR:** #780 | **Status:** ✅ Merged | **Endpoints:** 10 / 10

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /epochs/latest` | ✅ | |
| `GET /epochs/{number}` | ⚠️ | `total_fees` mismatch (see #781) |
| `GET /epochs/{number}/parameters` | ✅ | |
| `GET /epochs/{number}/next` | ✅ | |
| `GET /epochs/{number}/previous` | ✅ | |
| `GET /epochs/{number}/stakes` | ✅ | Requires `store.adapot.enabled=true`; returns `null` when disabled |
| `GET /epochs/{number}/stakes/{pool_id}` | ✅ | Same adapot dependency |
| `GET /epochs/{number}/blocks` | ⚠️ | Slow without composite index (see below) |
| `GET /epochs/{number}/blocks/{pool_id}` | ⚠️ | Same performance issue |

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

- **Block query performance (#791):** `GET /epochs/{number}/blocks` runs a full scan
  (~19 s on mainnet, 11.6 M rows) without a composite index.

## Indexes

```sql
-- Required for /epochs/{number}/blocks and /blocks/{pool_id} performance
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
