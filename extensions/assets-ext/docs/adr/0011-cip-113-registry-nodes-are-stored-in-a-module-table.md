# ADR 0011: CIP-113 registry nodes are stored in a table of the module

## Status

Accepted. It was proposed on 2026-11-20 to answer the maintainers' request to avoid a new table unless one is needed, and accepted at the
maintainers' meeting of 2026-11-25. The main reason given there is module boundaries: one module should not depend on another module's
storage. The measurements below support it. The design was then adjusted: the table records which registry each node belongs to
(Decision 3), and CIP-113 is on by default (Decision 5). How the API serves the nodes is in ADR 0012.

## Date

2026-11-20 (proposed), 2026-11-25 (accepted)

## Context

### What CIP-113 adds

CIP-113 defines programmable tokens. They are ordinary native assets, but a smart contract checks every transfer, so the tokens cannot leave
the rules of their issuer (for example a freeze-and-seize list). A token is programmable if its policy is registered in an on-chain registry.
The registry entry names the scripts that govern the token. It holds no display metadata: the name, ticker, decimals and logo still come from
CIP-26 or CIP-68, the token's *companion* metadata.

The module must serve the same data as cf-token-metadata-registry's V2 API, so that clients can move between the two:

- `type`: `PROGRAMMABLE` if the token's policy is registered, `NATIVE` if not.
- `extensions.cip113`: `minting_logic_script`, `transfer_logic_script`, `third_party_transfer_logic_script`, `unfracking_logic_script` and
  `global_state_policy_id`. Each one can be `null`.
- A token of a registered policy without CIP-26 or CIP-68 metadata is served with a `MANDATORY` validation error (`cip` `CIP-113`,
  `field_name` `companion_metadata`), as ADR 0007 already records.

Only the API has to match. The module does not have to store the data the way the registry does.

CIP-113 was part of the module before. It was added in #870 and removed in #1134 (`606591d29`, 2026-09-07). That code parsed an earlier
registry datum with 5 fields. The registry deployed today uses 7 fields (see below).

### The registry on-chain

From the reference implementation, `cardano-foundation/cip113-programmable-tokens` (`validators/registry.ak`, `lib/registry_node.ak`,
`lib/linked_list.ak`):

- **One node per registered policy.** Each node is a UTxO holding one NFT of the registry policy. The NFT's token name is the node's key, which
  is the registered policy ID. The first node of the list, the *origin* node, has an empty key and an empty token name.
- **Every node sits at the same address.** Its payment credential is the registry policy ID itself, because one script is both the NFT's
  minting policy and the nodes' spending validator.
- **The datum** is `Constr 0 [key, next, minting_logic_script, transfer_logic_script, third_party_logic_script, unfracking_logic_script,
  global_state_cs]`. The list is sorted by key; `next` points to the next node's key, and the last node points to a sentinel of `0xff` bytes.
- **A node is spent only to be re-created:**
  - when a new node is inserted after it, its `next` changes;
  - when it is updated in place, the transfer, third-party and unfracking scripts and `global_state_cs` may change. This is the "upgradeable"
    part of the registry. The key, `next` and the minting script cannot change in an update.
- **A node is never removed.** The registry's redeemer has only `RegistryInit` and `RegistryInsert`, and the spend handler requires the node to
  be re-created. A registration stays even if every token of the policy is burned.

So the current state of a registered policy is always **the one unspent output holding the NFT `registry policy ID + policy ID`**.

Mainnet registry `484e733d122af44e6101988bcc47ed261a7af5c43c797e89d60e3075`, 2026-11-20:

- **Node outputs ever created:** 7.
- **Unspent:** 4, the origin node and 3 registered policies (`01c24df7…`, `b025efe5…`, `ed5f6bb6…`).
- **Datum shape:** every one has 7 fields.

### What the rest of yaci-store already holds

- **The `assets` store** records mints and burns: policy, asset name, quantity, slot and transaction. It has no datums. A node NFT is minted
  once, when the policy is registered, so the store cannot show any later update of the node.
- **The utxo store** keeps every output in `address_utxo`, with the full inline datum and its assets in `amounts`. It records spent outputs in
  `tx_input`.
  - **It already has all the data the API needs,** and its rollback handling would keep it consistent.
  - **assets-ext already depends on it.** The CIP-68 processor reads the `AddressUtxoEvent` the utxo store publishes.
  - **Indexes:** its migrations create only the `address_utxo` primary key `(output_index, tx_hash)`, `address_utxo(slot)`,
    `address_utxo(reference_script_hash)`, the `tx_input` primary key, and `tx_input(spent_at_slot)` and `tx_input(spent_at_block)`.
    An index on `owner_payment_credential` (or a GIN index on `amounts`) exists only if the operator creates it after the sync, for example
    with the `create-index.sql` of `applications/all`.
  - **Pruning:** `store.utxo.pruning-enabled` (off by default) deletes every *spent* output older than `pruning-safe-blocks` (2,160 by default),
    both its `address_utxo` row and its `tx_input` row. Unspent outputs are never deleted. Many clients prune: those who need only the current
    UTxO set, for example to build transactions, have no use for the history.
  - **Plugin filters:** a filter on `utxo.unspent.save` can stop some outputs from being saved.
    `AddressUtxoEvent` still carries every output of the block, because the filter applies only when the outputs are saved.

The maintainers asked to avoid a new table if CIP-113 can be served from what yaci-store already stores, and, if a table is needed, to say
exactly why.

## Decision

### 1. The module keeps the registry nodes in its own table, `cip113_registry_node`

**The data is never the problem.** A live node is never a spent output, so pruning never deletes it. Every client, pruned or not, has the
current state of the registry in `address_utxo`.

**Finding it fast is the problem.** We know the registry policy ID, which is the nodes' payment credential and the first half of each node
NFT's unit. We don't know the live node's transaction or slot, because the node moves to a new output on every insert and every update. None
of the indexes the utxo store's migrations create can look up an output by payment credential or by asset:

| Index created by the migrations | What it needs | Why it does not help |
|---|---|---|
| `address_utxo` primary key `(output_index, tx_hash)` | The transaction hash | We are looking for the transaction |
| `address_utxo(slot)` | The slot | The node's slot is unknown |
| `address_utxo(reference_script_hash)` | A reference script hash | Registry nodes carry no reference script |
| `tx_input` primary key | A spent output | It leads from an output to the transaction that spent it: this is the walk of alternative C, which pruning breaks |

So finding the nodes without a table needs one of the following, and each fails some clients or costs them more than a table:

| Without a table | Not pruned | Pruned | Cost (mainnet, see Measurements) |
|---|---|---|---|
| Follow the spends from the origin node (alternative C) | Works, 14.8 ms | **Fails:** pruning deletes the spends it follows | An origin outpoint per network in the configuration |
| Require an index on `address_utxo(owner_payment_credential)` (D) | Works | Works | About 8.4 GB of index on an unpruned database, and 6.4 times the insert cost of every output of every block |
| A partial index on the registry address (E) | Works | Works | A few kB, but PostgreSQL only; created at runtime on a table of another module |
| Neither: scan `address_utxo` | 5 min per scan | Shorter, not measured | A full scan on every lookup, or on every start if the result is kept in memory (F) |

The table needs none of them:

- **It is filled from `AddressUtxoEvent`,** as `cip68_metadata` is, so it never depends on the utxo store's pruning, indexes or plugin filters.
- **It is tiny:** 7 rows and 48 kB on mainnet, one write per node output, one indexed read per lookup (0.027 ms).
- **It works the same for every client** and on every database the module supports (PostgreSQL, MySQL, H2).

**The reason for the table is module boundaries.** assets-ext reads only events from other modules, never their tables, so the utxo store
can change its schema, indexes or pruning without breaking it. This is how `cip68_metadata` works already, and it was the main reason given
at the maintainers' meeting.

**Performance points the same way.** Data loss is not the issue: the registry can be found fast for every client only through a table of
the module, or through an index on the largest table of another module.

### 2. How CIP-113 fits into the module

CIP-113 is a third source next to CIP-26 and CIP-68, with its own package (`cip113`) and the same layers:

```
AddressUtxoEvent (utxo store, every block)
  └─ Cip113Processor ──── Cip113RegistryNodeParser (7-field datum)
       │  real node ──────────────► cip113_registry_node        (Decision 3)
       │  not a node ─────────────► invalid_token               (Decision 4, ADR 0007)
RollbackEvent
  └─ Cip113RollbackProcessor ─────► delete rows with slot > rollback slot
GET /subject/{subject}, POST /subject/query
  └─ TokenQueryService ── Cip113StorageReader ── cip113_registry_node (configured registries only)
       └─ Subject: type, extensions.cip113, companion_metadata error   (ADR 0012)
Cip113Configuration: store.assets.ext.cip113.*, bundled registry IDs per network (Decision 5)
```

- **Write side:** `Cip113Processor` listens to the `AddressUtxoEvent` the utxo store publishes for each block, as the CIP-68 processor
  does. It never reads the utxo store's tables (Decision 1). It is only created when `store.assets.ext.cip113.enabled` is on.
- **Read side:** `TokenQueryService` asks `Cip113StorageReader` for the current node of a subject's policy and adds it to the response
  (ADR 0012). The reader is the only code that reads the table. It returns nothing when CIP-113 is off.
- **Rollback:** `Cip113RollbackProcessor` runs even when CIP-113 is off, so rows written while it was on never outlive their blocks.
  admin-cli's `rollback-data` lists the table too (`components/dbutils/.../rollback.yml`).
- **No dependency on another standard.** CIP-113 does not read `cip68_metadata` or the CIP-26 tables. They only meet in the response.

### 3. The table: `cip113_registry_node` (migration `V0_1700_4`)

One row per **version** of a node: each output that carries a node is a row.

| Column | Type | Content |
|---|---|---|
| `key` | `VARCHAR(64)` NOT NULL | The node's key: the registered policy ID (56 hex), or empty for the origin node. 64 because the parser accepts up to 32 bytes, as some linked-list libraries write longer tail sentinels |
| `registry_policy_id` | `VARCHAR(56)` NOT NULL | The policy of the registry NFT the output holds: which registry the node belongs to (see below) |
| `slot` | `BIGINT` NOT NULL | The slot of the output |
| `tx_hash` | `VARCHAR(64)` NOT NULL | The transaction of the output |
| `tx_index` | `INTEGER` NOT NULL | Orders two versions of a node in one slot. Not in the key |
| `output_index` | `INTEGER` NOT NULL | Which output of the transaction |
| `next` | `VARCHAR(64)` NOT NULL | The next key in the sorted list, or the `0xff` sentinel |
| `minting_logic_script`, `transfer_logic_script`, `third_party_transfer_logic_script`, `unfracking_logic_script` | `VARCHAR(56)` | The 28-byte hash of each credential, `NULL` when absent. A `NULL` unfracking script means unfracking is forbidden |
| `global_state_policy_id` | `VARCHAR(56)` | `global_state_cs` of the datum, `NULL` when empty |
| `datum` | `TEXT` NOT NULL | The inline datum (CBOR hex), kept so the node can be re-parsed if the parser changes, and for audit. At most 4,096 hex characters (the parser rejects longer ones) |
| `last_synced_at` | `TIMESTAMP` | When the row was written |

**Primary key `(key, registry_policy_id, slot, tx_hash)`.**

- **`key` first,** because every read looks up a key: one policy, or the policies of a batch.
- **`slot, tx_hash`** make each version its own row. Nothing is updated in place, as in `cip68_metadata` (alternative G).
- **`registry_policy_id`** is in the key because the same key can be a node of two registries. Within one registry, a node sits in
  at most one output per transaction, since it holds the one NFT of its key. Two registries can each re-create a node of the same policy
  in one transaction, and without this column those two rows would have the same key.

**Index `idx_cip113_registry_node_slot`** serves rollback, which deletes by `slot`. No other index is needed: the table has a few rows per
registration.

**Why the registry is recorded.** A network can have more than one registry, one after another or side by side:

- **Registries are redeployed.** Preprod and preview have each been deployed again since the module first bundled them (Decision 5).
  A client that indexed the old registry and then configures the new one would otherwise serve old and new nodes as if they were one list.
- **The configuration can change without a resync.** A client can add a registry, or replace one, in
  `store.assets.ext.cip113.registry-nft-policy-ids`.

So each row records the registry whose NFT it holds, and **reads use only the configured registries**. The rows of a registry that is no
longer configured stay in the table, but they are not served. A policy registered in more than one configured registry is served from its
most recent node: the latest by `slot`, then `tx_index`, then `output_index`. To keep serving the old registrations when a new registry is
deployed, a client lists both.

**The current node of a policy** is its latest row in the configured registries, by `slot`, then `tx_index`, then `output_index`. A node is
never removed (Context), so the latest version is the live one.

**What the table does not hold, on purpose:**

- **No foreign key and no `CHECK` constraint.** The module checks its input in the write path (ADR 0005): the parser enforces every bound
  the columns have, so a hostile datum is rejected before it can fail an insert and stop the sync.
- **Whether a credential is a key or a script.** The registry API does not serve it, so the old module's `*_type` columns are dropped.
- **The registered tokens themselves.** The table holds nodes, which are policies. Which assets of a policy exist is the `assets` store's
  business. The API answers for any subject of a registered policy (ADR 0012).

### 4. Only real nodes are stored; the rest goes to `invalid_token`

An output is looked at only if it holds one unit of a configured registry policy: the registry NFT. Then:

| Check | When it fails |
|---|---|
| The datum is a 7-field registry node (`Cip113RegistryNodeParser`) | `registry_node_unparseable`: not stored |
| The output is at the registry's own script address: its payment credential is the registry policy ID | `registry_node_spoofed`: not stored, WARN |
| The NFT's token name is the datum's key | `registry_node_spoofed`: not stored, WARN |
| Indexing the output throws something unexpected | `processing_failure`: not stored, WARN. As for CIP-68, one output never stops the sync (ADR 0004) |

*Why the spoof checks:* anyone can send an output with any datum to the registry's address, but only the registry can mint its NFT and
the registry's validator decides where it goes. A datum that claims a key without the NFT of that key, or an NFT away from the registry's
address, is not a node.

*Why these are dropped and not stored with an error,* unlike CIP-26 and CIP-68 data that breaks its CIP (ADR 0006): a registry node is
not metadata that an owner can get wrong. The registry's validator only accepts well-formed nodes, so an output that fails these checks
was not written by the registry. Each case is still recorded in `invalid_token` (ADR 0007, with the registry NFT as the token and the
first 256 characters of the datum in `details`), so it can be found without reading the logs.

**Nodes are indexed straight from each block's event,** not at the end of the batch as CIP-68 is (ADR 0010). Whether a node is stored does
not depend on any row stored before it, and two versions of a node are ordered by their own slot and `tx_index`, so the blocks of a
parallel sync can be indexed in any order.

### 5. CIP-113 has its own flag, and the registries are bundled per network

The configuration the module had before #1134 is brought back, so CIP-113 is switched like CIP-26 and CIP-68:

- **`store.assets.ext.cip113.enabled`** switches CIP-113 on or off, under the master flag `store.assets.ext.enabled`. **It is on by
  default,** like CIP-26 and CIP-68, now that the registry is live on mainnet. Before #1134 it was off until then. When it is off, no
  registry node is read or stored and every token is served as `NATIVE`.
- **The policy IDs of the deployed registries are bundled per network** in `cip113/cip113-mainnet.properties`, `cip113-preprod.properties`
  and `cip113-preview.properties`, and picked by `store.cardano.protocol-magic`, as CIP-26 picks its registry (`Cip26NetworkDefaults`):

  | Network | Registry policy ID |
  |---|---|
  | mainnet | `484e733d122af44e6101988bcc47ed261a7af5c43c797e89d60e3075` |
  | preprod | `3083d387537f9318b6ff5aeab7de5f5629d26495319b8fafd409222a` |
  | preview | `e5b339ef5b16d6c460759aca1d60e5e045a6f01da4a11b4ee0fec09a` |

  The preprod and preview IDs differ from the ones the module bundled before #1134 (`b9b19dc6…` and `dc8a7d1f…`): the registries have been
  deployed again since.
- **`store.assets.ext.cip113.registry-nft-policy-ids`** replaces the bundled IDs, for another registry or a private network. On a network
  with no bundled registry and none configured, every token is served as `NATIVE`.

The registry has no flag: CIP-113 is on when policy IDs are configured. A flag is kept here because the IDs are bundled, so without it a
client could not turn CIP-113 off.

## Measurements

Mainnet, on a yaci-store test database on a Linux test machine, 2026-11-20: PostgreSQL 16.13, unpruned, synced to block 14,180,866.

- `address_utxo`: about 334 M rows, 254 GB; its primary key is 42 GB.
- `tx_input`: about 333 M rows, 90 GB.
- No `owner_payment_credential` index.

| What | Result |
|---|---|
| Find the registry's outputs by payment credential, no index | Full scan: 5 min 14 s (count), 4 min 42 s (rows) |
| Follow the spends from the origin node, one indexed query per step (PL/pgSQL loop) | **14.8 ms**: 7 outputs visited, the same 4 live nodes found |
| The same walk as one recursive SQL query | The planner chose a parallel scan of `address_utxo`; cancelled after 10 min |
| Index on `owner_payment_credential`, built on a 1% sample (3,632,317 rows, 3,397,171 with a credential) | 92 MB, built in 5.3 s; **about 8.4 GB** scaled to the whole table |
| Inserting the 3.6 M sampled rows, without and with that index | 2.8 s and 18.0 s: **6.4 times** the cost |
| Share of `address_utxo` rows a pruned database keeps (0.1% sample, 363,158 rows) | 2.96% unspent; 2.97% with the default 2,160 `pruning-safe-blocks`; 4.05% with 129,600 |
| The same index on a pruned database, scaled | **About 250–340 MB** of live entries |
| The proposed table, with the 7 real mainnet node outputs | 8 kB of data, 32 kB of indexes, **48 kB** in total; 745 bytes per row |
| Lookup of 2 policies in the table | **0.027 ms** |

Notes on the estimates:

- **Index size:** the index was not built on the whole table. The sample takes whole pages, so it may not repeat credentials as often as the
  whole table does, and PostgreSQL stores a repeated key once per leaf page.
- **Insert cost:** the sample's index fits in memory. On an 8 GB index, which mostly does not, an insert costs more, not less.
- **A pruned database** keeps only the unspent outputs and the spent outputs of the last `pruning-safe-blocks`: about 3% of the rows, so its
  index holds about 3% of the entries. A pruned database still inserts every output before pruning deletes the spent ones, so the index
  also holds space freed by deletes until it is reused. The pruned sync below measures this.

### A pruned sync from genesis with the index (2026-11-23 to 2026-11-25)

To complete alternative D, mainnet was synced from genesis with `store.utxo.pruning-enabled=true` and the
`address_utxo(owner_payment_credential)` index created on the empty table, so it was kept up to date during the whole sync. Same build and
configuration as an unpruned run of the same week.

- **The index does not stay small.** It was 5.05 GB at the tip, and it stayed 5.05 GB after pruning deleted 237 M rows. Its leaf pages were
  only 12.6% full. A B-tree does not shrink after deletes without a `REINDEX`, so a pruned database synced from genesis carries an index
  close to the unpruned size, not the 250–340 MB that the live entries alone would need.
- **Pruning falls behind during a sync from genesis.** The deletes per epoch grew from milliseconds to 40 minutes, then 2.8 h (28 M rows),
  then 11.35 h (103 M rows, about 2,500 rows/s). A run in progress blocks the next ones. At block 11.2 M the table still held 198 M live rows
  (156 GB of heap), and the index was 4.5 GB.

So alternative D costs a pruned client about as much index as an unpruned one, unless the operator rebuilds the index after the sync.

## Consequences

- **CIP-113 works for every client the same way,** pruned or not, with or without extra indexes or plugin filters, on PostgreSQL, MySQL and H2.
- **One migration and one table,** tiny: 2 rows per registration plus 1 per update.
- **The module keeps a copy of data the utxo store also holds.** For mainnet that is 48 kB, against about 8.4 GB of index (alternative D).
- **The sync must start before the registry was deployed** (mainnet slot 199,384,339), or the nodes created before the start are missing.
  This holds for every option, with or without a table, and the same is true of CIP-68.
- **The parser must change if the registry's datum changes,** as it did from 5 to 7 fields.
- **The nodes of a registry that is no longer configured stay in the table.** They are a few rows, and they are not served.
- **The CIP-113 regression suite** is in `regression-tests` (`shared/cip113_suite.py`, `mainnet/test_cip113.py`, `preprod/test_cip113.py`,
  marker `cip113`). Its fixtures are generated from the chain (`shared/generate_cip113_fixtures.py`), and it compares the module's CIP-113
  answers with cf-token-metadata-registry's, as for CIP-26 and CIP-68.

## Alternatives considered

### A. Read from the `assets` store

Rejected. It records the node NFT's mint, so it could say that a policy was registered, and when. It has no datum, and an update mints
nothing, so it cannot give the current scripts. It is also a separate store a client may not enable.

### B. Query `address_utxo` with the indexes its migrations create

Rejected. None of them looks up an output by payment credential or by asset (Decision 1). Without an extra index, the query is a full scan:
5 minutes on mainnet.

### C. No table: follow the spends from the origin node

**How it works:** start at the origin node's output. Find the transaction that spent it in `tx_input` (primary key), then that transaction's
registry outputs in `address_utxo` (slot index). Repeat until every output found is unspent. This uses only indexes every client has: 14.8 ms
on mainnet. The live nodes could then be kept in memory and updated from `AddressUtxoEvent`.

Rejected because:

- **It fails for pruned clients.** The walk passes through every spent node output, from the origin node to the live nodes. Pruning deletes
  those outputs and their `tx_input` rows once they are older than `pruning-safe-blocks`. The live nodes are still there, but nothing leads to
  them.
- **It needs one more setting per registry:** the origin node's outpoint (on mainnet `bfefbd22…#1`).
- **It must run step by step.** As one recursive query, PostgreSQL scans the whole of `address_utxo`.
- **Its cost grows with the registry's history** (2 outputs per registration, 1 per update), not with the number of registered policies.

### D. No table: require an index on `address_utxo(owner_payment_credential)` from pruned clients

**How it works:** with the index, the live nodes are one indexed lookup, pruned or not.

Rejected because the cost falls on the largest table of another module, for a handful of rows:

- **About 8.4 GB** on an unpruned mainnet database, and about 250–340 MB of live entries on a pruned one (Measurements), for a handful of
  rows the module needs.
- **Every output of every block pays for it,** CIP-113 or not, pruned or not: 6.4 times the insert cost in the sample. A pruned database
  inserts every output too, and pruning then deletes the spent ones from the index as well. That includes the hundreds of millions of outputs
  of a sync from genesis.
- **Building it** takes a full scan of `address_utxo` (5 minutes on this database) and a sort of about 310 M keys.
- **No migration of the utxo store creates it.** The module would have to create an index on a table it does not own, or ask every pruned
  client to.
- **Pruned clients without it would get no CIP-113,** or the module would have to stop at startup and ask for it.

### E. No table: a partial index on the registry address

**How it works:** `CREATE INDEX … ON address_utxo (owner_payment_credential) WHERE owner_payment_credential IN (<registry policy IDs>)` holds
only the registry's outputs: a few kB, and each insert only evaluates the condition.

Rejected because:

- **MySQL and H2 have no partial indexes,** and the module supports both.
- **The registry IDs differ per network,** so a migration cannot contain them. The module would create and rebuild the index at runtime,
  from the configuration, on a table of another module.
- **Building it still takes a full scan of `address_utxo`.**

### F. No table: keep the nodes in memory and rebuild them on start

**How it works:** `AddressUtxoEvent` keeps an in-memory map of the live nodes up to date; on start, the map is rebuilt from `address_utxo`.

Rejected because the rebuild is alternative B, C or D. A pruned client without the index would pay a full scan on every start, 5 minutes on an
unpruned mainnet database. yaci-store's `PluginStateService` cannot hold the map across a restart either: it is kept in memory.

### G. Store only the current node, as the registry does

The registry's primary key is `(key, slot)`, and the last write of a slot replaces the earlier ones. Not chosen: assets-ext never updates a
row in place and keeps history ordered by `slot` and `tx_index`, as `cip68_metadata` does. The history costs one row per node output: 7 on
mainnet.

## Open questions

None. The two questions of the proposal were settled on 2026-11-25:

- **The default of `store.assets.ext.cip113.enabled`:** on (Decision 5).
- **The size of the index on a pruned database:** measured by the pruned sync from genesis (Measurements). It is about 5 GB, not 250–340 MB.

## References

- CIP-113; `cardano-foundation/cip113-programmable-tokens`: `validators/registry.ak`, `lib/registry_node.ak`, `lib/linked_list.ak`
- cf-token-metadata-registry: ADR-016 (CIP-113), migrations `V3__adding_support_for_cip113.sql` and
  `V5__cip113_registry_node_minting_and_unfracking_scripts.sql`
- ADR 0007 (`invalid_token`, CIP-113 codes and the `companion_metadata` error)
- #870 (CIP-113 added), #1134 (`606591d29`, CIP-113 removed); issue #1265
- `UtxoProcessor` (`AddressUtxoEvent`), `UtxoStorageImpl.deleteBySpentAndBlockLessThan` (pruning), `applications/all/src/main/resources/sql/create-index.sql`
