-- Column type bounds across this file follow the CIP-26 / CIP-68 / CIP-113 specs
-- and their canonical implementations. Rationale is inline per column. Tightening
-- beyond these bounds is likely unsafe; loosening is acceptable but wasteful.
-- Kept in sync with the postgresql and mysql dialects.

-- =====================================================================
-- CIP-26 — off-chain GitHub registry
-- One table for metadata + logo + arbitrary properties.
-- =====================================================================
--
-- All text-field bounds match the CIP-26 validator in cf-tokens-cip26:
-- name ≤ 50, ticker 2–9, url ≤ 250, description ≤ 500.
-- Anything exceeding these limits is rejected by Cip26MetadataValidator before insert.
-- Reference: https://github.com/cardano-foundation/CIPs/blob/main/CIP-0026/README.md
CREATE TABLE cip26_metadata (
    -- subject = policyId (28 bytes) + optional assetName (0-32 bytes), hex-encoded.
    -- CIP-26 spec: minLength 56, maxLength 120.
    subject        VARCHAR(120) PRIMARY KEY,
    -- CIP-26 'policy' field: base16 CBOR-encoded phase-1 monetary script (a native script),
    -- NOT the 28-byte policyId hash (that lives in the first 56 hex chars of 'subject').
    -- The CIP-26 spec places NO upper bound on this field; real entries with time-locked
    -- or k-of-n multisig scripts routinely exceed 200 hex chars (MCOS at 216, Incy at 586).
    -- An earlier VARCHAR(120) caused the DB to reject such entries; Cip26MetadataService
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
    -- base64-encoded image, up to ~87400 chars per CIP-26 — kept as TEXT.
    -- Merged in here (was a separate ft_offchain_logo table previously).
    logo           TEXT,
    updated        TIMESTAMP,
    updated_by     VARCHAR(255),
    -- Catch-all JSON string. Holds the full original Mapping JSON with per-property
    -- signed envelopes ({value, signatures, sequenceNumber}) and any non-well-known
    -- properties from the registry entry.
    properties     TEXT,
    last_synced_at TIMESTAMP
);

-- CIP-26 GitHub sync state (housekeeping; one row tracks the last commit synced).
CREATE TABLE cip26_sync_state (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- git SHA-1 hash = 40 hex chars, never varies.
    last_commit_hash VARCHAR(40) NOT NULL,
    last_synced_at   TIMESTAMP
);


-- =====================================================================
-- CIP-68 — on-chain reference NFT metadata (FT/NFT/RFT unified)
-- =====================================================================
CREATE TABLE cip68_metadata (
    -- policy_id: exactly 28 bytes = 56 hex chars (Blake2b-224). Protocol-bounded.
    policy_id      VARCHAR(56)  NOT NULL,
    -- asset_name: 0–32 bytes = 0–64 hex chars (ledger max). Protocol-bounded.
    asset_name     VARCHAR(64)  NOT NULL,
    slot           BIGINT       NOT NULL,
    -- CIP-68 label: 222 (NFT), 333 (FT), 444 (RFT). Set by Cip68Processor based
    -- on the co-minted user-token prefix observed in the same transaction's outputs.
    label          INTEGER      NOT NULL,
    -- CIP-68 metadata 'name': variable UTF-8 string from the datum.
    name           VARCHAR(255),
    -- CIP-68 'description': can be arbitrarily long multi-sentence text. Kept as TEXT.
    description    TEXT,
    -- CIP-68 'ticker': short symbol, typically only label=333/444.
    ticker         VARCHAR(32),
    -- CIP-68 'url': aligns with CIP-26 url cap of 250 chars.
    url            VARCHAR(250),
    -- CIP-68 'decimals': unsigned integer in the datum. In practice 0–19.
    decimals       INTEGER,
    -- base64-encoded logo (PNG/JPG/SVG). Variable-length, can be tens of KB.
    logo           TEXT,
    -- CIP-68 NFT 'image': IPFS / data: URL / https URL.
    image          TEXT,
    -- CIP-68 NFT 'mediaType': MIME type of the image (e.g. image/png).
    media_type     VARCHAR(255),
    -- CIP-68 schema version, typically 1.
    version        BIGINT       NOT NULL,
    -- Full CBOR hex of the inline datum. Variable length, kept for reparsing/auditing.
    datum          TEXT         NOT NULL,
    -- Catch-all JSON string. Holds the parsed datum's non-scalar bits:
    --   files[]:               array of { name, mediaType, src }
    --   additional_properties: collection-specific (attributes, traits, custom keys)
    properties     TEXT,
    last_synced_at TIMESTAMP,
    PRIMARY KEY (policy_id, asset_name, slot)
);

CREATE INDEX idx_cip68_metadata_slot  ON cip68_metadata(slot);
CREATE INDEX idx_cip68_metadata_label ON cip68_metadata(label);


-- =====================================================================
-- CIP-113 — programmable token registry nodes (unchanged)
-- =====================================================================
--
-- The datum is an Aiken `RegistryNode` sorted linked list entry. Column names
-- mirror the on-chain datum field names (see CIP-143 — the parent spec — and
-- cardano-foundation/cip113-programmable-tokens).
--
-- NOTE — why "key" and not "policy_id":
-- The registry is a sorted linked list. For real registered tokens the key column
-- equals the programmable token's policy ID (56 hex chars). But two rows per registry
-- are *sentinel* nodes — the head (empty string) and the tail (conventionally 32 bytes
-- of 0xFF) — that are linked-list machinery, not registrations, and do NOT hold policy
-- IDs. Naming the column 'policy_id' would overclaim what is stored. See the Javadoc
-- on Cip113RegistryNode.key for the full rationale. KEY and NEXT are H2 reserved words
-- (unlike in PostgreSQL), so they are quoted here.
CREATE TABLE cip113_registry_node (
    -- 'key' field of the registry node datum. Three possible values:
    --   * empty string     — head sentinel of the sorted linked list (NOT a policy_id)
    --   * 56 hex chars     — a real 28-byte policy_id of a registered token
    --   * 58–64 hex chars  — tail sentinel (conventionally 32 bytes of 0xFF
    --                        in the aiken-linked-list library; NOT a policy_id)
    -- VARCHAR(64) is the tightest bound that fits all three. DO NOT shrink to 56.
    "key"                             VARCHAR(64)  NOT NULL,
    slot                              BIGINT       NOT NULL,
    -- Cardano transaction hash: exactly 32 bytes = 64 hex chars. Protocol-bounded.
    tx_hash                           VARCHAR(64)  NOT NULL,
    -- Aiken Credential inner hash (either VerificationKey or Script): 28 bytes = 56 hex.
    -- The constructor variant is preserved in the *_type companion column as the string
    -- 'VKEY' or 'SCRIPT'. The hash and its type are populated together — both NULL when
    -- the on-chain Credential field encodes "absent", both non-NULL otherwise.
    transfer_logic_script                  VARCHAR(56),
    transfer_logic_script_type             VARCHAR(8),
    third_party_transfer_logic_script      VARCHAR(56),
    third_party_transfer_logic_script_type VARCHAR(8),
    -- Currency symbol of the global-state NFT (28-byte policy_id). Protocol-bounded.
    global_state_policy_id                 VARCHAR(56),
    -- 'next' field — sorted linked list pointer. Same length range as 'key' above
    -- (head is never a 'next'; real policy or tail sentinel 58–64 hex chars).
    "next"                                 VARCHAR(64)  NOT NULL,
    -- Full CBOR hex of the inline datum. Variable length.
    datum                                  TEXT         NOT NULL,
    last_synced_at                         TIMESTAMP,
    PRIMARY KEY ("key", slot, tx_hash)
);

-- Cip113RegistryNodeRepository.deleteBySlotGreaterThan(Long)
CREATE INDEX idx_cip113_slot ON cip113_registry_node(slot);
