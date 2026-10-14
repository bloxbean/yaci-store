# ADR 0004: A CIP-68 processing failure and the sync

## Status

Proposed. The decision is not implemented: today the behaviour is option A below. The record asks for the choice
between A and C and gives a recommendation. The issue is #1243.

## Date

2026-10-14

## Context

The CIP-68 indexer, `Cip68Processor.processTransaction`, is a synchronous `@EventListener` annotated `@Transactional`. It
runs in the same transaction as the block it belongs to. The core catches an exception from the listeners of a block,
logs it, stops the sync (`BlockFetchService.stopSyncOnError()`: it records the error and shuts the sync down) and
rethrows. So **any exception thrown while indexing one CIP-68 datum stops the whole store**, including the
modules that have nothing to do with token metadata (utxo, blocks, ledger state).

`assets-ext` is an optional module. The metadata of a token is the least critical thing the store holds, and the code
that interprets a datum reads untrusted, arbitrary on-chain data, so unexpected failures are a matter of when.

This has happened. #1240: a datum with a property nested more than 1000 levels deep made Jackson fail when the row was
written, at flush, inside the block transaction, and the sync stopped (reproduced with a test, #1240). It was fixed at the source
(ADR 0002 decision 6: such a property is dropped at a depth of 100), but any other failure of the same kind stops the sync
the same way. The parser already turns the failures it knows into WARNs and skips (ADR 0002 decision 9); this record is about
the ones it does not know.

Two facts about the transaction make a simple fix insufficient:

- The failure often appears at flush or commit, not where the entity is built, so a `try/catch` around the
  parsing does not see it.
- On PostgreSQL a failed statement aborts the whole transaction, so even a caught exception leaves the block transaction
  unusable. H2, which the tests use, does not do that, so a test on H2 passes for code that fails on PostgreSQL.

## Options

**A. Let the sync stop (today).** Nothing is lost silently and the bad datum is found at once. The price is that an
optional module can halt a node that is otherwise healthy, until a person fixes the code.

**B. A `try/catch` per output.** Cheap, and catches what the parser throws. It does not catch a flush or commit failure
(above), so it only moves the problem.

**C. Isolate the write: a savepoint around the save, and a fallback.** The save of the batch runs in a nested
transaction (`Propagation.NESTED`, or `REQUIRES_NEW`). If it fails, the batch is saved again one entity at a time, each in
its own savepoint, and only the entity that fails is skipped. The failure is logged with the policy id, the asset name and the
datum hex, and counted. The block transaction stays usable and the sync continues.

**D. Take the indexing out of the block transaction.** The module would read the blocks after they are committed, in its own
unit of work. This isolates it completely and also removes the module's cost from the block path, but it is a different
architecture: the module would need its own cursor and its own rollback handling, a change to the store that one optional
module should not decide alone.

## Decision

Recommended: **C.** A failure to index one datum skips that datum, leaves an explicit trace in the log and the metrics, and
never stops the sync.

*Why:* the module is optional and its input is untrusted, so the failure mode "one bad datum halts the node" is the
wrong default. A is the right behaviour only for data the store cannot do without. C keeps the cost where the failure is
(one entity) instead of the whole batch, and it does not change what the module stores for any datum that works today. D
solves more but costs a new extension point that no other module needs.

*What is skipped is reported, not hidden.* A skipped datum is the case where a person must be able to find out:

- One WARN with the policy id, the asset name, the exception class and message, and the datum hex, so that the datum can be
  reproduced in a test. The stack trace goes to DEBUG, the same rule as for parse failures (ADR 0002 decision 9).
- The counter `datums.skipped` with a new reason `processing_failure` (label `unknown`, as for `parse_failure`), so that an
  alert can use `increase()` and a rate rises visibly when something new appears on chain.

*No configuration flag.* A switch between stopping and skipping would give every operator a decision they are not in a position
to make. One behaviour is documented instead.

## Consequences

- The sync no longer stops for a CIP-68 datum. A token whose datum fails is not indexed from that datum, so what the API
  returns for it follows the same rule as any other skipped datum: the earlier row stays (ADR 0003 decision 5).
- A skipped datum is not retried. As with the other skips, it is applied again only when the datum is processed again (a
  resync). Finding it needs the WARN or the counter, so the alert on `processing_failure` matters.
- The savepoint costs a little on every block that has a CIP-68 datum, and the fallback costs one write per entity of the
  failed batch. Neither is measured (a full mainnet index holds 250,876 rows, and the fallback runs only after a failure).
- The change has to be tested on PostgreSQL, not only on H2. It is the reason the issue was split from the fix for #1240.
- `Cip68RollbackProcessor` is a separate listener and is not covered. A failure in a rollback is not a skipped datum, and
  stopping there is the safer behaviour, so it keeps option A.

## Open questions

- Whether `Propagation.NESTED` (a JDBC savepoint) works with the transaction manager of every supported database
  (PostgreSQL, MySQL, H2), or `REQUIRES_NEW` is needed for one of them. `REQUIRES_NEW` needs a second connection and does not see
  the uncommitted rows of the block.
- Whether to also skip a failure in the CIP-26 path. CIP-26 runs in its own scheduled job, outside the block transaction
  (ADR 0001), so it does not stop the sync; the question does not arise there.
- What an unexpected failure that is not about one datum should do: for example the database being unavailable. That is not
  a bad datum and must still stop the sync, so the skip has to be limited to failures that come from the entity.

## References

- #1243 (this decision), #1240 and PR #1220 (the case that exposed it)
- ADR 0002 decisions 6 and 9: bounded values and what is reported
- ADR 0003 decision 5: what the API returns when a datum is skipped
- `Cip68Processor.processTransaction`, `BlockFetchService.stopSyncOnError`
