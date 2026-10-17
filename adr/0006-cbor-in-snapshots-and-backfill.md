# ADR 0006: Block and Transaction CBOR in Snapshots, and a CBOR Backfill

## Status

Proposed. Extends [ADR 0003](0003-snapshot-export-import.md) (export and import) and
[ADR 0005](0005-snapshot-distribution.md) (distribution and the pruned tier, D11).

## Date

2026-10-17

## Context

`block_cbor` and `transaction_cbor` hold the raw CBOR of blocks and transactions. They are filled
only when the source runs with `store.blocks.save-cbor`, `store.transaction.save-cbor` (transaction
body) or `store.transaction.save-full-tx-cbor` (the whole signed transaction). Today:

- **No analytics export.** No exporter writes either table to the DuckLake catalog. Their snapshot
  specifications are `NOT_RESTORED`, so a restored node always starts with both tables empty, even
  when the source has them.
- **No backfill.** Turning `save-cbor` on for a node that has already synced records CBOR only for
  blocks it receives from then on. The only way to get the history is a full resync.
- **Optional by nature.** Most indexers never read CBOR. A wallet backend, a re-broadcaster or a
  witness verifier does. Some keep only recent CBOR (`cbor-pruning-enabled`, 43,200 slots by default).

The data is large, so a snapshot that always carried it would penalise every user who does not
want it.

### Size on mainnet

Measured on 2026-10-17 at block 14.08M (epoch 660), from the source database and a uniform sample of
2,503 transactions (0.002% Bernoulli sample of the 120M transactions, CBOR fetched from Koios):

| Quantity | Value | How |
|---|---|---|
| Block bodies, Shelley onwards | 219 GB raw | `sum(body_size)` over 9.59M blocks (Byron body sizes are not recorded) |
| Block headers, Shelley onwards | ~9 GB raw | ~0.9 KB per header (VRF proofs, operational certificate, KES signature); estimate |
| Byron blocks | ~5 GB raw | 4.49M mostly empty blocks plus ~2M transactions; estimate |
| Full transaction CBOR | 1,774 bytes per transaction on average; ~213 GB for 120M | sample |
| Body-only transaction CBOR | 39.7% of the full transaction; ~85 GB | sample, CBOR item lengths |
| Parquet (zstd) compression | 2.53x (snappy 1.31x) | DuckDB `COPY` of the sample |
| PostgreSQL storage | 1.10x raw with lz4, 1.27x with pglz (default), with the primary key and slot index | sample loaded into PostgreSQL 18 |
| Growth, last 30 days | 1.8 GB of block bodies, 2.61M transactions; ~60 MB a day raw | `block` table |

TOAST compresses only values over about 2 KB, and most transactions are smaller, so PostgreSQL
keeps transaction CBOR close to raw size. Most block bodies are larger and do get compressed. The
block figures in the next table assume the transaction sample's ratios hold for blocks.

| Table (full history) | PostgreSQL | Analytics store and snapshot (Parquet zstd) |
|---|---|---|
| `transaction_cbor`, full transaction (`save-full-tx-cbor`) | ~235 to 270 GB | ~84 GB (~78 GiB) |
| `transaction_cbor`, body only (`save-cbor`) | ~95 to 110 GB | ~34 GB (~31 GiB) |
| `block_cbor` | ~190 to 300 GB (estimate) | ~92 GB (~86 GiB) (estimate) |
| Recent CBOR only (43,200 slots, about 12 hours) | ~30 MB | ~12 MB |

For comparison, today's mainnet snapshot is 142.9 GiB. Full-history block and full transaction CBOR
would add about 164 GiB, more than doubling it. Recent CBOR adds almost nothing.

## Decision

### D1 — Export CBOR to the analytics store only when asked

New `TransactionCborExporter` and `BlockCborExporter` in the analytics store write daily partitions
(`slot`-ranged, like the other chain tables) with `tx_hash`/`block_hash`, `cbor_data` (BLOB),
`cbor_size`, `slot` and the partition date. They run only when the source saves the CBOR and
`yaci.store.analytics.export-cbor=true` (default false). An analytics node that never asked for CBOR
therefore does not grow by the figures above.

The exporters export exactly what the table holds. The manifest records the source's
`store.transaction.save-full-tx-cbor` with the other pruning settings, so an importer knows whether
`transaction_cbor` holds full transactions or bodies.

### D2 — Optional at import, in parts of their own

The specifications become `IMPORT` with a new `optional: true`. An optional table is loaded only when
the target asks for it through its own configuration, the same way scoped imports already work:

| Target setting | Effect |
|---|---|
| `store.blocks.save-cbor=true` | `block_cbor` loaded |
| `store.transaction.save-cbor=true` or `save-full-tx-cbor=true` | `transaction_cbor` loaded, if the snapshot holds the same form (full or body); otherwise preflight refuses, since mixing forms in one table breaks the API's contract |
| neither | the tables stay empty, and their parts are not downloaded |

Each optional table's files go in parts of their own: one part group per table (`blocks/block_cbor`,
`transaction/transaction_cbor`), as D11's tiers do. A part therefore holds only `block_cbor` or only
`transaction_cbor`, never both and never another table. A target downloads a CBOR table's parts only
when it enables that table, so `block_cbor` and `transaction_cbor` are pulled independently. A target
that does not want CBOR never downloads it, even though `block_cbor` belongs to the `blocks` module,
which every target enables.

### D3 — Recent CBOR as a pruned tier

With CBOR pruning on, the target keeps 43,200 slots. As in ADR 0005 D11, the export adds a pruned copy
of the newest `cbor-pruning-safe-slots` (retention key `transaction_cbor.safe-slots`, likewise for
blocks), marked `pruned`, and marks the full history `full`. A target with
`store.transaction.cbor-pruning-enabled=true` and a window no wider than the copy's loads the copy:
about 12 MB instead of 84 GB.

### D4 — Backfill with a second event publisher

The idea: a second process syncs the chain again from genesis, or from the first slot missing CBOR,
with its own `store.event-publisher-id` (say 2), and writes only CBOR, while the main process (id 1)
keeps following the tip. This works with today's core:

- **Cursor.** The cursor table is keyed by publisher id (`CursorStorage`), so the two processes keep
  separate cursors in the same schema.
- **Range.** `store.sync-start-slot` / `sync-start-blockhash` and `store.sync-stop-slot` bound the
  backfill. It stops at the slot the main process had reached when CBOR saving was turned on, so the
  two ranges meet without a gap. Everything below that point is immutable, so the backfill never
  rolls back.
- **New: a CBOR-only mode.** `store.cbor-backfill.enabled=true` makes the block and transaction
  processors write only `block_cbor` and `transaction_cbor`, with insert-or-ignore, and turns every
  other store off for that process. Without it, the second process would write `block`,
  `transaction` and the rest a second time.
- **Analytics.** The CBOR exporters' daily partitions must not be marked complete before the CBOR is
  there. Partitions below the backfill's start are exported only once publisher 2's cursor has passed
  them, and the state service's gap detection exports them as they complete.
- **Retention.** A target with CBOR pruning has no reason to backfill. The backfill refuses to run
  when `cbor-pruning-enabled` is on, or starts at `tip - cbor-pruning-safe-slots`.

The backfill reads blocks over node-to-node like any sync. On mainnet that is the same time as a
full sync without the ledger state, so it is an option, not something a restore depends on. A
restored node that wants full CBOR history gets it from a snapshot that carries it (D1, D2).

## Alternatives considered

- **Always export CBOR, always import it.** Rejected: it would double the snapshot, and the analytics
  store, for users who never read CBOR.
- **A separate CBOR snapshot.** Rejected for the same reasons as a separate pruned snapshot (ADR 0005,
  D11): two artifacts to publish, sign, list and choose between. Part groups give the same download
  saving inside one artifact.
- **Rebuild CBOR from the stored rows.** Not possible: the stored rows are decoded and normalised.
  Re-encoding them does not give back the original bytes, and the original bytes are the point.
- **Backfill by fetching blocks by hash** (block-fetch over node-to-node for a list of points) instead
  of a second sync. It may be faster with many parallel connections. It is a possible later
  optimisation of D4 that keeps the same CBOR-only writer.

## Consequences

**Positive**

- A node that wants CBOR can restore it instead of resyncing, and one that does not want it pays
  nothing: no download, no disk.
- Recent-only CBOR users restore it at almost no cost.
- An existing node can get its CBOR history without a resync from scratch.

**Negative**

- Analytics nodes that export CBOR need about 86 to 164 GiB more for full history (body-only
  transactions and blocks, up to full transactions and blocks).
- A snapshot with full-history CBOR is about twice as large to produce, store and publish, even
  though most importers skip those parts.
- The CBOR-only mode is a new processing path in the block and transaction stores, with its own tests.

## Implementation plan

Each step is its own commit on `feature/snapshot-cbor`, stacked on ADR 0005's branch.

1. Exporters and the `export-cbor` switch, with a DuckDB round trip of `bytea` to BLOB to `bytea` in
   the analytics store's tests.
2. `optional` specifications, part groups, the target check (including full versus body), and import
   and validation tests.
3. The pruned tier for CBOR (D3).
4. The CBOR-only backfill mode (D4). Qualify it on preprod: back-fill a range, then compare the CBOR
   with a node that saved it from the start.
5. Mainnet measurement: a preprod or mainnet export with CBOR, replacing the estimates above with
   real sizes.

## Open questions

- **Which form for official snapshots.** Full transaction CBOR is about 2.5 times the body-only form
  in size. One form per snapshot keeps the import check simple.
- **`block_cbor` at all.** Blocks can be reconstructed from the node. Transaction CBOR is the one most
  APIs serve, so `block_cbor` may be worth exporting only on request.
