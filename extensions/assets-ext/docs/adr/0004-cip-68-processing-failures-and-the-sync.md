# ADR 0004: A CIP-68 processing failure and the sync

## Status

Proposed. Partly implemented by PR #1220 (open): option B below, plus an early write of the `properties` as JSON, which
covers the failure we have seen (#1240). Option C, the savepoint, is not implemented and is still the recommendation for
a failure inside the database, which still stops the sync as in option A. The record asks for the choice about that
remainder and gives a recommendation. #1243 is closed by #1220 for the failures it covers; the remainder is #1265 (low
priority, for discussion).

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
written, at flush, inside the block transaction, and the sync stopped (reproduced with a test, #1240). The datum is a real
input: a transaction with a 1,100-level datum was accepted into a block by `cardano-node` 11.0.1 on a local devnet. The
failure is Jackson's, before any SQL is sent, so it does not depend on the database (the test ran on H2; it has not been run
on PostgreSQL). It was fixed at the source
(ADR 0002 decision 6: such a property is dropped at a depth of 100), but any other failure of the same kind stops the sync
the same way. The parser already turns what departs from the CIP into validation errors, and drops only an obvious attack, with a WARN (ADR 0002 decisions 6 and 8, ADR 0006); this record is about
the ones it does not know.

Two facts about the transaction make a simple fix insufficient:

- The failure often appears at flush or commit, not where the entity is built, so a `try/catch` around the
  parsing does not see it.
- On PostgreSQL a failed statement aborts the whole transaction, so even a caught exception leaves the block transaction
  unusable. H2, which the tests use, does not do that, so a test on H2 passes for code that fails on PostgreSQL.

## Options

**A. Let the sync stop (today for a failure inside the database; before #1220, for any failure).** Nothing is lost silently and the bad datum is found at once. The price is that an
optional module can halt a node that is otherwise healthy, until a person fixes the code.

**B. A `try/catch` per output.** Cheap, and catches what the parser throws. It does not catch a flush or commit failure
(above), so on its own it only moves the problem. **Implemented in PR #1220**, with the one addition that lets it reach
the failure we know: the `properties` are written as JSON once while the row is built, with a default Jackson mapper (what
Hibernate does later, at flush), so a value Jackson refuses (#1240) fails inside the `try` and skips the output. The skip is
the WARN and the `processing_failure` count described under Decision. Only work in memory is inside the `try`; the
`saveAll` is outside it, so a database that is down still stops the sync. A failure in one token of an output with several
reference NFTs skips the rest of that output. Not covered: a failure inside the database at flush or commit (a rejected
value, for example), which is what C is for. The mapper of the early write is a default `ObjectMapper`, an approximation
of Hibernate's; the repository does not configure Hibernate's JSON mapper, so today they agree. The code carries a comment
with the links to #1240 and #1243, so that it is not removed as redundant.

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
  reproduced in a test. The stack trace goes to DEBUG, the same rule as for parse failures (ADR 0002 decision 8).
- The counter `datums.skipped` with a new reason `processing_failure` (label `unknown`, as for `parse_failure`), so that an
  alert can use `increase()` and a rate rises visibly when something new appears on chain. Unlike the other skip reasons,
  which are expected bad data, this one means something nobody foresaw, so it is the one to alert on.

Both are implemented in PR #1220 for option B and would be the same for C.

*No configuration flag.* A switch between stopping and skipping would give every operator a decision they are not in a position
to make. One behaviour is documented instead.

## Consequences

- The sync no longer stops for a CIP-68 datum. A token whose datum fails unexpectedly is not indexed from that datum, so the earlier
  row of the token stays the latest (the one case, with a datum dropped as an attack, where the API still serves an earlier row,
  ADR 0003 decision 5).
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
  a bad datum and must still stop the sync, so the skip has to be limited to failures that come from the entity. For B in
  PR #1220 this does not arise, because the `try` holds no database call. For C it does.
- Whether the rest of an output should be skipped when one token of it fails (B in PR #1220 does that), or only the token.
  Only a failure caused by an attacker's datum reaches it, so the simpler behaviour was kept.

## References

- #1243 (the issue, closed by PR #1220 for failures in memory), #1265 (the remainder and this decision), #1240 and PR #1220
  (the case that exposed it)
- ADR 0002 decisions 6 and 8: what is dropped and what is reported
- ADR 0006: why a datum is otherwise indexed, whatever is wrong with it
- ADR 0003 decision 5: what the API returns when a datum is skipped
- `Cip68Processor.processTransaction`, `BlockFetchService.stopSyncOnError`
