# Address Module — Status & Gaps

**PR:** #784 · **Status:** ✅ Merged · **Endpoints:** 6 / 6
Validated endpoint-by-endpoint against live Blockfrost using `scripts/compare-blockfrost`.

## Status

All six address endpoints are implemented and return Blockfrost-compatible responses. Two
things to be aware of: `/extended` can't fill two CIP-68 fields, and very large ("whale")
accounts hit a deliberate timeout. Both are explained under **Open Gaps** and **Limitations**.

| Endpoint | Status | Notes |
|----------|--------|-------|
| `GET /addresses/{address}` | ✅ | Falls back to summing `address_utxo` when account aggregation is off |
| `GET /addresses/{address}/extended` | ⚠️ | Missing `decimals` and `has_nft_onchain_metadata` (needs CIP-68) |
| `GET /addresses/{address}/utxos` | ✅ | Ordered `(slot, tx_hash, output_index)`; `data_hash` derived for inline datums |
| `GET /addresses/{address}/utxos/{asset}` | ✅ | Same behavior as `/utxos`, filtered by asset |
| `GET /addresses/{address}/transactions` | ✅ | Two-phase paging; supports `from`/`to` as `block[:txIndex]` |
| `GET /addresses/{address}/total` | ✅ | Received/sent totals aggregated by unit |

## Performance (per network)

Benchmarked against official Blockfrost on live databases. On normal addresses Yaci is
consistently faster (the Blockfrost figures are public-endpoint round-trips, shown only for
reference).

| Network | Result | Yaci latency | Blockfrost latency |
|---------|--------|--------------|--------------------|
| mainnet | All endpoints match, 0 diffs | sub-second | ~0.4–1.4 s |
| preprod | All endpoints match, 0 diffs | ~40–125 ms | ~0.37–1.4 s |
| preview | All endpoints match, 0 diffs | ~40–65 ms  | ~0.37–0.78 s |

Highlights:

- **`/transactions` was restructured** into two-phase paging (collect the page of distinct tx
  hashes first, then look up only those rows). On a ~4M-UTXO mainnet whale this cut the query
  planner cost from **157M → 35M** and removed full-table scans over `transaction` and `tx_input`.
- **`/utxos` runs index-only** (owner index → unspent anti-join) and stays under ~100 ms on
  normal addresses.

## Open Gaps

- **CIP-68 fields in `/extended`** — `decimals` and `has_nft_onchain_metadata` can't be returned
  because the indexer doesn't ingest CIP-68 metadata. No fix planned unless CIP-68 ingestion is
  added to the store.
- **Balance fallback when `store.account.enabled=false`** — balances are summed live from
  `address_utxo`, so during a rollback they can differ from Blockfrost until re-indexing catches up.

## Limitations

These are intentional trade-offs, not bugs. None of them lose or duplicate data.

- **`/transactions` ordering at a page boundary** — pages are always complete and non-overlapping.
  The only edge case: when one block has several of the address's transactions *and* it sits right
  on a page boundary, the split between those two pages follows `tx_hash` instead of on-chain
  `tx_index`, so a few transactions can appear slightly out of order across that one boundary.
- **`/utxos` ordering within a block** — Blockfrost's order for UTXOs in the *same* block can't be
  reproduced from the columns we store, so `tx_hash` is used as a stable tie-break. Ordering across
  different blocks always matches Blockfrost.
- **Whale accounts** — for multi-million-UTXO addresses, the heavier endpoints (`/transactions`,
  `/utxos`, `/total`, balances) hit a ~15 s query timeout and return a fast `500` instead of hanging.

**Long-term fix for all three:** store `tx_index` directly on `address_utxo` at write time, indexed
`(owner_addr, block, tx_index, output_index)`. That gives exact on-chain ordering in a single cheap
query and makes whale paging scale per-page.

## Indexes Proposed

The base migration (`V0_200_1__init.sql`) ships only `idx_address_utxo_slot` and
`idx_reference_script_hash` — there is **no owner-based index**. The address endpoints depend on the
owner indexes below, which are added *on top of* the migration (via `index.yml` / the dbutils index
service). Use `CONCURRENTLY` so the live table isn't write-locked, and run each statement on its own.

| Index | Columns | Why it's needed |
|-------|---------|-----------------|
| `idx_address_utxo_owner_addr` | `owner_addr` | avoids a full scan on every address endpoint |
| `idx_address_utxo_owner_addr_full` | `owner_addr_full` | Byron-length addresses (apply manually — not in `index.yml`) |
| `idx_address_utxo_owner_stake_addr` | `owner_stake_addr` | stake-address UTXO endpoints |
| `idx_address_utxo_owner_paykey_hash` | `owner_payment_credential` | payment-credential endpoints |
| `idx_address_utxo_owner_stakekey_hash` | `owner_stake_credential` | stake-credential endpoints |

```sql
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

Notes:

- The `/utxos` and two-phase `/transactions` queries need **no extra `address_utxo` index** — they
  ride existing primary keys (`tx_input_pkey`, `transaction_pkey`) and are seq-scan-free in EXPLAIN.

## Configuration

| Property | Required for |
|----------|--------------|
| `store.extensions.blockfrost.address.enabled=true` | all address endpoints |
| `store.account.enabled=true` | accurate balance in `/addresses/{address}` |
| `store.account.stake-address-balance-enabled=true` | the `controlled_amount` field |

## Release Notes

No functional blockers. The `/extended` CIP-68 gap is accepted. The module is production-ready with
the owner indexes above in place.
