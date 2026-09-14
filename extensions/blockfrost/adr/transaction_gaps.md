# Transaction Module — Gaps & Open Items

**PR:** #818 | **Status:** ✅ Merged | **Endpoints:** 13 / 13

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /txs/{hash}` | ⚠️ | `size` field undercount; `invalid_before` ambiguity |
| `GET /txs/{hash}/utxos` | ✅ | |
| `GET /txs/{hash}/stakes` | ✅ | |
| `GET /txs/{hash}/delegations` | ✅ | |
| `GET /txs/{hash}/withdrawals` | ✅ | |
| `GET /txs/{hash}/mirs` | ✅ | |
| `GET /txs/{hash}/pool_updates` | ⚠️ | Off-chain metadata fields null |
| `GET /txs/{hash}/pool_retires` | ✅ | |
| `GET /txs/{hash}/metadata` | ✅ | |
| `GET /txs/{hash}/metadata/cbor` | ⚠️ | Returns empty list — raw CBOR not stored at ingest time |
| `GET /txs/{hash}/redeemers` | ✅ | Fee: `ceil(priceMem × unitMem + priceStep × unitSteps)` |
| `GET /txs/{hash}/cbor` | ⚠️ | Body-only CBOR (`a8xx`) — not full envelope |
| `GET /txs/{hash}/dreps` | ✅ | |

## Open Gaps

### Accepted Limitations

- **CBOR body-only format:** `TransactionProcessor` stores body-only CBOR. Full envelope
  (`84xx` — body + witnesses + validity + metadata) requires an ingestion-layer change.
  Shared root cause with [Block module](block_gaps.md).

- **`size` undercount:** `TRANSACTION_CBOR.CBOR_SIZE` stores only the body byte count.
  Blockfrost reports the full serialized transaction size. Systematic undercount sharing
  the same root cause as the CBOR format gap.

- **`invalid_before` ambiguity:** When `validity_interval_start = 0`, the system cannot
  distinguish "field absent" from "explicitly set to slot 0". Returns `null` in both cases,
  which is correct for virtually all transactions.

- **Metadata CBOR empty list:** Raw CBOR metadata bytes are not stored at ingestion — only JSON
  is persisted. Re-encoding from JSON is not attempted.

- **Pool update off-chain metadata null:** `name`, `ticker`, `homepage`, `description` always
  `null` in `GET /txs/{hash}/pool_updates`. Requires a separate off-chain metadata crawler.

- **Transaction submission unsupported:** `POST /tx/submit` is outside the scope of this module.

## Indexes

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_tx_hash_tx_index
    ON transaction (tx_hash, tx_index);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_cbor_tx_hash
    ON transaction_cbor (tx_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tx_input_tx_hash
    ON tx_input (tx_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_delegation_tx_hash
    ON delegation (tx_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stake_registration_tx_hash
    ON stake_registration (tx_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pool_registration_tx_hash
    ON pool_registration (tx_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_drep_registration_tx_hash
    ON drep_registration (tx_hash);
```

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.transaction.enabled=true` | All transaction endpoints |
| `store.transaction.saveCbor=true` | `GET /txs/{hash}/cbor` (otherwise 404) |
| `store.transaction.enabled=true` | Underlying transaction data |

## Release Notes

No functional blockers. Three gaps (CBOR format, `size`, metadata CBOR) share the same
ingestion-layer root cause. All are accepted limitations for the current release.
