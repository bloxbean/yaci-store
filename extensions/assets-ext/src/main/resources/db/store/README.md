# assets-ext schema

Flyway migrations for the `assets-ext` extension, one file per supported SQL dialect:
[`h2`](h2), [`mysql`](mysql), [`postgresql`](postgresql). The three dialects describe
the same schema — the differences are type names (`TEXT` ↔ `LONGTEXT`, `JSONB` ↔ `JSON`),
identifier quoting, and a `TIMESTAMP` nullability quirk on MySQL.

Optional indexes are declared in `components/dbutils/src/main/resources/asset-ext-index.yml`
and are NOT applied automatically. Apply them once the initial sync reaches chain tip:
`apply-optional-indexes --indexes asset-ext-index` in the admin CLI.

A token that breaks its CIP is **indexed and served as written**, with the list of what is wrong in the
`validation_errors` column of its row (see below); it is not dropped. The text columns therefore hold what is on-chain or in the
registry, without a length cap (the CIP limits are reported as validation errors). `V0_1700_1` created them with the CIP bounds,
`V0_1700_2` widened them to `TEXT` and added `validation_errors`. A datum is at most one transaction (16 KB), so these stay bounded.

---

## `cip26_metadata` — CIP-26 off-chain GitHub registry

One row per `subject` (a single CIP-26 entry). Bundles metadata, logo, and a catch-all
JSON column. Historically this was three tables (`ft_offchain_metadata`,
`ft_offchain_logo`, `off_chain_sync_state`); the logo was folded in and the sync_state
renamed to `cip26_sync_state` below.

The CIP-26 limits (name 50, ticker 2–9, url 250, description 500 characters, the `cf-tokens-cip26` validator in
[`cf-token-metadata-registry`](https://github.com/cardano-foundation/cf-token-metadata-registry)) are checked by
`Cip26MetadataValidator`, which reports what an entry breaks in `validation_errors`; the entry is stored as written. Only an entry
whose `subject` cannot be a key, or a logo far beyond any real size, is dropped, with a warning.

| Column | Type | Bound / Rationale |
|---|---|---|
| `subject` | `VARCHAR(120)` PK | policyId (28 B) + optional assetName (0–32 B), hex-encoded, **lower case** (a registry file named in upper case is stored under its lower-case subject, `V0_1700_4` converts the rows stored before). CIP-26 spec: 56–120 chars. |
| `policy` | `TEXT` | CIP-26 `policy` is a base16 CBOR-encoded phase-1 monetary script (a native script), **not** the 28-byte policyId hash (that lives in the first 56 hex chars of `subject`). The spec places no upper bound; real entries with time-locked or k-of-n multisig scripts routinely exceed 200 hex chars (MCOS 216, Incy 586). An earlier `VARCHAR(120)` caused PostgreSQL to reject such entries; `Cip26MetadataService` caught the exception, logged ERROR, and skipped the row — entries were silently dropped with a log line. **Do not cap.** |
| `name` | `TEXT` | CIP-26 says at most 50 characters; a longer one is stored and reported. |
| `ticker` | `TEXT` | CIP-26 says 2–9 characters; stored as written, reported otherwise. |
| `url` | `TEXT` | CIP-26 says at most 250 characters; stored as written, reported otherwise. |
| `description` | `TEXT` | CIP-26 says at most 500 characters; stored as written, reported otherwise. |
| `decimals` | `BIGINT` | 0–19 in practice; a value outside [0, 255] (`TokenDecimals`) is stored and reported, no DB constraint. Stored as `BIGINT` to match the rest of the pipeline (Java `Long` in parser, entity, DTO) and avoid type-conversion noise at every boundary. The 4-byte overhead vs `INTEGER` is negligible. |
| `logo` | `TEXT` (PG/H2) / `LONGTEXT` (MySQL) | Base64-encoded image, up to ~87 KB per CIP-26. MySQL `TEXT` caps at 64 KB so `LONGTEXT` is required. PostgreSQL TOAST keeps the bulky column out of the row when not selected. |
| `updated`, `updated_by` | `TIMESTAMP`, `VARCHAR(255)` | Audit fields from the registry entry. |
| `properties` | `JSONB` (PG) / `JSON` (MySQL) / `TEXT` (H2) | Catch-all: full original `Mapping` JSON with per-property signed envelopes (`{value, signatures, sequenceNumber}`) and any non-well-known properties. |
| `validation_errors` | `JSONB` (PG) / `JSON` (MySQL) / `TEXT` (H2) | List of `{cip, field_name, field_category, code, error}` for what the entry breaks in CIP-26 (`sub_standard` is absent for CIP-26). `NULL` on rows written before `V0_1700_2` (never checked; the API leaves the field out and `valid` falls back to name and description present; a resync fills it); `[]` when the entry follows the CIP. |
| `last_synced_at` | `TIMESTAMP` | |

## `cip26_sync_state` — sync housekeeping

One row tracks the last commit synced from the CF GitHub registry.

| Column | Type | Note |
|---|---|---|
| `id` | `BIGSERIAL` (PG) / `BIGINT AUTO_INCREMENT` (H2, MySQL) PK | |
| `last_commit_hash` | `VARCHAR(40)` NOT NULL | Git SHA-1 = exactly 40 hex chars. |
| `last_synced_at` | `TIMESTAMP` | |

---

## `cip68_metadata` — on-chain reference NFT metadata (FT/NFT/RFT unified)

One row per *reference-NFT update*. Holds rows for all three CIP-68 label types
(222 NFT / 333 FT / 444 RFT) — different columns are populated depending on label.
`Cip68Processor` classifies each row at index time using cross-output detection
(observing the co-minted user-token prefix in the same transaction's outputs).

Primary key is `(policy_id, asset_name, slot, tx_hash)` — keying on `tx_hash` (like the rest of
yaci-store: stake_registration, governance, etc.) means the table preserves the full history of
reference-NFT updates, including multiple updates within the same slot (intra-block tx chaining),
without in-place overwrites. `tx_index` is a non-key ordering column: the latest row is resolved by
`ORDER BY slot DESC, tx_index DESC`. **`label` is per-row, not per-asset**: a single reference
NFT accumulates rows under multiple labels over its lifetime (an FT whose later
updates happened to be co-minted alongside a 222-prefixed NFT will have its newest
row stored under label=222). Read-path queries must **not** filter by label — see
`Cip68MetadataRepository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc`.

| Column | Type | Bound / Rationale |
|---|---|---|
| `policy_id` | `VARCHAR(56)` NOT NULL | 28 B Blake2b-224. Protocol-bounded. |
| `asset_name` | `VARCHAR(64)` NOT NULL | 0–32 B ledger max. |
| `slot` | `BIGINT` NOT NULL | |
| `tx_hash` | `VARCHAR(64)` NOT NULL | Cardano transaction hash. PK component — tx identity preserves per-update history. |
| `tx_index` | `INTEGER` NOT NULL | Producing tx's index within its block. Non-key ordering column — disambiguates same-slot updates. |
| `label` | `INTEGER` NOT NULL | 222 / 333 / 444. See note above. |
| `name` | `TEXT` | CIP-68 datum field. Stored as written, however long. |
| `description` | `TEXT` | Can be arbitrarily long. |
| `ticker` | `TEXT` | Typically only label 333/444. |
| `url` | `TEXT` | CIP-68 datum field. |
| `decimals` | `BIGINT` | See `cip26_metadata.decimals` rationale. |
| `logo` | `TEXT` (PG/H2) / `LONGTEXT` (MySQL) | Base64-inline (mostly FTs). |
| `image` | `TEXT` | IPFS / `data:` / `https:` URL (mostly NFTs/RFTs). |
| `media_type` | `TEXT` | MIME type of the image. |
| `version` | `BIGINT` | CIP-68 schema version, typically 1. A version CIP-68 does not define (for example Greenland Reserve Coin's 0) is stored as it is, with an error; `NULL` when the datum holds an integer that does not fit a `BIGINT`. |
| `datum` | `TEXT` (PG/H2) / `LONGTEXT` (MySQL) NOT NULL | Full CBOR hex of the inline datum, kept for re-parse / audit. |
| `properties` | `JSONB` (PG) / `JSON` (MySQL) / `TEXT` (H2) | Catch-all: `files[]` (array of `{name, mediaType, src}`, kept as written even when an entry breaks the CIP) + collection-specific traits. A Plutus constructor is stored as `{"constructor": n, "fields": [...]}` and reported. A property is left out, and the rest of the datum is indexed, only when its value is nested deeper than 100 levels, which is an attack (Jackson cannot write a JSON document nested deeper than 1000). The full datum stays in `datum`. |
| `validation_errors` | `JSONB` (PG) / `JSON` (MySQL) / `TEXT` (H2) | List of `{cip, sub_standard, field_name, field_category, code, error}` for what the datum breaks in CIP-68, per label it was checked for (`sub_standard` is the label; absent for a rule that does not depend on one). `NULL` on rows written before `V0_1700_2` (never checked; the API leaves the field out and `valid` falls back to name and description present; a resync fills it); `[]` when the datum follows the CIP. |
| `last_synced_at` | `TIMESTAMP` | |

Indexes: `idx_cip68_metadata_slot`, `idx_cip68_metadata_label`. See
`asset-ext-index.yml` for additional indexes that pay off at chain-tip scale.

---

## `invalid_token` (V0_1700_3)

What the module **drops** or cannot use, one row per item (ADR 0007). A token that is indexed keeps its problems in the
`validation_errors` of its own row and is not here. The table is written, not served: there is no API for it, read it with SQL.

| Column | Type | Meaning |
|---|---|---|
| `id` | `VARCHAR(64)` PK | SHA-256 of `standard, reason_code, policy_id, asset_name, tx_hash, output_index, origin`: the same item has the same id, so a replayed block or a re-run sync adds nothing |
| `standard` | `VARCHAR(16)` | the standard whose rule found the problem: `CIP-26`, `CIP-68` (`CIP-113` is reserved) |
| `combined_with` | `VARCHAR(16)` NULL | the other standard of a combination (CIP-113 with CIP-26 or CIP-68) |
| `reason_code` | `VARCHAR(64)` | stable code: CIP-26 `filename_subject_mismatch`, `unusable_subject`, `unparseable_mapping`, `missing_git_history`; CIP-68 `datum_undecodable`, `datum_too_deep_to_decode`, `processing_failure` |
| `reason` | `TEXT` | message for people, at most 500 characters |
| `policy_id`, `asset_name` | NULL | the token, when known (CIP-68: the reference NFT) |
| `subject` | `VARCHAR(256)` NULL | the identifier as written |
| `slot`, `tx_hash`, `tx_index`, `output_index` | NULL | the on-chain origin (NULL for CIP-26) |
| `origin` | `VARCHAR(512)` NULL | the registry file name (CIP-26) |
| `details` | `JSONB` / `JSON` / `TEXT` NULL | bounded context (at most 2 KB), for example `{"inner_subject": "..."}` or `{"datum_prefix": "...", "exception": "..."}` |
| `first_seen_at` | `TIMESTAMP` | |

Indexes: `idx_invalid_token_slot`, `idx_invalid_token_reason (standard, reason_code)`, `idx_invalid_token_policy`. A rollback deletes the
rows with `slot >` the rollback slot; rows from the CIP-26 registry (no slot) are not touched. The CIP-26 rows of a file are replaced each
time the file is processed and removed when it is deleted from the registry.

```sql
SELECT standard, reason_code, count(*) FROM invalid_token GROUP BY 1, 2 ORDER BY 3 DESC;
```

---

## Dialect differences

| Concept | PostgreSQL | H2 | MySQL |
|---|---|---|---|
| Big text columns | `TEXT` | `TEXT` | `LONGTEXT` (`TEXT` caps at 64 KB) |
| JSON column | `JSONB` | `TEXT` | `JSON` |
| Auto-increment PK | `BIGSERIAL` | `BIGINT AUTO_INCREMENT` | `BIGINT AUTO_INCREMENT` |
| `TIMESTAMP` nullability | implicit nullable | implicit nullable | requires explicit `NULL` |

## Upgrading from an earlier release (including within 2.x)

`V0_1700_2` and `V0_1700_3` only add things: nullable `validation_errors` columns, the `invalid_token` table, text columns widened to
`TEXT`, and `cip68_metadata.version` made nullable. They rewrite no data, so a node upgrades in place and keeps serving. On PostgreSQL and
H2 the statements use `IF NOT EXISTS` and can be run again; on MySQL the column changes rewrite the two tables once (a few hundred thousand
rows on mainnet).

`V0_1700_4` lower-cases the CIP-26 subjects already stored (39 on mainnet), since the sync now stores them in lower case and would
otherwise find a stored upper-case subject stale on the next full sync. It rewrites only those rows and can be run again. On PostgreSQL and
H2 an upper-case entry whose lower-case twin is also stored is dropped first (none on mainnet); on MySQL the usual case-insensitive collation
makes such a twin impossible, and the comparison is binary.

Rows written before the upgrade have `validation_errors = NULL`: never checked, which is not the same as "no errors". The API leaves
`validation_errors` out for them and `metadata.valid` falls back to the rule it had before (name and description both present). Only a
resync fills them in, and brings back the tokens that were dropped before; it is not needed for the node to work.
