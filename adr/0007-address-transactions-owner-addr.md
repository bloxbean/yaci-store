# ADR 0007: Address Transactions from tx_input.owner_addr

## Status

Proposed. Builds on the `store_state` table added with `address_utxo_unspent` (PR #1271).

## Date

2026-10-26

## Context

`GET /api/v1/addresses/{address}/transactions` hangs for busy addresses (issue #1269).
`UtxoStorageReaderImpl.findTransactionsByAddress` takes the `UNION` of the address's outputs
(`address_utxo` by `owner_addr`) and the spends of those outputs (`tx_input` joined back to
`address_utxo`), then `SELECT DISTINCT ... ORDER BY slot ... LIMIT`. Nothing stops early: every
page reads, sorts and de-duplicates the address's whole history.

Measured on a mainnet store (block ~14.11M, 363M outputs, 352M spends) for the busy address from
#1269 (1.95M outputs, 1.81M transactions), first page of 20, newest first:

| `address_utxo` index | Time | What it reads |
|---|---|---|
| none | 25 min 52 s | two full scans of `address_utxo` (253 GB), ~543 GB read |
| `owner_addr` | 12 min 17 s to 16 min 33 s | 2 x 1.48M scattered `address_utxo` pages, 1.95M `tx_input` lookups |

Both sort 3.9M rows on disk (400 MB) to return 20. An index on `owner_addr` cannot fix it.

The spends are the hard half. `tx_input` holds only the key of the output it spends, not its
address, so "the latest spends from address A" cannot be read from an index: any of A's outputs,
however old, may have been spent a moment ago.

## Decision

Add `tx_input.owner_addr varchar(500)`: the `owner_addr` of the output the row spends, the same
value and type as `address_utxo.owner_addr`. With two indexes

```sql
create index idx_tx_input_owner_addr_slot    on tx_input     (owner_addr, spent_at_slot) where owner_addr is not null;
create index idx_address_utxo_addr_hash_slot on address_utxo (hashtextextended(owner_addr, 0), slot);
```

both halves come out of an index in slot order. They are optional indexes like the other read
indexes: listed in dbutils `index.yml` (PostgreSQL only) and applied after the sync with
`admin-cli apply-indexes`. The `tx_input` index leaves out the rows without the address, so a
store with the flag off that applies `index.yml` gets an almost empty index there. The `address_utxo` index is on a 64-bit hash of the
address, computed by PostgreSQL on both sides, so it is a third of the size of an index on the
address; `owner_addr = :address` rules out another address with the same hash. Each half takes its
own first `offset + count` transactions along its index and stops; the halves are then merged and
the page cut out:

```sql
select distinct tx_hash, block, block_time, slot from (
    (select distinct tx_hash, block, block_time, slot from address_utxo
     where hashtextextended(owner_addr, 0) = hashtextextended(:address, 0) and owner_addr = :address
     order by slot desc, tx_hash desc limit :offset + :count)
  union all
    (select distinct spent_tx_hash, spent_at_block, spent_block_time, spent_at_slot from tx_input
     where owner_addr = :address
     order by spent_at_slot desc, spent_tx_hash desc limit :offset + :count)
) t order by slot desc, tx_hash desc limit :count offset :offset
```

Every transaction before the page is before it in its own half too, so the page is within the rows
the halves return. With the limit only on the outer query, PostgreSQL appends, sorts and
de-duplicates the whole history first: a review run on PostgreSQL 18 with 600k rows for one address
read 242k buffers in 270-680 ms that way, and 191 buffers in 3.7 ms with the per-half limits. A deep
page costs about `offset + count` index entries per half.

### Writing the address: from the first block, with a cache

Behind `store.utxo.tx-input-owner-addr-enabled` (default `false`), PostgreSQL 16+ only.

A spend needs the address of the output it spends. `UtxoStorageImpl` keeps the address of every
unspent output in a cache: `saveUnspent` puts `(tx_hash, output_index) -> owner_addr`, and
`saveSpent` takes it out and writes it with the `tx_input` row. On a miss it reads `address_utxo`
by primary key, in one query for the block's misses. The cache only saves those reads; it never
decides a result, since an output's address never changes.

- **The cache** is a column family (`utxo_owner_addr`) of the process's RocksDB cache,
  `RocksDBCacheStore` in `components/common`: one RocksDB per Yaci Store process, in which any
  store can make its own column family. Its folder (`store.rocksdb-cache-path`) is deleted at every
  start, and writes skip RocksDB's write-ahead log: after a restart every spend of an older output
  is a miss, read from the database, until those outputs are spent. Keys are 34 bytes (tx hash and
  index), values the address. Mainnet's ~11M unspent outputs take about 1-1.5 GB on disk.
- **A spend saved before its output.** A block's spends are saved before its outputs, and in a
  parallel batch a block can spend an output another block of the batch has not saved yet. Such a
  spend is held in memory and inserted at the batch's commit, when every output of the batch is
  saved. The row is written once, with its address: no update.
- **Why not a hash in `tx_input`.** A first version stored `hashtextextended(owner_addr, 0)` in
  `tx_input`. Java cannot compute that hash without reimplementing PostgreSQL's, so a cache of
  addresses would have had to hash in SQL anyway, and the full address needs no recheck join.

### State

`TxInputOwnerAddr` keeps a `store_state` row, `utxo.tx_input_owner_addr`, whose slot says up to
which slot every `tx_input` row has the address:

1. **A new store** (no `tx_input` row when it first saves; its cursor may already be at the genesis
   block) starts at its cursor slot. Each commit moves the slot to
   its last block while the previous commit left it at the cursor.
2. **At the first `PreBlockSyncEvent`** (the store reaches the tip, or starts near it) a background
   thread fills the rows still missing the address (none on a new store) and sets `CURRENT`.
3. **Reads** use the query above once the state is `CURRENT` and both indexes exist and are valid,
   which is checked every minute; until then they use the full query, and the store logs that the
   indexes are to be applied. `admin-cli apply-indexes` builds them, then runs `ANALYZE address_utxo`:
   PostgreSQL keeps statistics of an index expression only from an `ANALYZE`, and autovacuum may not
   analyze a 350M-row table for weeks. On the mainnet store `ANALYZE address_utxo` took 5.8 s
   (30,000 sampled rows).
4. **A store synced before the flag was on** writes the address from then on and starts with no
   slot; the background fill at the tip fills its older rows: chunks of about 50M rows by
   `spent_at_slot`, each an `UPDATE ... FROM address_utxo` hash join with a `VACUUM` after it. That
   is slow on mainnet (below). The sync is not paused.

A store that saves rows without the address (the flag turned off) puts the state back to `FILLING`
from before those rows at its first such save. A writer of a version without this change cannot do
that, so readers also check that the slot is at or past the cursor; the cursor is saved after the
commit that moves the slot. A rollback (chain, or `admin-cli rollback-data` when it deletes
`tx_input` rows) moves the slot back to the point. A read-only API instance never fills; it
re-checks the state and the cursor every minute.

### Databases

PostgreSQL 16 or later: the per-half `DISTINCT ... ORDER BY ... LIMIT` needs incremental sort for
`DISTINCT`, added in 16. The shipped compose files use 17.4 and 18. On older PostgreSQL the flag is
ignored with a warning. The column is added on H2 and MySQL too, so `tx_input` has the same columns
on every database and the jOOQ classes are shared, but it stays null there and the full query is
used. H2 backs devkit/devnet stores, where histories are small.

### Disk (mainnet)

| | Size |
|---|---|
| `tx_input.owner_addr` | ~32 GB (98 bytes per row, measured on a sample of 123k spends) |
| `idx_tx_input_owner_addr_slot` | ~27 GB (84 bytes per entry, measured on the same sample) |
| `idx_address_utxo_addr_hash_slot` | ~12 GB (estimate) |
| **Total** | **~71 GB** |
| RocksDB cache | ~1-1.5 GB, outside the database, recreated at every start |

### Filling a store synced without the address (mainnet)

Measured with the first version (an 8-byte hash, so these are lower bounds for the address):

- The first 50M-row chunk took 3 h 18 min; the second had not finished after 3 h 07 min. The
  updates are not HOT (`tx_input` pages are full), so each row adds a version to the table and an
  entry to each `tx_input` index; `tx_input_pkey` (41 GB) is in hash order, so its entries are read
  and written at random, about 5,500 rows/s on the benchmark VM. All 352M rows: about a day.
- `tx_input` grew from 90 to 103 GB, `tx_input_pkey` from 41 to 43 GB, `idx_tx_input_slot` from
  3.4 to 4.1 GB; temporary files peaked at 32 GB.
- For comparison, joining all 352M spends with their outputs into a new table (four hash-join
  slices) took 68 minutes.

So the flag is meant to be on from the initial sync. An admin-cli command that fills a stopped
store by rebuilding `tx_input` is a possible follow-up.

## Alternatives considered

| Option | Extra disk (mainnet) | Why not |
|---|---|---|
| `tx_input.addr_hash` (8-byte hash) | ~26 GB | Java cannot compute PostgreSQL's hash for the cache; needs a recheck join |
| Address-to-transaction table, one row per (address, tx) | ~180-223 GB | repeats address and tx hash per row, plus two wide indexes |
| `address_tx_amount` (account module) | larger still | one row per (address, unit, tx), off by default |
| `tx_input.address_id` from the optional `address` table | ~27 GB plus `address` | needs the `address` table on, an id lookup per write, ids differ between stores |
| Fill only at the tip, nothing during the initial sync | same | about a day of random I/O on mainnet, see above |
| Rolling table of the last 30 days of activity | ~1.5 GB | old pages and dormant busy addresses still fall back to the slow query |
| Choose the plan per address, no new data | 0 | no guarantee: busy addresses with no recent activity stay slow |

## Consequences

- `/addresses/{address}/transactions` costs about `page x count` index entries, not the history.
- With the flag on, each spend looks its output up in the RocksDB cache, or in `address_utxo` on a
  miss, and `tx_input` rows are about 98 bytes wider.
- After the initial sync, `admin-cli apply-indexes` builds the two indexes with the other read
  indexes (`CREATE INDEX`, which holds writes to the table while it builds, as for every index in
  `index.yml`).
- Out of scope: Blockfrost `/addresses/{address}/transactions` (its own reader), stake-address and
  payment-credential transaction lists (possible by joining the optional `address` table on
  `owner_addr`), an admin-cli command to fill a stopped store.
