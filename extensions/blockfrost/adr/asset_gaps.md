# Asset Module — Gaps & Open Items

**PR:** #780 | **Status:** ✅ Merged | **Endpoints:** 7 / 7

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /assets` | ⚠️ | Pagination drift from phantom assets; slow without summary table |
| `GET /assets/{asset}` | ⚠️ | `metadata` and `onchain_metadata` fields null |
| `GET /assets/{asset}/history` | ✅ | |
| `GET /assets/{asset}/txs` | ✅ | |
| `GET /assets/{asset}/transactions` | ✅ | |
| `GET /assets/{asset}/addresses` | ✅ | |
| `GET /assets/policy/{policy_id}` | ✅ | |

## Open Gaps

### Accepted Limitations

- **Metadata fields null:** `metadata` and `onchain_metadata` are always `null` — no token
  registry or CIP-68 metadata ingestion is implemented. Requires a separate integration component.

- **Phantom asset pagination drift:** 2 phantom assets in preprod data cause a 1-row shift
  on paginated results. Root cause is a preprod data inconsistency, not a query bug.

- **`asset_name` as hex:** Returned as hex substring of `unit` (chars 57+) to match Blockfrost
  convention. Intentional.

### Tracked Issues

- **`GET /assets` performance:** Response time ~2.5 s warm. Proper fix is a materialized
  `asset_first_mint` summary table to bring this under 10 ms (no issue number yet).

## Indexes

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_amounts
    ON address_utxo USING GIN (amounts);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_assets_unit_slot
    ON assets (unit, slot);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_assets_unit_policy
    ON assets (unit, policy);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_assets_unit_qty
    ON assets (unit, quantity);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_assets_policy
    ON assets (policy);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_assets_mint_unit_slot
    ON assets_mint (unit, slot);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_assets_mint_slot_tx_hash
    ON assets_mint (slot, tx_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_tx_hash_tx_index
    ON transaction (tx_hash, tx_index);
```

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.asset.enabled=true` | All asset endpoints |
| PostgreSQL | JSONB GIN index and lateral expansion required |

## Release Notes

No functional blockers. Performance of `GET /assets` is the main open item.
Metadata fields gap is accepted (requires separate integration work).
