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

Analytics exports begin automatically:
- **Daily tables** export at midnight (configurable via `yaci.store.analytics.daily-export-cron`)
- **Epoch tables** export at 1 AM (configurable via `yaci.store.analytics.epoch-export-cron`)
- **Continuous sync** fills gaps every 15 minutes

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
  exporter/        - 47 table exporter implementations
  gap/             - Gap detection for continuous sync
  helper/          - DuckDB connection helper utilities
  query/           - DuckLake query controller and service
  scheduler/       - Export schedulers and export monitor
  state/           - Export state management (JPA entities)
  writer/          - Parquet and DuckLake writer services
```

### Schedulers

| Scheduler | Schedule | Purpose |
|---|---|---|
| `UniversalExportScheduler` | Daily + Epoch cron | Exports all enabled tables on schedule |
| `ContinuousSyncScheduler` | Every 15 min | Detects and fills export gaps |
| `ExportMonitor` | Every 5 min | Monitors health, recovers stuck exports |

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

To query exported analytics data from external tools, you need the DuckDB command-line interface:

**Linux/macOS:**
```bash
# Download and install latest DuckDB CLI
wget https://github.com/duckdb/duckdb/releases/latest/download/duckdb_cli-linux-amd64.zip
unzip duckdb_cli-linux-amd64.zip
sudo mv duckdb /usr/local/bin/
```

**macOS (Homebrew):**
```bash
brew install duckdb
```

**Windows:**
```powershell
# Download from: https://github.com/duckdb/duckdb/releases/latest
# Extract duckdb.exe and add to PATH
```

**Verify installation:**
```bash
duckdb --version
# Example output: v1.4.2 (or later)
```

**Install DuckLake extension (only needed for DuckLake mode):**
```bash
duckdb -c "INSTALL ducklake;"
```

> **Note**: The DuckLake extension is only required if you're using DuckLake storage mode (`yaci.store.analytics.storage.type=ducklake`). For direct Parquet file queries, the extension is not needed.

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
| `yaci.store.analytics.daily-export-cron` | `0 0 0 * * *` | Cron for daily table exports |
| `yaci.store.analytics.epoch-export-cron` | `0 0 1 * * *` | Cron for epoch table exports |
| `yaci.store.analytics.finalization-lag-days` | `2` | Days to lag behind tip (ensures immutability) |
| `yaci.store.analytics.enabled-tables` | _(empty = all)_ | Comma-separated list of tables to export |
| `yaci.store.analytics.admin.enabled` | `false` | Enable admin REST API |
| `yaci.store.analytics.storage.type` | `parquet` | Storage format: `parquet` or `ducklake` |
| `yaci.store.analytics.state-management.stale-timeout-minutes` | `60` | Timeout before stuck exports are recovered |
| `yaci.store.analytics.state-management.max-retries` | `3` | Max retry attempts for failed exports |
| `yaci.store.analytics.continuous-sync.buffer-days` | `3` | Buffer days for continuous sync |
| `yaci.store.analytics.continuous-sync.sync-check-interval-minutes` | `15` | Gap detection interval |
| `yaci.store.analytics.export-monitor.enabled` | `false` | Enable/disable the export monitor |
| `yaci.store.analytics.export-monitor.check-interval-seconds` | `300` | Export monitor health check interval |
| `yaci.store.analytics.parquet-export.codec` | `ZSTD` | Compression codec |
| `yaci.store.analytics.parquet-export.compression-level` | `3` | ZSTD compression level (1-22) |
| `yaci.store.analytics.ducklake.catalog-type` | `postgresql` | DuckLake catalog: `postgresql` or `duckdb` |
| `yaci.store.analytics.ducklake.catalog-url` | _(main datasource)_ | Custom PostgreSQL URL for catalog |
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
| GET | `/api/v1/analytics/admin/health` | Scheduler health and stale export detection |
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

# Check health status
curl http://localhost:8080/api/v1/analytics/admin/health

# Query the exported data
duckdb -c "SELECT COUNT(*) FROM read_parquet('./data/analytics/transaction/date=2024-01-15/data.parquet');"
```

## Table Exporters

The module includes 50+ table exporters covering all indexed Cardano data. Each exporter uses a Template Method pattern (`AbstractTableExporter`) and is auto-discovered by the `TableExporterRegistry`.

### Daily Tables (partitioned by date)

`block`, `transaction`, `transaction_outputs`, `transaction_metadata`, `transaction_scripts`, `transaction_witness`, `invalid_transaction`, `address`, `address_balance`, `address_tx_amount`, `assets`, `stake_address_balance`, `delegation`, `delegation_vote`, `drep`, `drep_registration`, `stake_registration`, `pool`, `pool_registration`, `pool_retirement`, `voting_procedure`, `gov_action_proposal`, `committee_registration`, `committee_deregistration`, `script`, `datum`, `cost_model`, `spent_outputs`, `rollback`, `protocol_params_proposal`, `move_instantaneous_reward`, `withdrawal`

### Epoch Tables (partitioned by epoch)

`epoch`, `epoch_param`, `epoch_stake`, `reward`, `reward_rest`, `unclaimed_reward_rest`, `adapot`, `drep_dist`, `committee`, `committee_member`, `committee_state`, `constitution`, `gov_action_proposal_status`, `gov_epoch_activity`, `instant_reward`

### Selecting specific tables

Export only the tables you need:

```properties
yaci.store.analytics.enabled-tables=transactions,transaction_outputs,blocks,address_balance
```

## Export State Management

Each export is tracked in the `analytics_export_state` table with status transitions:

```
PENDING --> IN_PROGRESS --> COMPLETED
                |
                +--> FAILED (retried up to max-retries times)
```

The `ExportMonitor` automatically recovers stuck `IN_PROGRESS` exports that exceed the stale timeout (default: 60 minutes) by marking them as `FAILED`, allowing them to be retried on the next scheduler run.
