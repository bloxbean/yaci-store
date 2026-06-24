# Fast DRep `active_until` Verifier

This tool verifies `drep_dist.active_until` without rolling back and resyncing Yaci Store.

It reads the source tables that `DRepExpiryService` uses, recomputes the expected `active_until`
in-process, and compares the result with the existing `drep_dist` snapshot. It can also compare
the recomputed value with DB Sync `drep_distr.active_until`.

The verifier never overwrites `drep_dist.active_until`. Recomputed values are written to a
separate report column named `recomputed_active_until` and to a persistent result table.

## Why use this tool

Use it when changing DRep expiry or dormant-epoch logic and you need a fast feedback loop.

The normal DB Sync comparator checks this:

```text
DB Sync drep_distr.active_until <-> existing Yaci Store drep_dist.active_until
```

That still requires Yaci Store to have regenerated `drep_dist`, which often means rollback/resync.
This verifier checks this instead:

```text
Yaci source tables + verifier logic -> recomputed active_until
recomputed active_until <-> existing Yaci Store drep_dist.active_until
recomputed active_until <-> DB Sync drep_distr.active_until (optional)
```

So you can validate an epoch or a specific DRep hash without mutating Yaci Store data.

## Requirements

- Python 3.8+
- PostgreSQL-backed Yaci Store schema
- `psycopg2`

Install the Python dependency if needed:

```bash
pip3 install psycopg2-binary
```

The tool uses the same connection argument style as the scripts in `scripts/compare`.

## Quick start

From `yaci-store/scripts/compare/drep-active-until-fast`:

```bash
python3 verify_drep_active_until.py --epoch 624
```

Verify one epoch without DB Sync:

```bash
python3 verify_drep_active_until.py --epoch 624 --skip-dbsync
```

Verify a small epoch range:

```bash
python3 verify_drep_active_until.py --start-epoch 620 --end-epoch 624 --max-mismatches 50
```

Verify one DRep hash:

```bash
python3 verify_drep_active_until.py --epoch 624 \
  --drep-hash 0123456789abcdef0123456789abcdef0123456789abcdef01234567 \
  --skip-dbsync
```

Write results to a custom persistent table:

```bash
python3 verify_drep_active_until.py --epoch 624 \
  --result-table drep_active_until_verify_result_dev
```

Use explicit connection settings:

```bash
python3 verify_drep_active_until.py --epoch 624 \
  --store-url "postgresql://localhost:5432/yaci_store" \
  --store-user yaci \
  --store-password dbpass \
  --store-schema yaci_store \
  --dbsync-url "postgresql://localhost:5678/cexplorer" \
  --dbsync-user dbsync \
  --dbsync-password dbsync
```

## Config file

You can also pass `--config` with the same shape used by the existing compare scripts:

```json
{
  "store_url": "postgresql://yaci:dbpass@localhost:5432/yaci_store",
  "store_schema": "yaci_store",
  "dbsync_url": "postgresql://dbsync:dbsync@localhost:5678/cexplorer",
  "skip_dbsync": false,
  "reports_dir": "./reports",
  "logs_dir": "./logs",
  "max_mismatches": 50
}
```

Run:

```bash
python3 verify_drep_active_until.py --epoch 624 --config config.json
```

CLI values override config-file values.

For a server without DB Sync, omit `dbsync_url` and enable recompute-only mode:

```json
{
  "store_url": "postgresql://yaci:dbpass@localhost:5432/yaci_store",
  "store_schema": "yaci_store",
  "skip_dbsync": true,
  "reports_dir": "./reports",
  "logs_dir": "./logs"
}
```

## Conway first epoch

By default, the tool tries to detect the first Conway epoch from Yaci Store data:

1. `era` + `block`
2. first `epoch_param` row where `protocol_major_ver >= 9`
3. fallback to `0`

For devnets or partial schemas, pass the value explicitly:

```bash
python3 verify_drep_active_until.py --epoch 624 --era-first-epoch 492
```

## Output and exit codes

The tool writes:

- console output unless `--quiet` is used
- a text log under `scripts/compare/logs`
- per-epoch `active_until` recompute timing and total recompute/process timing in the log
- a structured report under `scripts/compare/reports`
- full per-epoch result CSV files under the run's `results` directory
- mismatch samples as CSV under the run's `mismatches` directory
- a persistent result table in the configured Yaci Store schema
- optional JSON output with `--result-json FILE`

Each result CSV includes both columns:

- `yaci_store_active_until`: the existing value read from `drep_dist.active_until`
- `recomputed_active_until`: the value recomputed by the verifier

The persistent result table defaults to:

```text
drep_active_until_verify_result
```

Each run drops and recreates that table in `--store-schema`. Use `--result-table NAME` to choose
a different table name. The table status compares `recomputed_active_until` with
`dbsync_active_until` only. The table includes:

- `run_id`
- `epoch`
- `drep_hash`
- `drep_type`
- `recomputed_active_until`
- `dbsync_active_until`
- `status`
- `detail`
- `created_at`

Exit codes:

- `0`: all checked data matches
- `1`: mismatches were found
- `2`: a runtime or database error occurred

Mismatch issue types:

- `YACI_ACTIVE_UNTIL_MISMATCH`: recomputed value differs from existing `drep_dist.active_until`
- `DBSYNC_ACTIVE_UNTIL_MISMATCH`: recomputed value differs from DB Sync `drep_distr.active_until`
- `MISSING_REGISTRATION`: target `drep_dist` row has no registration source row up to the evaluated epoch
- `ONLY_IN_DBSYNC`: DB Sync has an `active_until` row that is absent from the Yaci target set

## What the verifier recomputes

For snapshot epoch `E`, the verifier uses `evaluated_epoch = E - 1`, matching
`DRepExpiryService.calculateAndUpdateExpiryForEpoch(E)`.

It reads:

- target DReps from `drep_dist` for epoch `E`
- latest `REG_DREP_CERT` from `drep_registration` up to `E - 1`
- DRep updates from `drep_registration`
- DRep votes from `voting_procedure`
- proposal submissions from `gov_action_proposal`
- non-dormant proposal epochs from `gov_action_proposal_status`
- protocol parameters from `epoch_param`

It mirrors `DRepExpiryUtil.calculateDRepActiveUntil`.

## Safety

The verifier does not update `drep_dist` and does not add columns to `drep_dist`.

It does intentionally drop and recreate the configured result table on every run. By default that
table is `drep_active_until_verify_result` in `--store-schema`.

It also creates a temporary target table in the PostgreSQL session to keep source queries small;
that temporary table still drops automatically when the session ends.

## Limitations

- PostgreSQL is supported. The SQL uses PostgreSQL JSON operators and temporary table syntax.
- The recompute logic is intentionally duplicated in Python for speed. When changing
  `DRepExpiryUtil.calculateDRepActiveUntil`, update this verifier if the algorithm changes.
- The tool verifies `active_until`; it does not recompute `expiry` or stake amounts.
