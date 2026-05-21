# Address Module — Gaps & Open Items

**PR:** #784 | **Status:** ✅ Merged | **Endpoints:** 6 / 6

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /addresses/{address}` | ✅ | Falls back to `address_utxo` sum when account aggregation disabled |
| `GET /addresses/{address}/extended` | ⚠️ | Mirrors base response; `decimals` and `has_nft_onchain_metadata` missing |
| `GET /addresses/{address}/utxos` | ✅ | |
| `GET /addresses/{address}/utxos/{asset}` | ✅ | |
| `GET /addresses/{address}/transactions` | ✅ | Supports `from`/`to` with `block[:txIndex]` format |
| `GET /addresses/{address}/total` | ✅ | JSONB aggregation by unit |

## Open Gaps

### Accepted Limitations

- **`/extended` mirrors base response:** `decimals` and `has_nft_onchain_metadata` are unavailable
  because the indexer does not ingest CIP-68 metadata. No fix planned unless CIP-68 ingestion
  is added to the store.

- **`address_utxo` fallback:** When `store.account.enabled=false`, balance fields aggregate live
  from `address_utxo`. Values will differ from Blockfrost during rollbacks until re-indexed.

## Indexes

```sql
-- Prevents full table scan on address_utxo for all address endpoints
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_addr
    ON address_utxo (owner_addr);

-- Needed for Byron-length addresses
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_addr_full
    ON address_utxo (owner_addr_full);
```

> `idx_address_utxo_owner_stake_addr` is already included in `index.yml`.

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.address.enabled=true` | All address endpoints |
| `store.account.enabled=true` | Accurate balance in `/addresses/{address}` |
| `store.account.stake-address-balance-enabled=true` | `controlled_amount` field |

## Release Notes

No functional blockers. The `extended` limitation is accepted (requires CIP-68 ingestion).
Module is production-ready with correct index configuration.
