# ADR 0007: A table for what is dropped (`invalid_token`)

## Status

Proposed. Implemented in PR #1220 (open).

## Date

2026-10-21

## Context

ADR 0006 indexes every CIP-26 entry and CIP-68 datum that can be read and reports what is wrong in `validation_errors`. What is still
dropped (decision 3 of ADR 0006: an undecodable datum, a datum too deep to decode, an unexpected processing failure, a CIP-26 file whose
name differs from its subject, a subject that cannot be a key, a file that cannot be parsed) left a WARN in the log of one node and a counter,
and nothing in the database. A mainnet or preprod operator could not ask "which tokens did we drop, and why" without grepping logs, and the
owner of a dropped token could not be told anything.

CIP-113 (programmable tokens) will add more such items. A registry node links policies; tokens are held at a "jail" address
(`programmableLogicBase`); the token's metadata is CIP-26 or CIP-68, so a programmable token is CIP-26 + CIP-113 or CIP-68 + CIP-113.
Failures there are common and are not about one datum: a jail minted without a token to put in it, a policy found in the jail but not in the
registry, a registry node that cannot be parsed or that looks spoofed. The module for CIP-113 was removed from `main`; this record makes room
for it without implementing it.

## Decision

### 1. One generic table, one row per dropped item

`invalid_token` (migration `V0_1700_3`, per dialect) holds the item, not the token, so the same schema serves any standard:

| Column | Meaning |
|---|---|
| `id` | SHA-256 of `standard, reason_code, policy_id, asset_name, tx_hash, output_index, origin` |
| `standard` | the standard whose rule found the problem: `CIP-26`, `CIP-68`, `CIP-113` |
| `combined_with` | the other standard of a combination, so a CIP-68 token in a jail is `standard=CIP-113, combined_with=CIP-68` (or the reverse, as the writer sees it) |
| `reason_code`, `reason` | a stable code for programs, a message for people (at most 500 characters) |
| `policy_id`, `asset_name`, `subject` | the token, when it is known |
| `slot`, `tx_hash`, `tx_index`, `output_index` | the on-chain origin |
| `origin` | the off-chain origin (the registry file name) |
| `details` | bounded JSON (at most 2 KB) for what is specific to a standard: `inner_subject`, `datum_prefix`, `exception`; later `registry_key`, `script_hash`, `jail_address` |
| `first_seen_at` | |

*Why generic:* a column per standard would need a migration for every new one, and the question asked of it is the same: what, where, why.

### 2. Only what is dropped or unusable

A token that is indexed keeps its problems in `validation_errors` of its row (ADR 0006) and is **not** in this table. The table is the
complement: the items that have no row to carry a list of errors. The two never describe the same thing.

Codes written now: CIP-26 `filename_subject_mismatch`, `unusable_subject`, `unparseable_mapping`, `missing_git_history`, `logo_too_large`; CIP-68
`datum_undecodable`, `datum_too_deep_to_decode`; both standards `processing_failure` (something nobody foresaw threw). A datum that is not CIP-68 at
all (a reference NFT output can carry any datum) is ignored, not recorded.

Two of them follow the review of PR #1220: an exception of the CIP-26 check was recorded as `unusable_subject`, although the subject may be fine,
and is `processing_failure`; and a CIP-26 logo far beyond any real size, dropped while its entry is stored, had no record and is `logo_too_large`.
That logo is the one item here whose token has a row: the row has no logo, so the dropped item is the logo.

Reserved for CIP-113, documented and not written: `jail_without_token`, `unregistered_policy_in_jail`, `registry_node_unparseable`,
`registry_node_spoofed`. CIP-113 says a registration with no mint is valid for real-world assets, so `jail_without_token` is a **finding**,
not always a failure; whether it belongs in this table or only in a report is left open until CIP-113 is indexed.

### 3. Written safely from the block transaction

The CIP-68 processor runs in the block transaction, where an exception stops the sync (ADR 0004). The writer is therefore:

- **bounded:** every text and `details` are cut, so an attacker cannot make a row large;
- **idempotent:** the id is derived from the item, an existing id is left alone, so a replayed block, a rollback or a re-run adds nothing
  and `first_seen_at` is kept;
- **contained:** an exception of its own (building or saving the row) is a WARN, never thrown. A failure that shows only at flush or commit,
  inside the database, is not caught here; it is #1265.

A flat datum shared by several reference NFTs is recorded once per output. A `processing_failure` records one row per reference NFT of the output.

### 4. CIP-26 rows say what is wrong now

The registry is synchronised outside the block transaction. At the start of each file the rows of that file are forgotten and the file is
judged again, and when a file is deleted its rows go, so a file that was fixed upstream stops appearing. A row without a slot is not touched by a
chain rollback.

### 5. Rollback and API (no endpoint for now)

Rows with a `slot` greater than the rollback slot are deleted (`InvalidTokenRollbackProcessor`, and `rollback.yml` for the DB utilities). There
is no API: the table is read with SQL and in the sync report, and the counter `yaci.store.assets.invalid_tokens{standard,reason_code}` counts
the rows written.

## Consequences

- An operator can list what was dropped and why, and can tell an owner. The mainnet report reads the table instead of grepping logs.
- Writes add one `existsById` and one insert per dropped item. Dropped items are rare (thousands of CIP-26 files on testnets, a handful of
  datums on mainnet), so the cost is small; an attacker is bounded by the transaction size, not by the table.
- The table is not complete for a database synced before it existed: a full resync is required anyway (ADR 0006 decision 4).
- Adding CIP-113 needs new codes and a writer, no migration, unless a combination needs a column that `details` cannot carry.

## Alternatives considered

- **Log only.** The status quo: not queryable, per node, lost with the log files.
- **Record every item, including tokens with `validation_errors`.** Duplicates the errors and would hold tens of thousands of rows that
  already have the information.
- **One table per standard.** Rejected as above; it would also make a combination such as CIP-68 + CIP-113 awkward.
- **Write from a separate transaction or thread.** It would isolate a failure better, but a record could then be lost or outlive a rolled-back
  block; the bounded, idempotent write in the same transaction gives a consistent view with a simpler design.

## Open questions

- Whether `jail_without_token` is a failure row or a finding only in the report.
- How long CIP-26 rows are kept for a file that disappears and comes back.

## References

- ADR 0004 (processing failures), ADR 0006 (validation errors; decision 3 lists what is dropped)
- CIP-113 (programmable tokens); issue #1265
- `InvalidTokenRecorder`, `InvalidTokenRollbackProcessor`, `InvalidTokenReason`
