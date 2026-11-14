# ADR 0008 — M0 spike results (DuckDB 2.0)

M0 is the entry gate for M1 and M2 in ADR 0008 (PR #1357). It changes no production code. This
directory holds the probe kit and what it found. Every result below was produced by the scripts
next to this file. Run on 2026-11-14 on macOS arm64, with Docker for Linux and PostgreSQL.

## Versions

| Component | Version |
|---|---|
| DuckDB CLI | `v2.0.0 (Cyanoptera) d54cbbe3df`, from the GitHub release `v2.0.0` |
| DuckDB JDBC | `org.duckdb:duckdb_jdbc:2.0.0.1` (Maven Central), engine `v2.0.0 d54cbbe3df` |
| DuckLake extension on 2.0 | `417a9117f5` |
| Baseline | DuckDB `v1.5.5 d8cdaa33fd`, DuckLake `d8a1881e`; pre-1.0 source: DuckDB 1.4.1, DuckLake `f134ad8` (catalog spec 0.3) |
| PostgreSQL catalog | `postgres:17.6-alpine` |
| Native image | Mandrel builder `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25`, `linux/arm64` |

## Summary

1. **D6 holds on 2.0, for both DuckLake 1.0 and 1.1 catalogs, file and PostgreSQL.** A
   whole-partition `DELETE`+`INSERT` in one transaction is one snapshot and retires the old file
   with no delete file (P1). A partial delete still writes a positional `-delete.parquet` (P1b,
   p12). Deletion vectors (Puffin) appear only when opted in (`WRITE_DELETION_VECTORS true`), and
   a whole-partition replace drops the file even then.
2. **The inlining precondition is still required, and the failure mode changed.** With an
   inlining limit above the partition size, 1.5.5 keeps the old file and records an inlined delete
   (a file-list reader sees stale rows). 2.0 retires the old file and inlines the new rows (a
   file-list reader sees 50 of 55 rows). Both diverge from the catalog, so D6's
   `data_inlining_row_limit = 0` precondition and post-check stay.
3. **D10's marker works unchanged on 2.0.** `set_commit_message(…, extra_info => …)` lands in the
   data's snapshot (p14). A no-change transaction records no snapshot (p15). The `error()` key
   guard aborts the transaction and keeps the previous rows (p16).
4. **ICU is still built in. The ADR's "ICU is no longer bundled" is wrong for the official
   builds.** The 2.0 migration guide says 2.0 "removed the dependency on the ICU library"; time
   zones "are now implemented natively by the icu extension", and only custom builds lose it.
   `icu` is `STATICALLY_LINKED` in the CLI and in the JDBC 2.0.0.1 jar on `osx_arm64`,
   `linux_amd64` and `linux_arm64`, and works with networking off. Both current expressions
   (`DuckLakeWriterService:83`, `UnifiedViewBuilder:210`) return the right UTC date in a
   locked-down session whose `TimeZone` is `Asia/Singapore`.
5. **Only `ducklake` and `postgres` need delivering (Q1).** Built in on 2.0 JDBC: `core_functions`,
   `icu`, `json`, `parquet`. The CLI also has `autocomplete` and `shell`.
6. **Catalog compatibility matches D2.**
   - A 1.0 catalog (file or PostgreSQL) is used as is by 2.0 without migration.
   - `AUTOMATIC_MIGRATION true`, which the code passes today, silently migrates a 1.0 catalog to
     1.1.
   - After a 2.x write, DuckDB 1.5.5 can no longer open a **file** catalog ("storage version 7 …
     lower than what the database itself uses (69)"). It still reads a 1.0 **PostgreSQL** catalog.
   - A 0.3 catalog (DuckDB 1.4.x) is refused without migration ("catalog version is 0.3, but the
     extension requires version 1.1"). With `AUTOMATIC_MIGRATION true` it migrates straight to 1.1,
     and the data reads back. The migration target is 1.1, and 0.3 → 2.x is feasible.
   - New catalogs default to spec 1.1; `DUCKLAKE_VERSION '1.0'` creates a 1.0 catalog (Q7).
7. **The query layer needs two one-line changes; both work on 1.5.5 too** (`m1-query-layer.patch`).
   - **Lambda syntax (blocking).** `ParquetReadConnectionProvider:390` builds the DuckLake view
     projection with `COLUMNS(c -> NOT starts_with(c, '_ducklake_internal_'))`. 1.5.5 only warns
     about the arrow; 2.0 rejects it ("Binder Error: Deprecated lambda arrow (->) detected"), so
     **no DuckLake historical view is created on 2.0**. `COLUMNS(lambda c: …)` works on both. It
     is the only SQL arrow lambda in the main code.
   - **Quote `hive_types` values.**
     `ParquetReadConnectionProvider.hiveTypesClause` (`:464`) emits `hive_types={'epoch': INTEGER}`
     with a bare type identifier. 2.0 warns ("Deprecated implicit conversion of unbound
     identifiers to strings in table function arguments"), and it fails under
     `table_function_identifier_conversion = 'DISABLE_IMPLICIT_STRING'`. The quoted form
     `{'epoch': 'INTEGER'}` works on 1.5.5 and 2.0.
8. **Native image.** The 2.0.0.1 jar is the first to ship GraalVM metadata
   (`META-INF/native-image/org.duckdb/duckdb_jdbc/reachability-metadata.json`, 49 reflection
   entries, no resources).
   - 2.0.0.1 works in a native image when the platform library is included
     (`-H:IncludeResources=libduckdb_java.so_linux_arm64`). Without it, loading fails:
     `DuckDB JNI library not found, path: '…/libduckdb_java.so_linux_arm64'`.
   - 1.5.5.0 fails in a native image either way (`NoSuchMethodError` from JNI with the library
     included). **The analytics store does not work in today's native `all` images.**
9. **File-format behaviour is unchanged.** P2 (`ducklake_add_data_files` with a Hive path), P3
   (positional insert fails on a new column; `BY NAME` with `ADD COLUMN` works), P4 (mixed schemas
   need `union_by_name`) and p10 give the 1.5.5 results.
10. **`METADATA_SCHEMA` works on 2.0** with a PostgreSQL catalog (30 `ducklake_*` tables in the
    chosen schema for spec 1.1; 28 on 1.0). See Q8.

## Existing test suites on JDBC 2.0.0.1

`./gradlew :aggregates:analytics-store:test :aggregates:analytics-query:test` on JDK 21.0.3 (Temurin),
from `main` at `828e240ad`, with only `gradle/libs.versions.toml` changed to `duckdb_jdbc:2.0.0.1`.
Neither change is committed.

| Run | analytics-store | analytics-query |
|---|---|---|
| 2.0.0.1 | 88 / 88 pass | 43 / 47 pass |
| 2.0.0.1 + `m1-query-layer.patch` | 88 / 88 pass | 47 / 47 pass |
| 1.5.5.0 + `m1-query-layer.patch` | 88 / 88 pass | 47 / 47 pass |

The four 2.0.0.1 failures are all in `ParquetReadConnectionProviderDuckLakeTest`
(`epochPartitionedTablesKeepFileColumnTypeAndSupportAggregates`,
`refreshKeepsCurrentViewsWhileAnExportHoldsTheWriter`,
`servesCommittedDuckLakeDataUnderLockdownWhileWriterKeepsExporting`,
`servesTablesThatMixPartitionedAndUnpartitionedDataFiles`). Each one fails with
`Catalog Error: Table with name … does not exist`, because view creation logged
`Failed to create historical view … Binder Error: Deprecated lambda arrow (->) detected`.

Not run here: `mcp-server` tests, the unified views with live PostgreSQL federation, and Intel
macOS. These remain M1 exit criteria.


## ADR changes these results call for

These go into ADR 0008 as a revision; nothing here changes the ADR by itself.

- **C7, D1, I10, R2:** replace "2.0 no longer bundles ICU" with the finding above. The write path
  still benefits from the D6 literal partition value (no `timezone()` per row). The query layer
  can keep `AT TIME ZONE 'UTC'`, or switch to the ICU-free
  `CAST(make_timestamp(<seconds>::BIGINT * 1000000) AS DATE)`, which gives the same date (see
  `jdbc/Dates.java`).
- **D1, Q1:** only `ducklake` and `postgres` must be pre-seeded per architecture.
- **D1 (native):** record the native-image result; the native starter needs
  `-H:IncludeResources` for the platform library, or analytics is documented as JVM-only.
- **D1 / M1:** apply `m1-query-layer.patch` (the lambda syntax and quoted `hive_types`). It is
  safe on 1.5.5, so it can ship before M1.
- **p8:** record the changed 2.0 failure mode; the precondition is unchanged.
- **Appendix A, R8:** add the 2.0 results and drop "No probe has been run on 2.x yet".

## Reproducing

```bash
# CLIs: DuckDB 2.0.0 (gh release download v2.0.0 -R duckdb/duckdb), 1.5.5 and 1.4.1.
# A throwaway PostgreSQL with empty databases cat11 and cat10:
docker run -d --rm --name m0-pg -e POSTGRES_PASSWORD=probe -p 55433:5432 postgres:17.6-alpine
docker exec m0-pg psql -U postgres -c "CREATE DATABASE cat11"
docker exec m0-pg psql -U postgres -c "CREATE DATABASE cat10"

export DUCKDB2=… DUCKDB15=… DUCKDB14=… WORK=$(mktemp -d)
PGCONN="host=localhost port=55433 user=postgres password=probe" ./matrix.sh   # catalogs × core.sql
./files.sh                                                                   # P2, P4, p10, deletion vectors

# JDBC 2.0.0.1 (Java 21+ single-file launch):
java -cp duckdb_jdbc-2.0.0.1.jar jdbc/Ext.java     # built-in extensions, ICU functions
java -cp duckdb_jdbc-2.0.0.1.jar jdbc/Dates.java   # date expressions, locked-down, non-UTC session
docker run --rm --network none --platform linux/amd64 -v "$PWD/jdbc:/w:ro" -v "<jar dir>:/j:ro" \
  eclipse-temurin:21-jdk java -cp /j/duckdb_jdbc-2.0.0.1.jar /w/Ext.java

# Native image (copy the jar into native/ first):
cd native && docker run --rm --platform linux/arm64 --network none -v "$PWD":/w --entrypoint bash \
  quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25 /w/build.sh duckdb_jdbc-2.0.0.1.jar linux_arm64
```

`core.sql` expects the lake attached as `lake`. `run.sh` adds the extension setup for the CLI's
version (`extension_directories` on 2.x, `extension_directory` on 1.x).
