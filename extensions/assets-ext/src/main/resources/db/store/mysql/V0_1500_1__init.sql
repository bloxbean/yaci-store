-- Column type bounds across this file follow the CIP-26 / CIP-68 / CIP-113 specs
-- and their canonical implementations. Rationale is inline per column. Tightening
-- beyond these bounds is likely unsafe; loosening is acceptable but wasteful.
-- Kept in sync with the postgresql and h2 dialects.

-- CIP-26 offchain fungible token metadata
--
-- All text-field bounds match the CIP-26 validator in cf-tokens-cip26:
-- name ≤ 50, ticker 2–9, url ≤ 250, description ≤ 500.
-- Anything exceeding these limits is rejected by TokenMetadataValidator before insert.
-- Reference: https://github.com/cardano-foundation/CIPs/blob/main/CIP-0026/README.md
CREATE TABLE ft_offchain_metadata (
    -- subject = policyId (28 bytes) + optional assetName (0-32 bytes), hex-encoded.
    -- CIP-26 spec: minLength 56, maxLength 120.
    subject        VARCHAR(120) PRIMARY KEY,
    -- CIP-26 'policy' field: base16 CBOR-encoded phase-1 monetary script (a native script),
    -- NOT the 28-byte policyId hash (that lives in the first 56 hex chars of 'subject').
    -- The CIP-26 spec places NO upper bound on this field; real entries with time-locked
    -- or k-of-n multisig scripts routinely exceed 200 hex chars (MCOS at 216, Incy at 586).
    -- An earlier VARCHAR(120) caused the DB to reject such entries; TokenMetadataService
    -- caught the exception, logged ERROR, and skipped them — dropped with a log line but
    -- no fatal failure. DO NOT cap this column.
    policy         TEXT,
    -- CIP-26 name: max 50 chars (enforced by cf-tokens-cip26 validator).
    name           VARCHAR(50),
    -- CIP-26 ticker: 2-9 chars (enforced by cf-tokens-cip26 validator).
    ticker         VARCHAR(9),
    -- CIP-26 url: max 250 chars (enforced by cf-tokens-cip26 validator).
    url            VARCHAR(250),
    -- CIP-26 description: max 500 chars (enforced by cf-tokens-cip26 validator;
    -- see MetadataValidationRules.MAX_DESCRIPTION_LENGTH in cf-tokens-cip26).
    description    VARCHAR(500),
    -- CIP-26 decimals: spec range [0, 19] inclusive (well-known property 'decimals').
    -- INTEGER is oversized but standard; SMALLINT would also work.
    decimals       INTEGER,
    updated        TIMESTAMP NULL,
    updated_by     VARCHAR(255),
    properties     JSON,
    last_synced_at TIMESTAMP NULL
);

-- CIP-26 fungible token logos
CREATE TABLE ft_offchain_logo (
    -- Matches ft_offchain_metadata.subject bounds (CIP-26 spec).
    subject        VARCHAR(120) PRIMARY KEY,
    -- base64-encoded image, up to ~87400 chars per CIP-26 — kept as LONGTEXT (MySQL TEXT is only 64 KB).
    logo           LONGTEXT,
    last_synced_at TIMESTAMP NULL
);

-- CIP-26 GitHub sync state tracking
CREATE TABLE off_chain_sync_state (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- git SHA-1 hash = 40 hex chars, never varies.
    last_commit_hash VARCHAR(40) NOT NULL,
    last_synced_at   TIMESTAMP NULL
);

-- CIP-68 on-chain reference NFT metadata
--
-- On-chain columns (policy_id, asset_name) are protocol-bounded by the Cardano
-- multi-asset ledger rules. Datum-sourced string columns (name, ticker, url) use
-- lenient but explicit bounds — the datum itself is schema-flexible PlutusMap, but
-- realistic values fit within the limits below; anything exceeding them almost
-- certainly indicates a malformed or hostile datum.
CREATE TABLE metadata_reference_nft (
    -- policy_id: exactly 28 bytes = 56 hex chars (Blake2b-224). Protocol-bounded.
    policy_id      VARCHAR(56)  NOT NULL,
    -- asset_name: 0–32 bytes = 0–64 hex chars (ledger max). Protocol-bounded.
    asset_name     VARCHAR(64)  NOT NULL,
    slot           BIGINT       NOT NULL,
    -- CIP-68 label: 222 (NFT), 333 (FT), 444 (RFT). Only 333 is currently indexed.
    label          INTEGER      NOT NULL DEFAULT 333,
    -- CIP-68 FT metadata 'name': variable UTF-8 string from the datum. 255 is lenient
    -- but bounded; real tokens are typically ≤ 32 chars.
    name           VARCHAR(255) NOT NULL,
    -- CIP-68 FT 'description': can be arbitrarily long multi-sentence text. Kept as TEXT.
    description    TEXT         NOT NULL,
    -- CIP-68 FT 'ticker': short symbol, protocol convention 2–9 chars; 32 is lenient.
    ticker         VARCHAR(32),
    -- CIP-68 FT 'url': aligns with CIP-26 url cap of 250 chars.
    url            VARCHAR(250),
    -- CIP-68 FT 'decimals': unsigned integer in the datum, no explicit CIP-68 spec cap.
    -- In practice 0–19 (aligned with CIP-26's well-known 'decimals' property).
    decimals       INTEGER,
    -- base64-encoded image (PNG/JPG/SVG). Variable-length, can be tens of KB.
    logo           LONGTEXT,
    -- CIP-68 schema version, typically 1. BIGINT is legacy; INTEGER would suffice.
    version        BIGINT       NOT NULL,
    -- Full CBOR hex of the inline datum. Variable length, kept for reparsing/auditing.
    datum          LONGTEXT     NOT NULL,
    last_synced_at TIMESTAMP NULL,
    PRIMARY KEY (policy_id, asset_name, slot)
);

CREATE INDEX idx_metadata_reference_nft_slot ON metadata_reference_nft(slot);

-- CIP-113 programmable token registry nodes
--
-- The datum is an Aiken `RegistryNode` sorted linked list entry. Column names
-- mirror the on-chain datum field names (see CIP-143 — the parent spec — and
-- cardano-foundation/cip113-programmable-tokens).
--
-- NOTE — why `key` and not `policy_id`:
-- The registry is a sorted linked list. For real registered tokens the key column
-- equals the programmable token's policy ID (56 hex chars). But two rows per registry
-- are *sentinel* nodes — the head (empty string) and the tail (conventionally 32 bytes
-- of 0xFF) — that are linked-list machinery, not registrations, and do NOT hold policy
-- IDs. Naming the column 'policy_id' would overclaim what is stored. See the Javadoc
-- on Cip113RegistryNode.key for the full rationale. KEY and NEXT are MySQL reserved
-- words (unlike in PostgreSQL), so they are backtick-quoted here.
CREATE TABLE cip113_registry_node (
    -- 'key' field of the registry node datum. Three possible values:
    --   * empty string     — head sentinel of the sorted linked list (NOT a policy_id)
    --   * 56 hex chars     — a real 28-byte policy_id of a registered token
    --   * 58–64 hex chars  — tail sentinel (conventionally 32 bytes of 0xFF
    --                        in the aiken-linked-list library; NOT a policy_id)
    -- VARCHAR(64) is the tightest bound that fits all three. DO NOT shrink to 56.
    `key`                             VARCHAR(64)  NOT NULL,
    slot                              BIGINT       NOT NULL,
    -- Cardano transaction hash: exactly 32 bytes = 64 hex chars. Protocol-bounded.
    tx_hash                           VARCHAR(64)  NOT NULL,
    -- Aiken Credential inner hash (either VerificationKey or Script): 28 bytes = 56 hex.
    -- The constructor variant (VKey vs Script) is currently NOT preserved — if that
    -- distinction becomes important downstream, add a separate column.
    transfer_logic_script             VARCHAR(56),
    third_party_transfer_logic_script VARCHAR(56),
    -- Currency symbol of the global-state NFT (28-byte policy_id). Protocol-bounded.
    global_state_policy_id            VARCHAR(56),
    -- 'next' field — sorted linked list pointer. Same length range as 'key' above
    -- (head is never a 'next'; real policy or tail sentinel 58–64 hex chars).
    `next`                            VARCHAR(64)  NOT NULL,
    -- Full CBOR hex of the inline datum. Variable length.
    datum                             LONGTEXT     NOT NULL,
    last_synced_at                    TIMESTAMP NULL,
    PRIMARY KEY (`key`, slot, tx_hash)
);

-- Cip113RegistryNodeRepository.deleteBySlotGreaterThan(Long)
CREATE INDEX idx_cip113_slot ON cip113_registry_node(slot);
