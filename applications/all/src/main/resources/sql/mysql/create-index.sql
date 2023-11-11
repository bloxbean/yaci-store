-- set search_path  to mainnet;

-- transaction store
CREATE INDEX idx_transaction_block
    ON transaction(block);

CREATE INDEX idx_transaction_block_hash
    ON transaction(block_hash);

-- utxo store

CREATE INDEX idx_address_utxo_owner_addr
    ON address_utxo(owner_addr);

CREATE INDEX idx_address_utxo_owner_stake_addr
    ON address_utxo(owner_stake_addr);

CREATE INDEX idx_address_utxo_owner_paykey_hash
    ON address_utxo(owner_payment_credential);

CREATE INDEX idx_address_utxo_owner_stakekey_hash
    ON address_utxo(owner_stake_credential);

CREATE INDEX idx_address_utxo_epoch
    ON address_utxo(epoch);

CREATE INDEX idx_address_utxo_spent_epoch
    ON address_utxo(spent_epoch);

-- asset store

CREATE INDEX idx_assets_tx_hash
    ON assets(tx_hash);

CREATE INDEX idx_assets_policy
    ON assets(policy);

CREATE INDEX idx_assets_policy_assetname
    ON assets(policy, asset_name);

CREATE INDEX idx_assets_unit
    ON assets(unit);

CREATE INDEX idx_assets_fingerprint
    ON assets(fingerprint);

-- account balance

CREATE INDEX idx_address_balance_address
    ON address_balance (address);

CREATE INDEX idx_address_balance_block_time
    ON address_balance (block_time);

CREATE INDEX  idx_address_balance_epoch
    ON address_balance (epoch);

CREATE INDEX idx_address_balance_unit
    ON address_balance (unit);

CREATE INDEX idx_address_balance_policy
    ON address_balance (policy);

CREATE INDEX idx_address_stake_address
    ON address_balance (stake_address);

CREATE INDEX idx_address_balance_policy_asset
    ON address_balance (policy, asset_name);

-- stake address balance

CREATE INDEX idx_stake_addr_balance_stake_addr
    ON stake_address_balance (address);

CREATE INDEX idx_stake_addr_balance_block_time
    ON stake_address_balance (block_time);

CREATE INDEX idx_stake_addr_balance_epoch
    ON stake_address_balance (epoch);

CREATE INDEX idx_stake_addr_balance_unit
    ON stake_address_balance (unit);

CREATE INDEX idx_stake_addr_balance_policy
    ON stake_address_balance (policy);

CREATE INDEX idx_stake_addr_balance_policy_asset
    ON stake_address_balance (policy, asset_name);

-- transaction_witness

CREATE INDEX idx_transaction_witness_tx_hash
    ON transaction_witness(tx_hash);