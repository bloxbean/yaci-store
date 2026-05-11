# DB-Sync Parquet Exporter

Export DB Sync data needed by `scripts/compare` into Parquet files. The output
models are comparison-oriented and are not raw one-to-one copies of DB Sync
tables.

The exporter uses `psycopg2` and `pyarrow`. It is intended to run on the same
server or same LAN as the DB Sync PostgreSQL database.

## Setup

```bash
pip3 install psycopg2-binary pyarrow
```

## Configuration

Configuration priority:

1. CLI arguments
2. `.env` file or environment variables
3. `DEFAULTS` inside `export_dbsync_parquet.py`

Supported settings:

- DB connection: `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`
- `OUTPUT_DIR`
- `START_EPOCH`
- optional `END_EPOCH`

Example:

```bash
cp .env.example .env
python3 export_dbsync_parquet.py --env-file .env
```

`END_EPOCH` is optional. If it is not set, filtered exports run from
`START_EPOCH` onward. If it is set, the export range is inclusive:
`START_EPOCH <= epoch <= END_EPOCH`.

## Usage

Export all comparison models:

```bash
python3 export_dbsync_parquet.py --env-file .env
```

Export a bounded epoch range:

```bash
python3 export_dbsync_parquet.py --env-file .env --start-epoch 740 --end-epoch 902
```

Export selected models:

```bash
python3 export_dbsync_parquet.py --env-file .env --tables adapot drep_distr
python3 export_dbsync_parquet.py --env-file .env --tables epoch_stake
```

CLI options:

```text
--tables          Models to export. Choices: adapot, epoch_stake,
                  drep_distr, reward_rest, gov_action_proposal
--output-dir      Output directory for parquet files
--env-file        Path to .env file
--start-epoch     Starting epoch
--end-epoch       Optional inclusive ending epoch
--pg-host         PostgreSQL host
--pg-port         PostgreSQL port
--pg-user         PostgreSQL user
--pg-password     PostgreSQL password
--pg-database     PostgreSQL database name
```

## Exported Models

| Model | Output file without `END_EPOCH` | Output file with `END_EPOCH` | Source tables | Compare script |
| --- | --- | --- | --- | --- |
| `adapot` | `adapot_from504.parquet` | `adapot_from504_to624.parquet` | `ada_pots` | `compare_adapot.py` |
| `epoch_stake` | `epoch_stake_from504.parquet` | `epoch_stake_from504_to624.parquet` | `epoch_stake`, `stake_address`, `pool_hash` | `compare_epoch_stake.py` |
| `reward_rest` | `reward_rest_from504.parquet` | `reward_rest_from504_to624.parquet` | `reward_rest`, `stake_address` | `compare_reward_rest.py` |
| `drep_distr` | `drep_distr_from504.parquet` | `drep_distr_from504_to624.parquet` | `drep_distr`, `drep_hash` | `compare_drep_amount.py`, `compare_drep_active_until.py` |
| `gov_action_proposal` | `gov_action_proposal_from504.parquet` | `gov_action_proposal_from504_to624.parquet` | `gov_action_proposal`, `tx`, `block` | `compare_gov_action_proposal_status.py` |

The supported export set is intentionally limited to the comparison models
listed above. Add new models to `TABLE_CONFIGS` and document their output schema
when additional comparison workflows require them.

## Range Semantics

`--end-epoch` is optional.

- `adapot`: exports epochs where `ada_pots.epoch_no >= start_epoch` and,
  when set, `ada_pots.epoch_no <= end_epoch`.
- `epoch_stake`: exports epochs where `epoch_stake.epoch_no >= start_epoch`
  and, when set, `epoch_stake.epoch_no <= end_epoch`.
- `reward_rest`: exports rows where `reward_rest.earned_epoch >= start_epoch`
  and, when set, `reward_rest.earned_epoch <= end_epoch`.
- `drep_distr`: exports rows where `drep_distr.epoch_no >= start_epoch` and,
  when set, `drep_distr.epoch_no <= end_epoch`.
- `gov_action_proposal`: exports proposals that can affect status comparison
  from `start_epoch` onward. When `end_epoch` is set, proposals must have
  `submit_epoch < end_epoch`, matching the comparator rule that a proposal first
  appears at `submit_epoch + 1`.

## Output Schemas

### `adapot`

Grain: one row per epoch, using the latest `ada_pots.slot_no` for that epoch.

| Column | Meaning |
| --- | --- |
| `epoch_no` | Epoch number |
| `slot_no` | Latest DB Sync slot for this epoch row |
| `treasury` | Treasury amount |
| `reserves` | Reserves amount |

### `epoch_stake`

Grain: one row per `(epoch_no, stake_address, pool_id)`.

| Column | Meaning |
| --- | --- |
| `epoch_no` | DB Sync epoch number |
| `stake_address` | Bech32 stake address |
| `amount` | Staked amount in lovelace |
| `pool_id` | Hex-encoded raw pool hash, aligned with `compare_epoch_stake.py` |

### `reward_rest`

Grain: one DB Sync `reward_rest` row with stake address resolved.

| Column | Meaning |
| --- | --- |
| `stake_address` | Bech32 stake address |
| `type` | Reward type: `treasury`, `reserves`, or `proposal_refund` |
| `amount` | Reward amount in lovelace |
| `earned_epoch` | Epoch when the reward was earned |
| `spendable_epoch` | Epoch when the reward can be spent |

### `drep_distr`

Grain: one row per DRep distribution entry and epoch.

| Column | Meaning |
| --- | --- |
| `epoch_no` | Epoch number |
| `drep_hash` | Hex-encoded raw DRep hash; null for special DRep entries |
| `drep_id` | Human-readable DB Sync DRep identifier |
| `has_script` | Whether the credential is script-controlled |
| `amount` | Voting power delegated to the DRep |
| `active_until` | Epoch until which the DRep is active |

### `gov_action_proposal`

Grain: one proposal row that may be relevant to the exported comparison range.

| Column | Meaning |
| --- | --- |
| `tx_hash` | Hex-encoded governance action transaction hash |
| `index` | Governance action index in the transaction |
| `type` | Governance action type |
| `ratified_epoch` | DB Sync ratified epoch |
| `enacted_epoch` | DB Sync enacted epoch |
| `dropped_epoch` | DB Sync dropped epoch |
| `expired_epoch` | DB Sync expired epoch |
| `expiration` | Proposal expiration epoch |
| `submit_epoch` | Epoch of the block containing the proposal transaction |

## Operational Notes

- `epoch_stake` is exported epoch-by-epoch and then merged into one final
  Parquet file.
- Other models are exported in one query each.
- Final Parquet writes use `.tmp` plus atomic rename.
- There is still no resume mode. If an export fails, rerun the failed model in a
  clean output directory or remove partial artifacts for that model first.
