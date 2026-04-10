-- CIP-26 offchain fungible token metadata
CREATE TABLE ft_offchain_metadata (
    subject        VARCHAR(255) PRIMARY KEY,
    -- CIP-26 'policy' field: base16 CBOR-encoded phase-1 monetary script (a native script),
    -- NOT the 28-byte policyId hash (that lives in the first 56 hex chars of 'subject').
    -- CIP-26 spec bounds: minLength 56, maxLength 120. DO NOT shrink to VARCHAR(56) —
    -- many real registry entries (e.g. any time-locked or multisig script) exceed 56 chars.
    -- Reference: https://github.com/cardano-foundation/CIPs/blob/main/CIP-0026/README.md
    policy         VARCHAR(120),
    name           VARCHAR(255),
    ticker         VARCHAR(32),
    url            VARCHAR(255),
    description    TEXT,
    decimals       INTEGER,
    updated        TIMESTAMP,
    updated_by     VARCHAR(255),
    properties     TEXT,
    last_synced_at TIMESTAMP
);

-- CIP-26 fungible token logos
CREATE TABLE ft_offchain_logo (
    subject        VARCHAR(255) PRIMARY KEY,
    logo           TEXT,
    last_synced_at TIMESTAMP
);

-- CIP-26 GitHub sync state tracking
CREATE TABLE off_chain_sync_state (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    last_commit_hash VARCHAR(40) NOT NULL,
    last_synced_at   TIMESTAMP
);

-- CIP-68 on-chain reference NFT metadata
CREATE TABLE metadata_reference_nft (
    policy_id      VARCHAR(56)  NOT NULL,
    asset_name     VARCHAR(255) NOT NULL,
    slot           BIGINT       NOT NULL,
    label          INTEGER      NOT NULL DEFAULT 333,
    name           TEXT         NOT NULL,
    description    TEXT         NOT NULL,
    ticker         TEXT,
    url            TEXT,
    decimals       INTEGER,
    logo           TEXT,
    version        BIGINT       NOT NULL,
    datum          TEXT         NOT NULL,
    last_synced_at TIMESTAMP,
    PRIMARY KEY (policy_id, asset_name, slot)
);

CREATE INDEX idx_metadata_reference_nft_slot ON metadata_reference_nft(slot);

-- CIP-113 programmable token registry nodes
CREATE TABLE cip113_registry_node (
    -- policy_id stores the 'key' field from the CIP-113 registry node datum (first field in the
    -- linked list node). Usually the empty string (head sentinel) or a 28-byte policy_id
    -- (56 hex chars), but some aiken-linked-list implementations materialize a physical tail
    -- sentinel node whose key is longer — hence VARCHAR(128), not VARCHAR(56). DO NOT shrink.
    policy_id                         VARCHAR(128) NOT NULL,
    slot                              BIGINT       NOT NULL,
    tx_hash                           VARCHAR(64)  NOT NULL,
    -- transfer_logic_script / third_party_transfer_logic_script are Aiken Credentials (28-byte
    -- vkey or script hashes, 56 hex chars). global_state_policy_id is a currency symbol
    -- (28-byte policy_id). All three are protocol-bounded to 56 hex chars.
    transfer_logic_script             VARCHAR(56),
    third_party_transfer_logic_script VARCHAR(56),
    global_state_policy_id            VARCHAR(56),
    -- next_key stores the 'next' field (sorted linked list pointer). For the last real node in
    -- the list, this is the TAIL SENTINEL — conventionally ~32 bytes of 0xFF (64+ hex chars) in
    -- the aiken-linked-list library. DO NOT shrink to VARCHAR(56) on the (wrong) assumption that
    -- this is always a 28-byte policy_id.
    next_key                          VARCHAR(128) NOT NULL,
    datum                             TEXT         NOT NULL,
    last_synced_at                    TIMESTAMP,
    PRIMARY KEY (policy_id, slot, tx_hash)
);

-- Cip113RegistryNodeRepository.deleteBySlotGreaterThan(Long)
CREATE INDEX idx_cip113_slot ON cip113_registry_node(slot);
