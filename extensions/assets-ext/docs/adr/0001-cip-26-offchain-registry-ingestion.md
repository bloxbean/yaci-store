# ADR 0001: CIP-26 off-chain registry ingestion

## Status

Proposed. Implemented in PR #1220. An entry that breaks CIP-26 is stored with `validation_errors` instead of being rejected (decisions
3 to 5, ADR 0006).

## Date

2026-10-07

## Context

CIP-26 token metadata does not come from the chain. It comes from a Git repository (the token
registry), which is a separate trust source with its own data quality problems: files that do not
match their own subject, entries that fail the CIP-26 rules, test files, files that are later
removed. The module has to ingest it periodically, keep the sync alive when some entries are bad,
and still reflect removals.

This ADR does not restate CIP-26. It records the choices the module made where the standard or the
data left room, and why. For how CIP-26 and CIP-68 are combined when a subject has both, see
[ADR 0002](0002-cip-68-onchain-datum-handling.md), section "Read model". For what the module does with an entry that does
not follow CIP-26, see [ADR 0006](0006-validation-errors-are-reported-not-enforced.md).

## Decisions

### 1. The registry is chosen from the connected network

`Cip26NetworkDefaults` derives the registry from `store.cardano.protocol-magic`:

| Network | Repository | Folder |
|---|---|---|
| Mainnet | `cardano-foundation/cardano-token-registry` | `mappings` |
| Preprod | `input-output-hk/metadata-registry-testnet` | `registry` |
| Any other | none: CIP-26 sync is disabled | |

Explicit `git-organization` / `git-project-name` / `git-mappings-folder` settings win, and are the
only way to enable CIP-26 on another network.

*Why:* a registry that does not exist for a network must not make the module fail or try to clone a
wrong repository. *Trade-off:* a network with a registry we do not know about needs configuration.

### 2. Sync is periodic, incremental and resumable

- A scheduled job (fixed delay, 60 minutes by default, first run after one minute) clones or pulls
  the repository into a temporary folder and records the processed HEAD commit in
  `cip26_sync_state`.
- With a stored commit, only the files changed or deleted since then are processed. Without one
  (first run), everything is processed. If HEAD cannot be determined, it falls back to a full sync
  without tracking.
- The job is not scheduled in read-only mode: it would otherwise clone and write on a timer.

**Chain rollbacks do not apply.** The registry is read from Git, not from the chain, and a row has no slot or
block, so there is nothing to undo when the chain rolls back. (CIP-68 is different, see ADR 0002 decision 1.)

*Why:* the registry is large (about 8,000 files on mainnet) and changes slowly, so re-reading all
of it every hour is wasteful. *Trade-off:* a Git clone needs network access and disk, and the
sync state lives in the database next to the data.

### 3. A bad entry never blocks the sync, a recoverable failure does

Each file ends in one of: inserted (with its validation errors, if any), skipped (no mapping, filename mismatch, a subject that
cannot be a key, a logo far beyond any real size), permanently failed, or transiently failed. The stored commit advances only if **no**
entry failed transiently, so those entries are retried on the next run. Permanent skips do not
hold it back.

A database error is classified as permanent (constraint violation, column too narrow, encoding)
or transient. An error of an unknown type is treated as **transient**.

*Why:* if permanent skips blocked the cursor the sync would loop forever on one bad file, and if an
unknown error were treated as permanent a recoverable problem would silently drop a token.
*Trade-off:* an unclassified error that is really permanent is retried on every run and shows up
in the logs each time; it needs a person to look at it.

### 4. What is checked: errors for the CIP, a drop only for what cannot be stored

An entry is **stored as written** whatever is wrong with it, and what is wrong is kept in its `validation_errors` (ADR 0006). The checks are
the rules of the shared `cf-tokens-cip26` library (the registry's validator) and the module's range check of `decimals`; each finding is one
entry `{cip: "CIP-26", field_name, field_category, code, error}` (there is no `sub_standard` for CIP-26), and is returned by the API.

- **`name` and `description` are the two properties the registry requires**, so they are `MANDATORY`: a missing one is a `missing_value` error, and
  one that breaks a limit (`name` over 50 characters, `description` over 500) is an `invalid_length` error on a mandatory field. Note that CIP-26
  itself requires only the `subject`: its JSON schema lists `subject` as the one required property, and `name`, `description`, `ticker`,
  `decimals`, `url` and `logo` are well-known properties whose values must be well-formed if present. The requirement of `name` and `description`
  comes from the library and the registry, not from the CIP text, and it is what `field_category` reports.
- **`ticker` (2 to 9 characters), `url` (250), `decimals`, `logo`** are `OPTIONAL`: a value outside its limit is an `invalid_length`, `wrong_type`
  or `out_of_range` error, and the value is stored as written.
- **A `url` must be a URI with `https` and a host.** The library checks the scheme only, so `https:/rosen.tech` (one slash) passed. A `url` that
  is not a valid URI, not `https`, or has no host is an `OPTIONAL` `invalid_value` error that quotes it, unless the library already reported the
  `url`. Found on the mainnet validation run of cf-token-metadata-registry (#117).
- **`decimals` is in [0, 255]** (`TokenDecimals`). The library only checks `>= 0` and does so on an `int`, so the module range-checks the stored
  `long` first (4294967301 would otherwise pass as 5); a value outside the range is stored and reported as `out_of_range`. A `decimals` that is
  not a number is stored as no value, with a `wrong_type` error that quotes it (the raw string stays in the mapping).
- **The columns have no length limit** (`TEXT`): the CIP-26 limits are reported, not enforced, so a long `name` does not fail the insert.

What is still dropped, with one row in `invalid_token` (ADR 0007) and a WARN where it looks like an attack (a file whose name differs from its subject is a DEBUG line, since a registry can have thousands; the sync tally counts them):

- **The filename is the subject.** The file `<subject>.json` must contain that same `subject`. Otherwise the file is skipped. Preprod's registry has
  5,008 such files (about 90% of it), mostly copies of one test token under padded filenames; mainnet has none. They are spam or orphans, and the
  rows would collide (last write wins), so they are not indexed.
- **A subject that cannot be a key**: missing, not an even-length hex string, or not 56 to 120 characters. Nothing could look the entry up.
- **An unparseable file** (not JSON, or not a mapping).

*Why report and not reject* (the alternative is to reject an entry for any failure, including an optional field): the entry is from the registry,
which is the maintainers' own curated source, and the owners of a token whose entry is rejected see nothing; with errors they see what to fix. The registry also validates entries on GitHub before they are merged (the maintainers'
statement), so few errors are expected, and those that do arrive are better returned than hidden. *Trade-off:* a registry entry with a typo
in its subject is still absent (the subject is what keys it). On preprod that is correct (a "Diffusion" entry with a dropped hex digit is
rejected while the correct entry is stored), but it also means a registry mistake can hide a token: the only registry entry for the preprod USDM
token under policy `d8906ca5…` has the policy id pasted twice (128 characters) and is not indexed.

### 5. The logo is checked and stored separately

Metadata and logo are two steps. The logo should be valid base64 and at most 64 KiB decoded; the image format (PNG, SVG) is not checked, so the
module is not stricter than the library. A logo that is not valid base64, or is over 64 KiB, is **kept as written** with an `OPTIONAL` error
(`invalid_base64`, `too_large`) added to the row. Only a logo far beyond any real size (over about 1.4 million characters, about 1 MiB) is
dropped, with a WARN that says it looks like an attack: the logo is then missing, and the metadata is still saved. A logo with no metadata row
(an orphan logo) is skipped.

*Why:* the registry is curated, so an oversized logo is more likely a mistake to fix than an attack, and a token with good metadata and a big
logo is still worth serving. *Trade-off:* a stored token can have a logo that no client can show, which the error says; and a logo between 64 KiB
and 1 MiB is stored in the row.

### 6. Removals are followed, with a guard

A subject whose mapping file is deleted upstream is deleted locally, logo included (it is on the
same row). In an incremental sync this comes from the Git diff. In a full sync the module
reconciles: a locally stored subject with no file in the registry is deleted, **unless the registry
contains no mapping files at all**, in which case the cleanup is skipped and a warning is logged.

*Why:* removals matter (a token can be withdrawn), but an empty or broken clone must not wipe the
table. *Trade-off:* if the registry is really emptied, the local data stays until someone cleans it.

### 7. One row per subject, in lower case, no signatures

The table keeps the latest version of a subject (`updated` and `updated_by` come from the Git
history of the file).

**A subject is stored in lower case.** It is base16, and its case carries no meaning, but 39 mainnet registry files are named and
written in upper case, and they were stored as written: a client that asked in lower case, as every CIP-68 and on-chain subject is
written, got a `404`. The subject is lower-cased when the entry and its logo are stored, when an entry is deleted, and when a full sync
compares the stored subjects with the file names; the file itself is kept as written in the mapping. On 3.x a full resync is required
anyway (ADR 0006 decision 4), so there is no migration, and 3.x must not run on a database that keeps CIP-26 rows of an earlier release.
The 2.0.x backport upgrades in place, so it has `V0_1700_4`, which lower-cases the subjects already stored (on MySQL with a binary
comparison); without it the next full sync would find them stale and delete them. The signatures that registry entries carry are not stored and not verified.

*Why:* the reason is not recorded in the code. What the code shows is that the module serves the
V2 shape (plain values), and the regression README says a V1 comparison is impossible because
signatures are not persisted. *Trade-off:* the module cannot serve or check signature data, so the
CF registry's V1 API cannot be reproduced from it.

## Consequences

- The errors of CIP-26 entries are not counted by a Micrometer counter (the CIP-68 error counter was removed for the same reason): they are in the
  rows, so a count by code is a query, and what is dropped is counted by `invalid_tokens{standard,reason_code}` (ADR 0007).
- A file whose name differs from its inner subject is never indexed, under either name; it is recorded in `invalid_token` with the subject it
  claims (`details.inner_subject`), so it can be found and reported to the registry. This is a decision, not an open question.
- A registry file that is dropped (a subject mismatch, an unusable subject, an unparseable file, an attack-sized logo) is logged and left out. The
  WARN lines and the `invalid_token` table (ADR 0007: `filename_subject_mismatch`, `unusable_subject`, `unparseable_mapping`, `missing_git_history`; a
  row is replaced each time the file is processed and removed when the file is deleted) are the record, so they are the first place to look when a token is missing. An entry that breaks CIP-26 is not dropped: it is in
  the API with its errors, so the API is the first place to look for what is wrong with a token that is there.
- Preprod logs thousands of "Skipping ... filename does not match" lines on the first sync. That
  is expected.

## Open questions

- **Known gap: a rewritten registry history is not handled.** Read from the code, not run: each sync pulls with rebase
  and, if that fails, clones again. An incremental sync then diffs the stored commit against HEAD. If the stored
  commit no longer exists (a force push, then a fresh clone), the diff is empty, a WARN says the hashes could not
  be resolved, and, because nothing failed, the stored commit is advanced to HEAD. The changes made by the rewrite
  are then never applied until a full sync. The obvious fix is to fall back to a full sync (which also removes the
  subjects that are gone) when either commit does not resolve. It is not done.
- A known registry mistake (such as the USDM entry above) cannot be fixed on our side without
  overriding the registry; today the answer is to fix it upstream.

## References

- #1102: propagate CIP-26 token deletions from the upstream registry
- cardano-foundation/cf-token-metadata-registry#117: the `url` host check and lower-case subjects, found on its mainnet validation run and
  ported here (PR #1220; the 2.0.x backport #1242 with `V0_1700_4`)
- `Cip26NetworkDefaults`, `Cip26MetadataSyncService`, `Cip26MetadataService`,
  `Cip26MetadataValidator`
