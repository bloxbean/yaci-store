# ADR 0001: CIP-26 off-chain registry ingestion

## Status

Proposed

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
[ADR 0002](0002-cip-68-onchain-datum-handling.md), section "Read model".

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

Each file ends in one of: inserted, skipped (no mapping, filename mismatch, rejected by
validation), permanently failed, or transiently failed. The stored commit advances only if **no**
entry failed transiently, so those entries are retried on the next run. Permanent skips do not
hold it back.

A database error is classified as permanent (constraint violation, column too narrow, encoding)
or transient. An error of an unknown type is treated as **transient**.

*Why:* if permanent skips blocked the cursor the sync would loop forever on one bad file, and if an
unknown error were treated as permanent a recoverable problem would silently drop a token.
*Trade-off:* an unclassified error that is really permanent is retried on every run and shows up
in the logs each time; it needs a person to look at it.

### 4. What is accepted

An entry is stored only if all of these hold. Each rejection is a WARN with the subject.

- **The filename is the subject.** The file `<subject>.json` must contain that same `subject`.
  Otherwise the file is skipped. Preprod's registry has 5,008 such files (about 90% of it), mostly
  copies of one test token under padded filenames; mainnet has none.
- **The rules of the shared `cf-tokens-cip26` library**, which is the registry's validator: `name` and
  `description` are required, length limits apply, the subject is 56 to 120 hex characters (an even count).
  Note that CIP-26 itself requires only the `subject`: its JSON schema lists `subject` as the one required
  property, and `name` (at most 50 characters), `description` (at most 500), `ticker`, `decimals`, `url` and
  `logo` are well-known properties whose values must be well-formed if present. The requirement of `name` and
  `description` comes from the library and the registry, not from the CIP text.
- **`decimals` is in [0, 255]** (`TokenDecimals`). The library only checks `>= 0` and does so on an
  `int`, so the module range-checks the stored `long` first; otherwise 4294967301 would pass as 5.
- **`name` and `description` are both required.** This is the one place where the module is
  deliberately strict and has no leniency: a token without either has nothing useful to show and is
  not indexed. The API enforces the same on what it returns (see ADR 0002).

*Why strict here:* the registry is curated input that anyone can submit to, and the shared library
the registry itself uses already rejects an entry without them, so being lenient would mean overriding the
registry's own validator. (The CIP does not require them; the rule is the registry's, and we follow it.) *Trade-off:* a valid-looking entry with a typo in its subject is simply absent. On preprod
that is correct (a "Diffusion" entry with a dropped hex digit is rejected while the correct entry
is stored), but it also means a registry mistake can hide a token: the only registry entry for the preprod
USDM token under policy `d8906ca5…` has the policy id pasted twice (128 characters) and is not
indexed.

### 5. The logo is validated and stored separately

Metadata and logo are two steps. The logo must be valid base64 and at most 64 KiB decoded; the
image format (PNG, SVG) is not checked, so the module is not stricter than the library. A bad
logo is skipped and the metadata is still saved. A logo with no metadata row (an orphan logo) is
skipped.

*Why:* a token with good metadata and an oversized logo is still worth serving.
*Trade-off:* a stored token can have no logo although the registry has one.

### 6. Removals are followed, with a guard

A subject whose mapping file is deleted upstream is deleted locally, logo included (it is on the
same row). In an incremental sync this comes from the Git diff. In a full sync the module
reconciles: a locally stored subject with no file in the registry is deleted, **unless the registry
contains no mapping files at all**, in which case the cleanup is skipped and a warning is logged.

*Why:* removals matter (a token can be withdrawn), but an empty or broken clone must not wipe the
table. *Trade-off:* if the registry is really emptied, the local data stays until someone cleans it.

### 7. One row per subject, no signatures

The table keeps the latest version of a subject (`updated` and `updated_by` come from the Git
history of the file). The signatures that registry entries carry are not stored and not verified.

*Why:* the reason is not recorded in the code. What the code shows is that the module serves the
V2 shape (plain values), and the regression README says a V1 comparison is impossible because
signatures are not persisted. *Trade-off:* the module cannot serve or check signature data, so the
CF registry's V1 API cannot be reproduced from it.

## Consequences

- A registry file that fails any of the above is logged and left out. The WARN lines are the only
  record, so they are the first place to look when a token is missing.
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
- Whether a failed validation should be counted and exposed (for example through the health
  indicator) rather than only logged.

## References

- #1102: propagate CIP-26 token deletions from the upstream registry
- `Cip26NetworkDefaults`, `Cip26MetadataSyncService`, `Cip26MetadataService`,
  `Cip26MetadataValidator`
