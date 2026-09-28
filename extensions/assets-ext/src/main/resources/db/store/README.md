# assets-ext schema

Flyway migrations for the `assets-ext` extension, one file per supported SQL dialect:
[`h2`](h2), [`mysql`](mysql), [`postgresql`](postgresql). The three dialects describe
the same schema — the differences are type names (`TEXT` ↔ `LONGTEXT`, `JSONB` ↔ `JSON`),
identifier quoting, and a `TIMESTAMP` nullability quirk on MySQL.

Optional indexes are declared in `components/dbutils/src/main/resources/asset-ext-index.yml`
and are NOT applied automatically. Apply them once the initial sync reaches chain tip:
`apply-optional-indexes --indexes asset-ext-index` in the admin CLI.

Column type bounds across the SQL files follow the CIP-26 / CIP-68 specs and
their canonical implementations. Tightening beyond these bounds is likely unsafe;
loosening is acceptable but wasteful.

---

## `cip26_metadata` — CIP-26 off-chain GitHub registry

One row per `subject` (a single CIP-26 entry). Bundles metadata, logo, and a catch-all
JSON column. Historically this was three tables (`ft_offchain_metadata`,
`ft_offchain_logo`, `off_chain_sync_state`); the logo was folded in and the sync_state
renamed to `cip26_sync_state` below.

All text-field bounds match the CIP-26 validator in
[`cf-tokens-cip26`](https://github.com/cardano-foundation/cf-token-metadata-registry).
Anything exceeding the limits is rejected by `Cip26MetadataValidator` before insert.

| Column | Type | Bound / Rationale |
|---|---|---|
| `subject` | `VARCHAR(120)` PK | policyId (28 B) + optional assetName (0–32 B), hex-encoded. CIP-26 spec: 56–120 chars. |
| `policy` | `TEXT` | CIP-26 `policy` is a base16 CBOR-encoded phase-1 monetary script (a native script), **not** the 28-byte policyId hash (that lives in the first 56 hex chars of `subject`). The spec places no upper bound; real entries with time-locked or k-of-n multisig scripts routinely exceed 200 hex chars (MCOS 216, Incy 586). An earlier `VARCHAR(120)` caused PostgreSQL to reject such entries; `Cip26MetadataService` caught the exception, logged ERROR, and skipped the row — entries were silently dropped with a log line. **Do not cap.** |
| `name` | `VARCHAR(50)` | CIP-26 cap. |
| `ticker` | `VARCHAR(9)` | CIP-26 cap, 2–9 chars. |
| `url` | `VARCHAR(250)` | CIP-26 cap. |
| `description` | `VARCHAR(500)` | CIP-26 cap (see `MetadataValidationRules.MAX_DESCRIPTION_LENGTH`). |
| `decimals` | `BIGINT` | Spec range [0, 19]. Stored as `BIGINT` to match the rest of the pipeline (Java `Long` in parser, entity, DTO) and avoid type-conversion noise at every boundary. The 4-byte overhead vs `INTEGER` is negligible. |
| `logo` | `TEXT` (PG/H2) / `LONGTEXT` (MySQL) | Base64-encoded image, up to ~87 KB per CIP-26. MySQL `TEXT` caps at 64 KB so `LONGTEXT` is required. PostgreSQL TOAST keeps the bulky column out of the row when not selected. |
| `updated`, `updated_by` | `TIMESTAMP`, `VARCHAR(255)` | Audit fields from the registry entry. |
| `properties` | `JSONB` (PG) / `JSON` (MySQL) / `TEXT` (H2) | Catch-all: full original `Mapping` JSON with per-property signed envelopes (`{value, signatures, sequenceNumber}`) and any non-well-known properties. |
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
| `name` | `VARCHAR(255)` | CIP-68 datum field. |
| `description` | `TEXT` | Can be arbitrarily long. |
| `ticker` | `VARCHAR(32)` | Typically only label 333/444. |
| `url` | `VARCHAR(250)` | Aligned with CIP-26 cap. |
| `decimals` | `BIGINT` | See `cip26_metadata.decimals` rationale. |
| `logo` | `TEXT` (PG/H2) / `LONGTEXT` (MySQL) | Base64-inline (mostly FTs). |
| `image` | `TEXT` | IPFS / `data:` / `https:` URL (mostly NFTs/RFTs). |
| `media_type` | `VARCHAR(255)` | MIME type of the image. |
| `version` | `BIGINT` NOT NULL | CIP-68 schema version, typically 1. |
| `datum` | `TEXT` (PG/H2) / `LONGTEXT` (MySQL) NOT NULL | Full CBOR hex of the inline datum, kept for re-parse / audit. |
| `properties` | `JSONB` (PG) / `JSON` (MySQL) / `TEXT` (H2) | Catch-all: `files[]` (array of `{name, mediaType, src}`) + collection-specific traits. |
| `last_synced_at` | `TIMESTAMP` | |

Indexes: `idx_cip68_metadata_slot`, `idx_cip68_metadata_label`. See
`asset-ext-index.yml` for additional indexes that pay off at chain-tip scale.

---

## Dialect differences

| Concept | PostgreSQL | H2 | MySQL |
|---|---|---|---|
| Big text columns | `TEXT` | `TEXT` | `LONGTEXT` (`TEXT` caps at 64 KB) |
| JSON column | `JSONB` | `TEXT` | `JSON` |
| Auto-increment PK | `BIGSERIAL` | `BIGINT AUTO_INCREMENT` | `BIGINT AUTO_INCREMENT` |
| `TIMESTAMP` nullability | implicit nullable | implicit nullable | requires explicit `NULL` |
