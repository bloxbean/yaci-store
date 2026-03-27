-- CIP-26 offchain token metadata
CREATE TABLE token_metadata (
    subject     VARCHAR(255) PRIMARY KEY,
    policy      TEXT,
    name        VARCHAR(255),
    ticker      VARCHAR(32),
    url         VARCHAR(255),
    description TEXT,
    decimals    INTEGER,
    updated     TIMESTAMP,
    updated_by  VARCHAR(255),
    properties  JSONB
);


-- CIP-26 logos
CREATE TABLE token_logo (
    subject VARCHAR(255) PRIMARY KEY,
    logo    TEXT,
    CONSTRAINT fk_token_logo_metadata_subject FOREIGN KEY(subject) REFERENCES token_metadata(subject)
);

-- CIP-26 GitHub sync state tracking
CREATE TABLE off_chain_sync_state (
    id               BIGSERIAL PRIMARY KEY,
    last_commit_hash VARCHAR(40) NOT NULL
);

-- CIP-68 on-chain reference NFT metadata
CREATE TABLE metadata_reference_nft (
    policy_id   TEXT,
    asset_name  TEXT,
    slot        BIGINT,
    name        TEXT NOT NULL,
    description TEXT NOT NULL,
    ticker      TEXT,
    url         TEXT,
    decimals    BIGINT,
    logo        TEXT,
    version     BIGINT NOT NULL,
    datum       TEXT NOT NULL,
    PRIMARY KEY (policy_id, asset_name, slot)
);

CREATE INDEX idx_metadata_reference_nft_slot ON metadata_reference_nft(slot);

-- CIP-113 programmable token registry nodes
CREATE TABLE cip113_registry_node (
    policy_id                         TEXT   NOT NULL,
    slot                              BIGINT NOT NULL,
    tx_hash                           TEXT   NOT NULL,
    transfer_logic_script             TEXT,
    third_party_transfer_logic_script TEXT,
    global_state_policy_id            TEXT,
    next_key                          TEXT,
    datum                             TEXT   NOT NULL,
    PRIMARY KEY (policy_id, slot, tx_hash)
);

CREATE INDEX idx_cip113_slot ON cip113_registry_node(slot);
CREATE INDEX idx_cip113_policy_slot ON cip113_registry_node(policy_id, slot DESC);
