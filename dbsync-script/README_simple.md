# DB-Sync Parquet Exporter (Simple)

Export specific tables from a Cardano db-sync PostgreSQL database to Parquet format, with foreign key IDs resolved to human-readable bech32 values.

**Best for**: running on the **same server** or **same LAN** as the db-sync database.
Uses `psycopg2` + `pyarrow` only — no DuckDB dependency.

## Prerequisites

- Python 3.8+
- Access to a running Cardano db-sync PostgreSQL database
- Sufficient disk space for output files

## Setup

```bash
pip3 install psycopg2-binary pyarrow
```

## Configuration

There are **3 ways** to provide DB connection settings and output directory. They can be mixed, with this priority order:

**CLI arguments > .env file / environment variables > DEFAULTS in script**

### Option 1: `.env` file

```bash
cp .env.example .env
nano .env
```

```env
PGHOST=localhost
PGPORT=5432
PGUSER=your_user
PGPASSWORD=your_password
PGDATABASE=dbsync
OUTPUT_DIR=./output
```

```bash
python3 export_dbsync_parquet_simple.py --env-file .env
```

### Option 2: Edit defaults in script

Open `export_dbsync_parquet_simple.py` and edit the `DEFAULTS` dict at the top:

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

```bash
python3 export_dbsync_parquet_simple.py
```

### Option 3: CLI parameters

```bash
python3 export_dbsync_parquet_simple.py \
    --pg-host localhost \
    --pg-port 5432 \
    --pg-user admin \
    --pg-password secret \
    --pg-database dbsync \
    --output-dir /data/parquet
```

### Mixing options

```bash
# .env for DB, CLI for output and table selection
python3 export_dbsync_parquet_simple.py --env-file .env --output-dir /data/parquet --tables drep_hash

# Shell env vars + CLI override
export PGHOST=localhost PGUSER=admin PGPASSWORD=secret PGDATABASE=dbsync
python3 export_dbsync_parquet_simple.py --output-dir /data/parquet
```

## Usage

### Export all tables

```bash
python3 export_dbsync_parquet_simple.py --env-file .env
```

### Export specific tables

```bash
# Quick test with tiny tables (~seconds)
python3 export_dbsync_parquet_simple.py --env-file .env --tables drep_hash drep_registration

# Large tables
python3 export_dbsync_parquet_simple.py --env-file .env --tables epoch_stake reward
```

### Recommended execution order

```bash
# Step 1: Quick test (~seconds)
python3 export_dbsync_parquet_simple.py --env-file .env --tables drep_hash drep_registration

# Step 2: Small filtered tables (~seconds to minutes)
python3 export_dbsync_parquet_simple.py --env-file .env --tables drep_distr reward_rest

# Step 3: Large tables (~3-5s per epoch on same server)
python3 export_dbsync_parquet_simple.py --env-file .env --tables epoch_stake
python3 export_dbsync_parquet_simple.py --env-file .env --tables reward
```

### CLI reference

```
python3 export_dbsync_parquet_simple.py --help

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
[2026-04-08 16:00:00] Output directory: /data/parquet
[2026-04-08 16:00:00] Connecting to PostgreSQL: localhost:5432/cexplorer
[2026-04-08 16:00:00] PostgreSQL connected.
[2026-04-08 16:00:00] Starting export of 1 table(s): epoch_stake

[2026-04-08 16:00:00] Exporting epoch_stake...
[2026-04-08 16:00:01]   Found 121 epochs to export: 504 - 624
[2026-04-08 16:00:04]   Epoch 504: 1,333,448 rows, PG COPY 165.5 MB in 2.1s, Parquet 51.4 MB in 0.8s | Total: 1,333,448 rows, 2.9s elapsed, ETA: 5.8m (1/121)
[2026-04-08 16:00:07]   Epoch 505: 1,340,000 rows, PG COPY 166.2 MB in 2.0s, Parquet 51.6 MB in 0.7s | Total: 2,673,448 rows, 5.7s elapsed, ETA: 5.6m (2/121)
...
```

## Performance

### Pipeline

| Step | Tool | Time (per epoch, local) |
|---|---|---|
| JOIN + extract | PostgreSQL `COPY TO STDOUT` | ~2-3s |
| CSV -> Parquet | pyarrow `read_csv` + `write_table` | ~0.5-1s |
| **Total per epoch** | | **~3-4s** |

**Important**: This script is designed for **local or same-LAN** execution. If running over a slow network, the COPY transfer becomes the bottleneck (e.g., 155s for 165 MB at ~1 MB/s). For remote databases, consider the DuckDB version (`export_dbsync_parquet.py`) or run this script directly on the db-sync server.

## Exported Tables

| Table | Filter | Output File | Description |
|---|---|---|---|
| `epoch_stake` | epoch >= 504 | `epoch_stake_from504.parquet` | Stake distribution with bech32 addresses and pool IDs |
| `reward` | earned_epoch >= 504 | `reward_from504.parquet` | Staking rewards (member, leader, refund) |
| `reward_rest` | earned_epoch >= 504 | `reward_rest_from504.parquet` | Non-pool rewards (reserves, treasury, proposal_refund) |
| `drep_distr` | epoch >= 504 | `drep_distr_from504.parquet` | DRep voting power distribution per epoch |
| `drep_hash` | All rows | `drep_hash.parquet` | DRep identifier lookup |
| `drep_registration` | All rows | `drep_registration.parquet` | DRep registration/deregistration events |

## Output Schema

### epoch_stake_from504.parquet

| Column | Type | Description |
|---|---|---|
| `epoch_no` | int64 | Epoch number |
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
| `epoch_no` | int64 | Epoch number |
| `drep_id` | string | DRep bech32 identifier (drep1...) |
| `has_script` | bool | Whether this is a script-controlled DRep |
| `amount` | int64 | Total voting power delegated to this DRep |
| `active_until` | int64 | Epoch until which this DRep is active (nullable) |

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
| `cert_index` | int64 | Certificate index within the transaction |
| `tx_id` | int64 | Transaction ID (db-sync internal) |
| `voting_anchor_id` | int64 | Voting anchor reference (nullable) |

## Verifying Output

```bash
# Quick preview
python3 -c "
import pyarrow.parquet as pq
t = pq.read_table('output/drep_hash.parquet')
print(f'Rows: {t.num_rows}')
print(t.to_pandas().head(10))
"

# Check metadata
python3 -c "
import pyarrow.parquet as pq
meta = pq.read_metadata('output/epoch_stake_from504.parquet')
print(f'Rows: {meta.num_rows}, Row groups: {meta.num_row_groups}')
"

# Validate row counts against database
psql -c "SELECT count(*) FROM epoch_stake WHERE epoch_no >= 504;"
```

## Notes

- Uses **PostgreSQL `COPY TO STDOUT`** — the fastest way to extract data from PostgreSQL.
- Large tables exported **epoch-by-epoch** with per-epoch timing breakdown and ETA.
- All Parquet files use **ZSTD compression** (level 3).
- Atomic writes: `.tmp` file + rename on success.
- Dependencies: `psycopg2-binary` + `pyarrow` only.
