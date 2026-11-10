# ADR 0008: Analytics Store on DuckDB 2.x, DuckLake by Default, Safe Partition Re-export

## Status

Proposed. Design only. No implementation is part of this ADR.

## Date

2026-11-10

## Context

The analytics store (`aggregates/analytics-store`, read side `aggregates/analytics-query`)
copies finalized chain data from PostgreSQL into Parquet. It runs on DuckDB in-process, through
`org.duckdb:duckdb_jdbc:1.5.5.0` (`gradle/libs.versions.toml:30`). It has two storage modes,
selected by `yaci.store.analytics.storage.type`:

- **`parquet`** (`ParquetWriterService`). One `COPY ... TO '<export>/<table>/<date=|epoch=>/data.parquet'`
  per partition. The file is written to `.tmp` and then renamed. There is no catalog.
- **`ducklake`** (`DuckLakeWriterService`). Each partition is `INSERT INTO <table> <query>` into a
  DuckLake table partitioned by `date` or `epoch`. The catalog is stored in PostgreSQL or in a
  DuckDB file.

This ADR covers four things the maintainers asked for: move to DuckDB 2.x, make DuckLake the
default, deprecate the `parquet` mode, and fix the edge cases around deleting and re-creating
partitions through the admin API. The facts below were checked at `origin/main` `b965a19ad`,
with probes run on the DuckDB CLI `v1.5.5 (Variegata) d8cdaa33fd`, DuckLake extension
`d8a1881e`. The probe SQL is in [Appendix A](#appendix-a-probes).

### C1. The default is inconsistent today

- The code default is `parquet`. `AnalyticsStoreProperties.Storage.type = "parquet"`, and
  `ParquetWriterService` is `@ConditionalOnProperty(... havingValue = "parquet", matchIfMissing = true)`.
- The shipped profiles set `ducklake` (`config/application-analytics.properties:38`,
  `docker/config/application-analytics.properties:38`), and so do the v3 docs
  (`docs/app/docs/v3/analytics/storage-modes/page.mdx`: "**ducklake** (default)").
- `aggregates/analytics-store/README.md` still says `parquet` is the default.

So a deployment that sets only `yaci.store.analytics.enabled=true`, without the shipped profile,
runs `parquet` today. If the code default is flipped naively, such a deployment silently becomes
a new, empty DuckLake on the next upgrade (see C5).

### C2. Re-exporting a DuckLake partition duplicates rows

`DuckLakeWriterService.exportWithDuckLake` always runs `INSERT INTO`. The admin API even states
this in its own documentation: "the DuckLake writer is append-only — reset the partition state
first if you intend to re-export", and "with the append-only DuckLake writer a re-export appends
rows" (`AnalyticsAdminController`, `exportDate` and `resetState`). Every path that runs a
partition a second time therefore doubles its rows:

| Path | Why it runs the partition again |
|---|---|
| `DELETE /admin/state/{table}/{partition}`, then an export or the next gap scan | The state row is gone, so the partition counts as missing |
| A crash after the DuckLake commit but before `markCompleted` | The state stays `IN_PROGRESS`. Gap detection treats it as missing, because it is not `COMPLETED`, and it is retried |
| `AbstractTableExporter` sees `COMPLETED`, but `verifyExport` returns false | It logs "files missing on disk, re-exporting" and appends |
| State lost, or the `analytics_export_state` table truncated | Every partition looks missing |
| `verifyExport` fails after the commit, or `markCompleted` throws (r2) | `markFailed`, then a retry, then an append |

A local, unmerged draft (ADR-014, 2026-04-11, `adr/014-ducklake-idempotent-exports.md`, not on
any branch) reported "exactly 2x duplicate rows" on 2022 partitions after a state reset, and
balances doubled in the MCP `analytics-top-balances` tool. It proposed `DELETE` then `INSERT`
as two separate statements. This ADR takes over that proposal and makes it atomic (D6).

### C3. The admin reset endpoints are wrong or unsafe

- **`DELETE /state/{table}/range` never matches anything.** State keys are
  `PartitionValue.toPathSegment()`, for example `date=2024-01-15`.
  `ExportStateAdminService.resetDateRange` compares that key with the bare `startDate`/`endDate`
  strings (`2024-01-01` ... `2024-01-31`), and `"date=…"` sorts after every digit. The JShell
  probe `"date=2024-01-15".compareTo("2024-01-01") >= 0 && "date=2024-01-15".compareTo("2024-01-31") <= 0`
  returns `false`, so the endpoint answers "Reset 0 partition states" for the documented
  `yyyy-MM-dd` format. Only bounds that the caller happens to prefix with `date=` match, because
  the parameters are plain strings. It also loads the whole
  state table with `findAll()`. No test covers either reset method
  (`grep -rn 'resetDateRange\|resetExportState'` finds only the controller and the service).
- **`DELETE /state/{table}/{partition}` returns 200 "State reset successfully"** when the key does
  not exist (for example a bare `2024-01-15`, which is the format its own Javadoc shows), and when
  the table is unknown.
- **No endpoint deletes data.** Re-creating a partition takes two calls (a reset, then an export).
  Between them the query layer has no completed state for that partition. In DuckLake mode the
  second call appends (C2).
- **No finality guard on manual daily exports.** (r2: for epoch tables, each exporter's
  `preExportValidation` applies on both paths; see D11 for where it is wrong.) `ContinuousSyncScheduler` bounds automatic exports by
  `GapDetectionService.getExportEndDate()` (the finalized tip minus `buffer-days`). The admin
  `export/date`, `export/table/.../date`, `.../range` and `.../epoch` endpoints call
  `exportForPartition` directly, with no bound. An operator can export today's partially synced
  day, or a future day, and it is marked `COMPLETED` with partial data. After that it is never
  re-exported automatically, and the query layer's federation boundary
  (`CutoffSlotResolver`, which is built from the completed partitions) serves the partial data as
  history.
- **No concurrency guard.** `markInProgress` overwrites an existing `IN_PROGRESS` row. Any of
  these can run the same partition at the same time: an admin export on an HTTP thread, the
  scheduled `syncGaps`, or `POST /sync/trigger` (which calls `syncGaps()` on the HTTP thread).
  `DuckLakeWriterLock` serializes only the write itself. Both callers insert, so the partition
  ends up duplicated.
- **Errors are unclear.** An unknown table name throws `IllegalArgumentException` from
  `TableExporterRegistry.getExporter` and becomes a 500. Reversed or unbounded ranges are
  accepted, and a range runs synchronously on the request thread.

### C4. A DuckLake DELETE is safe for the query layer only at partition granularity

The query layer never attaches the catalog. `DuckLakeCatalogSnapshotReader` lists the committed
data files with `ducklake_list_files()`, and `ParquetReadConnectionProvider` builds views over
`read_parquet([files])`. It **cannot apply DuckLake delete files**; it only logs a warning when
it sees one. Probes on 1.5.5 (Appendix A, P1) show:

- `BEGIN; DELETE FROM t WHERE date = X; INSERT INTO t …(date = X)…; COMMIT;` commits as **one**
  snapshot (`{tables_inserted_into=[1], tables_deleted_from=[1]}`). The old data file of `X`
  disappears from `ducklake_list_files`, the new one appears, and **no delete file** is written.
- `DELETE … WHERE date = X AND id < 10` (part of a file) writes a positional delete file
  (`…-delete.parquet`). The query layer would ignore it and still return the deleted rows.
- The replaced file **stays on disk** until snapshots are expired and old files are cleaned up.
  DuckLake does not do that on its own.

That last point weakens `DuckLakeWriterService.verifyExport`. It only checks that
`<export>/main/<table>/<partition>/` contains *some* file ending in `.parquet`. After a replace,
a retired file or a `-delete.parquet` satisfies that check, so it no longer proves that the
committed snapshot has the data. (r2: r1 said a 0-row replace "passes" the check, which was
moot, because the check is skipped when the row count is 0, `AbstractTableExporter:127`.) With
data inlining on, a whole-partition delete is not even file-level (review probe p8; D6).

### C5. Switching modes leaves the federation boundary pointing at nothing

`analytics_export_state` (`V0_1500_1__create_export_state.sql`) does not record which storage
mode wrote a partition. After a switch from `parquet` to `ducklake`, every historic partition is
still `COMPLETED`, so:

- `GapDetectionService` never schedules those partitions.
- `CutoffSlotResolver` puts them on the "exported" side of the boundary, so unified views read
  them from a DuckLake catalog that is empty.

`AbstractTableExporter` re-checks `verifyExport` only when a partition is explicitly exported
again. This is the silent-data-loss path that a naive default flip (C1) would trigger.

### C6. Schema drift breaks existing DuckLake tables

A DuckLake table is created once, from the exporter's first query (`CREATE TABLE … AS … LIMIT 0`).
Later inserts are positional. Commit `b2f6e2508` ("export tx_index in assets analytics
exporter", first in tag `v3.0.0-beta6-dev1`) adds `a.tx_index` to `AssetsExporter`. On any
deployment whose `assets` table already exists, the next insert fails:
`Binder Error: table assets has 3 columns but 4 values were supplied` (P3 reproduces it). The
`parquet` mode had the same class of problem, and commit `01955711c` worked around it by dropping
`sr.deposit` from `StakeRegistrationExporter`.

On the read side, `read_parquet([...])` without `union_by_name` takes its schema from the
**first** file (P4):

- If the old files come first, the new column is silently dropped.
- If a new file comes first, the read fails with `schema mismatch in glob: column "x" … could
  not be found in file "old.parquet"`.

### C7. The DuckDB version is a migration boundary

DuckDB extensions are built per DuckDB version. `INSTALL ducklake` and `INSTALL postgres_scanner`
run at startup (`DuckDbConnectionHelper.installDuckLake` and `installPostgresScanner`, and
`DuckDbCatalogPreInitializer`) and download from the default repository on first use. The
read-write catalog attach already passes `AUTOMATIC_MIGRATION true`, and its Javadoc warns that
"Migration is one-way — an older DuckDB/DuckLake can no longer open the migrated catalog — so
back up the catalog before upgrading DuckDB." Today nothing takes that backup, and nothing
reports which catalog version was found. The docs also tell operators to query the catalog from
their own DuckDB CLI (`README.md`, "Installing DuckDB CLI"). An operator CLI older than the
application will stop opening the catalog once the application migrates it.

**DuckDB 2.x facts.** These were fetched on 2026-11-10 and are pinned in
[References](#references) (R1–R8).

- **Releases.** DuckDB **2.0.0** came out on 2026-10-21 and is an LTS release, supported "until
  at least October 2027". Support for 1.5 was extended "until March 2027" (R1). On Maven Central,
  `duckdb_jdbc` **2.0.0.1** (2026-10-31) is the latest release, and **1.5.6.0** is the latest
  1.5.x (R4).
- **DuckLake versions.**
  - DuckDB 1.5.2 and later ship DuckLake **1.0**; 1.5.0 and 1.5.1 shipped 0.4 (R9). Our 1.5.5
    catalogs report `version = 1.0`.
  - DuckDB 2.0 ships DuckLake **1.1**. Its extension "keeps reading and writing DuckLake 1.0
    catalogs… Migrating is opt-in": migration happens only with `ATTACH … (AUTOMATIC_MIGRATION true)`.
    "A migrated catalog cannot be downgraded."
  - Catalogs from before 1.0, and `1.1-dev1` catalogs, "require migration even for read-only
    use". `ducklake_default_version` must be reset before migrating (R3).
  - DuckLake 1.1 can write delete files as Puffin deletion vectors. This is experimental and off
    by default (R3).
- **This repository's history.** It shipped `duckdb_jdbc` 1.4.1.0 and 1.4.4.0 in `v2.1.0-pre*`
  and `v3.0.0-beta1`…`beta3`, and 1.5.5.0 from `v3.0.0-beta4` on (`git tag --contains 49915668a`
  and `412a9c6ca`). A deployment that jumps from beta3 or earlier straight to the 2.x release
  therefore has a pre-1.0 catalog. **Today's read-write attach passes `AUTOMATIC_MIGRATION true`
  unconditionally, so on 2.x it would silently and irreversibly move every catalog to 1.1.**
- **The DuckDB-file catalog has a stricter limit** (R3): "DuckLake opens a DuckDB metadata file
  with the latest storage version of the running DuckDB, so any write from a newer DuckDB release
  can make the file unreadable by older DuckDB clients." Older engines fail with "Trying to read
  a database file with version number 69, but we can only read versions between 64 and 68" (R2).
  So with `catalog-type=duckdb`, the first export on 2.x ends the possibility of a binary
  rollback, whatever the DuckLake spec version.
- **ICU is no longer bundled** in 2.0 (R2). On 1.5.5 it is `STATICALLY_LINKED`
  (`duckdb_extensions()`), and the `timezone(VARCHAR, TIMESTAMP WITH TIME ZONE)` overload that
  `DuckLakeWriterService` uses to derive `date` (`CAST(timezone('UTC', <col>) AS DATE)`) comes
  from it. On 2.x that expression needs ICU to be auto-installed at runtime, which is a network
  download on first use.
- **Other 2.0 behaviour changes that matter here** (R2, except the extension items, which are
  R5):
  - "any error inside an explicit transaction aborts the transaction";
  - "An untyped `NULL` now keeps the `NULL` type instead of defaulting to `INTEGER`" (this affects
    `CREATE TABLE … AS` on a custom exporter query that selects `NULL AS x`);
  - integer division or modulo by zero now throws instead of returning NULL;
  - a new PEG parser;
  - `~` is now a partial regex match;
  - the extension directory is per version, `~/.duckdb/extensions/<version>/<platform>/`, so an
    offline mirror needs a new set. 2.0 deprecates `extension_directory` in favour of
    `extension_directories` (R5).
- **JDBC jar** (R6). The artifact name is unchanged and the bytecode is still Java 8. The default
  jar now bundles `macos_arm64` instead of the macOS universal binary, so **Intel macOS needs the
  `macos_amd64` classifier**, and "DuckDB 2.1 will stop supporting `osx_amd64`". Version 2.0.0.0
  also changed JDBC behaviour: `getResultSet` auto-closes, batch failures report
  `BatchUpdateException`, and SQL exceptions carry an error code and SQL state.

## Decision

### D1 — Upgrade to DuckDB 2.0.0.1, pinned exactly, with ICU-independent SQL

- **Pin.** Bump `duckdb-jdbc` from `1.5.5.0` to exactly **`2.0.0.1`**, never a range.
  `analytics-store` and `analytics-query` share the catalog entry, so both move together.
- **Platforms (r2).** The default jar has no Intel macOS library (R6). That affects users of the
  platform-independent distribution and of `java -jar`, not only contributors.
  - The distribution and the starter detect an unsupported platform and fail the analytics store
    at startup with a clear message ("DuckDB 2.x has no bundled library for macOS x86_64; add
    `org.duckdb:duckdb_jdbc:2.0.0.1:macos_amd64` or run on arm64/Linux"), instead of an
    `UnsatisfiedLinkError`. The release notes say the same.
  - The native `all` builds include `analytics-spring-boot-starter` (`applications/all/build.gradle:32`),
    but the repository has no native-image configuration for DuckDB's JNI library.
    `find . -path '*native-image*' -name '*.json' | xargs grep -il duckdb` finds nothing. M0
    establishes whether the analytics store works in the native images at all. If it does not,
    it is documented as JVM-only and the native starter disables it with a clear message.
- **No ICU on any path (r2).** DuckDB 2.0 does not bundle ICU (R2), and today **two** expressions
  depend on it:
  - the writer derives `date` with `CAST(timezone('UTC', <col>) AS DATE)` (`DuckLakeWriterService`);
  - the query layer federates with `CAST(to_timestamp(x) AT TIME ZONE 'UTC' AS DATE)`
    (`UnifiedViewBuilder.java:210`). This runs after the sandbox lockdown has turned off
    extension autoload and autoinstall, so on 2.x the views would fail to bind.

  For the writer, D6 now injects the partition value as a literal, so no expression is needed
  there. For the query layer, the expression is rewritten without ICU, or `icu` is loaded
  explicitly before `lockDown`; M0 picks one. ICU **is** loaded explicitly in the query layer
  before lockdown in any case, so that users' ad-hoc and MCP SQL that relies on `TIMESTAMPTZ` and
  `TimeZone` behaves as it did on 1.5.
- **Extensions (r2).** Load `ducklake`, `postgres_scanner` (`postgres`) and, in the query layer,
  `icu` explicitly for the running engine version. M0 lists which extensions 2.0 still builds in;
  `parquet` is not assumed. For air-gapped and container deployments, pre-seeding works through
  `extension_directories`, the 2.0 replacement for the deprecated `extension_directory` (R5). A
  new property, `yaci.store.analytics.duckdb.extension-directories`, is applied to the writer and
  to the query layer's parent connection before lockdown. The Docker image pre-installs the
  extensions in a per-architecture build step (`linux/amd64` and `linux/arm64`, as in the buildx
  release), so the first start of an upgraded container needs no network access.
- **The 2.0 behaviour changes the code depends on are pinned or tested.**
  - The D6 transaction relies on "an error aborts the transaction", and rolls back explicitly
    anyway.
  - The D8 table creation casts any column of type `NULL` to `VARCHAR` and logs a WARN (R2,
    untyped `NULL`; DuckLake 1.0 cannot store the `NULL` type, R3).
  - Every built-in exporter query and the sample custom exporters
    (`config/application-custom-exporters.yml`) are parsed and run on 2.x in M0. This covers the
    PEG parser, the division-by-zero error and `~`.
- **JDBC impact (r2): none found.** A grep finds no `getResultSet`, `executeBatch` or
  `getMoreResults` use in `aggregates/` or `components/`. The only SQL-state dependency is the
  query timeout check `"57014".equals(e.getSQLState())` (`AnalyticsQueryExecutor.java:256`);
  2.0 now fills in SQL states (R6), so M1 adds a test for timeout classification.

### D2 — Catalog upgrade: no implicit migration, a full recovery point, and reporting

This D2 has two separate parts: the DuckLake **spec version** (0.x, 1.0, 1.1), which changes only
when the catalog is migrated, and, for a file catalog, the **DuckDB storage version**, which
changes on the first write by a newer engine (R3).

1. **No implicit migration.** `duckLakeAttachOptions` no longer adds `AUTOMATIC_MIGRATION true`.
   The new property `yaci.store.analytics.ducklake.catalog-migration` takes one of:
   - `required-only` (the default): migrate only a catalog that 2.x cannot read without migrating
     (pre-1.0 and `1.1-dev1`, R3). In practice that is a deployment coming from beta1–beta3. M0
     establishes which spec version this produces, and whether 2.x can migrate a 1.4.x (0.3)
     catalog at all. If it cannot, such deployments must step through 1.5.5 first, and the
     upgrade guide says so.
   - `latest`: also migrate 1.0 to 1.1. `RESET ducklake_default_version` runs first (R3).
   - `never`: refuse to start rather than migrate.

   New catalogs are created at the spec chosen in Q7. `DUCKLAKE_VERSION` is passed at both
   creation points: the `ATTACH` in `DuckDbCatalogPreInitializer` (`:99-103`, which has no options
   today), and the first read-write attach of an empty PostgreSQL catalog.
2. **Detect without touching.** Read the spec version with a plain metadata query: for a
   PostgreSQL catalog, `SELECT value FROM ducklake_metadata WHERE key = 'version'` through
   postgres_scanner; for a file catalog, a read-only attach as a plain DuckDB database, not
   through `ducklake:`. This also works for pre-1.0 catalogs, which cannot be attached read-only
   through DuckLake (R3). A file catalog's storage version is read from its header. Log
   `found spec X / storage S; engine DuckDB V, DuckLake Y; action: …`, and expose the same values
   in `GET /admin/status`.
3. **A recovery point covers the catalog, the data files and the state (r2).** Restoring only the
   catalog would leave `analytics_export_state` claiming partitions that the restored catalog
   does not have (C5 again), and D9 could already have deleted the files that the old catalog
   references. R3 itself says to back up "catalog and data files". So, before any one-way step (a
   spec migration, or the first 2.x write to an older file catalog):
   - **Catalog.**
     - File catalog: copy the `.db` **and** any `.wal` left by an unclean shutdown to
       `<catalog-path>.pre-<engine>.bak[.wal]`. Checkpointing first is not an option: it would
       need the new engine, which upgrades the file.
     - PostgreSQL catalog: copy every `ducklake_*` table into a backup schema
       `ducklake_backup_<spec>_<yyyyMMdd>` with `CREATE TABLE … AS`. These tables are metadata
       only, so the copy is small, and it gives a PostgreSQL deployment the same automatic
       protection as a file deployment.
   - **State.** Copy `analytics_export_state` into the same backup location, and record the
     recovery point: the time, the catalog snapshot id, and the engine and spec versions.
   - **Data files.** Open a **rollback window** (`ducklake.rollback-window`, default 14 days)
     during which D9 deletes no files. Data files are immutable and are only ever added or
     retired, so the files the backup references stay on disk for as long as the window is open.

   If any step fails, the analytics store does not start. The upgrade guide gives the restore
   procedure:
   1. stop;
   2. restore the catalog and the state from the backup;
   3. start the previous release.

   Files written after the recovery point are orphans, which `ducklake_delete_orphaned_files`
   removes later.
4. **Refuse a catalog newer than the engine**, for example after a binary rollback without a
   restore, with a clear message. Do not attach.
5. **PostgreSQL ownership.** A migration must run as the owner of the metadata tables (R3). A
   permission failure is reported as such, not as a generic attach error.

The upgrade guide, and the docs right away (r2: `querying-data/page.mdx:46` currently says "at
least as new as"), state that operators' own DuckDB clients must use **the same minor version**
as the application. A newer client writing to a file catalog makes it unreadable to the
application (R3); an older one cannot read a migrated catalog.

### D3 — DuckLake becomes the default, without silently abandoning legacy data

- **The resolved mode is authoritative everywhere, decided in two phases (r2).** A bean cannot
  gate `@ConditionalOnProperty`: conditions are evaluated before any bean exists.
  - **Phase 1, before the context.** An `EnvironmentPostProcessor` (`StorageModeEnvironmentPostProcessor`)
    resolves the mode from configuration plus **file-system evidence only**, and writes
    `yaci.store.analytics.storage.resolved-type` into the environment. The evidence is any
    `<export-path>/*/(date|epoch)=*/data.parquet`, any table directory, so it needs no exporter
    registry, and the catalog file. All six storage-gated beans switch to that property:
    - `ParquetWriterService:35`
    - `DuckLakeWriterService:40`
    - `DuckLakeCatalogInitializer:31`
    - `DuckLakeCatalogSnapshotReader:50`
    - `DuckDbCatalogPreInitializer:42`
    - `DuckLakeWriterLock:11`

    So do `ParquetTableRegistry.isDuckLake()` (`:167`) and `CutoffSlotResolver` in analytics-query.
  - **Phase 2, after Flyway and before any export or view refresh.** A validator checks the
    evidence that needs the database: state-row provenance (D5) and a PostgreSQL catalog's
    `ducklake_metadata`. If that contradicts phase 1 (for example, the type is unset, phase 1
    chose DuckLake, but `PARQUET` state rows exist), the analytics store stops with a message
    telling the operator to set `storage.type`. The scheduler and the query layer's first refresh
    wait for the validator.

  Application-context tests cover every row of the table below, including an unset type.
- **Resolution:**

  | `storage.type` | Legacy Parquet evidence¹ | DuckLake catalog present² | Effective mode |
  |---|---|---|---|
  | `ducklake` | any | any | DuckLake. D5 applies to any legacy evidence. |
  | `parquet` | any | any | Parquet, with a deprecation WARN (D4) |
  | unset | none | any | **DuckLake** (the new default) |
  | unset | some | no | **Parquet**, with a WARN telling the operator to set the type |
  | unset | some | yes | Refuse to start: the state is ambiguous |

  ¹ Phase 1: a legacy `data.parquet` under the export path. Phase 2 adds any state row whose
  provenance (D5) is `PARQUET`; state rows survive a changed `export-path` or an unmounted
  volume, which files do not.
  ² Phase 1: the catalog file exists. Phase 2: `ducklake_metadata` exists in a PostgreSQL catalog.
- **Catalog type default (r2).** `ducklake.catalog-type` keeps its code default, `postgresql`
  (`AnalyticsStoreProperties:323`). Changing it would move existing explicit-DuckLake deployments
  that rely on that default to an empty file catalog. A deployment that reaches DuckLake through
  the new default therefore gets `ducklake_*` tables in the `public` schema of its yaci-store
  database, unlike the shipped profile, which uses a file catalog. This is logged at startup.
  On PostgreSQL 15 and later, missing `CREATE` permission on `public` is detected up front and
  reported with the fix. See Q8.
- The README is aligned with the v3 docs and the shipped profiles.

### D4 — The `parquet` mode is deprecated, then removed

- **This release.** The mode keeps working. A startup WARN, a `@Deprecated` on
  `ParquetWriterService` and on its properties (`yaci.store.analytics.parquet-export.*`), and a
  "Deprecated" callout in the docs. It gets bug fixes only.
- **The next minor release.** No change, apart from more prominent wording.
- **The first release allowed to break configuration (4.0 by default; Q2).** Remove
  `ParquetWriterService` and the glob path of `ParquetTableRegistry`. `storage.type=parquet` then
  fails at startup with a pointer to D5.

### D5 — Migrating from Parquet: provenance-based state, re-export guarded against pruning

- **Provenance column.** Add `storage_format VARCHAR(16)` to `analytics_export_state`, in Flyway
  migration `V0_1500_3`, shipped in all three dialects (`db/store/{postgresql,mysql,h2}`).
  `V0_1500_2` is taken by `create_address_utxo_flattened_view`.
- **Backfill from evidence, not from configuration (r2).** The r1 rule ("the effective mode of
  the first start") stamped Parquet-written rows as `DUCKLAKE` when an operator set
  `storage.type=ducklake` during the upgrade, which is C5 again. Each existing row is now
  classified by checking first for evidence in the **effective** mode:
  - **Effective mode is DuckLake:**
    - `DUCKLAKE` if the catalog lists committed files for that partition;
    - otherwise `PARQUET` if a legacy `data.parquet` exists or `checksum_sha256 IS NOT NULL`
      (only the Parquet path writes a checksum, `AbstractTableExporter:134-137`);
    - otherwise unknown.
  - **Effective mode is Parquet:** the same checks, in the opposite order.
  - **Unknown:** the row loses `COMPLETED` (it is set to `FAILED` with the reason "provenance
    unknown at upgrade"), so the partition becomes a gap.

  The backfill runs once, before any exporter or query-layer refresh, and logs a count per class.
- These all count only rows whose `storage_format` equals the effective mode:
  - gap detection and `getCompletedPartitions`;
  - `CutoffSlotResolver` (in analytics-query; it reads the resolved mode, D3);
  - the skip check in `AbstractTableExporter.exportForPartition` (`:90-98`), which calls `getState`
    directly.

  After a switch, every legacy partition is a gap. A deployment that **already** switched from
  Parquet to DuckLake in an earlier release has Parquet-era rows with no DuckLake files, and the
  evidence rule classifies those as `PARQUET`, so they too become gaps instead of phantom
  `COMPLETED` rows.
- **Pruned sources (r2).** Re-exporting needs the rows to still be in PostgreSQL. Pruning
  removes exactly the history of some exported tables:
  - `store.adapot.epoch-stake-pruning-enabled` (keeps 8 epochs by default,
    `AdaPotProperties.epochStakePruningSafeEpochs`);
  - `store.adapot.reward-pruning-enabled`;
  - `store.utxo.pruning-enabled`;
  - `store.account.pruning-enabled`.

  When the effective mode is DuckLake, there is legacy Parquet evidence for a table, and pruning
  is enabled for that table's source, the switch **refuses to start**. The message names the
  tables and the options: keep `parquet` for now; set
  `migration.accept-pruned-history-loss=true`; or use the Q3 fast path once it exists. Q3 is
  raised from "defer" to "needed for pruned deployments".
- **During the migration (r2).** Partitions that are not yet re-exported are not `COMPLETED`
  under the new mode. So:
  - **With `query.live-data-enabled=true`**, the boundary keeps them on the PostgreSQL side.
  - **Without it** (the default, `AnalyticsStoreProperties:194`), the historical views have no
    such fallback and return only what has been migrated so far. `GET /admin/status` reports the
    progress (partitions done out of total, per table), and the docs say so.
  - **Views exist before any export lands.** D8 creates every enabled table in the catalog at
    startup, from the exporter's query with `LIMIT 0`, so the query layer can build empty views
    from catalog columns from the first refresh. Today a table appears only after its first
    export.
- The legacy files stay where they are. The admin status reports them, and the upgrade guide
  gives the cleanup step. DuckLake writes under `<export-path>/main/…`, so the two layouts do not
  collide.

### D6 — A DuckLake partition is replaced atomically, with verified preconditions

`DuckLakeWriterService.export(PartitionValue, query)` replaces one partition, on the single writer
connection and under `DuckLakeWriterLock`:

```sql
-- illustrative
BEGIN;
DELETE FROM <table> WHERE <date|epoch> = <literal>;          -- the partition key only
INSERT INTO <table> BY NAME
  SELECT *, DATE '<yyyy-MM-dd>' AS date FROM (<query>);      -- daily; epoch tables carry `epoch`
-- post-checks (below), then
COMMIT;
```

- **The partition value is a literal (r2).** It comes from `PartitionValue`. It no longer comes
  from row data such as `CAST(timezone('UTC', col) AS DATE)` or from the output path. This
  matters in three ways:
  - Every inserted row carries the partition value whose `DELETE` will later remove it.
    Today, a row whose time column is NULL gets `to_timestamp(COALESCE(x, 0))` and lands in
    `date=1970-01-01`, where no replace of its real partition ever deletes it, so every replace
    duplicates it.
  - The write path no longer needs ICU (D1).
  - The query is already bounded by the partition's slot range, so the literal is the true
    partition.

  For epoch tables, a check inside the transaction asserts `count(*) WHERE epoch <> <literal>` = 0.
  Rows already sitting in `date=1970-01-01` from earlier releases are reported by
  `GET /admin/status`; cleaning them up is an operator step in the upgrade guide.
- **Preconditions, checked inside the transaction before the `DELETE` (r2).** If any fails, the
  writer refuses and rolls back:
  - `data_inlining_row_limit` is `0` in the catalog. Probe p8 (Appendix A) shows that with
    inlining on, a "whole-partition" `DELETE` is stored as an inlined delete. The old file stays
    listed and the query layer reads both. Setting the limit at startup becomes mandatory instead
    of best effort (`DuckDbConnectionHelper:746` logs a failure at debug today), and a failure to
    set it stops the analytics store.
  - The table is partitioned on exactly the expected key. Today `CREATE TABLE … AS … LIMIT 0` and
    `ALTER TABLE … SET PARTITIONED BY` are separate statements. A crash between them leaves an
    unpartitioned table that later exports never repair, because the table already exists.
    Creation and partitioning now run in one transaction. An existing table without the right
    partitioning is reported, and replace is refused for it until the operator rebuilds it.
    Probe p11 shows that one-value files written before partitioning are dropped cleanly, but
    p12 shows that mixed-value files are not.
- **Post-checks, before `COMMIT`.** In the transaction's view of the catalog:
  - no file that existed before the transaction is still listed for this partition;
  - no `delete_file` for the table;
  - no rows in any `ducklake_inlined_delete_*` table for it;
  - no deletion vector (Puffin) file, which DuckLake 1.1 can produce (R3; experimental and off
    by default).

  If any check fails, the transaction rolls back and the export is `FAILED`. This makes I2 an
  enforced property rather than a consequence of the predicate.
- **Shrink guard (r2).** If the partition currently holds `n > 0` rows and the new insert holds
  fewer than `n`, the replace rolls back, unless the call carries an explicit `allow-shrink`
  (admin only, D7). A pruned PostgreSQL (D5) would otherwise turn every "re-export" (a `force`,
  lost state, a failed verify, a range reset) into a silent loss of history. Before D6 those paths
  duplicated rows; with D6 alone they would quietly destroy them. Automatic paths never pass
  `allow-shrink`. A shrink stays `FAILED` and keeps the old data and the old state (D10).
- **Verification.** The row count comes from the `INSERT`. `verifyExport` counts in the committed
  snapshot (`SELECT count(*) … WHERE <key> = <literal>`). It does not list the directory, which
  still holds replaced files (C4).
- R7: "a `DELETE` that matches zero rows still records a delete attempt for conflict checking."
  That is harmless with one writer process, but it is one more reason the single-writer
  assumption must be revisited before anyone runs several writers (Related findings).

### D7 — Admin API: explicit delete and re-export, validated, single-flight

| Endpoint | Behaviour |
|---|---|
| `POST /export/table/{t}/date/{d}?force=true`, `…/epoch/{e}?force=true`, `…/range?…&force=true`, `…/epoch-range?…&force=true`, plus `&allow-shrink=true` | Without `force`, unchanged (skip if `COMPLETED`). With `force`, replace the partition through D6, following the D10 protocol. |
| `DELETE /data/{t}/{partition}` (new) | Delete the partition's data and state through the D10 protocol (state first). Allowed for disabled tables that are still registered (r2): disabled means "not exported", not "unknown". |
| `DELETE /state/{t}/{partition}` | Normalizes `2024-01-15`, `date=2024-01-15`, `450` and `epoch=450` through `PartitionValue`. Returns 404 for an unknown table or partition. Takes the partition lock and returns 409 for a partition whose operation is in flight (r2). The response says the data is untouched. |
| `DELETE /state/{t}/range` | Normalizes keys. Uses a bounded repository query instead of `findAll()`. Adds an `epoch-range` variant. Same locking as the single reset. |

Rules for every export or delete endpoint:

- **Validation.** An unknown table gives 404. A partition type that does not match the table's
  strategy, or `start > end`, gives 400. A range larger than `admin.max-range-partitions`
  (default 366 days or 73 epochs) gives 400.
- **Readiness.** Every endpoint uses the shared readiness check of D11. A partition that is not
  ready gives 409 with `rejected-unfinalized` or `rejected-dependency`. There is no override.
- **Single flight.** It is enforced by D10's attempt protocol, not by status alone. A
  concurrent call on the same partition gets 409. `POST /sync/trigger` returns 409 while a sync
  is running (an `AtomicBoolean` shared with the scheduled run).
- **Long ranges** stay synchronous and bounded by the range cap (Q4).
- **Responses** report the outcome per partition: `exported`, `skipped-completed`,
  `rejected-unfinalized`, `rejected-dependency`, `rejected-shrink`, `failed: …`.

### D8 — Schema evolution is additive and explicit

- **Before each insert**, compare the query's columns (`DESCRIBE <query>`) with the table's
  columns (`duckdb_columns()`):
  - **New column**: a column only in the query gets `ALTER TABLE … ADD COLUMN`, in the D6
    transaction.
  - **Dropped column**: a column only in the table is filled with NULL by `BY NAME`.
  - **Type change**: the export fails, naming the column, the old type and the new type.
  - **`NULL`-typed column**: created as `VARCHAR` (D1).

  P3 confirms that `ADD COLUMN` followed by `INSERT … BY NAME` works on 1.5.5. M0 confirms it on
  2.x.
- **Tables exist from startup (r2, D5).** Every enabled exporter's table is created and
  partitioned at startup, from its query with `LIMIT 0`, in one transaction.
- **Read side.**
  - DuckLake views use `read_parquet([...], union_by_name = true)` (P4). They then project the
    catalog columns in catalog order, using `CAST(NULL AS <type>) AS <col>` for a catalog column
    that no file has yet (r2). Projecting such a column directly fails with
    `Binder Error: Referenced column … not found`, for example after an `ADD COLUMN` with a 0-row
    insert.
  - On 1.5.5, `union_by_name` together with `hive_partitioning` and `hive_types` works (review
    probe p10). M0 repeats this on 2.x.
- This fixes the `tx_index` breakage (C6) without a manual step.

### D9 — Catalog maintenance that respects readers and the rollback window

Replaced and deleted files stay on disk until snapshots expire and old files are cleaned up
(C4). A maintenance task runs under `DuckLakeWriterLock`, on a configurable interval (default
daily). It does nothing while D2's rollback window is open.

- **Reader leases (r2).** The r1 rationale ("retention much longer than the 5-minute refresh")
  was wrong on two counts:
  - `expire_snapshots` works on snapshot **age**. After ten idle days, a replace makes the
    previous snapshot expirable at once, and `cleanup_all` then deleted the replaced file seconds
    later (review probe p9).
  - Views are not refreshed every 5 minutes for certain. The snapshot read is deferred while the
    writer is busy, and a failed refresh keeps the old views.

  So the query layer records the **snapshot id** behind each published view set (D12) in an
  in-JVM registry. Maintenance:
  - expires only snapshots older than both the retention (default 7 days) and the oldest snapshot
    any published view set still uses;
  - then runs `ducklake_cleanup_old_files(older_than => now() - retention)`, never with
    `cleanup_all`. Probe p13 shows that `older_than` keeps recently retired files.
- **Compaction** (`ducklake_merge_adjacent_files`) is not enabled (Q5). It can merge files across
  partition values, which would break D6's whole-file premise (I2, probe p12).

### D10 — Two systems, one protocol: state and catalog changes are ordered and reconciled (r2)

`analytics_export_state` (PostgreSQL, through JPA) and the DuckLake catalog commit separately.
r1 left the gap between them undefined. The reviews found three failures in that gap:

- `DELETE /data` deleted data before state, so a crash left a `COMPLETED` row over no data,
  permanently.
- A `force` collapsed the federation boundary for the whole table, and a rolled-back force left
  it collapsed.
- `markCompleted` re-inserted a row that a concurrent `DELETE /state` had removed.

New columns, in the same migration as D5:
- `attempt_id UUID`;
- `pending_op VARCHAR(16)` (`NULL`, `EXPORT`, `REPLACE` or `DELETE`);
- `catalog_snapshot_id BIGINT`;
- `prev_status VARCHAR(20)` and `prev_row_count BIGINT`.

Every operation follows the same steps:

1. **Claim.** One atomic conditional update:
   `UPDATE … SET attempt_id = :new, pending_op = :op, prev_status = export_status, prev_row_count = row_count WHERE pk AND pending_op IS NULL`,
   or the matching insert for a new partition. Zero rows updated means another operation owns
   the partition: the admin gets 409, the scheduler skips it. The **status is not changed** by a
   `REPLACE`: a `COMPLETED` partition stays `COMPLETED` (and inside the boundary) while it is
   being replaced, because D6 keeps the old data visible until the new data commits. For a
   `DELETE` the status becomes `DELETING`, so the partition leaves the boundary **before** the
   data goes.
2. **Catalog work.** The D6 transaction, or the delete. DuckLake `commit_extra_info`, if M0
   confirms it is available, records the `attempt_id`; otherwise the snapshot id read straight
   after the commit is used.
3. **Finish.** A conditional update `WHERE pk AND attempt_id = :mine`:
   - success: write the status, counts and `catalog_snapshot_id`, and clear `pending_op`;
   - rollback: restore `prev_status` and `prev_row_count`, store the error in `error_message`,
     and clear `pending_op`. A failed force therefore leaves the partition `COMPLETED` with its
     old data and its old boundary.

   Zero rows updated means the row was taken over or removed; this is logged, and nothing is
   re-inserted.

**Startup reconciler.** There is one writer process, so at startup every row with
`pending_op IS NOT NULL` is orphaned. This replaces the 60-minute stale timeout for this purpose;
the timeout also wrongly reaps exports that take longer than an hour. For each orphan:

- **`DELETE`:** run the data delete again (it is idempotent), then remove the row.
- **`EXPORT` / `REPLACE`:** look in the catalog for a commit carrying the `attempt_id`, or a
  snapshot later than `catalog_snapshot_id` that touches the partition.
  - If there is one, finish as success, with counts taken from the catalog.
  - If not, finish as rollback.

`StaleExportRecoveryService` keeps its periodic sweep only for observability: it logs operations
that have been pending for unusually long.

### D11 — Readiness (finality and dependencies) is one check, for every caller (r2)

r1 put the finality guard on the admin endpoints only. The scheduler path also violates it:

- `GapDetectionService.findMissingEpochExports` loops up to and **including** the current epoch
  (`GapDetectionService.java:234-246`).
- `CustomTableExporter.preExportValidation` passes any epoch when `dependsOnAdapotJob=false`
  (`CustomTableExporter.java:69-72`).
- `AbstractTableExporter.isRewardCalcAdaPotJobCompleted` returns `true` **on an exception** ("allow
  export to prevent permanent blocking").

`TableExporter.isReady(PartitionValue)` becomes the single gate, called inside the export service
for every caller (the scheduler, the admin API and the reconciler):

- **Readiness is defined per exporter, with a safe default (r2, refines r1's blanket rule).**
  Some epoch tables are legitimately final *during* their epoch. `EpochParam`, `AdaPot` and
  `DRepDist` depend on the REWARD_CALC job for epoch N, which completes during epoch N.
  `EpochStake` depends on the job for N+1, and `EpochExporter` requires `epoch < current`
  (`preExportValidation` of each exporter). r1's "epoch ≥ current → 409" would have blocked
  legitimate exports. So:
  - **Daily tables:** ready when the partition is `≤ getExportEndDate()`.
  - **Epoch tables with a declared dependency** (built-in, or a custom exporter with
    `depends-on-adapot-job=true`): ready when the dependency is satisfied.
  - **Epoch tables without one** (for example a custom exporter with
    `depends-on-adapot-job=false`, which today exports the current, incomplete epoch, at
    `CustomTableExporter.java:69-72`): ready when the epoch's last slot is older than the
    finalized tip, using the same finality window and margin as daily exports.
  - Gap detection asks `isReady` instead of scanning up to and including the current epoch.
- **Dependencies fail closed.** An error in a dependency check means "not ready, retry later",
  reported as `rejected-dependency`. It never means "export anyway".

### D12 — The query layer publishes files and boundaries together (r2)

`ParquetReadConnectionProvider.refreshViews` reads the catalog file snapshot (through
`discoverTables`) and only then refreshes the cutoffs from the state database
(`cutoffResolver.refresh()`), lines 148–166. An export that commits and is marked `COMPLETED`
between the two reads moves the boundary past files the views do not have, so the partition is
missing from both sides until the next refresh. A failed historical-view refresh also lets the
cutoff move on. The new order:

1. Read the completed set (the cutoff inputs) **first**.
   - D10 guarantees that a partition is `COMPLETED` only after its catalog commit, so any
     partition in the set has its files in any catalog snapshot read afterwards.
   - A `DELETE` leaves the set (`DELETING`) before its data goes, so a partition whose data is
     missing from the snapshot is never inside the cutoff.
2. Read the catalog snapshot, and record its snapshot id (D9).
3. For each table, compute the cutoff, then intersect it with the partitions present in the
   snapshot. This guards against a cutoff that would cover a partition the snapshot does not
   have.
4. Publish a table's historical view and its unified view **together**. If either fails, keep
   the table's previous pair, previous cutoff and previous lease.

Partitions committed after step 1 but present in the snapshot are filtered out by the cutoff, as
the unified view already does (Parquet `≤` cutoff, PostgreSQL `>` cutoff). They become visible on
the next refresh.

## Invariants

Each invariant is tested in the milestone named in the plan.

- **I1 (no duplicate rows).** After any sequence of exports, forced re-exports, retries, crashes,
  resets and reconciler runs, each `(table, partition)` in the current snapshot holds exactly the
  rows of one successful export of that partition. *D6, D10.*
- **I2 (no partial deletes).** The exporter never commits a delete file, an inlined delete or a
  deletion vector. It is enforced by the D6 post-checks and tested after every operation.
  *D6, D9.*
- **I3 (atomic replace).** Any committed snapshot holds either the complete previous contents of
  a partition or the complete new contents. *D6.*
- **I4 (COMPLETED means present).** A `COMPLETED` row with `storage_format = M` and
  `pending_op IS NULL` implies that the partition's committed data in mode `M` has exactly
  `row_count` rows. *D5, D6, D10.* It is tested by fault injection at every step boundary of
  every D10 operation.
- **I5 (readiness).** No partition outside the readiness check is ever `COMPLETED`, through any
  path, including the scheduler. *D11.*
- **I6 (one operation).** At most one operation per `(table, partition)` is in flight. It is
  enforced through `attempt_id`, not through in-JVM state alone. *D7, D10.*
- **I7 (no silent mode switch).** An upgrade with unchanged configuration never changes the
  effective storage mode of a deployment that has legacy evidence, and no Parquet-written state
  row is ever counted as DuckLake. *D3, D5.*
- **I8 (no unannounced one-way change).**
  - The catalog never migrates beyond what `catalog-migration` allows.
  - Every one-way step is preceded by a complete recovery point: catalog, state, and the file
    retention of the rollback window.
  - A catalog newer than the engine is never attached read-write.

  *D1, D2, D9.*
- **I9 (schema).** An exporter that adds a column never fails on an existing table, and readers
  see NULL for it in older files. *D8.*
- **I10 (ICU-independent dates).** The partition value, and the federation's date mapping, do not
  depend on ICU or the session `TimeZone`. *D1, D6.*
- **I11 (no shrink without consent).** No automatic path replaces a non-empty partition with
  fewer rows. *D6, D7.*
- **I12 (published consistency).** For every published table, every partition inside its cutoff
  has its files in the view's file list, and no file in any published view has been deleted.
  *D9, D12.*

## Alternatives considered

- **Stay on 1.5.x (1.5.6.0).** This defers every one-way step. But 1.5 support ends in March 2027,
  while 2.0 is LTS until at least October 2027 (R1), and DuckLake 1.1 features land only on 2.x.
  Rejected as the end state. D3 to D12 do not depend on 2.x and ship first, on 1.5.5.
- **Let 2.x migrate every catalog to DuckLake 1.1 (the code's behaviour today).** This silently
  breaks operators' 1.5 CLIs and makes a binary rollback impossible (C7). Rejected in favour of
  D2 `required-only`.
- **Back up only the catalog (r1).** A restored catalog contradicts the state table, and D9 can
  delete the files the catalog references. Rejected in favour of D2's full recovery point.
- **Two statements, DELETE then INSERT, outside a transaction.** This is the 2026-03 design
  (`1e2ca77a5`, removed by `8d57af85e`, both in PR #858) and the local ADR-014 draft. A crash
  between the statements empties the partition while the state may still say `COMPLETED`.
  Without the D6 preconditions, the delete can also be inlined or partial, which the query layer
  cannot apply. Rejected in favour of D6. Why `8d57af85e` removed it is not recorded (Q9).
- **`INSERT OVERWRITE`, or `MERGE`.**
  - `INSERT OVERWRITE` does not exist (`Parser Error: syntax error at or near "OVERWRITE"` on
    1.5.5).
  - `MERGE INTO … WHEN MATCHED THEN DELETE` is supported (R7), but it is keyed by row. It deletes
    parts of files (I2), and many exported tables have no natural unique key.

  Rejected.
- **Let the query layer attach the catalog and apply deletes.** Blocked by the in-process
  `Unique file handle conflict` (`DuckLakeCatalogSnapshotReader` Javadoc), and it would widen the
  sandbox. Rejected; I2 keeps direct file reads correct.
- **Keep finality on the admin endpoints only (r1).** The scheduler path violates it too.
  Rejected in favour of D11.
- **Use a status flip (`IN_PROGRESS`) as the concurrency guard (r1).** It strands crashed
  partitions behind the 60-minute stale timeout, it does not stop a re-insert after a concurrent
  delete, and a `force` collapses the federation boundary. Rejected in favour of D10.
- **Flip the default with no detection, or drop `parquet` in the same release.** These violate
  I7 or remove the migration window. Rejected.

## Consequences

**Positive**

- Re-exports, retries, crashes and state loss can no longer duplicate rows or silently shrink
  them, and the admin API gets a one-step, atomic "re-create this partition".
- The default matches the docs and profiles, and an upgrade cannot empty the history of a
  Parquet deployment, either silently or through a mislabelled backfill.
- The DuckDB upgrade becomes explicit and logged, and can be rolled back from a complete
  recovery point.
- Epoch partitions are no longer exported before they are final.
- New exporter columns stop breaking existing deployments.

**Negative**

- `analytics_export_state` gains six columns and a reconciler. The state handling becomes a
  small protocol (D10) instead of a status flip.
- A Parquet deployment that migrates re-exports its history from PostgreSQL. That is hours on
  mainnet; M4 measures it. Without live federation, historical queries are incomplete until it
  finishes. A deployment with pruning enabled cannot migrate this way at all (D5, Q3).
- A replace that would shrink a partition fails until an operator decides. Exports on a pruned
  PostgreSQL therefore stop being "fire and forget".
- Each export adds a `DELETE`, the precondition and post-condition checks, and a schema
  comparison: a small, constant overhead per partition.
- Replaced files use disk until maintenance runs, and for the whole rollback window after an
  upgrade (D9, D2).
- Operators must keep their own DuckDB clients on the application's minor version.
- Intel macOS users need an extra jar for analytics on 2.x (D1).

## Implementation plan

Each milestone is one PR, with tests, against `main`. Every milestone's tests include the
invariants it names.

- **M0 — 2.x spike (no production change).** This is the entry gate for M1, and also for M2,
  because it decides whether D6 holds on DuckLake 1.1.
  - Run P1–P4 and review probes p8–p13 on DuckDB 2.0.0.1, against both a 1.0 and a 1.1 catalog,
    including whether DuckLake 1.1 deletion vectors appear.
  - List the extensions built into 2.0.
  - Check `commit_extra_info` availability for D10.
  - Find the migration target and the feasibility of 0.3 (1.4.x) → 2.x.
  - Determine the native-image status.
  - Write the ICU-free date expression for `UnifiedViewBuilder`.
  - Record the results in Appendix A. If any D6 premise fails on 1.1, revise D6 before M2 is
    merged.
- **M2 — Replace, schema, state protocol and readiness (D6, D8, D10, D11).** On 1.5.5.
  - Exit: tests for I1–I6, I9, I10 and I11, including:
    - fault injection at every D10 step boundary;
    - the inlining-on case (probe p8) and the unpartitioned-table case, both refused;
    - the `1970-01-01` NULL-time case;
    - the shrink guard on a pruned source;
    - the current-epoch case for a custom exporter without a dependency, on the scheduler path;
    - a dependency check that throws;
    - the `tx_index` drift case;
    - `union_by_name` reads with old and new files in both orders, and the projection of a column
      that is in no file yet.

    `DuckLakeWriterServiceVerifyExportTest` is rewritten for catalog-based verification.
- **M3 — Admin API and query-layer publication (D7, D12).**
  - Exit:
    - controller tests for every row of the D7 table and every rule;
    - concurrent admin, scheduler and reset calls on one partition;
    - the range-reset key normalization (the C3 regression);
    - a refresh racing an export and a delete (I12);
    - a failed view refresh leaving the cutoff unchanged.
- **M1 — DuckDB 2.0.0.1 (D1, D2).**
  - Exit:
    - the full analytics test suite, including unified views with live federation, green on
      2.0.0.1;
    - the upgrade matrix: file and PostgreSQL catalogs (Testcontainers) created by 1.4.4.0 and by
      1.5.5.0, started under each `catalog-migration` value, checking the spec version, the
      recovery point, the row counts and the view results;
    - **a rollback rehearsal**: upgrade, export, force-replace, restore the recovery point, run
      1.5.5, and check that the state and catalog agree (I4);
    - a catalog newer than the engine is refused;
    - all exporters run with external access disabled and only pre-seeded extensions;
    - a platform-detection test;
    - timeout SQL-state classification;
    - the docs fix for operators' CLI versions ships with M1 at the latest.
- **M4 — Default, deprecation and migration (D3, D4, D5).**
  - Exit:
    - application-context tests for each row of the D3 table, in both phases;
    - backfill classification tests for each evidence case, including a deployment that
      switched in an earlier release;
    - the pruned-source refusal;
    - migration progress reporting;
    - a mainnet-sized re-export timing on the benchmark VM;
    - the docs, the README and the upgrade guide.
- **M5 — Maintenance (D9).**
  - Exit:
    - tests with aged snapshots (probe p9) showing that nothing a published view uses is
      deleted;
    - no cleanup at all while a rollback window is open;
    - `cleanup_old_files` is always bounded by `older_than`.

**Order:** M0, then M2, then M3 (these fix live correctness bugs and need only 1.5.5), then M1,
then M4 and M5. M4 depends on M2, because the migration re-export must already be idempotent and
shrink-guarded.

## Open questions

- **Q1 — Extension delivery.** Should the Docker image bake in the extensions (per architecture),
  or only document `extension-directories`? Lean: bake them in.
- **Q2 — When to remove `parquet`.** 4.0, or "two minor releases later"? Lean: 4.0, because
  removal breaks configuration.
- **Q3 — A fast migration with `ducklake_add_data_files`.** Raised in r2 from "defer" to "needed
  for pruned deployments" (D5). Its constraints:
  - the legacy files lack the partition column (P2);
  - the query layer needs a uniform layout;
  - the files may predate schema fixes such as `01955711c`.

  Lean: a follow-up ADR once M4 lands.
- **Q4 — An asynchronous admin job API for long ranges.** Lean: defer; the D7 range cap bounds
  request time.
- **Q5 — Compaction.** Lean: a separate ADR. Any compaction must preserve one partition value
  per file (I2).
- **Q6 — Removed in r2.** It asked about an automatic PostgreSQL catalog backup, which D2 step 3
  now does (it copies the metadata tables into a backup schema).
- **Q7 — The spec version of new catalogs.** 1.1 (the 2.x default), or 1.0 for one release? Lean:
  1.0 for one release. This helps only PostgreSQL catalogs, which stay readable by operators'
  1.5.2+ CLIs. A file catalog becomes unreadable to 1.5 after the first 2.x write whatever its
  spec (R3).
- **Q8 — The catalog-type default for new deployments that reach DuckLake by default.** Keep
  `postgresql`, which reuses the yaci-store database and its `public` schema, or default those
  deployments to `duckdb`, as the shipped profile does? Lean: keep `postgresql`, because changing
  it would move existing explicit-DuckLake deployments to an empty file catalog. Document it and
  check the PostgreSQL 15+ permissions.
- **Q9 — Why was delete-before-insert removed in PR #858 (`8d57af85e`)?** Neither the commit nor
  the PR records a reason. Plausible causes:
  - inlined or partial deletes that the query layer could not apply, which D6's preconditions and
    post-checks now cover;
  - cost on large partitions.

  A maintainer who remembers the reason should confirm it.

## Related findings (out of scope)

- The DuckLake `DATA_PATH` is stored relative to the JVM working directory (`./data/analytics`).
  Changing `export-path` or the working directory across restarts is not detected.
- The docs claim multi-instance support with a PostgreSQL catalog, but the writer design
  (one pooled connection and an in-JVM lock) assumes a single writer process.
- `ExportStateAdminService.getStatistics` also loads the whole state table with `findAll()`.

## References

All were fetched on 2026-11-10.

- **R1.** DuckDB 2.0.0 announcement, 2026-10-21: <https://duckdb.org/2026/10/21/announcing-duckdb-200.html>.
  Release `v2.0.0` on GitHub (`gh api repos/duckdb/duckdb/releases`). It states the LTS status
  and the 1.5 support extension.
- **R2.** DuckDB 2.0 migration guide: <https://duckdb.org/docs/2.0/operations_manual/migration>.
  It covers the storage version 69 error, ICU no longer bundled, transaction invalidation, `NULL`
  typing, division by zero, and the PEG parser.
- **R3.** DuckLake 1.1: <https://ducklake.select/2026/10/21/ducklake-11/>, and
  <https://ducklake.select/docs/stable/duckdb/migrations/upgrading_to_ducklake_1_1>. It covers
  opt-in `AUTOMATIC_MIGRATION`, the one-way migration, pre-1.0 catalogs, the DuckDB-file
  storage-version caveat, the table-owner requirement for PostgreSQL, `1.1-dev1`,
  `RESET ducklake_default_version`, Puffin deletion vectors, the `NULL` type requiring 1.1, and
  `DUCKLAKE_VERSION`.
- **R4.** Maven Central `org.duckdb:duckdb_jdbc` metadata
  (<https://repo1.maven.org/maven2/org/duckdb/duckdb_jdbc/maven-metadata.xml>, `latest` =
  2.0.0.1, last updated 2026-10-31; 1.5.6.0 is the latest 1.5.x).
- **R5.** DuckDB 2.0, installing extensions:
  <https://duckdb.org/docs/2.0/extensions/installing_extensions>. It covers the per-version
  directory, `custom_extension_repository` (the source `INSTALL` downloads from, with signature
  checks), and the deprecation of `extension_directory` in favour of `extension_directories`.
- **R6.** duckdb-java `v2.0.0.0` release notes: <https://github.com/duckdb/duckdb-java/releases/tag/v2.0.0.0>.
  The platform lists come from inspecting the 1.5.5.0 and 2.0.0.1 jars (duckdb-java #946 split
  the macOS binary).
- **R7.** DuckLake docs: transactions (one committed transaction is one snapshot), upserting
  (`MERGE`), and conflict resolution ("A `DELETE` that matches zero rows still records a delete
  attempt for conflict checking"), under <https://ducklake.select/docs/stable/duckdb/>.
- **R8.** Local probes: DuckDB 1.5.5 CLI `d8cdaa33fd`, DuckLake extension `d8a1881e`, catalog spec
  1.0 (Appendix A). **No probe has been run on 2.x yet.** Downloading the 2.0 CLI was not
  possible in this session, so P1 to P4 on 2.x are part of the M0 spike.
- **R9.** The DuckLake/DuckDB compatibility matrix (`_data/compatibility_matrix.csv` in the
  ducklake-web repository): DuckDB 1.5.0 and 1.5.1 ship DuckLake 0.4; 1.5.2 and later ship 1.0.

## Appendix A: probes

DuckDB CLI v1.5.5, DuckLake `d8a1881e`, DuckDB-file catalog, `data_inlining_row_limit = 0` (as
in `DuckDbConnectionHelper.disableDuckLakeDataInlining`).

**P1 — Atomic whole-partition replace drops files; a partial delete writes a delete file.**

```sql
CREATE TABLE t AS SELECT 1 AS id, DATE '2024-01-01' AS date LIMIT 0;
ALTER TABLE t SET PARTITIONED BY (date);
INSERT INTO t SELECT i, DATE '2024-01-01' FROM range(100) r(i);
INSERT INTO t SELECT i, DATE '2024-01-02' FROM range(50) r(i);
BEGIN;
DELETE FROM t WHERE date = DATE '2024-01-01';
INSERT INTO t SELECT i, DATE '2024-01-01' FROM range(100) r(i);
COMMIT;
-- ducklake_snapshots: snapshot 5 = {tables_inserted_into=[1], tables_deleted_from=[1]}
-- ducklake_list_files: date=2024-01-01 now lists only the new file; delete_file NULL for all
-- counts: 2024-01-01 → 100, 2024-01-02 → 50
-- on disk: both 2024-01-01 files remain
DELETE FROM t WHERE date = DATE '2024-01-02' AND id < 10;
-- ducklake_list_files: date=2024-01-02 file now has delete_file …-delete.parquet
```

**P2 — `ducklake_add_data_files` with a Hive path.** A legacy `t/date=2024-01-01/data.parquet`
without a `date` column, registered with `hive_partitioning => true` into a table partitioned by
`date`, gives `SELECT date, count(*)` = `2024-01-01 | 10`.

**P3 — Positional insert breaks on a new column; `BY NAME` with `ADD COLUMN` works.**
`INSERT INTO assets SELECT … (4 columns)` into a 3-column table →
`Binder Error: table assets has 3 columns but 4 values were supplied`.
`ALTER TABLE t ADD COLUMN extra VARCHAR; INSERT INTO t BY NAME SELECT DATE '2024-01-03' AS date, 7 AS id;`
gives `7 | 2024-01-03 | NULL`.

**P4 — `read_parquet` across files with different schemas.** With `old.parquet (a, b)` and
`new.parquet (a, x, b)`:
- old file first: returns `a, b` and silently drops `x`;
- new file first: `schema mismatch in glob: column "x" was read from the original file "new.parquet", but could not be found in file "old.parquet"`;
- `union_by_name = true`: returns `a, b, x`, with `x` NULL for the old row.

**Review probes (r2).** These were run by the reviewers on DuckDB 1.5.5 / DuckLake `d8a1881e`,
in the session scratch directory. Each is cited where it changes a decision.

- **p8 — An inlined delete.** Write 5 rows with `data_inlining_row_limit = 0`, set the limit to
  10, then run `BEGIN; DELETE … WHERE date = X; INSERT …; COMMIT`. The old file stays in
  `ducklake_list_files` with `delete_file` NULL. The delete is stored in
  `ducklake_inlined_delete_1` (5 rows). A reader of the file list sees 55 rows instead of 50.
  The same happens after `ducklake_flush_inlined_data` under the default limit. → D6
  preconditions and post-checks.
- **p9 — Expiry by age.** After 10 idle days, a replace made the previous snapshot expirable at
  once, and `cleanup_old_files(cleanup_all => true)` deleted the replaced file seconds later.
  → D9 leases.
- **p10 — `read_parquet`.** `union_by_name = true` with `hive_partitioning` and `hive_types` works.
  → D8.
- **p11 — Partitioning added later, one value per file.** A file holding one partition value,
  written before `SET PARTITIONED BY`, is dropped whole by a partition `DELETE`.
- **p12 — Mixed-value files.** Files compacted while the table was unpartitioned hold several
  values. A partition `DELETE` on them writes a delete file. → the D6 partition precondition, Q5.
- **p13 — `older_than`.** `ducklake_cleanup_old_files(older_than => now() - INTERVAL 1 HOUR)` keeps
  a file retired seconds ago. → D9.

## Revision history

- **r1** (2026-11-10): initial proposal.
- **r2** (2026-11-10; responds to the pre-PR review of r1 by four independent reviewers:
  code-claim accuracy [CC], upgrade safety [UP], data integrity [DI] and Codex [CX]).
  - **New decisions.**
    - D10, the state and catalog protocol and the reconciler: CX F3, DI F3, F4, F8, CC F5.
    - D11, per-exporter readiness for every caller: CX F6, DI F5, CC F4.
    - D12, consistent query-layer publication: CX F4.
  - **D1.**
    - ICU removed from the write path through the D6 literal, and loaded or removed in the query
      layer (UP F2, CC F3).
    - The Intel macOS and native-image impact (UP F4).
    - Pre-seeding through `extension_directories`, per architecture (UP F7).
    - JDBC: no impact found, plus a SQL-state test (UP F10).
  - **D2.**
    - A full recovery point: catalog plus WAL, the state table and a file-retention window. The
      PostgreSQL metadata is backed up automatically (CX F2, UP F3, F5, CC F9).
    - Detection by plain metadata queries.
    - The `DUCKLAKE_VERSION` creation points.
    - Q6 is withdrawn.
  - **D3.**
    - A two-phase resolver (an EnvironmentPostProcessor, then a post-Flyway validator) that gates
      all six storage beans and the query module (CX F9, CC F2, UP F8).
    - The catalog-type default made explicit as Q8.
  - **D5.**
    - The backfill decided by evidence, not by configuration (CX F1, UP F1, DI F9, CC F10).
    - Flyway `V0_1500_3`, because `V0_1500_2` exists (CC F1).
    - The refusal for pruned sources (DI F2).
    - Behaviour without live federation, and eager table creation (CX F8).
  - **D6.**
    - The partition value is a literal (DI F6a).
    - Mandatory inlining-off, and the partition-spec preconditions (DI F1, CC F6, CX F5).
    - Post-commit checks covering delete files, inlined deletes and deletion vectors.
    - The shrink guard (DI F2).
  - **D7.** Readiness through D11. `DELETE /data` is allowed for disabled tables, and the resets
    take the lock (DI F10, F8).
  - **D8.** `CAST(NULL AS type)` for columns that no file has yet (DI F10).
  - **D9.** Reader leases, `older_than` only, and no cleanup while the rollback window is open
    (CX F7, DI F7, CC F8).
  - **Plan.** The M0 2.x spike is the gate, the milestone order is fixed, and a rollback
    rehearsal is added (UP F9).
  - **Context.** The C2 retry path is corrected and C2 gains two paths; the C3 range-reset nuance
    is added; the C4 verify claim is corrected; the R2/R3 attributions are fixed and R9 is added
    (CC F11, UP reference check).
  - **History.** The removed delete-before-insert of PR #858 is recorded as Q9 (CC F7).
  - **Invariants.** I11 (no shrink) and I12 (published consistency) are added. The invariants and
    questions are reordered.
