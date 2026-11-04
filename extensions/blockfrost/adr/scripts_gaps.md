# Scripts Module — Gaps & Open Items

**PR:** #852 | **Status:** 🔄 Open PR | **Endpoints:** 7 / 7 (153 matched across 3 datasets)

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /scripts` | ⚠️ | First-page partial match — genesis-era script sync gap |
| `GET /scripts/{script_hash}` | ✅ | |
| `GET /scripts/{script_hash}/json` | ✅ | |
| `GET /scripts/{script_hash}/cbor` | ✅ | |
| `GET /scripts/{script_hash}/redeemers` | ✅ | Fee: `ceil(unitMem × priceMem + unitSteps × priceStep)` |
| `GET /scripts/datum/{datum_hash}` | ✅ | |
| `GET /scripts/datum/{datum_hash}/cbor` | ✅ | |

## Open Gaps

### Accepted Limitations

- **Genesis-era script sync gap:** `GET /scripts` first-page results diverge because the local
  node has indexed 136,214 of 137,732 scripts — the 1,518 missing scripts have `NULL` slot values
  (genesis-era scripts). Blockfrost has indexed scripts from further back in preprod history.
  The gap will close as the local node syncs. Not a code bug.

  **Workaround applied:** `ORDER BY slot NULLS FIRST` for ascending sorts so null-slot (genesis)
  scripts sort correctly once they are indexed.

## Indexes

```sql
-- Partial index on transaction_scripts to speed up redeemer purpose lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_scripts_purpose_not_null
    ON transaction_scripts (script_hash, slot)
    WHERE purpose IS NOT NULL;
```

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.scripts.enabled=true` | All script endpoints |
| `store.script.enabled=true` | Script data |
| `store.transaction.enabled=true` | Redeemer data |

## Release Notes

No functional blockers. The only gap is a sync-lag on genesis-era scripts which will
self-resolve as the node syncs. Module is ready for merge.
