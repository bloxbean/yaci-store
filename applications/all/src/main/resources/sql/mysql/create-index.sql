-- set search_path  to mainnet;

-- transaction store
CREATE INDEX idx_transaction_block
    ON transaction(block);

CREATE INDEX idx_transaction_block_hash
    ON transaction(block_hash);

CREATE INDEX idx_withdrawal_address
    ON withdrawal(address);

CREATE INDEX idx_withdrawal_tx_hash
    ON withdrawal(tx_hash);

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

CREATE INDEX idx_utxo_amount_owner_addr
    ON utxo_amount(owner_addr);

CREATE INDEX idx_utxo_amount_unit
    ON utxo_amount(unit);

CREATE INDEX idx_utxo_amount_policy
    ON utxo_amount(policy);

CREATE INDEX idx_utxo_amount_asset_name
    ON utxo_amount(asset_name);

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


-- stake address balance

CREATE INDEX idx_stake_addr_balance_stake_addr
    ON stake_address_balance (address);

CREATE INDEX idx_stake_addr_balance_block_time
    ON stake_address_balance (block_time);

CREATE INDEX idx_stake_addr_balance_epoch
    ON stake_address_balance (epoch);

-- transaction_witness

CREATE INDEX idx_transaction_witness_tx_hash
    ON transaction_witness(tx_hash);

-- metadata

CREATE INDEX idx_txn_metadata_tx_hash
    ON transaction_metadata(tx_hash);

CREATE INDEX idx_txn_metadata_label
    ON transaction_metadata(label);

-- scripts

CREATE INDEX idx_txn_scripts_tx_hash
    ON transaction_scripts (tx_hash);
