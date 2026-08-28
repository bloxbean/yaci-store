# Analytics Query

The analytics-query module is the **read side** of the analytics stack. [analytics-store](../analytics-store/README.md)
exports blockchain data from PostgreSQL to Parquet/DuckLake; this module queries those exports
in-process with DuckDB and exposes them as a REST API and as a set of reusable Spring beans.

Optionally it also **federates with live PostgreSQL**, so a single table name spans exported
history and the current chain tip.

Both front-ends over the engine — the REST controller here and the MCP tools in
[mcp-server](../mcp-server/README.md) — share the same connection provider, validator and limits.

## Quickstart

The query layer is off by default. Enable it in `application.properties`:

```properties
# Required: analytics data must be exported first
yaci.store.analytics.enabled=true

# Turn on the query layer (DuckDB views, executor, schema service)
yaci.store.analytics.query.enabled=true

# Expose the REST endpoints (separate switch — see "Two switches" below)
yaci.store.analytics.query.rest-api-enabled=true
```

Then discover what is queryable and run a query:

```bash
curl http://localhost:8080/api/v1/analytics/query/schema

curl -X POST http://localhost:8080/api/v1/analytics/query/sql \
  -H 'Content-Type: application/json' \
  -d '{"sql":"SELECT epoch, count(*) AS blocks FROM block WHERE epoch >= 640 GROUP BY epoch ORDER BY epoch","maxRows":100}'
```

### Two switches, on purpose

| Property | Default | Effect |
|---|---|---|
| `yaci.store.analytics.query.enabled` | `false` | The whole layer: DuckDB views, `AnalyticsQueryExecutor`, `AnalyticsSchemaService`. Everything below depends on it. |
| `yaci.store.analytics.query.rest-api-enabled` | `false` | Only `/api/v1/analytics/query/*`. Leave it `false` to keep the beans available to in-process callers (the MCP tools) without exposing HTTP. |

Ad-hoc SQL is unauthenticated. Both defaults are `false` so exposing it is always a deliberate
operator decision.

## REST API

Base path `/api/v1/analytics/query`.

| Method | Path | Returns |
|---|---|---|
| GET | `/schema` | All tables: row counts, partition strategy, date/epoch ranges, `dataAsOf`, `liveDataActive`, per-table `dataScope`, query hints |
| GET | `/schema/{tableName}` | Column names and DuckDB types for one table |
| GET | `/blocks/epoch-stats` | Blocks per epoch |
| GET | `/blocks/pool-production` | Blocks produced per pool |
| GET | `/transactions/epoch-stats` | Transaction counts/fees per epoch |
| GET | `/transactions/block-stats` | Transaction statistics per block |
| GET | `/transactions/fee-distribution` | Fee distribution buckets |
| GET | `/transactions/count` | Transaction count over a range |
| POST | `/sql` | Ad-hoc read-only SQL |

### `POST /sql` contract

Request body:

```json
{"sql": "SELECT ...", "maxRows": 100}
```

`maxRows` is optional. Omitted, `0` or negative → `default-max-rows`; above `max-rows` → reduced
to it.

Every successful response carries:

| Header | Meaning |
|---|---|
| `X-Analytics-Row-Limit` | The limit actually applied |
| `X-Analytics-Truncated` | `true` when more rows existed than were returned |

Errors are always `400` with `{"error": "..."}` — a rejected statement, an unknown table or
column, a syntax error and a timeout all come back the same shape, so a caller (or an LLM agent)
can correct the query rather than parse a stack trace. No `X-Analytics-*` headers on errors.

Rows are capped by wrapping the statement as `SELECT * FROM (<sql>) LIMIT n+1`, the way SQL
front-ends such as Superset cap results — DuckDB stops producing rows at the limit instead of
materializing the whole result, and the extra row is what sets `X-Analytics-Truncated`.

Temporal values are normalized to ISO-8601 strings: `DATE` → `2026-08-19`, `TIMESTAMP` →
`2026-08-19T10:00:00.123456`, `TIMESTAMPTZ` → UTC with a trailing `Z`, `TIME` → `10:00:00`.

## What SQL is accepted

`SqlValidator` rejects a statement before it reaches DuckDB when it:

- is empty, or exceeds the maximum statement length
- contains more than one statement (semicolons)
- is not a `SELECT` or `WITH`
- uses a blocked keyword or function, or an identifier prefixed `DUCKDB_`, `PRAGMA_`, `PG_`,
  `POSTGRES_`
- contains a string literal that looks like a filesystem path or a remote URL (`/`, `./`, `../`,
  `~`, `http://`, `https://`, `s3://`, `gs://`, `az://`, `abfss://`, `file://`)

The last rule is what stops `read_parquet('/etc/passwd')`-style access through an otherwise valid
`SELECT`. The DuckDB connection is additionally opened read-only and the analytics directory is
locked down at startup.

## Limits and timeouts

| Property | Default | Notes |
|---|---|---|
| `default-max-rows` | `100` | Used when the caller passes no `maxRows` |
| `max-rows` | `10000` | Hard ceiling. Can only be **lowered** — values above 10,000 or ≤ 0 are treated as 10,000 |
| `max-timeout-seconds` | `300` | Ceiling for a per-call timeout; never below the default timeout |
| `postgres-statement-timeout-seconds` | `30` | `statement_timeout` on the attached `pg_live` database (federation only) |
| `yaci.store.analytics.duckdb.reader.query-timeout-seconds` | `30` | Default per-query timeout |
| `yaci.store.analytics.duckdb.reader.maximum-pool-size` | CPU cores | Reader permits; a long query holds one for its whole duration |

`AnalyticsQueryExecutor.execute(sql, maxRows, timeoutSeconds)` accepts a per-call timeout for
heavy analytical queries. The REST endpoints always use the default; the MCP
`analytics-execute-sql` tool exposes it to the caller.

## Live PostgreSQL federation

Off by default (`live-data-enabled=false`), in which case each exported table is registered as a
DuckDB view under its own name and the data is as of the last completed export.

With `yaci.store.analytics.query.live-data-enabled=true`:

1. `postgres_scanner` is installed and PostgreSQL is attached read-only as `pg_live`.
2. Each historical view is renamed to `parquet_<table>`.
3. `UnifiedViewBuilder` creates `<table>` as a `UNION ALL` of the two halves:

```sql
CREATE OR REPLACE VIEW "block" AS
  SELECT * FROM "parquet_block" WHERE "slot" <= <cutoff>
  UNION ALL
  SELECT ... FROM "pg_live"."<schema>"."block" WHERE "slot" > <cutoff>
```

So the same `SELECT ... FROM block` reaches the chain tip without the caller knowing where the
boundary is. Renamed and type-mismatched columns are mapped automatically (PG `BIGINT` block_time
→ `to_timestamp(...)`, `BIGINT` amounts → `DECIMAL(38,0)`, synthetic `date` partition column
derived from the source column).

**Not every table federates.** The boundary column is declared by the table's exporter
(`TableExporter.getFederationBoundaryColumn()`): `slot` for DAILY tables, `epoch` for EPOCH
tables, `null` for MONTHLY — those stay Parquet-only. `GET /schema` reports the outcome per table
as `dataScope`: `historical` or `historical+live`, plus a global `liveDataActive`.

Only join tables with the same `dataScope`, and remember that an epoch-level live table's current
epoch keeps changing until the epoch closes.

Exclude specific tables with
`yaci.store.analytics.query.live-data-excluded-tables=epoch_stake,reward`.

If federation cannot be set up (extension missing, PostgreSQL unreachable, a table whose columns
cannot be mapped) the provider falls back to Parquet-only for that table — or globally — rather
than failing startup.

## Module structure

```
analytics/query/
  config/       - AnalyticsQueryConfig: beans, gated on query.enabled
  connection/   - DuckDbReadConnectionProvider, ParquetReadConnectionProvider (view lifecycle),
                  ParquetTableRegistry, CutoffSlotResolver, UnifiedViewBuilder
  controller/   - AnalyticsQueryController (/api/v1/analytics/query/*)
  executor/     - AnalyticsQueryExecutor: limits, per-call timeout, temporal normalization,
                  sanitized errors
  model/        - SchemaOverview, TableDescription, TableInfo, ColumnSchema, TableMetadata
  service/      - AnalyticsSchemaService, AnalyticsBlockQueryService,
                  AnalyticsTransactionQueryService, SqlValidator
```

## Using it from Java

The beans are ordinary Spring beans — that is how `mcp-server` reuses this layer:

```java
private final AnalyticsSchemaService schemaService;
private final AnalyticsQueryExecutor queryExecutor;
private final ParquetReadConnectionProvider connectionProvider;

SchemaOverview overview = schemaService.listTables();
SqlValidator.validate(sql);
var result = queryExecutor.execute(sql, 1000, 60);   // rows, per-call timeout
boolean live = connectionProvider.isLiveDataActive();
```

Anything reaching the executor this way gets the same validation, row cap and timeout ceiling as
the REST API.
