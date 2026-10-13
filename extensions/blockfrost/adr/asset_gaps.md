# Blockfrost Asset Module

**Endpoints:** 7 / 7 implemented
**Verified:** 2026-06-10 against live Blockfrost on preprod, preview and mainnet, including extreme units
(a 994k-event mainnet token, a 122k-event preview token, a 292k-asset policy, a 3,939-holder token).

## Endpoint status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /assets` | ✅ | Order matches Blockfrost. ~6 s at mainnet scale (15M mint rows) — summary table planned. |
| `GET /assets/{asset}` | ⚠️ | Correct except `metadata` / `onchain_metadata` / `onchain_metadata_standard` are `null` (see Known differences). Fast: 147–282 ms on testnets, ~3.7 s on a 994k-event mainnet unit. |
| `GET /assets/{asset}/history` | ✅ | Order and burn sign match the live API. |
| `GET /assets/{asset}/txs` | ✅ | Matches. Deprecated upstream in the Blockfrost OpenAPI. |
| `GET /assets/{asset}/transactions` | ✅ | Matches. |
| `GET /assets/{asset}/addresses` | ✅ | Order matches (including the asymmetric `desc`). Slow on huge units — see Open items. |
| `GET /assets/policy/{policy_id}` | ✅ | Matches; sub-second warm on every network, including a 292k-asset mainnet policy. |

## How ordering works

Blockfrost orders rows within a block by `tx_index`, so every query joins `transaction` where needed:

- **`/history`** — events ordered by `(slot, tx_index)`.
- **`/txs` & `/transactions`** — transactions ordered by `(slot, tx_index)`.
- **`/addresses`** — asc keys each holder on its **earliest** unspent UTXO `(slot, tx_index, output_index)`,
  desc on its **latest**. Because the two directions look at different UTXOs, desc is *not* asc reversed.
  Address is the final tie-break.
- **`/policy` & `/assets`** — assets ordered by their first mint `(slot, tx_index, unit)`.

`tx_index` is unique within a block, so these keys are fully deterministic. The only `tx_index = NULL` rows
in the data are genesis distribution entries, which cannot carry native assets and never enter these queries.

On Postgres, the paginated queries use a **slot-window** plan: pick the page's slot range cheaply first, then
join `transaction` and aggregate only for the rows in that range. This keeps deep pages as fast as page 1.
The `/policy` query additionally exploits that `unit` is policy-prefixed, so its first-mint pass runs as a
unit-range scan on the partial mint index.

## Known differences vs Blockfrost (accepted)

- **`metadata`, `onchain_metadata`, `onchain_metadata_standard` are `null`** — token-registry / CIP-25 / CIP-68
  ingestion is not implemented. This is the single remaining `--strict` diff on every network.

## Required indexes

```sql
-- Unit containment for /addresses, /txs, /transactions
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_amounts
    ON address_utxo USING GIN (amounts);

-- First-mint scans for /assets and /policy
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_assets_mint_unit_slot
    ON assets (unit, slot) WHERE mint_type = 'MINT';

-- Makes all per-unit assets reads index-only (/assets/{asset}, /history, quantity sums)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_assets_unit_covering
    ON assets (unit, slot)
    INCLUDE (tx_hash, mint_type, quantity, policy, asset_name, fingerprint);
```

Joins rely on the table primary keys (`transaction_pkey`, `tx_input_pkey`, `address_utxo_pkey`).
Sizes on mainnet: GIN 32 GB, partial mint 1.8 GB, covering 6.5 GB.

After the covering index, `idx_assets_unit_slot`, `idx_assets_unit_policy` and `idx_assets_policy` are
redundant for this module and can be dropped (~4.4 GB on mainnet) if nothing else uses them.

## Performance (measured 2026-06-10, heaviest unit per network)

| Endpoint | preview (122k ev.) | preprod (100k ev.) | mainnet (994k ev.) | Blockfrost (same units) |
|----------|--------------------|--------------------|--------------------|--------------------------|
| `/assets/{asset}` | 282 ms | 147 ms | 3.7 s | 0.9–2.5 s |
| `/history` | ~400 ms | ~175 ms | ~4 s | 0.7–3.5 s |
| `/txs` | ~1.4 s | ~200 ms | ~6.3 s | 0.3–0.7 s |
| `/transactions` | ~1.45 s | ~200 ms | ~6.3 s | 0.5–1.6 s |
| `/addresses` | 2.1–2.5 s | ~0.5 s | 8.8–10.8 s | 0.4–2.5 s |
| `/policy` | 61–99 ms | 55–112 ms | 0.8–1.05 s | 0.44–3.7 s |

A **normal** asset (a 2-event mainnet token) is 170–760 ms on every endpoint and beats Blockfrost on most.
The multi-second numbers above are worst-case units only.

## Open items

- **Summary tables for mainnet scale.** The remaining floors are all "scan everything about a huge unit once"
  costs that indexes cannot remove:
  - `/assets` list (~6 s): `DISTINCT ON` over 15M mint rows → needs a per-unit / first-mint summary table.
  - `/txs`, `/transactions` (~6 s) and `/addresses` (~9–11 s): GIN scan + per-row work over ~1M UTXOs → needs a
    holder / per-unit-tx summary table.
  - `/history` (~4 s): the slot-window CTE still materializes all events of the unit once per call.
- **Asset metadata ingestion** (token registry, CIP-25, CIP-68) — separate component; removes the last
  `--strict` diff.

## Configuration

| Property | Required for |
|----------|--------------|
| `store.extensions.blockfrost.enabled=true` | **All** blockfrost endpoints — master gate; sub-modules default to on when it is set. Without it no blockfrost route is registered. |
| `store.extensions.blockfrost.asset.enabled=true` | Asset endpoints (default on when the master gate is set). |
| PostgreSQL | The slot-window query plans, JSONB GIN containment, and `DISTINCT ON`. Other dialects use slower fallback queries (tests only). |
