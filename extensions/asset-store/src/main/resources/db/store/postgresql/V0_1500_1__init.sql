-- CIP-26 offchain fungible token metadata
CREATE TABLE ft_offchain_metadata (
    subject     VARCHAR(255) PRIMARY KEY,
    policy      VARCHAR(56),
    name        VARCHAR(255),
    ticker      VARCHAR(32),
    url         VARCHAR(255),
    description TEXT,
    decimals    INTEGER,
    updated     TIMESTAMP,
    updated_by  VARCHAR(255),
    properties  JSONB
);


-- CIP-26 fungible token logos
CREATE TABLE ft_offchain_logo (
    subject VARCHAR(255) PRIMARY KEY,
    logo    TEXT
);

-- CIP-26 GitHub sync state tracking
CREATE TABLE off_chain_sync_state (
    id               BIGSERIAL PRIMARY KEY,
    last_commit_hash VARCHAR(40) NOT NULL,
    last_synced_at   TIMESTAMP
);

-- CIP-68 on-chain reference NFT metadata
CREATE TABLE metadata_reference_nft (
    policy_id   VARCHAR(56)  NOT NULL,
    asset_name  VARCHAR(255) NOT NULL,
    slot        BIGINT       NOT NULL,
    label       INTEGER      NOT NULL DEFAULT 333,
    name        TEXT         NOT NULL,
    description TEXT         NOT NULL,
    ticker      TEXT,
    url         TEXT,
    decimals    BIGINT,
    logo        TEXT,
    version     BIGINT       NOT NULL,
    datum       TEXT         NOT NULL,
    PRIMARY KEY (policy_id, asset_name, slot)
);

CREATE INDEX idx_metadata_reference_nft_slot ON metadata_reference_nft(slot);

-- CIP-113 programmable token registry nodes
CREATE TABLE cip113_registry_node (
    policy_id                         VARCHAR(56) NOT NULL,
    slot                              BIGINT      NOT NULL,
    tx_hash                           VARCHAR(64) NOT NULL,
    transfer_logic_script             VARCHAR(56),
    third_party_transfer_logic_script VARCHAR(56),
    global_state_policy_id            VARCHAR(56),
    next_key                          VARCHAR(56),
    datum                             TEXT        NOT NULL,
    PRIMARY KEY (policy_id, slot, tx_hash)
);

CREATE INDEX idx_cip113_slot ON cip113_registry_node(slot);
