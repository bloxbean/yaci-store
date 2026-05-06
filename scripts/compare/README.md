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
| `--quiet` | Write to log file only, do not echo to console |

Script-specific flags:

- `compare_reward_rest.py`: `--reward-type {treasury,reserves,proposal_refund}` (default `proposal_refund`)
- `compare_epoch_stake.py`: `--reverse` (iterate high → low), `--delay SECONDS` (sleep between epochs to avoid DB overload)
- `compare_all.py`: `--only KEY[,KEY...]` / `--skip KEY[,KEY...]` to filter the set of comparators run, and `--reward-types treasury,reserves,proposal_refund` to choose which reward types `compare_reward_rest.py` runs for (default: all three). Comparator keys: `adapot`, `epoch_stake`, `reward_rest`, `drep_amount`, `drep_active_until`, `gov_action_proposal_status`.

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
```

The wrapper exits with a non-zero status if any child comparator returned a non-zero exit code, so it can be wired into CI directly. Example overall summary:

```
============================================================
OVERALL SUMMARY (compare_all)
============================================================
  Total runtime : 312.4s
  Comparators   : 8

  Comparator                                 RC    Time(s)
  ---------------------------------------- ----  ----------
  adapot                                     OK        2.1
  epoch_stake                                OK      120.5
  reward_rest (type=treasury)                OK       18.2
  reward_rest (type=reserves)                OK       17.9
  reward_rest (type=proposal_refund)         OK       18.5
  drep_amount                                OK       45.0
  drep_active_until                          OK       40.1
  gov_action_proposal_status                 OK       50.1

  Failures      : 0/8
============================================================
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

- **epoch_stake**: Yaci Store query uses `epoch = epoch - 2`, mirroring the offset in the original Java comparator.
- **drep_dist**: `ABSTAIN` and `NO_CONFIDENCE` are excluded from the per-hash comparison and checked separately as aggregate totals.
- **gov_action_proposal_status**: Yaci Store has only three statuses (`ACTIVE`, `RATIFIED`, `EXPIRED`) and stores one row per (proposal, epoch) snapshot. DB Sync stores one row per proposal with end-state epoch columns; the script derives the expected Yaci status from `ratified_epoch`, `enacted_epoch`, `dropped_epoch`, `expired_epoch`, and `submit_epoch`. See the comments in `compare_gov_action_proposal_status.py` for the full lifecycle mapping.
- **reward_rest**: compared as a multiset (counts of identical rows must match), not just presence/absence.
