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

## Configuration

Nothing is hardcoded in the scripts anymore. Three things are configurable, all
outside the code:

| What | Where | Notes |
|---|---|---|
| Blockfrost API keys (secrets) | `.env` | Auto-loaded; gitignored |
| API endpoints (local + Blockfrost) | `.env` (optional) | Sensible defaults in code |
| Per-module test data (addresses, assets, pools, …) | `config/<module>.json` | One file per module |

### 1. Secrets and endpoints — `.env`

Copy the template and fill in your Blockfrost project IDs:

```bash
cd scripts/compare-blockfrost
cp .env.example .env
# edit .env
```

`.env` is loaded automatically at startup (no manual `export` needed). A real
shell `export` still takes precedence over the file.

```bash
# .env
BF_PROJECT_ID_MAINNET=mainnet...
BF_PROJECT_ID_PREPROD=preprod...
BF_PROJECT_ID_PREVIEW=preview...
```

You only need the key(s) for the network(s) you intend to test.

**Endpoint overrides (optional).** The local Yaci and Blockfrost base URLs have
built-in defaults but can be overridden via `.env` — e.g. to point at a different
host/port or a Blockfrost proxy:

| Env var | Default |
|---|---|
| `YACI_LOCAL_MAINNET` / `_PREPROD` / `_PREVIEW` | `http://localhost:8101` / `8301` / `8201` |
| `BF_URL_MAINNET` / `_PREPROD` / `_PREVIEW` | `https://cardano-<net>.blockfrost.io/api/v0` |
| `YACI_LOCAL_PREFIX` | `/api/v1/blockfrost` |

See `.env.example` for the full commented list.

**DB seeding (optional).** When a Postgres DSN is configured for a network, the
scripts seed test data (recent block/slot/tx/address/asset) straight from the DB
instead of walking the API — much faster. All keys are optional; without a DSN the
scripts fall back to the API path.

| Env var | Purpose | Default |
|---|---|---|
| `YACI_DB_MAINNET` / `_PREPROD` / `_PREVIEW` | libpq DSN/URI, e.g. `postgresql://user:pass@host:5432/yaci_store` | _(unset → API seeding)_ |
| `YACI_DB_SCHEMA` | Schema holding the store tables (global default) | `yaci_store` |
| `YACI_DB_SCHEMA_MAINNET` / `_PREPROD` / `_PREVIEW` | Per-network schema override; wins over `YACI_DB_SCHEMA`. Set only where a network's DB uses a different schema name. | _(falls back to `YACI_DB_SCHEMA`)_ |

The `psql` client must be on `PATH` for DB seeding. Queries are read-only and
bounded by a server-side `statement_timeout`.

### 2. Test data — `config/<module>.json`

Each module reads its test identifiers from `config/<module>.json`, keyed by
network. This keeps test cases out of shared code — add cases by editing JSON,
never the scripts.

```jsonc
// config/address.json
{
  "preprod": { "asset": "...", "policy_id": "...", "addresses": [] },
  "mainnet": { "asset": "...", "addresses": ["addr1...", "addr1..."] }
}
```

```jsonc
// config/epoch.json
{ "mainnet": { "epoch": "633", "pool_id": "pool1..." } }
```

If a file or network section is missing, the script falls back to
**auto-discovering** identifiers from the local chain, so it still runs
zero-config. Keys not present in config are auto-seeded (e.g. `block`, `slot`,
`tx_hash`, and — when not provided — `address`/`asset`).

### 3. Running Yaci Store instances

The scripts expect local Yaci Store containers (see `docker/deployments/`) on the
ports listed above. Override via `YACI_LOCAL_*` if your setup differs.

## Usage

Run from this directory:

```bash
cd scripts/compare-blockfrost

# Compare epoch endpoints on preprod (key comes from .env automatically)
python3 compare_epoch.py --network preprod

# Strict mode — treat known-divergent fields as real diffs too
python3 compare_epoch.py --network preprod --strict

# Save results to a specific CSV file
python3 compare_epoch.py --network preprod --csv /tmp/epoch_preprod.csv
```

The common flags apply to all `compare_*.py` scripts.

### Testing multiple datasets (address module)

`compare_address.py` tests **several addresses per run** for broader evidence.
Address selection precedence:

1. `--addresses a,b,c` — explicit comma-separated list (highest priority)
2. `config/address.json` → the network's `addresses` array (if non-empty)
3. auto-discovery — seed N distinct addresses from recent chain UTXOs

```bash
# Auto-discover and test 8 addresses
python3 compare_address.py --network preprod --samples 8 --strict

# Test specific addresses
python3 compare_address.py --network mainnet --addresses "addr1...,addr1..."

# Use the addresses pinned in config/address.json (e.g. mainnet whales)
python3 compare_address.py --network mainnet --strict
```

Each address is exercised across all endpoints and list variants; results are
tagged per address (see the `sample` column in the CSV).

### Paging and ordering (list endpoints)

List endpoints (`/blocks`, `/stakes`, `/utxos`, `/transactions`, …) accept
Blockfrost's `order` and `page`/`count` query parameters. By default each list
endpoint is compared across **both orders** (`asc`, `desc`) and **pages 1 and 2**.

| Flag | Default | Meaning |
|---|---|---|
| `--orders` | `asc desc` | Order values to exercise on list endpoints |
| `--pages` | `1 2` | Page numbers to exercise on list endpoints |
| `--count` | `100` | Page size (rows per page) for list endpoints |

```bash
# Both orders across the first three pages
python3 compare_epoch.py --network preprod --orders asc desc --pages 1 2 3

# Descending only, first page, smaller page size
python3 compare_epoch.py --network preprod --orders desc --pages 1 --count 20
```

Non-list endpoints (e.g. `/epochs/latest`) are compared once; `order`/`page` do
not apply to them.

## Output

Each run prints a per-endpoint summary:

```
OK    /addresses/{address}  [Yaci=38ms  BF=870ms]
DIFF  /addresses/{address}/utxos?order=asc&page=1  (2 field(s))  [Yaci=60ms  BF=397ms]
      [0].tx_index: missing in Yaci
```

- **OK** — responses match (after stripping known-volatile fields)
- **DIFF** — field-level differences listed with BF and Yaci values
- **STATUS** — HTTP status differs between Yaci and Blockfrost
- **SKIP** — required param missing for that endpoint
- **:warning: SLOW** badge — Yaci exceeded the latency threshold (>500 ms or >3× BF)

### CSV results

Every run appends results to a CSV. If `--csv` is not given, it defaults to:

```
scripts/compare-blockfrost/report/<module>_<network>.csv
```

Columns: `date, network, module, sample, endpoint, result, yaci_status,
bf_status, yaci_ms, bf_ms, slow, diff_count, diff_fields`. The `sample` column
identifies which dataset (e.g. address) produced the row. Rows are appended, so
repeated runs accumulate history; delete the file for a clean slate.

## Known expected divergences

Some diffs are accepted limitations rather than bugs. Strict mode (`--strict`)
surfaces them anyway; without it, fields listed in `IGNORE_DIVERGENT` (and the
always-volatile `IGNORE_VOLATILE`, e.g. `confirmations`, `next_block`) in
`bf_compare.py` are ignored. Findings per module are tracked under
`extensions/blockfrost/adr/` and in `report/` (e.g. `address_compare.html`).
