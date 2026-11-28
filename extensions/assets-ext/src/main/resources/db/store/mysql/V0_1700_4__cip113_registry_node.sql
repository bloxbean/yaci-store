-- assets-ext: CIP-113 programmable-token registry nodes (ADR 0011).
-- One row per version of a node: the output that carries it. A node is re-created on every insert after it and every
-- update, and never removed, so the current node of a key is its latest row by slot, then tx_index. Only outputs at a
-- configured registry's address that hold the registry NFT of their own key are stored (Cip113Processor).
-- key: the registered policy ID (56 hex), empty for the origin node; up to 64 hex, as the parser accepts.
-- registry_policy_id: the policy of the registry NFT the node holds, i.e. which registry the node belongs to. Reads use
-- only the configured registries, so a registry added later, or one that replaces another, does not mix their nodes.
CREATE TABLE cip113_registry_node (
    `key`                               VARCHAR(64) NOT NULL, -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    registry_policy_id                  VARCHAR(56) NOT NULL, -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    slot                                BIGINT      NOT NULL,
    tx_hash                             VARCHAR(64) NOT NULL, -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    tx_index                            INTEGER     NOT NULL,
    output_index                        INTEGER     NOT NULL,
    `next`                              VARCHAR(64) NOT NULL, -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    minting_logic_script                VARCHAR(56), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    transfer_logic_script               VARCHAR(56), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    third_party_transfer_logic_script   VARCHAR(56), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    unfracking_logic_script             VARCHAR(56), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    global_state_policy_id              VARCHAR(56), -- NOSONAR: plsql:VarcharUsageCheck, VARCHAR is right on MySQL
    datum                               TEXT        NOT NULL,
    last_synced_at                      TIMESTAMP NULL,
    PRIMARY KEY (`key`, registry_policy_id, slot, tx_hash)
);

-- rollback deletes by slot
CREATE INDEX idx_cip113_registry_node_slot ON cip113_registry_node(slot);
