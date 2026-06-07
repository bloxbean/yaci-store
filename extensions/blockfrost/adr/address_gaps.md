# Address Module — Gaps & Open Items

**PR:** #784 | **Status:** ✅ Merged | **Endpoints:** 6 / 6

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /addresses/{address}` | ✅ | Falls back to `address_utxo` sum when account aggregation disabled. Whale: bounded 500 (see below) |
| `GET /addresses/{address}/extended` | ⚠️ | `decimals` and `has_nft_onchain_metadata` missing (CIP-68). Whale: bounded 500 |
| `GET /addresses/{address}/utxos` | ✅ | Includes `tx_index`; `data_hash` derived for inline datums; ordered `(slot, tx_index, output_index)`. Whale: bounded 500 |
| `GET /addresses/{address}/utxos/{asset}` | ✅ | Same as `/utxos` |
| `GET /addresses/{address}/transactions` | ✅ | Two-phase paging; `from`/`to` as `block[:txIndex]`. Whale: bounded 500 |
| `GET /addresses/{address}/total` | ✅ | JSONB aggregation by unit. Whale: bounded 500 |

## Performance

Benchmarked vs official Blockfrost via `scripts/compare-blockfrost` (drift-isolated against
live DBs). Yaci is consistently faster than Blockfrost on normal addresses (Blockfrost numbers
are public-endpoint round-trips, shown for reference).

| Network | `/transactions` (normal) | `/utxos` order vs BF | Yaci latency | Blockfrost latency |
|---------|--------------------------|----------------------|--------------|--------------------|
| mainnet | OK, 0 diffs | match (3 same-slot trigger addrs) | sub-second | ~0.4–1.4 s |
| preprod | OK, 0 diffs | match (2 trigger addrs) | ~40–125 ms | ~0.37–1.4 s |
| preview | OK, 0 diffs | match | ~40–65 ms | ~0.37–0.78 s |

- **`/transactions` restructure:** two-phase paging (aggregate distinct tx hashes without the
  `transaction` join, then join only the ≤ page rows) cut the planner cost **157M → 35M** on a
  ~4M-UTXO mainnet whale and removed full-table seq scans over `transaction` (~51M) and
  `tx_input` (~336M).
- **`/utxos` ordering:** `(slot, tx_index, output_index)` via an index-only plan
  (`idx_address_utxo_owner_addr` → `tx_input_pkey` anti-join → `transaction_pkey` join); no seq
  scans; sub-100 ms on normal addresses.
- **Whale ceiling:** for multi-million-UTXO addresses `/transactions`, `/utxos`, `/total` and the
  balance endpoints exceed the query timeout and return a **bounded 500** (~15 s). The
  full-history aggregation (`ORDER BY max(block)`) cannot be paged cheaply on the read path; the
  timeout trades a connection-pool-exhausting hang for a fast per-request failure.
- **Path to O(page):** a writer-maintained per-`(address, tx)` row carrying `tx_index`, indexed
  `(address, block, tx_index)`, removes the aggregation and makes whale paging O(page). Writer-side,
  out of read-path scope.

## Open Gaps

### Accepted Limitations

- **`/extended` mirrors base response:** `decimals` and `has_nft_onchain_metadata` are unavailable
  because the indexer does not ingest CIP-68 metadata. No fix planned unless CIP-68 ingestion
  is added to the store.

- **`address_utxo` fallback:** When `store.account.enabled=false`, balance fields aggregate live
  from `address_utxo`. Values will differ from Blockfrost during rollbacks until re-indexed.

## Indexes

The base migration (`V0_200_1__init.sql`) creates only `idx_address_utxo_slot` and
`idx_reference_script_hash` on `address_utxo` — it has **no owner-based index**. The indexes the
address endpoints rely on were **added on top of the migration** (via `index.yml`, applied by the
dbutils index service, and some manually during testing); they are not part of the schema migration.

**Required for the address endpoints:**

| Index | Columns | Purpose | Source |
|-------|---------|---------|--------|
| `idx_address_utxo_owner_addr` | `owner_addr` | avoids a full table scan on every address endpoint | `index.yml` |
| `idx_address_utxo_owner_addr_full` | `owner_addr_full` | Byron-length addresses | **manual** — not in `index.yml` (present on preprod, absent on mainnet) |
| `idx_address_utxo_owner_stake_addr` | `owner_stake_addr` | stake-address utxo endpoints | `index.yml` |
| `idx_address_utxo_owner_paykey_hash` / `idx_address_utxo_owner_stakekey_hash` | credential cols | payment/stake-credential endpoints | `index.yml` |

```sql
-- Added on top of the base migration. CONCURRENTLY avoids a write lock on the live table;
-- run each statement on its own (CONCURRENTLY cannot run inside a transaction block).
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_addr
    ON address_utxo (owner_addr);

-- Byron-length addresses (not in index.yml; apply manually where needed).
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_addr_full
    ON address_utxo (owner_addr_full);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_stake_addr
    ON address_utxo (owner_stake_addr);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_paykey_hash
    ON address_utxo (owner_payment_credential);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_stakekey_hash
    ON address_utxo (owner_stake_credential);
```

The new `/utxos` and two-phase `/transactions` queries need **no additional `address_utxo`
index** — they ride existing primary keys: `transaction_pkey (tx_hash)` for the `tx_index` join
and `tx_input_pkey (output_index, tx_hash)` for the unspent anti-join (EXPLAIN is seq-scan-free).

**Tested but not adopted:** `idx_address_utxo_owner_slot (owner_addr, slot)` was added during perf
testing but no query exploits the `slot` column (EXPLAIN still prefers `owner_addr` + sort), so it
gives no benefit — do not add it.

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.address.enabled=true` | All address endpoints |
| `store.account.enabled=true` | Accurate balance in `/addresses/{address}` |
| `store.account.stake-address-balance-enabled=true` | `controlled_amount` field |

## Release Notes

No functional blockers. The `extended` limitation is accepted (requires CIP-68 ingestion).
Module is production-ready with correct index configuration.
