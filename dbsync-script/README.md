# DB-Sync Parquet Exporter

Export specific tables from a Cardano db-sync PostgreSQL database to Parquet format, with foreign key IDs resolved to human-readable bech32 values.

Uses **DuckDB + postgres_scanner** for high-performance columnar streaming — the same approach as yaci-store's analytics-store. Data flows directly: `PostgreSQL -> DuckDB (Arrow columnar) -> Parquet`, bypassing slow row-by-row Python processing.

## Prerequisites

- Python 3.8+
- Access to a running Cardano db-sync PostgreSQL database
- Sufficient disk space for output files (epoch_stake and reward are large tables)

## Setup

```bash
pip install duckdb
```

> **Note**: Only `duckdb` is required. DuckDB bundles its own postgres_scanner extension (auto-installed on first run). No need for `psycopg2` or `pyarrow`.

## Configuration

There are **3 ways** to provide DB connection settings and output directory. They can be mixed, with this priority order:

**CLI arguments > .env file / environment variables > DEFAULTS in script**

### Option 1: `.env` file

```bash
# Copy the example and edit
cp .env.example .env
nano .env
```

`.env` file format:

```env
PGHOST=localhost
PGPORT=5432
PGUSER=your_user
PGPASSWORD=your_password
PGDATABASE=dbsync
OUTPUT_DIR=./output
```

Then run with:

```bash
python export_dbsync_parquet.py --env-file .env
```

### Option 2: Edit defaults in script

Open `export_dbsync_parquet.py` and edit the `DEFAULTS` dict at the top:

```python
DEFAULTS = {
    "PGHOST": "localhost",
    "PGPORT": "5432",
    "PGUSER": "your_user",        # <-- edit here
    "PGPASSWORD": "your_password", # <-- edit here
    "PGDATABASE": "dbsync",        # <-- edit here
    "OUTPUT_DIR": "./output",       # <-- edit here
}
```

Then run without any arguments:

```bash
python export_dbsync_parquet.py
```

### Option 3: CLI parameters

Pass everything directly on the command line:

```bash
python export_dbsync_parquet.py \
    --pg-host db.example.com \
    --pg-port 5432 \
    --pg-user admin \
    --pg-password secret \
    --pg-database dbsync \
    --output-dir /data/parquet
```

### Mixing options

You can combine methods. For example, use `.env` for DB credentials and CLI for output dir and table selection:

```bash
python export_dbsync_parquet.py --env-file .env --output-dir /data/parquet --tables drep_hash
```

Or set environment variables in your shell and override just one via CLI:

```bash
export PGHOST=db.example.com PGUSER=admin PGPASSWORD=secret PGDATABASE=dbsync
python export_dbsync_parquet.py --output-dir /data/parquet
```

## Usage

### Export all tables

```bash
python export_dbsync_parquet.py --env-file .env
```

### Export specific tables

```bash
# Small tables first (quick test to verify connectivity)
python export_dbsync_parquet.py --env-file .env --tables drep_hash drep_registration

# Then the large ones
python export_dbsync_parquet.py --env-file .env --tables epoch_stake reward
```

### Recommended execution order

```bash
# Step 1: Quick test with tiny tables (~seconds)
python export_dbsync_parquet.py --env-file .env --tables drep_hash drep_registration

# Step 2: Small filtered tables (~seconds to minutes)
python export_dbsync_parquet.py --env-file .env --tables drep_distr reward_rest

# Step 3: Large tables (epoch-by-epoch with progress logging)
python export_dbsync_parquet.py --env-file .env --tables epoch_stake
python export_dbsync_parquet.py --env-file .env --tables reward
```

### CLI reference

```
python export_dbsync_parquet.py --help

Options:
  --tables          Tables to export (default: all)
                    Choices: epoch_stake, reward, drep_distr, drep_hash,
                             drep_registration, reward_rest
  --output-dir      Output directory for parquet files
  --env-file        Path to .env file to load DB variables from

  Database connection:
  --pg-host         PostgreSQL host
  --pg-port         PostgreSQL port
  --pg-user         PostgreSQL user
  --pg-password     PostgreSQL password
  --pg-database     PostgreSQL database name
```

### Sample output log

```
[2026-04-08 10:30:00] Output directory: /data/parquet
[2026-04-08 10:30:00] DuckDB connected to PostgreSQL: localhost:5432/dbsync
[2026-04-08 10:30:00] Starting export of 1 table(s): epoch_stake

[2026-04-08 10:30:00] Exporting epoch_stake...
[2026-04-08 10:30:01]   Found 50 epochs to export: 504 - 553
[2026-04-08 10:30:03]   Epoch 504: 1,200,000 rows, 18.5 MB, 2.1s | Total: 1,200,000 rows, 2.1s elapsed, ETA: 1.7m (1/50 epochs)
[2026-04-08 10:30:05]   Epoch 505: 1,210,000 rows, 18.7 MB, 1.9s | Total: 2,410,000 rows, 4.0s elapsed, ETA: 3.2m (2/50 epochs)
...
[2026-04-08 10:32:30]   Merging 50 epoch files into epoch_stake_from504.parquet...
[2026-04-08 10:32:35]   Merge complete in 5.2s
[2026-04-08 10:32:35] DONE epoch_stake: 62,000,000 rows, 1.20 GB, 2.6m -> epoch_stake_from504.parquet
```

## Performance

### Why DuckDB + postgres_scanner?

| Approach | epoch_stake (160M rows) | Bottleneck |
|---|---|---|
| **psycopg2 + pyarrow** (old) | ~2-3 hours | Row-by-row Python processing, Decimal->int conversion |
| **DuckDB postgres_scanner** (current) | ~5-10 minutes | Columnar Arrow streaming, vectorized COPY TO |

DuckDB's `postgres_scanner` streams data from PostgreSQL in **columnar Arrow format** and writes directly to Parquet via `COPY TO` — no Python row processing at all. This is the same approach used by yaci-store's analytics-store module.

### Epoch-by-epoch strategy

Large tables (`epoch_stake`, `reward`) are exported **one epoch at a time**, then merged. This provides:
- Per-epoch progress logging with ETA
- ~2s per epoch (~1M rows) instead of one monolithic query
- Atomic temp file writes (`.tmp` -> final rename)

## Exported Tables

| Table | Filter | Output File | Est. Size | Description |
|---|---|---|---|---|
| `epoch_stake` | epoch >= 504 | `epoch_stake_from504.parquet` | Large | Stake distribution per epoch with bech32 stake addresses and pool IDs |
| `reward` | earned_epoch >= 504 | `reward_from504.parquet` | Large | Staking rewards (member, leader, refund) with resolved addresses and pools |
| `reward_rest` | earned_epoch >= 504 | `reward_rest_from504.parquet` | Small | Non-pool rewards (reserves, treasury, proposal_refund) |
| `drep_distr` | epoch >= 504 | `drep_distr_from504.parquet` | Small | DRep voting power distribution per epoch |
| `drep_hash` | All rows | `drep_hash.parquet` | Tiny | DRep identifier lookup (raw hash + bech32 view) |
| `drep_registration` | All rows | `drep_registration.parquet` | Tiny | DRep registration/deregistration/update events |

## Output Schema

### epoch_stake_from504.parquet

| Column | Type | Description |
|---|---|---|
| `epoch_no` | int32 | Epoch number |
| `stake_address` | string | Bech32 stake address (stake1...) |
| `pool` | string | Bech32 pool ID (pool1...) |
| `amount` | int64 | Staked amount in lovelace |

### reward_from504.parquet

| Column | Type | Description |
|---|---|---|
| `stake_address` | string | Bech32 stake address |
| `type` | string | Reward type: member, leader, or refund |
| `amount` | int64 | Reward amount in lovelace |
| `earned_epoch` | int64 | Epoch when reward was earned |
| `spendable_epoch` | int64 | Epoch when reward becomes spendable |
| `pool` | string | Bech32 pool ID |

### reward_rest_from504.parquet

| Column | Type | Description |
|---|---|---|
| `stake_address` | string | Bech32 stake address |
| `type` | string | Reward type: reserves, treasury, or proposal_refund |
| `amount` | int64 | Reward amount in lovelace |
| `earned_epoch` | int64 | Epoch when reward was earned |
| `spendable_epoch` | int64 | Epoch when reward becomes spendable |

### drep_distr_from504.parquet

| Column | Type | Description |
|---|---|---|
| `epoch_no` | int32 | Epoch number |
| `drep_id` | string | DRep bech32 identifier (drep1...) |
| `has_script` | bool | Whether this is a script-controlled DRep |
| `amount` | int64 | Total voting power delegated to this DRep |
| `active_until` | int32 | Epoch until which this DRep is active (nullable) |

### drep_hash.parquet

| Column | Type | Description |
|---|---|---|
| `raw` | string | Hex-encoded raw hash (nullable) |
| `view` | string | Bech32 DRep identifier |
| `has_script` | bool | Whether this is a script-controlled DRep |

### drep_registration.parquet

| Column | Type | Description |
|---|---|---|
| `drep_id` | string | DRep bech32 identifier |
| `has_script` | bool | Whether this is a script-controlled DRep |
| `deposit` | int64 | Deposit amount (positive=register, negative=deregister, null=update) |
| `cert_index` | int32 | Certificate index within the transaction |
| `tx_id` | int64 | Transaction ID (db-sync internal) |
| `voting_anchor_id` | int64 | Voting anchor reference (nullable) |

## Verifying Output

```bash
# Check with DuckDB CLI or Python
python -c "
import duckdb
conn = duckdb.connect()
print(conn.execute(\"SELECT COUNT(*) FROM read_parquet('drep_hash.parquet')\").fetchone())
conn.execute(\"SELECT * FROM read_parquet('drep_hash.parquet') LIMIT 10\").show()
"

# Or with pyarrow (if installed)
python -c "
import pyarrow.parquet as pq
meta = pq.read_metadata('epoch_stake_from504.parquet')
print(f'Rows: {meta.num_rows}, Row groups: {meta.num_row_groups}')
"

# Validate row counts against database
psql -c "SELECT count(*) FROM epoch_stake WHERE epoch_no >= 504;"
```

## Notes

- Uses **DuckDB's postgres_scanner** for vectorized columnar streaming — no row-by-row Python processing.
- Large tables are exported **epoch-by-epoch** with per-epoch timing, row counts, and ETA.
- All Parquet files use **ZSTD compression** (level 3) for good compression ratio and speed.
- Atomic writes: data is written to `.tmp` files first, then renamed on success.
- Only dependency: `duckdb` Python package. DuckDB auto-installs `postgres_scanner` on first run.
