# Block Module — Gaps & Open Items

**PR:** #811 | **Status:** ✅ Merged | **Endpoints:** 11 / 11

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /blocks/latest` | ✅ | |
| `GET /blocks/latest/transactions` | ✅ | |
| `GET /blocks/{hash_or_number}` | ✅ | Accepts 64-char hash or numeric block number |
| `GET /blocks/{hash_or_number}/next` | ✅ | |
| `GET /blocks/{hash_or_number}/previous` | ✅ | Result list reversed to match BF ascending order |
| `GET /blocks/{hash_or_number}/transactions` | ✅ | |
| `GET /blocks/{hash_or_number}/addresses` | ⚠️ | JOIN may miss records if `address_utxo` is pruned |
| `GET /blocks/{hash_or_number}/txs/cbor` | ⚠️ | Body-only CBOR (`a8xx`) — not full envelope (`84xx`) |
| `GET /blocks/slot/{slot_number}` | ✅ | |
| `GET /blocks/epoch/{epoch_number}/slot/{slot_number}` | ✅ | Uses `epoch_slot`, not absolute slot |
| `GET /blocks/latest/transactions/cbor` | ⚠️ | Body-only CBOR — same root cause |

## Open Gaps

### Accepted Limitations

- **CBOR body-only format:** CBOR endpoints return body-only CBOR (`a8xx` prefix) instead of the
  full transaction envelope (`84xx` — body + witnesses + validity + metadata).
  Root cause: `TransactionProcessor` stores only the body. Shared root cause with Transaction module.
  Document as known difference.

- **`output`/`fees` null on empty blocks:** Fields return `null` (not `"0"`) when a block contains
  no transactions. This matches the Blockfrost schema where these fields are optional.

- **Address transactions pruning risk:** `findBlockAddressTransactions()` JOINs `address_utxo`.
  If UTxO records are pruned, historical address transaction counts will be incomplete.
  Documented in OpenAPI spec. Acceptable for deployments without UTxO pruning.

- **`store.transaction.saveCbor=true` requirement:** CBOR endpoints return HTTP 404 if CBOR
  storage is disabled.

## Indexes

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_block_tx_index_tx_hash
    ON transaction (block_hash, tx_index, tx_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_block
    ON address_utxo (block_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tx_input_spent_at_block_spent_tx_hash
    ON tx_input (spent_at_block, spent_tx_hash);
```

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.block.enabled=true` | All block endpoints |
| `store.transaction.saveCbor=true` | CBOR endpoints (otherwise 404) |

## Release Notes

No functional blockers. CBOR format limitation is a shared concern with the Transaction module
and requires an ingestion-layer change. All other gaps are accepted or have documented workarounds.
