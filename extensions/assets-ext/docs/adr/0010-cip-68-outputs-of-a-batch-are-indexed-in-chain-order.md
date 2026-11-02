# ADR 0010: The CIP-68 outputs of a batch are indexed in chain order

## Status

Proposed. Implemented in PR #1310, merged into PR #1220 (`84f50320c`), and in the 2.0.x backport, PR #1242. Amends ADR 0002 (decision 2, the two
skips) and ADR 0009 (decision 1), which depend on it. Chosen over the alternatives discussed in PR #1309 (closed).

## Date

2026-11-02

## Context

Two kinds of CIP-68 datum are stored only if their reference NFT is already stored:

- `not_token_metadata` (ADR 0002 decision 2): a datum with no user token in its transaction and none of the CIP-68 token fields.
- `nft_only` (ADR 0009): a datum whose transaction has the 222 NFT and no 333 or 444 token.

Both rules exist so that once a reference NFT is indexed, every later datum of it is. That way an owner who clears the metadata is not served the old
one. `Cip68Processor` checks this with `existsByPolicyIdAndAssetName`, against the rows stored when the block is indexed.

The answer depends on the order of the blocks. With `store.executor.enable-parallel-processing=true` (set in the `config/application.properties`
shipped with yaci-store), a sync that is not yet at the tip processes the blocks in batches (`blocks-batch-size`, 100 by default). Each batch is split
into partitions (`blocks-partition-size`) that run at the same time, and within a partition the blocks run in order. A later block can therefore be
indexed before an earlier block of the same batch has stored its row, and the check then depends on timing, in both directions:

- **An update is missed.** A later datum finds no row, because the earlier one is not stored yet, and is skipped.
- **An NFT's datum is stored.** An earlier datum finds a row that a later block of the batch has already stored, and is kept. Processed in chain
  order, it would have been skipped.

**Found on mainnet** by comparing the module with cf-token-metadata-registry at the tip, 2026-10-29. House of Titans #1001, #3377 and #4102 (policy
`43528549…`) were minted at slot 102,576,351. About 20 blocks later, at slot 102,576,755, their datums were replaced by an empty map, without a user
token. The module, synced with parallel processing, did not store the empty map and still served the old metadata. The registry stored it: it embeds
the same yaci-store sync, with parallel processing off.

At the tip, blocks are processed one at a time and the check is exact.

## Decision

### 1. The module indexes the blocks of a batch in chain order, on `PreCommitEvent`

`Cip68Processor` does not index a block when its `AddressUtxoEvent` arrives:

- **`collectForBatch`:** each block's event is kept in a map by block number. Several block threads fill it at once, so it is a `ConcurrentHashMap`.
- **`indexBatchInChainOrder`:** on `PreCommitEvent`, the kept blocks are sorted by block number and indexed one by one with the existing
  `processTransaction`. The map is then cleared (in `finally`).
- **`dropBatch`:** a `RollbackEvent` clears the map.

The indexing logic and the `existsByPolicyIdAndAssetName` check are unchanged; only *when* a block is indexed changes. When a block is indexed, every
earlier block has been, so the check is exact again.

Within one `PreCommitEvent` transaction, Hibernate flushes the rows saved for one block before the next block's query, so a block sees every earlier
block. The rows of one block are saved together after the whole block is read, as before. So a datum does not see a row from an earlier transaction
of the same block, in any sync mode.

`PreCommitEvent` is published by yaci-store in two cases, both before the cursor moves:

- **During a parallel sync:** once per batch, after every partition has finished.
- **At the tip, and with parallel processing off:** after every block, so a block is indexed right after it is processed, as before.

If the process stops before the event, the cursor has not moved, and the blocks are processed again.

### 2. Only what indexing reads is kept until the end of the batch

`collectForBatch` keeps:

- only the transactions with an output that holds an asset whose name starts with `000643b0`;
- each of them with **all** its outputs, since the user tokens it pairs with (ADR 0002 decision 2) are in the other outputs;
- none of their inputs, which indexing never reads.

A block without such a transaction keeps nothing. The memory held therefore grows with the CIP-68 activity of the batch, not with the size of its
blocks. On mainnet that is a few KB for 100 blocks (about 260,000 CIP-68 datums since 2022, roughly one every 30 blocks), against about 10 to 100 MB
for the whole events. At the tip the map holds at most one block.

The filter runs on the block thread, before the per-output error handling of `processTransaction` (ADR 0004). It is written so that it cannot throw:
it reads only the units, null-safe, and does not use an exception for control flow. It is broader than `Cip68TokenService.isReferenceNft`, which also
requires a quantity of 1: an output it cannot read keeps its transaction, so that `processTransaction` applies the exact check and its error handling
to it, as before.

### 3. Why this option

This is how yaci-store already handles state that depends on earlier events. The core processors collect during the batch, sort into chain order and
process on `PreCommitEvent`:

| Processor | What it collects | Sorted by | Why order matters |
|---|---|---|---|
| `AccountBalanceProcessor` (account) | each block's `AddressUtxoEvent` | block number | a balance is the sum of everything before it |
| `PoolStatusProcessor` (staking) | pool registrations and retirements | slot | a retirement depends on the pool's earlier status |
| `DRepStatusProcessor` (governance) | DRep registrations, updates, deregistrations | slot, transaction, certificate | an update needs the earlier registration |
| `Cip68Processor` (this ADR) | each block's transactions with a reference NFT output | block number | "already stored" needs the earlier datums |

It is the smallest change to the logic, and its outcome is by construction the one of a sync in chain order.

The other options discussed in PR #1309:

- **A. Decide at the end of the batch.** An `existsEarlierThan` query, plus an in-memory queue of the datums with no earlier row, decided on
  `PreCommitEvent`. It gives the same outcome, but changes the indexing logic and adds a concurrent queue. Together these are subtle to review.
- **B. Store every datum with a flag, and decide when the subject is served.** It is deterministic for any processing order and needs no state
  between blocks. But it stores the NFT datums and the junk that follows them again (about 152,000 reference NFTs on mainnet). That is the case
  ADR 0009 rejected, and it would need a new column.
- **C. Only the "earlier in the chain" query.** It fixes "an NFT's datum is stored", but not "an update is missed", which is the case seen on mainnet.
- **D. Make the two rules depend only on the transaction.** It needs no lookup, but loses correct cases. Always skipping `not_token_metadata` makes
  the House of Titans case permanent. Always storing it serves dApp datums as tokens again. Always skipping `nft_only` loses the update of a 333 or 444
  token's reference NFT made in a transaction that also moves its 222.
- **E. A chain-order hook in the core.** The core would deliver a listener's events in chain order. That is the cleanest place for it and would serve
  other order-sensitive modules, but it changes yaci-store core, and its design is the maintainers' call.
- **F. Document that the module needs parallel processing off.** The module cannot control it: it is a setting of the whole store, on in the shipped
  configuration, and a sync from scratch is much slower without it.

## Consequences

- **A sync gives the same rows with parallel processing on or off,** and the same as a sync in chain order.
  - Checked on mainnet, 2026-11-02: a full sync from genesis with parallel processing (build `72dd1377d`) was compared with what a sync in chain order
    decides. The comparison covered every output with a reference NFT and an inline datum: 263,831 outputs and 157,655 reference NFTs.
  - 387 decisions depend on the order of the blocks (384 `nft_only`, 3 `not_token_metadata`), and all 387 match.
  - No first row of a reference NFT is a conditional datum, and no update after a stored row was missed.
  - House of Titans #1001, #3377 and #4102 serve their cleared metadata.
- **During a parallel sync, the CIP-68 work of a batch runs on one thread at the end of the batch.** The rest of the store stays parallel. On mainnet
  the sync from genesis took about 27 hours, against 25.5 hours without this change, on a shared host with nothing controlled. That does not show a
  slowdown.
- **Remote consumer:** a remote consumer (`store.remote.consumer-enabled`) never receives `PreCommitEvent`, because yaci-store does not send it
  (#1311). So the module indexes nothing there. This is the same limitation as the other stores that finish their work on `PreCommitEvent` or
  `CommitEvent` (`AccountBalanceProcessor`, `DatumStorageImpl`, `ScriptStorageImpl`, `AddressProcessor`, ...). It is documented on `Cip68Processor`.
  There are no known users of remote consumers with this module.
- **Rows stored before this change** by a parallel sync can differ from a sync in chain order. A resync fixes them, and PR #1220 needs one anyway
  (ADR 0006).

## When to revisit

- If #1311 is solved in a way that does not deliver `PreCommitEvent` to a consumer, option B, which needs no core event, may fit better.
- If the core gets a chain-order hook (option E), the module can use it instead of keeping the batch itself.

## References

- ADR 0002 (decision 2: what is indexed and the two skips), ADR 0004 (processing failures), ADR 0009 (an NFT's datum is not stored)
- PR #1309 (the problem and options A to F, closed for this one), PR #1310 (the implementation), PR #1220, PR #1242, issue #1311 (remote consumer)
- `Cip68Processor.collectForBatch`, `Cip68Processor.indexBatchInChainOrder`, `Cip68Processor.dropBatch`
- Tests: `Cip68ProcessorTest.BatchInChainOrder`, `Cip68ProcessorTest.KeptForBatch`, `Cip68ProcessorH2IT`
- yaci-store core: `ShelleyBlockEventPublisher.processBlocksInParallel` and `ByronBlockEventPublisher.processBlocksInParallel` (partitions, then
  `PreCommitEvent`), `AccountBalanceProcessor`,
  `PoolStatusProcessor`, `DRepStatusProcessor`
