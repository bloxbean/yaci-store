# Blockfrost Compatibility Comparison Scripts

Python scripts that compare **Yaci Store** API responses against live **Blockfrost** responses, endpoint by endpoint. Used to validate parity between a local Yaci Store instance and the Blockfrost reference API.

## Contents

| Script | Endpoints compared |
|---|---|
| `compare_epoch.py` | `/epochs/*`, `/epochs/{epoch}/parameters`, `/epochs/{epoch}/next`, `/epochs/{epoch}/previous`, `/epochs/{epoch}/stakes`, `/epochs/{epoch}/blocks` |
| `compare_block.py` | `/blocks/*` |
| `compare_address.py` | `/addresses/*` |
| `compare_account.py` | `/accounts/*` |
| `compare_asset.py` | `/assets/*` |
| `compare_transaction.py` | `/txs/*` |
| `compare_metadata.py` | `/metadata/*` |
| `compare_scripts.py` | `/scripts/*` |
| `bf_compare.py` | Shared core — not run directly |

## Requirements

- Python 3.8+
- No third-party packages (uses only stdlib: `urllib`, `json`, `csv`, `argparse`)

## Setup

### 1. Running Yaci Store instances

The scripts expect local Yaci Store containers running on these ports:

| Network | Port |
|---|---|
| mainnet | `http://localhost:8101` |
| preprod | `http://localhost:8301` |
| preview  | `http://localhost:8201` |

See `docker/deployments/` for deployment instructions.

### 2. Blockfrost API keys

Obtain project IDs from [blockfrost.io](https://blockfrost.io) and export them as environment variables:

```bash
export BF_PROJECT_ID_MAINNET=mainnet...
export BF_PROJECT_ID_PREPROD=preprod...
export BF_PROJECT_ID_PREVIEW=preview...
```

You only need to set the key for the network(s) you intend to test.

## Usage

Run from this directory:

```bash
cd scripts/compare-blockfrost

# Compare epoch endpoints on preprod
python3 compare_epoch.py --network preprod

# Compare on preview
python3 compare_epoch.py --network preview

# Compare on mainnet
python3 compare_epoch.py --network mainnet

# Save results to a specific CSV file
python3 compare_epoch.py --network preprod --csv /tmp/epoch_preprod.csv

# Strict mode — exit non-zero if any diff found
python3 compare_epoch.py --network preprod --strict
```

The same flags apply to all `compare_*.py` scripts.

### Paging and ordering (list endpoints)

List endpoints (`/blocks`, `/stakes`, `/next`, `/previous`, …) accept Blockfrost's
`order` and `page`/`count` query parameters. By default each list endpoint is
compared across **both orders** (`asc`, `desc`) and **pages 1 and 2**, so a single
run validates that ordering and pagination match Blockfrost in every combination.

| Flag | Default | Meaning |
|---|---|---|
| `--orders` | `asc desc` | Order values to exercise on list endpoints |
| `--pages` | `1 2` | Page numbers to exercise on list endpoints |
| `--count` | `100` | Page size (rows per page) for list endpoints |

```bash
# Test both orders across the first three pages
python3 compare_epoch.py --network preprod --orders asc desc --pages 1 2 3

# Descending only, first page, smaller page size
python3 compare_epoch.py --network preprod --orders desc --pages 1 --count 20
```

Each (order, page) combination is reported as its own line, e.g.:

```
DIFF  /epochs/{epoch}/stakes?order=desc&page=2  (60 field(s))  [Yaci=68ms  BF=656ms]
OK    /epochs/{epoch}/blocks?order=desc&page=1  [Yaci=104ms  BF=416ms]
```

Non-list endpoints (e.g. `/epochs/latest`) are compared once; `order`/`page` do
not apply to them.

## Output

Each run prints a summary per endpoint:

```
OK    /epochs/291/blocks  [Yaci=105ms  BF=473ms]
DIFF  /epochs/291/previous  (315 field(s))  [Yaci=529ms  BF=568ms]
      [0].fees: BF='9206177808'  Yaci='678030'
      ...
```

- **OK** — responses match (after stripping known-volatile fields)
- **DIFF** — field-level differences listed with BF and Yaci values
- **SLOW** badge — Yaci response exceeded the threshold relative to BF

### CSV results

Every run also appends results to a CSV. If `--csv` is not given, it defaults to:

```
scripts/compare-blockfrost/report/<module>_<network>.csv
```

e.g. `report/epoch_preprod.csv`. The directory is created automatically. Rows are
appended (not overwritten), so repeated runs accumulate history. Pass `--csv FILE`
to write somewhere else.

## Known expected divergences

Some diffs are pre-existing data issues, not bugs. They are documented in `bf_compare.py` under `IGNORE_FIELDS` / comments. Current out-of-scope items:

| Field | Reason |
|---|---|
| `nonce` | Epoch nonce not yet computed (BUG-4, out of scope) |
| `fees` | Fee aggregation incomplete for in-progress epochs (BUG-2) |
| `stakes` data | Stake snapshot divergence vs Blockfrost reference data |
