# DB Sync Parquet Comparison

Compare a Yaci Store PostgreSQL database with DB Sync reference data exported
by `scripts/export-dbsync`.

This tool is for environments where a developer has Yaci Store plus exported
DB Sync Parquet files, but does not have direct access to the DB Sync database.

For architecture, implementation details, and design rationale, see
[`DESIGN.md`](DESIGN.md).

## Setup

```bash
pip3 install psycopg2-binary duckdb
```

## Configuration

Configuration priority matches the other scripts:

1. CLI arguments
2. JSON config file
3. defaults in `common/config.py`

Example:

```bash
cp config.example.json config.json
python3 compare.py --config config.json
```

Important settings:

- `store_url`, `store_user`, `store_password`, `store_schema`
- `dbsync_parquet_dir`
- `start_epoch`, `end_epoch`
- `models`
- `reward_types`
- `include_zero_amount`
- `max_mismatches`
- `duckdb_memory_limit`
- `reports_dir`, `logs_dir`

`start_epoch` and `end_epoch` are required for comparison. 

## Usage

Run every configured model:

```bash
python3 compare.py --config config.json
```

Run a single epoch:

```bash
python3 compare.py --config config.json --epoch 624
```

Run selected models:

```bash
python3 compare.py --config config.json --models adapot drep_amount
python3 compare.py --config config.json --models epoch_stake --include-zero-amount
```

Override the range:

```bash
python3 compare.py --config config.json --start-epoch 510 --end-epoch 520
```

## Compared Models

| Comparator | DB Sync Parquet dataset | Yaci Store table | Notes |
| --- | --- | --- | --- |
| `adapot` | `adapot` | `adapot` | Compares `treasury` and `reserves` per epoch |
| `epoch_stake` | `epoch_stake` | `epoch_stake` | Yaci Store uses `epoch = dbsync_epoch - 2`; zero amounts are ignored by default |
| `reward_rest` | `reward_rest` | `reward_rest` | Multiset comparison for each configured reward type |
| `drep_amount` | `drep_distr` | `drep_dist` | Compares normal DRep hashes plus `ABSTAIN` / `NO_CONFIDENCE` aggregate rows |
| `drep_active_until` | `drep_distr` | `drep_dist` | Compares normal DRep `active_until` values |
| `gov_action_proposal_status` | `gov_action_proposal` | `gov_action_proposal_status` | Derives Yaci Store status from DB Sync proposal lifecycle columns |

## Parquet File Resolution

For each required dataset, the tool first checks explicit `parquet_files` in the
config. If not configured, it searches `dbsync_parquet_dir` for a file covering
the requested range, for example:

```json
{
  "parquet_files": {
    "adapot": "/data/dbsync/adapot_from504_to624.parquet"
  }
}
```

Expected filenames from `scripts/export-dbsync` are supported:

- `{dataset}_from{start_epoch}_to{end_epoch}.parquet`
- `{dataset}_from{start_epoch}.parquet`

## Reports

The console and text log use the same summary style as `scripts/compare`:

```text
Comparator                               Status      Epochs  Bad epochs  Mismatches  Errors  Time(s)
adapot                                   OK             121       0/121           0       0      1.2
```

Structured reports are written to:

```text
reports/compare_dbsync_parquet_<timestamp>/
  summary.json
  summary.log
  mismatches/
    <model>_epoch_<epoch>.csv
```

The JSON summary is the source of truth for automation. The text summary is for
human review.

Exit codes:

- `0`: all selected comparators matched
- `1`: at least one comparator found mismatches
- `2`: runtime, config, schema, or connection error
