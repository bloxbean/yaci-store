# ADR 0004: Selective Snapshot Import

## Status

Proposed. Supersedes nothing; extends [ADR 0003](0003-snapshot-export-import.md),
which assumes the restore target is a complete Yaci Store schema.

## Date

2026-08-29

## Context

ADR 0003 delivers snapshot export and import for a *complete* Yaci Store schema.
Projects that embed Yaci Store as a library enable only a subset of stores.
Cardano Foundation's `rosetta-java` is the motivating case: it needs blocks,
transactions and the UTxO set, and has no use for governance, staking or
ledger-state aggregates. Today those projects cannot use snapshot import at all.

The obstruction is not the data model. It is three preflight checks that assume
schema identity, plus control-state reconstruction that assumes the `block` table
is present.

### Two deployment shapes, and why they differ

Investigation established a distinction that drives the whole design.

**Flyway migration is classpath-driven, not flag-driven.** Spring Flyway is
configured with a single location, `classpath:db/store/{vendor}`, and every store
module on the classpath contributes its `V*.sql` there. `store.<x>.enabled` is a
`@ConditionalOnProperty` gate on the module's *beans* (for example
`StakingStoreConfiguration`); it does not gate migrations. There is no
Flyway-conditional logic anywhere in the codebase.

Therefore:

| Deployment | What determines which tables exist |
|---|---|
| **Library** (rosetta-java) | The classpath. Only the store jars the consumer depends on migrate, so the schema genuinely is a subset. |
| **`all` distribution** | Nothing. Every store jar is present, so every table exists regardless of `store.*.enabled`. |

The schema is a sufficient signal in library mode and carries no information in
the `all` distribution. The design must handle both.

**The specification registry is always complete.** It is tempting to assume the
classpath also scopes specifications. It does not. The `collectSnapshotSpecs`
Gradle task copies all 73 specifications from every subproject into
`components:snapshot`'s own resources, and `SnapshotSpecRegistry` loads them from
`/snapshot/specs.index` in that single jar. The registry knows every table in
every deployment. Selection must therefore come from the target schema or from
explicit configuration, never from what happens to be on the classpath.

### What blocks a subset restore today

All three are in `SnapshotImporter`:

| ID | Check | Why it fires on a subset |
|---|---|---|
| **B1** | Whole-schema fingerprint equality (`SnapshotImporter:89`) | A subset schema has fewer tables, so the fingerprint can never match. |
| **B2** | Flyway history fingerprint (`SnapshotImporter:95`) | Absent modules contribute no migrations, so applied history differs. |
| **B3** | Every base table must have a specification (`SnapshotImporter:122`) | Fires on the *consumer's own* tables. A library embedder runs its own migrations in the same schema, and none of those have a Yaci Store specification. |

Two things already tolerate a subset and need no change:

- Absent target tables are skipped. `planEveryTable` runs `ColumnPlanner` against
  every table that *does* exist and `continue`s past those that do not
  (`SnapshotImporter:197`). That per-table planning is already a real
  compatibility check.
- The four `mode: SQL` specifications declare `dependencies: [block]`, but that
  resolves to Parquet **files**, not the PostgreSQL `block` table. `address_utxo`
  can already be restored into a schema whose `block` table is absent.

### Control state does not follow the schema

`cursor.yml` and `era.yml` are `module: core`, so those tables exist in every
schema. `block.yml` is `module: blocks`. Both mandatory handlers rebuild their
output by reading the *imported* `block` table:

```
CursorTailHandler     2 references to <schema>.block   rebuilds cursor_
EraTransitionHandler  2 references to <schema>.block   rebuilds era
AdaPotJobsHandler     1 reference (LEFT JOIN on slot)  fills adapot_jobs.block
AccountConfigHandler  0 references — uses the consistency point only
```

A utxo-only schema therefore has `cursor_` and `era` tables but no `block` table,
and would restore with an empty `cursor_` — a database with no resume point.
This is fatal, not cosmetic, and it is the reason the naive "import what exists"
rule is insufficient on its own.

`CursorTailHandler` reads the imported table deliberately: its comment records
that doing so "guarantees the invariant validation checks: the final cursor is an
imported block."

## Decision

### D1 — `full` is the only import mode; the target schema is the selection

The importer restores every table that exists in the target schema and skips
every table that does not. There is no `--tables a,b,c`. This keeps the
dangerous judgement — which tables constitute a consistent chain state — out of
a flag an operator can get subtly wrong.

For library deployments this is complete: the consumer's own Flyway built the
schema, so the schema *is* the selection and nothing needs configuring.

### D2 — For the `all` distribution, resolve stores from runtime configuration

Because the `all` schema is always complete, a second signal is required. It is
resolved in two steps:

1. an explicit `--stores utxo,transaction,blocks` argument, if given;
2. otherwise the `store.*.enabled` values present in the admin CLI's own runtime.

Step 2 requires the operator to launch admin-cli with the same Spring profile
they run the node with — the existing docker env-file or `-D` mechanism. This is
already how the tool is configured, so it adds no new operational concept.

A resolved store set narrows the schema-derived set; it never widens it. A store
named that has no tables in the schema is a blocker, not a silent no-op.

### D3 — The `blocks` store is a mandatory minimum for import

Restore requires the `block` table to be present and imported. This is enforced
as an explicit preflight blocker — *"import requires the blocks store; the
`block` table is absent"* — never discovered later as an empty `cursor_`.

Rationale: it removes the control-state problem entirely for Phase 1 at zero
implementation cost, and the consumer it burdens (one wanting UTxOs with no block
data at all) is unusual for a chain indexer. The motivating consumer is
unaffected — Rosetta's API is block-based, so `rosetta-java` enables `blocks`
regardless.

D7 records the alternative that makes this requirement removable later.

### D4 — Replace the three identity checks

| ID | Change | Replaces |
|---|---|---|
| **C1** | Demote whole-schema fingerprint equality to a *fast path*. When it matches, skip per-table checks (today's behaviour exactly). When it differs, fall through to per-table planning, which `planEveryTable` already performs. No new manifest field. | B1 |
| **C2** | Compare Flyway migrations **per module** rather than as one history. The manifest records module → applied migration versions; preflight asserts equality only for modules whose tables are present. | B2 |
| **C3** | Three-tier handling of unclassified tables: (a) a specification exists → classify as now; (b) no specification but explicitly acknowledged in configuration → treat as `NOT_RESTORED` and leave untouched; (c) otherwise → blocker, as now. Consumers wanting their own tables restored supply specifications of kind `EXTENSION_DATA` through the existing `SnapshotSpecRegistry.load(customPaths, allowCustom)` path. | B3 |

C2 is not hypothetical. Two live schemas on the development machine already
differ: `adapot_jobs` has `block_hash` under current migrations and lacks it in
an older sync. A whole-history comparison rejects that wholesale; a per-module
one localises it.

### D5 — Handlers become conditional

A handler runs if and only if its target table exists. `account_config` only when
the account store is present, `adapot_jobs` only with adapot. The handler loop
currently iterates `registry.handlerTables()` unconditionally.

### D6 — Pin the selection, and guard against enabling a store that was not imported

- The resolved selection (the spec set actually imported) is recorded in the
  import journal. A resume against a schema whose selection has changed blocks,
  mirroring the existing different-snapshot guard.
- On application startup, if a store is enabled whose tables were never imported,
  the application **refuses to start**. Without this, flipping
  `store.staking.enabled=true` later against empty staking tables produces
  silently wrong data — the same failure shape as the margin columns in
  [#1123](https://github.com/bloxbean/yaci-store/issues/1123), and just as hard
  to detect.

### D7 — Classify state by mutability, and source it accordingly

The investigation produced a taxonomy that should govern future decisions:

| Kind | Examples | Source |
|---|---|---|
| Immutable chain fact | `block`, `address_utxo`, `pool_registration`, `era` | **Exporter** |
| Point-in-time control state | `cursor_` | **Manifest** |
| Mutable operational state | `adapot_jobs`, `account_config` | **Handler**, reconstructed from durable evidence |

As a **future** alternative to D3, the manifest may carry the cursor tail and the
era rows, removing the `blocks` requirement:

- The manifest is a standalone JSON sidecar (`ManifestCodec`, Jackson) written
  beside the archive parts and already ~6.7 MB, since it carries a `FileEntry`
  for every file. A cursor tail of ~2,160 entries is roughly 325 KB — a 5%
  increase on a file that is 0.005% of the archive.
- `ConsistencyPoint` already holds exactly one cursor row's worth of fields
  (`network`, `protocolMagic`, `epoch`, `slot`, `blockNumber`, `blockHash`,
  `prevBlockHash`, `era`). The tail is N of them, plus a recorded
  `cursorTailDepth` so the importer can block when the local
  `cursor-no-of-blocks-to-keep` exceeds what was carried.
- Because the importer reads the manifest **before** extracting anything, this
  data is available at preflight, which is where the depth check belongs.
- `ArchiveVerifier` already digests the manifest, so this content inherits
  tamper-evidence at no cost.

### D8 — The consistency point remains an export-time decision

`--target-epoch` stays on `snapshot export`
(`ConsistencyPointSelector:108`). The importer restores to
`manifest.point()` unconditionally and offers no target selection.

The export cuts **through the end of the named epoch, inclusive**. Verified: a
`--target-epoch 298` export selected block 4,899,295 / slot 127,526,366, which
are exactly `max(number)` and `max(slot)` for epoch 298; epoch 299 begins at
block 4,899,296.

Note the opposite convention elsewhere in the tooling: `rollback-data --epoch N`
rolls back to the **start** of epoch N, so returning a database to a snapshot
taken at the end of epoch 430 requires rolling back to 431.

### D9 — Semantic co-requirements are a new specification field

`requires:` declares that one table's restore is meaningless without another. It
is distinct from the existing file-level `dependencies:`. The motivating case is
`address_utxo` without `tx_input`: the result is a UTxO set that never marks
spends — silently wrong rather than obviously empty. Preflight rejects an
incoherent selection.

### D10 — Selective export is deferred, and gated on a new blocker

Everything above lets a consumer import a subset from a **full** archive, at the
cost of transferring data they discard. Producing smaller archives is a later
phase, and must not land before this blocker exists:

> Preflight today verifies that every schema table has a *specification*. It
> never verifies that the archive *carries files* for it. With full archives that
> gap cannot bite. With selective export it becomes a silent-empty-table bug: the
> schema has `pool_registration`, the archive does not carry it, and the import
> "succeeds" with zero rows.

Selective export also requires transitive closure over file `dependencies` —
selecting `address_utxo` must pull the `block` Parquet files its transform reads
— and an explicit coverage declaration in the manifest.

### D11 — Import and sync must not run concurrently

A PostgreSQL advisory lock already prevents two concurrent imports
(`ADVISORY_LOCK_KEY = 0x59414349_534E4150L`, "YACISNAP"). Import versus sync is
currently only documented, via `store.sync-auto-start=false`.

The sync loop shall take the same lock, so a running sync makes import fail fast
rather than corrupt. The import entry point is exposed as an API so an embedding
application can decide when to call it; for the standard distribution, admin-CLI
remains the recommended path.

## Alternatives considered and rejected

**Export `cursor_` as an ordinary analytics table.** Rejected on four independent
grounds, any one sufficient: DuckLake `INSERT` is append-only while `cursor_` is a
rolling window trimmed to `cursor-no-of-blocks-to-keep`; rollbacks delete rows and
an append-only export cannot represent deletion; the depth is the *exporting*
instance's configuration, not a chain fact; and — decisively — the exporter runs
incrementally during sync while the consistency point is selected later by
`ConsistencyPointSelector`, so a cursor exported at time T is not the cursor at
the snapshot point.

**Export `adapot_jobs` as an ordinary analytics table.** Rejected: it is a mutable
state machine that can also move *backwards*. Rows are created `NOT_STARTED`
(`AdaPotJobManager:91`), updated to `STARTED` (`AdaPotJobProcessor:68`), then
`COMPLETED` with timings; and on restart `STARTED` is reset to `NOT_STARTED`
(`AdaPotJobManager:59-64`). An append-only export would accumulate copies in
different states with no marker for which is current, `export_state` gating would
freeze whichever state was current when the partition closed, and "export only
when COMPLETED" is unsafe because a restart can undo it afterwards.

The handler is therefore the *correct* design rather than a workaround: it
reconstructs the job log from durable evidence — an `adapot` row exists precisely
for each epoch whose reward calculation completed — and so cannot capture a torn
intermediate state.

**Add `block` to the adapot exporter to remove the `AdaPotJobsHandler` join.**
Rejected as out of scope for #1123 and unnecessary. The source table `adapot` has
no `block` or `block_hash` column at all, so this would synthesise a new derived
column rather than restore a dropped one — categorically different from #1123.
And it blocks nothing: `adapot_jobs` exists only when the adapot store is on,
which implies a ledger-state schema, where `block` is always present.

**Import-time `--target-epoch`.** Rejected. Manifest row counts, bounds, digests
and all `validate` checks are computed for the export's point and would be
invalidated. More seriously, batch identifiers are content-addressed over the file
set, specification and transform version but **not** the cut slot, so two imports
at different cuts would produce different rows under the same batch ID, corrupting
journal and resume identity. An archive is a distributable artifact, and "this
archive is epoch 430" is a stronger contract than "up to 450, ask for less".

**Per-store import profiles that narrow within a complete schema.** Rejected as
an import concept. If the schema contains `pool_registration`, the staking store
is enabled and will run against the table left deliberately empty — producing
wrong data rather than absent data. Store selection is meaningful as an *export*
filter (smaller archives), not as an import filter. D2's store resolution is not
this: it exists because the `all` schema carries no information, and it is paired
with the D6 startup guard.

**A separate YAML store → table matrix.** Rejected as redundant. Every
specification already carries `module:`, and 16 of 18 module values map 1:1 to a
`store.<x>.enabled` property. Only `analytics-store` and `assets-ext` do not.
Those two should be renamed, or an optional `store:` alias added to the
specification. A hand-maintained second mapping would drift from `module:` exactly
as a Flyway chain drifts from an exporter query — the lesson of
[#1122](https://github.com/bloxbean/yaci-store/issues/1122).

## Consequences

**Positive.** Library consumers can use snapshot import. The schema-derived rule
means the common case needs no configuration. Per-module Flyway comparison
localises a class of drift that currently rejects a restore wholesale. The state
taxonomy in D7 gives future tables a principled home.

**Negative.** Every consumer imports the `block` table under D3, even one that
never queries it. Store resolution for the `all` distribution (D2) depends on the
operator launching admin-CLI with the correct profile; the D6 startup guard exists
because that can be got wrong. `requires:` (D9) adds a specification field that
must be maintained as tables are added.

**Neutral.** Full-schema restore behaviour is unchanged: when the schema
fingerprint matches, C1 takes the fast path and preflight behaves exactly as
today.

**Explicitly out of scope.** Ledger-state cannot be subset. AdaPot reward
calculation reads across `epoch_stake`, `pool`, `delegation`, `pool_registration`
(including the margin rationals), `reward` and `withdrawal`. A partial restore
there produces numbers that look plausible and are wrong — the failure mode
already encountered with the margin columns. Documentation must say so plainly.

## Implementation plan

### Phase 1 — Import-side selection

Delivers the actual ask. No export changes, no new specification fields.

1. C1 — demote the schema fingerprint to a fast path (`SnapshotImporter:89`).
2. C2 — per-module Flyway comparison; add module → migration versions to the
   manifest (`SnapshotImporter:95`).
3. C3 — three-tier unclassified-table handling (`SnapshotImporter:122`).
4. D3 — add the `blocks`-store preflight blocker.
5. D5 — make the handler loop conditional on target-table existence.
6. D2 — `--stores` argument and `store.*.enabled` fallback resolution.
7. D6 — record the selection in the import journal; block a resume whose
   selection changed.
8. Scope `validate` to present tables.

### Phase 2 — Coherence and safety

9. D9 — the `requires:` specification field and preflight coherence check.
10. D6 — the startup guard for a store enabled but never imported.
11. D11 — have the sync loop take the import advisory lock.
12. The archive-coverage blocker from D10 (needed before, not after, Phase 3).

### Phase 3 — Optional

13. D7 — manifest-carried cursor tail and era rows; remove the D3 `blocks`
    requirement and delete `EraTransitionHandler`.
14. D10 — selective export with transitive dependency closure, and profiles as
    export-side sugar.
15. Auto-detection of enabled stores when the importer runs inside the
    application runtime rather than admin-CLI.

## Open questions

1. Should an acknowledged-but-unspecified consumer table (C3b) be configuration,
   a CLI flag, or a marker file in the consumer's own specification directory? A
   flag is the easiest to misuse.
2. Is per-module Flyway comparison sufficient, or must the manifest record each
   module's migration checksums? This depends on whether a consumer may legally
   run a Yaci Store module at a different migration level — which should probably
   simply be forbidden.
3. Does `requires:` belong in the specification, or in a separate coherence-rules
   file? Specification-local keeps ownership with the module; a central file makes
   cross-module rules visible in one place.
4. Should library consumers get a programmatic import API rather than shelling out
   to admin-CLI? They already embed Yaci Store, so a CLI may be the wrong shape.

## References

- [ADR 0003: Parquet Snapshot Export and PostgreSQL Import](0003-snapshot-export-import.md)
- [#1122 — additive schema evolution for DuckLake exporters](https://github.com/bloxbean/yaci-store/issues/1122)
- [#1123 — exporters drop block number and stake_credential](https://github.com/bloxbean/yaci-store/issues/1123)
