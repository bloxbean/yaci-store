# ADR 0007: Address Transactions from tx_input.addr_hash

## Status

Proposed. Builds on the `store_state` table added with `address_utxo_unspent` (PR #1271).

## Date

2026-10-25

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

The spends are the hard half. `tx_input` holds only the key of the output it spends, not the
address, so "the latest spends from address A" cannot be read from an index: any of A's outputs,
however old, may have been spent a moment ago.

## Decision

Add `tx_input.addr_hash bigint`: `hashtextextended(owner_addr, 0)` of the output the row spends.
With two indexes

```sql
create index idx_tx_input_addr_hash_slot     on tx_input     (addr_hash, spent_at_slot);
create index idx_address_utxo_addr_hash_slot on address_utxo (hashtextextended(owner_addr, 0), slot);
```

both halves come out of an index in slot order. PostgreSQL merges them and stops after the page:

```sql
select distinct tx_hash, block, block_time, slot from (
    select tx_hash, block, block_time, slot from address_utxo
    where hashtextextended(owner_addr, 0) = hashtextextended(:address, 0) and owner_addr = :address
  union all
    select i.spent_tx_hash, i.spent_at_block, i.spent_block_time, i.spent_at_slot
    from tx_input i join address_utxo o on o.tx_hash = i.tx_hash and o.output_index = i.output_index
    where i.addr_hash = hashtextextended(:address, 0) and o.owner_addr = :address
) t order by slot desc, tx_hash desc limit :count offset :offset
```

The `owner_addr = :address` checks rule out another address with the same 64-bit hash; a
collision costs one extra row read, never a wrong row.

`hashtextextended` is the function PostgreSQL's hash partitioning uses, so its values are fixed
across versions and `pg_upgrade`. It is computed in SQL from `owner_addr` on both sides, so Java
never reimplements it. `owner_addr` (not `owner_addr_full`) is hashed everywhere.

### Life cycle

Behind `store.utxo.tx-input-addr-hash-enabled` (default `false`), PostgreSQL only.
`TxInputAddrHash` keeps a `store_state` row, `utxo.tx_input_addr_hash`:

1. **Initial sync:** no row, nothing written. The sync is as fast as without the flag.
2. **First `PreBlockSyncEvent`** (the store reaches the tip, or starts near it): the row is
   created as `FILLING`. From then on every inserted `tx_input` row gets the hash from a primary
   key lookup of the output it spends, inside the insert. A spend saved before its output (a later
   transaction of the same block, or a later block of a parallel batch) is filled at the commit,
   from the range of slots saved since the last commit.
3. **Background fill**, while the sync continues: the older rows, in chunks of about 50M rows by
   `spent_at_slot`. Each chunk is one `UPDATE ... FROM address_utxo` with nested loops off, so it
   hashes the chunk and reads `address_utxo` once instead of 50M random lookups. The chunk commits
   with the row's slot moved to its end, so a restart continues after the last finished chunk.
   `VACUUM tx_input` after each chunk lets the next one reuse the space of the old row versions,
   so the table grows by about one chunk, not by a copy of itself. The last half day before the tip
   is a separate small chunk, so a chain rollback never waits for a long one.
4. **Indexes** with `CREATE INDEX CONCURRENTLY` (an interrupted build's invalid index is dropped
   and built again), then `ANALYZE` for the expression's statistics. The row becomes `CURRENT`.
5. **Current:** reads use the hash query; each commit moves the row's slot to its last block.

A store that synced blocks with the flag off has its slot behind the cursor at startup: it goes
back to `FILLING` from that slot, and reads use the full query until it is filled again. A rollback
(chain or `admin-cli rollback-data`) moves the slot back to the rollback point.

A read-only API instance never fills; it re-reads the state every minute until it is `CURRENT`.

### Databases

PostgreSQL only. The column is added on H2 and MySQL too, so `tx_input` has the same columns on
every database and the jOOQ classes are shared, but it stays null there and the full query is used.
H2 backs devkit/devnet stores, where histories are small. A MySQL version would compute the hash in
Java and needs a matching expression on each database; not done until a large MySQL store needs it.

### Disk (mainnet)

| | Estimate |
|---|---|
| `tx_input.addr_hash` | 3-5 GB (352M x 8 bytes) |
| `idx_tx_input_addr_hash_slot` | ~11 GB |
| `idx_address_utxo_addr_hash_slot` | ~12 GB |
| **Total** | **~26-28 GB** |

The background fill also leaves about one chunk of free space inside `tx_input`.

## Alternatives considered

| Option | Extra disk (mainnet) | Why not |
|---|---|---|
| `tx_input.owner_addr` (the full address, ~94 bytes) and `(owner_addr, slot)` indexes | ~93 GB | the same design at 3.5 times the disk |
| Address-to-transaction table, one row per (address, tx) | ~180-223 GB | repeats address and tx hash per row, plus two wide indexes |
| `address_tx_amount` (account module) | larger still | one row per (address, unit, tx), off by default |
| `tx_input.address_id` from the optional `address` table | ~27 GB plus `address` | needs the `address` table on, an id lookup per write, and ids differ between stores |
| Rolling table of the last 30 days of activity | ~1.5 GB | old pages and dormant busy addresses still fall back to the slow query; a good later addition |
| Bloom filter per block range | ~0.5-1 GB | much more code: sizing, false positives, rollback of the last range |
| Choose the plan per address, no new data | 0 | no guarantee: busy addresses with no recent activity stay slow |

Measured for the comparison: a 30-day window holds 73,268 transactions of the #1269 address, and
the full join of all 352M spends with their outputs (four hash-join slices) takes 68 minutes.

## Consequences

- `/addresses/{address}/transactions` costs about `page x count` index entries, not the history.
- On PostgreSQL with the flag on, each `tx_input` insert does one primary-key lookup on
  `address_utxo` once the store has reached the tip. The initial sync does not.
- An existing mainnet store fills in the background after it is switched on; the sync is not paused.
- Out of scope: Blockfrost `/addresses/{address}/transactions` (its own reader), stake-address and
  payment-credential transaction lists (possible later by joining the optional `address` table on
  the same hash), an admin-cli command to fill while the store is stopped.
