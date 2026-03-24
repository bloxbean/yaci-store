# Analytics Store

The analytics-store module exports Cardano blockchain data from PostgreSQL to Parquet files for analytical workloads. It supports both direct Parquet file export and DuckLake-managed catalogs with ACID transactions, time-travel queries, and schema evolution.

## Quickstart

### 1. Enable analytics in your `application.properties`

```properties
# Enable analytics store
yaci.store.analytics.enabled=true

# Enable admin API for manual export control
yaci.store.analytics.admin.enabled=true

# Storage type: "parquet" (default) or "ducklake"
yaci.store.analytics.storage.type=parquet
```

### 2. Start yaci-store

Analytics exports begin automatically via the `ContinuousSyncScheduler`:
- Uses adaptive gap detection to find and export missing daily and epoch partitions
- **Catching up**: checks every 1 minute by default (`yaci.store.analytics.continuous-sync.catch-up-interval-minutes`)
- **Fully synced**: checks every 15 minutes by default (`yaci.store.analytics.continuous-sync.sync-check-interval-minutes`)

Exported files are written to `./data/analytics/` by default.

## Architecture

### Data Flow

```
PostgreSQL (blockchain data)
  |
  v
DuckDB (in-process, via JDBC)
  |
  +---> Parquet files (direct export)
  |         or
  +---> DuckLake catalog --> Parquet files (managed export)
```

### Module Structure

```
analytics/
  admin/           - Admin REST controller and service
  config/          - Spring configuration and properties
  ducklake/        - DuckLake catalog initialization
  exporter/        - 47 built-in table exporters (31 daily + 16 epoch)
  gap/             - Gap detection for continuous sync
  helper/          - DuckDB connection helper utilities
  query/           - DuckLake query controller and service
  scheduler/       - Export schedulers and recovery services
  state/           - Export state management (JPA entities)
  writer/          - Parquet and DuckLake writer services
```

### Schedulers

| Scheduler | Schedule | Purpose |
|---|---|---|
| `ContinuousSyncScheduler` | Adaptive: 1 min (catching up) / 15 min (synced) | Detects and fills daily + epoch export gaps via `UniversalExportService` |
| `StaleExportRecoveryService` | On startup + shutdown | Recovers stuck IN_PROGRESS exports on application lifecycle events |

## Storage Modes

### Parquet (default)

Direct Parquet file export. Files are organized by table name and partition (date or epoch).

```
./data/analytics/
  block/
    date=2024-01-15/data.parquet
    date=2024-01-16/data.parquet
  epoch_stake/
    epoch=450/data.parquet
```

- Simple, no external dependencies beyond DuckDB JDBC
- Files are immutable once written
- No built-in catalog metadata

### DuckLake

DuckLake-managed Parquet files with a catalog (PostgreSQL or DuckDB) storing metadata.

```properties
yaci.store.analytics.storage.type=ducklake

# PostgreSQL catalog (recommended for production)
yaci.store.analytics.ducklake.catalog-type=postgresql

# Or DuckDB file catalog (development/single-instance)
yaci.store.analytics.ducklake.catalog-type=duckdb
yaci.store.analytics.ducklake.catalog-path=./data/analytics/ducklake.catalog.db
```

- ACID transactions, time-travel queries, schema evolution
- PostgreSQL catalog supports multi-instance deployments
- DuckDB catalog is lightweight for development

## Connecting External Tools

### Prerequisites: Installing DuckDB CLI

To query exported analytics data from external tools, you need the DuckDB command-line interface. Follow the official installation guide: https://duckdb.org/docs/installation/

If you're using DuckLake storage mode (`yaci.store.analytics.storage.type=ducklake`), also install the DuckLake extension:

```bash
duckdb -c "INSTALL ducklake;"
```

### Querying from DuckDB CLI

**Parquet mode:**

```bash
# Query Parquet files directly
duckdb -c "SELECT * FROM read_parquet('./data/analytics/block/date=2024-01-15/data.parquet') LIMIT 10;"

# Query all partitions with Hive partitioning
duckdb -c "SELECT * FROM read_parquet('./data/analytics/block/**/*.parquet', hive_partitioning=true) LIMIT 10;"
```

**DuckLake with DuckDB catalog:**

```bash
# Method 1: Interactive shell
cd /path/to/yaci-store
duckdb
# Then in DuckDB shell:
D INSTALL ducklake;
D LOAD ducklake;
D ATTACH 'ducklake:duckdb:./data/analytics/ducklake.catalog.db' AS analytics (DATA_PATH './data/analytics');
D SELECT COUNT(*) FROM analytics.block;
D SELECT * FROM analytics.transaction WHERE date = '2024-01-15' LIMIT 10;

# Method 2: Script mode with heredoc
cd /path/to/yaci-store
duckdb << 'EOF'
INSTALL ducklake;
LOAD ducklake;
ATTACH 'ducklake:duckdb:./data/analytics/ducklake.catalog.db' AS analytics (DATA_PATH './data/analytics');
SELECT COUNT(*) FROM analytics.block;
SELECT * FROM analytics.transaction WHERE date = '2024-01-15' LIMIT 10;
SELECT epoch, total_stake FROM analytics.epoch_stake WHERE epoch >= 450 ORDER BY epoch;
EOF

# Common mistake: Querying without prefix will fail
# ❌ SELECT * FROM block;           -- Table not found!
# ✅ SELECT * FROM analytics.block; -- Correct
```


**DuckLake with PostgreSQL catalog:**

```bash
# Important: Start from the same directory as yaci-store if using relative DATA_PATH
cd /path/to/yaci-store

duckdb -c "
  INSTALL ducklake; LOAD ducklake;
  
  ATTACH 'ducklake:postgres:dbname=yaci_store host=localhost port=5432 user=postgres password=pass options=-csearch_path=public'
    AS ducklake_catalog (DATA_PATH './data/analytics');
  
  -- Query DuckLake tables directly
  SELECT COUNT(*) FROM ducklake_catalog.block;
  SELECT * FROM ducklake_catalog.transaction WHERE date = '2024-01-15' LIMIT 10;
  SELECT epoch, total_stake FROM ducklake_catalog.epoch_stake WHERE epoch >= 450 ORDER BY epoch;
"
```

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `yaci.store.analytics.enabled` | `false` | Enable analytics store |
| `yaci.store.analytics.export-path` | `./data/analytics` | Output directory for exported files |
| `yaci.store.analytics.finalization-lag-days` | `2` | Days to lag behind tip (ensures immutability) |
| `yaci.store.analytics.enabled-tables` | _(empty = all)_ | Comma-separated list of tables to export |
| `yaci.store.analytics.admin.enabled` | `false` | Enable admin REST API |
| `yaci.store.analytics.storage.type` | `parquet` | Storage format: `parquet` or `ducklake` |
| `yaci.store.analytics.state-management.stale-timeout-minutes` | `60` | Timeout before stuck exports are recovered |
| `yaci.store.analytics.continuous-sync.buffer-days` | `2` | Buffer days for continuous sync |
| `yaci.store.analytics.continuous-sync.sync-check-interval-minutes` | `15` | Gap detection interval when fully synced |
| `yaci.store.analytics.continuous-sync.catch-up-interval-minutes` | `1` | Gap detection interval when catching up |
| `yaci.store.analytics.parquet-export.codec` | `ZSTD` | Compression codec |
| `yaci.store.analytics.parquet-export.compression-level` | `3` | ZSTD compression level (1-22) |
| `yaci.store.analytics.parquet-export.row-group-size` | `-1` | Parquet row group size (-1 = DuckDB default ~122,880 rows) |
| `yaci.store.analytics.ducklake.catalog-type` | `postgresql` | DuckLake catalog: `postgresql` or `duckdb` |
| `yaci.store.analytics.ducklake.catalog-url` | _(main datasource)_ | PostgreSQL URL for catalog (used when `catalog-type=postgresql`) |
| `yaci.store.analytics.ducklake.catalog-username` | _(main datasource)_ | PostgreSQL catalog username (used when `catalog-type=postgresql`) |
| `yaci.store.analytics.ducklake.catalog-password` | _(main datasource)_ | PostgreSQL catalog password (used when `catalog-type=postgresql`) |
| `yaci.store.analytics.ducklake.catalog-path` | `./data/analytics/ducklake.catalog.db` | DuckDB catalog file path |
| `yaci.store.analytics.logging.file` | `./logs/analytics-store.log` | Dedicated analytics log file path |

## Known Caveats

### Workspace-sensitive catalog path (DuckLake)

DuckLake stores `DATA_PATH` in catalog metadata. If you use a relative `export-path` (e.g., `./data/analytics`), the Parquet file paths in the catalog are relative to the working directory where yaci-store was started.

When querying with DuckDB CLI or Python, you **must start from the same working directory** as the application. Otherwise, DuckLake will fail to locate the Parquet files.

**Recommendation:** Use absolute paths in production:
```properties
yaci.store.analytics.export-path=/var/lib/yaci-store/analytics
```

### DuckLake requires `public` schema in PostgreSQL

DuckLake stores its metadata tables in the `public` schema. The application automatically creates this schema if it doesn't exist. If your PostgreSQL instance restricts schema creation, ensure `public` exists before enabling DuckLake.

## Admin API

The admin API provides manual control over exports when enabled (`yaci.store.analytics.admin.enabled=true`).

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/analytics/admin/tables` | List all registered exporters |
| GET | `/api/v1/analytics/admin/status` | Current sync status |
| GET | `/api/v1/analytics/admin/statistics/{table}` | Export statistics for a table |
| POST | `/api/v1/analytics/admin/export/date/{date}` | Export all daily tables for a date |
| POST | `/api/v1/analytics/admin/export/table/{table}/date/{date}` | Export specific table for a date |
| POST | `/api/v1/analytics/admin/export/table/{table}/epoch/{epoch}` | Export specific table for an epoch |
| POST | `/api/v1/analytics/admin/export/table/{table}/range` | Backfill date range |
| POST | `/api/v1/analytics/admin/export/table/{table}/epoch-range` | Backfill epoch range |
| POST | `/api/v1/analytics/admin/sync/trigger` | Trigger immediate gap sync |
| DELETE | `/api/v1/analytics/admin/state/{table}/{partition}` | Reset export state |
| DELETE | `/api/v1/analytics/admin/state/{table}/range` | Reset date range state |

### Example: Export and query a specific date

```bash
# Trigger export for a specific date
curl -X POST http://localhost:8080/api/v1/analytics/admin/export/date/2024-01-15

# Query the exported data
duckdb -c "SELECT COUNT(*) FROM read_parquet('./data/analytics/transaction/date=2024-01-15/data.parquet');"
```

## Table Exporters

The module includes 47 built-in table exporters (31 daily + 16 epoch) covering all indexed Cardano data. Each exporter uses a Template Method pattern (`AbstractTableExporter`) and is auto-discovered by the `TableExporterRegistry`.

### Daily Tables (partitioned by date)

`address`, `address_balance`, `address_tx_amount`, `address_utxo`, `assets`, `block`, `committee_deregistration`, `committee_registration`, `cost_model`, `datum`, `delegation`, `delegation_vote`, `drep`, `drep_registration`, `gov_action_proposal`, `invalid_transaction`, `pool`, `pool_registration`, `pool_retirement`, `protocol_params_proposal`, `rollback`, `script`, `stake_address_balance`, `stake_registration`, `transaction`, `transaction_metadata`, `transaction_scripts`, `transaction_witness`, `tx_input`, `voting_procedure`, `withdrawal`

### Epoch Tables (partitioned by epoch)

`adapot`, `committee`, `committee_member`, `committee_state`, `constitution`, `drep_dist`, `epoch`, `epoch_param`, `epoch_stake`, `gov_action_proposal_status`, `gov_epoch_activity`, `instant_reward`, `mir`, `reward`, `reward_rest`, `unclaimed_reward_rest`

### Selecting specific tables

There are two ways to control which tables are exported:

**Option 1 — Whitelist (enable only specific tables):**

```properties
# Only these tables will be exported; all others are skipped
yaci.store.analytics.enabled-tables=transaction,transaction_outputs,block,address_balance
```

**Option 2 — Per-exporter disable (disable individual tables):**

```properties
# Disable specific exporters while keeping all others enabled
yaci.store.analytics.exporter.reward.enabled=false
yaci.store.analytics.exporter.epoch_stake.enabled=false
```

Both options can be combined: per-exporter flags are evaluated first, then the whitelist filter is applied.

### Custom Exporters

You can define custom SQL-based exporters to export any data from PostgreSQL to Parquet/DuckLake without writing Java code. Custom exporters are configured in a YAML file and activated via Spring profile.

**Sample Configuration file:** `config/application-custom-exporters.yml`

> **Note:** The file `config/application-custom-exporters.yml` shipped in this repository is just a **sample configuration** with examples. You can define your own exporters.

Each custom exporter requires:

| Property | Required | Description                                                                                                                                 |
|---|---|---------------------------------------------------------------------------------------------------------------------------------------------|
| `name` | Yes | Target table name in the analytics catalog                                                                                                  |
| `query` | Yes | SQL template with placeholders (see below)                                                                                                  |
| `partition-strategy` | No | `DAILY` (default) or `EPOCH`                                                                                                                |
| `depends-on-adapot-job` | No | If `true`, need to wait for AdaPot reward calculation to complete before exporting. Only meaningful with `EPOCH` strategy. Default: `false` |

**Query placeholders:**

| Placeholder | Description |
|---|---|
| `{source}` | Resolves to the schema name (e.g. `mainnet`) |
| `{start_slot}` | Slot range start (inclusive) |
| `{end_slot}` | Slot range end (exclusive) |
| `{epoch}` | Epoch number (only available with `EPOCH` strategy) |

**Example — Daily exporter:**

```yaml
yaci:
  store:
    analytics:
      custom-exporters:
        - name: daily_tx_count
          query: >-
            SELECT b.number,
                   to_timestamp(COALESCE(b.block_time, 0)) as block_time,
                   b.slot, b.epoch, b.no_of_txs
            FROM {source}.block b
            WHERE b.slot >= {start_slot}
              AND b.slot <  {end_slot}
            ORDER BY b.slot
```

**Example — Epoch exporter:**

```yaml
        - name: epoch_pool_rewards
          partition-strategy: EPOCH
          depends-on-adapot-job: true
          query: >-
            SELECT r.address,
                   r.earned_epoch AS epoch,
                   r.spendable_epoch, r.type, r.pool_id,
                   r.amount, r.slot
            FROM {source}.reward r
            WHERE r.earned_epoch = {epoch}
            ORDER BY r.earned_epoch, r.address
```

> **Note:** When using `EPOCH` partition strategy, the query **must** output a column named `epoch`. If the source column has a different name, use an alias (e.g. `r.earned_epoch AS epoch`). Without this, the DuckLake table will not be partitioned and all Parquet files will be written flat without `epoch=N/` subdirectories.

## Export State Management

Each export is tracked in the `analytics_export_state` table with status transitions:

```
PENDING --> IN_PROGRESS --> COMPLETED
                |
                +--> FAILED --> IN_PROGRESS (retried on next scheduler run)
```

- **PENDING**: initial state when a new export record is created
- **IN_PROGRESS**: export is running; `retryCount` increments on each retry attempt
- **COMPLETED**: export finished successfully
- **FAILED**: export failed; gap detection automatically retries it on the next scheduler run. Use the admin API to manually reset if needed.

`StaleExportRecoveryService` handles two lifecycle scenarios:
- **On startup**: resets stale `IN_PROGRESS` exports (stuck due to a previous crash, exceeding `stale-timeout-minutes`) to `FAILED` so they are retried on the next scheduler run
- **On shutdown**: resets all active `IN_PROGRESS` exports to `FAILED` to prevent inconsistent state after the application stops
