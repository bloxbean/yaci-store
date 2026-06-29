-- assets-ext schema. See ../README.md for column rationale, CIP references, and dialect notes.

-- CIP-26 — off-chain GitHub registry
CREATE TABLE cip26_metadata (
    subject        VARCHAR(120) PRIMARY KEY,
    policy         TEXT,
    name           VARCHAR(50),
    ticker         VARCHAR(9),
    url            VARCHAR(250),
    description    VARCHAR(500),
    decimals       BIGINT,
    logo           LONGTEXT,
    updated        TIMESTAMP NULL,
    updated_by     VARCHAR(255),
    properties     JSON,
    last_synced_at TIMESTAMP NULL
);

CREATE TABLE cip26_sync_state (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    last_commit_hash VARCHAR(40) NOT NULL,
    last_synced_at   TIMESTAMP NULL
);

-- CIP-68 — on-chain reference NFT metadata (label 222/333/444 unified)
CREATE TABLE cip68_metadata (
    policy_id      VARCHAR(56)  NOT NULL,
    asset_name     VARCHAR(64)  NOT NULL,
    slot           BIGINT       NOT NULL,
    tx_hash        VARCHAR(64)  NOT NULL,
    tx_index       INTEGER      NOT NULL,
    label          INTEGER      NOT NULL,
    name           VARCHAR(255),
    description    TEXT,
    ticker         VARCHAR(32),
    url            VARCHAR(250),
    decimals       BIGINT,
    logo           LONGTEXT,
    image          TEXT,
    media_type     VARCHAR(255),
    version        BIGINT       NOT NULL,
    datum          LONGTEXT     NOT NULL,
    properties     JSON,
    last_synced_at TIMESTAMP NULL,
    PRIMARY KEY (policy_id, asset_name, slot, tx_hash)
);

CREATE INDEX idx_cip68_metadata_slot  ON cip68_metadata(slot);
CREATE INDEX idx_cip68_metadata_label ON cip68_metadata(label);

-- CIP-113 — programmable token registry nodes (`key` / `next` backtick-quoted: MySQL reserved words)
CREATE TABLE cip113_registry_node (
    `key`                                  VARCHAR(64)  NOT NULL,
    slot                                   BIGINT       NOT NULL,
    tx_hash                                VARCHAR(64)  NOT NULL,
    tx_index                               INTEGER      NOT NULL,
    transfer_logic_script                  VARCHAR(56),
    transfer_logic_script_type             VARCHAR(8),
    third_party_transfer_logic_script      VARCHAR(56),
    third_party_transfer_logic_script_type VARCHAR(8),
    global_state_policy_id                 VARCHAR(56),
    `next`                                 VARCHAR(64)  NOT NULL,
    datum                                  LONGTEXT     NOT NULL,
    last_synced_at                         TIMESTAMP NULL,
    PRIMARY KEY (`key`, slot, tx_hash)
);

CREATE INDEX idx_cip113_slot ON cip113_registry_node(slot);
