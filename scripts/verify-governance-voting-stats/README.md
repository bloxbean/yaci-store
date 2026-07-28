# Governance Voting-Stats Verifier

Standalone Python verifier for
`gov_action_proposal_status.voting_stats`. It compares each proposal's latest
Yaci Store status with AdaStat governance-action data and does not access
Cardano DB Sync.

AdaStat is treated as an independent reference, not an authoritative ledger
oracle. Missing bodies, live proposals, and temporal incompatibility remain
visible instead of being converted to zero or reported as matches.

## Requirements

- Python 3
- `psycopg2-binary`

```bash
pip3 install psycopg2-binary
cp config.example.json config.json
```

## Usage

Run commands from this directory:

```bash
# All latest proposal statuses
python3 compare_gov_action_voting_stats.py \
  --network preview \
  --config config.json

# One proposal
python3 compare_gov_action_voting_stats.py \
  --network preview \
  --proposal '06dcb60f4b6ee78024bd4c7978e8e093437903198cf06c3c5d34bf825129bc73#0' \
  --config config.json

# Optional batching filter on the latest Store row's epoch
python3 compare_gov_action_voting_stats.py \
  --network mainnet \
  --start-epoch 640 \
  --end-epoch 650 \
  --config config.json

# Fail when any selected proposal is inconclusive
python3 compare_gov_action_voting_stats.py \
  --network preprod \
  --fail-on-inconclusive \
  --config config.json
```

Epoch options are not required. Without them, the verifier selects the latest
Store row for every proposal. The optional filters are:

- `--epoch E`: latest row is at exactly `E`;
- `--start-epoch E`: latest row is at or after `E`;
- `--end-epoch E`: latest row is at or before `E`;
- `--start-epoch A --end-epoch B`: latest row is in the inclusive range.

These options limit the number of proposals in one run. They do not request
historical AdaStat data.

The required `--network` value selects one fixed endpoint:

| Network | AdaStat base URL |
|---|---|
| `mainnet` | `https://adastat.net/api/rest/v2` |
| `preprod` | `https://preprod.adastat.net/api/rest/v2` |
| `preview` | `https://preview.adastat.net/api/rest/v2` |

No network is selected by default. `--adastat-base-url` is available only for
a test server or mirror.

The AdaStat route ID is the lowercase transaction hash followed by the action
index as two lowercase hexadecimal characters. For example, proposal index
`#10` has suffix `0a`. The CLI filter remains
`TX_HASH#DECIMAL_INDEX`; indexes above 255 are reported as unsupported.

## Latest-status selection

`gov_action_proposal_status` contains one snapshot per proposal and epoch. The
verifier first selects the row with the greatest `epoch` for each
`(gov_action_tx_hash, gov_action_index)` across the whole table. If an epoch
filter is provided, it is then applied to the epoch of that latest row.

For example:

```text
Proposal A
  epoch 100: ACTIVE
  epoch 101: ACTIVE
  epoch 102: RATIFIED  <- selected
```

Proposal A is processed once as `RATIFIED`. Its historical ACTIVE snapshots
are not logged or sent to AdaStat. An epoch filter therefore scopes proposals
by their latest-row epoch; it does not replay snapshots from those epochs.

## Temporal contract

AdaStat exposes live data for an active proposal, not arbitrary historical
epoch snapshots. Comparison is therefore limited to:

| Yaci row | AdaStat requirement | Result |
|---|---|---|
| `RATIFIED` at epoch `E` | `ratified_epoch == E` | Compare available fields |
| `EXPIRED` at epoch `E` | `expired_epoch == E` | Compare available fields |
| `ACTIVE` | No historical snapshot | `INCONCLUSIVE_LIVE` |
| Terminal row with another outcome epoch | Snapshot does not align | `INCONCLUSIVE_EPOCH_MISMATCH` |

ACTIVE proposals are counted in the summary and JSON report, but are not
logged individually or sent to AdaStat. An ACTIVE-only run exits `2` because
zero fields were compared.

## AdaStat reference coverage

Coverage is the number of Yaci fields for which AdaStat provides enough input
data to build an expected value. It is **not** a correctness percentage and
does not say how many fields matched.

The canonical Yaci `voting_stats` model contains 23 fields in three voting
bodies:

```text
DRep: 10 fields
SPO:   8 fields
CC:    5 fields
----------------
Total: 23 fields
```

AdaStat does not expose every voting body for every action type. The following
table describes the maximum reference coverage available from AdaStat:

| Action type | Comparable DRep fields | Comparable SPO fields | Comparable CC fields | Max comparable fields |
|---|---:|---:|---:|---:|
| No Confidence | 10 | 8 | — | 18/23 |
| New Committee | 10 | 8 | — | 18/23 |
| New Constitution | 10 | — | 5 | 15/23 |
| Hard Fork Initiation | 10 | 8 | 5 | 23/23 |
| Treasury Withdrawal | 10 | — | 5 | 15/23 |
| Parameter Change | 10 | 8 | 5 | 23/23 |
| Info Action | 10 | 8 | 5 | 23/23 |

Here, `—` means that AdaStat does not provide that body as a reference. It does
not mean the corresponding Yaci values are zero or incorrect.

For example, a New Committee action has a maximum coverage of 18/23:

```text
DRep: 10 fields compared
SPO:   8 fields compared
CC:    5 fields unavailable from AdaStat
```

If all 18 comparable fields match, the current result is `PARTIAL_MATCH`.
This means **maximum AdaStat reference coverage was reached and every
comparable field matched**. It does not mean that five fields mismatched.

Reports keep the following counts separate:

- `selected_fields`: all 23 canonical Yaci fields per selected proposal;
- `compared_fields`: fields for which a safe AdaStat expected value exists;
- `matched_fields`: compared fields whose values are equal;
- `unavailable_fields`: fields AdaStat cannot safely provide;
- `total_mismatches`: compared fields whose Yaci value differs, is missing,
  null, or invalid.

The table is the theoretical maximum. Actual coverage can be lower when an
expected AdaStat body is entirely null. Such a body is
`BODY_UNAVAILABLE`; a partially populated body is a schema error. A null
reference value is never treated as zero.

Stake and count values are exact non-negative integers. Approval ratios use
`Decimal`, four decimal places, and `ROUND_HALF_UP`. The basic remainders are:

```text
DRep do-not-vote =
  total - yes - no - abstain - always-abstain
        - always-no-confidence - inactive

SPO do-not-vote =
  total - yes - no - abstain - always-abstain
        - always-no-confidence

CC do-not-vote = total - yes - no - abstain
```

The final totals apply Yaci's action-type, bootstrap-period, Hard Fork, Always
Abstain, and Always No Confidence rules. The AdaStat mapping was checked
against source revision
`96b2c78d980335983a5bfa2100dbaf8ec3b9c930`.

## HTTP behavior

The client is sequential and:

- uses a 20-second default timeout;
- allows three retries after the first attempt;
- maintains a 1.1-second default minimum request interval;
- honors `Retry-After`;
- retries timeout, connection failure, `429`, and `5xx`;
- does not retry `400` or `404`;
- requests `Accept-Encoding: identity`;
- caches each action for one run.

## Results and reports

Proposal results are:

- `MATCH`: all 23 fields are available and match.
- `PARTIAL_MATCH`: every field available from AdaStat matches, but AdaStat
  cannot cover all 23 canonical fields. For 15/23 and 18/23 action types this
  can represent the maximum expected reference coverage.
- `MISMATCH`: a comparable Yaci field differs, is missing, null, or invalid.
- `INCONCLUSIVE`: there is no temporally safe comparison.
- `ERROR`: Store, HTTP, response-schema, or identity validation failed.

Exit codes are:

- `0`: at least one field was compared, with no mismatch or error.
- `1`: at least one valid field mismatch and no operational error.
- `2`: operational error, zero compared fields, or strict inconclusive
  failure.

Each run writes:

```text
reports/compare_gov_action_voting_stats_<timestamp>/
  summary.log
  summary.json
  coverage.csv
  mismatches/
    gov_action_voting_stats_epoch_<epoch>.csv
  references/
    <66-hex-action-id>.json
```

The mismatch CSV includes source components and signed deltas. Coverage is
reported separately, and saved references contain no Store credentials.

## Validation

```bash
python3 -m unittest discover -s tests -p 'test_*voting_stats*.py' -v
python3 compare_gov_action_voting_stats.py --help
```

Recorded read-only checks cover mainnet, preprod, preview, RATIFIED, EXPIRED,
bootstrap and post-bootstrap cases, and 15/18/23-field action types. A preview
range over epochs `1369..1370` selected 53 rows: five terminal proposals made
exactly five HTTP requests, while 48 ACTIVE rows remained inconclusive.
