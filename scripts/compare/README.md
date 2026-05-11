# Yaci Store ↔ DB Sync Comparison Scripts

Python scripts that compare data between a **Cardano DB Sync** PostgreSQL database and a **Yaci Store** PostgreSQL database, epoch by epoch. They replace the older Java-based `*DataComparator` utilities — no build step, configurable via JSON file or CLI flags.

## Contents

| Script | What it compares | Source tables |
|---|---|---|
| `compare_all.py` | Runs every comparator below in one command | — |
| `compare_adapot.py` | `treasury` and `reserves` per epoch | `ada_pots` ↔ `adapot` |
| `compare_epoch_stake.py` | Per-(stake address, pool) staked `amount` | `epoch_stake` ↔ `epoch_stake` (Yaci uses `epoch - 2` offset) |
| `compare_reward_rest.py` | `reward_rest` rows as a multiset (address, type, earned_epoch, amount, spendable_epoch) | `reward_rest` ↔ `reward_rest` |
| `compare_drep_amount.py` | DRep distribution `amount` per drep hash + `ABSTAIN` / `NO_CONFIDENCE` aggregates | `drep_distr` ↔ `drep_dist` |
| `compare_drep_active_until.py` | DRep `active_until` epoch per drep hash | `drep_distr` ↔ `drep_dist` |
| `compare_gov_action_proposal_status.py` | Governance action proposal status (`ACTIVE` / `RATIFIED` / `EXPIRED`) — derived from DB Sync epoch columns | `gov_action_proposal` ↔ `gov_action_proposal_status` |
| `common.py` | Shared utilities: config loading, DB connection, `Logger`, hash normalization | — |
| `config.json` / `config.example.json` | Default connection settings | — |

## Requirements

- Python 3
- `psycopg2-binary`

```bash
pip3 install psycopg2-binary
```

## Configuration

Connections can be supplied through a JSON config file, environment URLs, or individual CLI flags. Resolution order: built-in defaults → config file → CLI args (CLI wins).

`config.example.json`:

```json
{
  "dbsync_url": "postgresql://10.4.10.135:5678/cexplorer",
  "dbsync_user": "dbsync",
  "dbsync_password": "dbsync",
  "store_url": "postgresql://10.4.10.112:5432/yaci_store",
  "store_user": "yaci",
  "store_password": "dbpass",
  "store_schema": "yaci_store",
  "quiet": false,
  "max_mismatches": 0,
  "delay": 0
}
```

Copy it to `config.json` and edit:

```bash
cp config.example.json config.json
```

## Common CLI flags

All scripts accept the same base flags:

| Flag | Purpose |
|---|---|
| `--config FILE` | Path to a JSON config file |
| `--dbsync-url URL` | DB Sync PostgreSQL connection URL |
| `--dbsync-user` / `--dbsync-password` | Override userinfo on the DB Sync URL |
| `--store-url URL` | Yaci Store PostgreSQL connection URL |
| `--store-user` / `--store-password` | Override userinfo on the Yaci Store URL |
| `--store-schema NAME` | Yaci Store schema (default `yaci_store`) |
| `--epoch N` | Compare a single epoch |
| `--start-epoch N --end-epoch M` | Compare an inclusive epoch range |
| `--max-mismatches N` | Stop printing once `N` mismatches are reached for that epoch (0 = unlimited) |
| `--quiet` | Write to log file only, do not echo to console. In `compare_all.py`, suppress child output but still print wrapper progress and final summary |

Script-specific flags:

- `compare_reward_rest.py`: `--reward-type {treasury,reserves,proposal_refund}` (default `proposal_refund`)
- `compare_epoch_stake.py`: `--reverse` (iterate high → low), `--delay SECONDS` (sleep between epochs to avoid DB overload), `--include-zero-amount` (compare raw `epoch_stake` rows instead of ignoring `amount = 0`)
- `compare_all.py`: `--only KEY[,KEY...]` / `--skip KEY[,KEY...]` to filter the set of comparators run, `--reward-types treasury,reserves,proposal_refund` to choose which reward types `compare_reward_rest.py` runs for (default: all three), and `--summary-file FILE` to override the final summary output path. Comparator keys: `adapot`, `epoch_stake`, `reward_rest`, `drep_amount`, `drep_active_until`, `gov_action_proposal_status`.

## Run everything in one command

`compare_all.py` is a wrapper that runs every comparator in this directory sequentially against the same epoch (or epoch range), forwards all common flags, and prints a combined summary at the end. Each child script still writes its own timestamped log under `logs/`.

```bash
# Run every comparator for a single epoch
python3 compare_all.py --epoch 800 --config config.json

# Run every comparator across an epoch range
python3 compare_all.py --start-epoch 740 --end-epoch 902 --config config.json

# Run only a subset
python3 compare_all.py --epoch 800 --only adapot,drep_amount --config config.json

# Run everything except the heavy ones
python3 compare_all.py --epoch 800 --skip epoch_stake,reward_rest --config config.json

# Restrict reward_rest to specific types (default runs all three)
python3 compare_all.py --epoch 1075 --reward-types treasury,reserves --config config.json

# Write the final summary to a custom file
python3 compare_all.py --epoch 800 --summary-file /tmp/yaci_compare_summary.log --config config.json
```

The wrapper streams each child comparator as it runs, then repeats the important result fields at the end so you do not have to scroll back through the output. It also writes the same final summary to `logs/compare_all_summary_<timestamp>.log` by default, or to `--summary-file FILE` when provided. It exits with a non-zero status if any comparator reports mismatches, errors, or an unparseable summary. Example final summary:

```
====================================================================================================
FINAL RESULT SUMMARY (compare_all)
====================================================================================================
  Started at        : 2026-05-11T10:00:00
  Finished at       : 2026-05-11T10:05:12
  Command           : /usr/bin/python3 compare_all.py --start-epoch 740 --end-epoch 902 --config config.json
  Epoch scope       : epochs 740 -> 902
  Total runtime     : 312.4s
  Comparators run   : 8
  Status counts     : OK=7, MISMATCH=1, ERROR=0, UNKNOWN=0
  Total mismatches  : 12

  Comparator                               Status      Epochs  Bad epochs  Mismatches   RC  Time(s)
  ---------------------------------------- --------- -------- ----------- ----------- ---- --------
  adapot                                   OK             163       0/163           0    0      2.1
  epoch_stake                              MISMATCH       163       4/163          12    0    120.5
  reward_rest (type=treasury)              OK             163       0/163           0    0     18.2
  reward_rest (type=reserves)              OK             163       0/163           0    0     17.9
  reward_rest (type=proposal_refund)       OK             163       0/163           0    0     18.5
  drep_amount                              OK             163       0/163           0    0     45.0
  drep_active_until                        OK             163       0/163           0    0     40.1
  gov_action_proposal_status               OK             163       0/163           0    0     50.1

  Logs:
    adapot: /path/to/scripts/compare/logs/adapot_compare_20260511_100000.log
    epoch_stake: /path/to/scripts/compare/logs/epoch_stake_compare_20260511_100002.log
    ...
====================================================================================================

Final summary written to: /path/to/scripts/compare/logs/compare_all_summary_20260511_100000.log
```

## Logging

Every run writes a timestamped log file under `logs/`:

```
logs/adapot_compare_<timestamp>.log
logs/epoch_stake_compare_<timestamp>.log
logs/reward_rest_compare_<type>_<timestamp>.log
logs/drep_compare_amount_<timestamp>.log
logs/drep_compare_active_until_<timestamp>.log
logs/gov_action_proposal_status_compare_<timestamp>.log
logs/compare_all_summary_<timestamp>.log
```

Each log ends with a summary block:

```
SUMMARY (...):
  Epochs compared     : N
  Epochs w/ mismatch  : X/N
  Total mismatches    : M
```

## Examples

```bash
# Compare AdaPot treasury/reserves for one epoch
python3 compare_adapot.py --epoch 800 --config config.json

# Compare epoch_stake across a range, with 5-second pause between epochs
python3 compare_epoch_stake.py --start-epoch 740 --end-epoch 902 --delay 5 --config config.json

# Compare reward_rest with a specific reward type
python3 compare_reward_rest.py --epoch 1075 --reward-type treasury --config config.json

# Compare DRep distribution amounts for a range
python3 compare_drep_amount.py --start-epoch 510 --end-epoch 520 --config config.json

# Compare DRep active_until
python3 compare_drep_active_until.py --epoch 624 --config config.json

# Compare governance action proposal status
python3 compare_gov_action_proposal_status.py --start-epoch 510 --end-epoch 520 --config config.json

# Run every comparator above in one command
python3 compare_all.py --start-epoch 510 --end-epoch 520 --config config.json
```

## Notes on the underlying data

- **epoch_stake**: Yaci Store query uses `epoch = epoch - 2`, mirroring the offset in the original Java comparator. By default, zero-amount rows are ignored on both sides to match current DB Sync active-stake semantics; use `--include-zero-amount` if you need to inspect raw table differences.
- **drep_dist**: `ABSTAIN` and `NO_CONFIDENCE` are excluded from the per-hash comparison and checked separately as aggregate totals.
- **gov_action_proposal_status**: Yaci Store has only three statuses (`ACTIVE`, `RATIFIED`, `EXPIRED`) and stores one row per (proposal, epoch) snapshot. DB Sync stores one row per proposal with end-state epoch columns; the script derives the expected Yaci status from `ratified_epoch`, `enacted_epoch`, `dropped_epoch`, `expired_epoch`, and `submit_epoch`. See the comments in `compare_gov_action_proposal_status.py` for the full lifecycle mapping.
- **reward_rest**: compared as a multiset (counts of identical rows must match), not just presence/absence.
