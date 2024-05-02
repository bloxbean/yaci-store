-- set search_path  to mainnet;

-- transaction store
CREATE INDEX if not exists idx_transaction_block
    ON transaction(block);

CREATE INDEX if not exists idx_transaction_block_hash
    ON transaction(block_hash);

CREATE INDEX if not exists idx_withdrawal_address
    ON withdrawal(address);

CREATE INDEX if not exists idx_withdrawal_tx_hash
    ON withdrawal(tx_hash);

-- utxo store

CREATE INDEX if not exists idx_address_utxo_owner_addr
    ON address_utxo(owner_addr);

CREATE INDEX if not exists idx_address_utxo_owner_stake_addr
    ON address_utxo(owner_stake_addr);

CREATE INDEX if not exists idx_address_utxo_owner_paykey_hash
    ON address_utxo(owner_payment_credential);

CREATE INDEX if not exists idx_address_utxo_owner_stakekey_hash
    ON address_utxo(owner_stake_credential);

CREATE INDEX if not exists idx_address_utxo_epoch
    ON address_utxo(epoch);


-- asset store

CREATE INDEX if not exists idx_assets_tx_hash
    ON assets(tx_hash);

CREATE INDEX if not exists idx_assets_policy
    ON assets(policy);

CREATE INDEX if not exists idx_assets_policy_assetname
    ON assets(policy, asset_name);

CREATE INDEX if not exists idx_assets_unit
    ON assets(unit);

CREATE INDEX if not exists idx_assets_fingerprint
    ON assets(fingerprint);

-- account balance

CREATE INDEX if not exists idx_address_balance_address
    ON address_balance (address);

CREATE INDEX if not exists idx_address_balance_block_time
    ON address_balance (block_time);

CREATE INDEX if not exists idx_address_balance_epoch
    ON address_balance (epoch);

CREATE INDEX if not exists idx_address_balance_unit
    ON address_balance (unit);


-- stake address balance

CREATE INDEX if not exists idx_stake_addr_balance_stake_addr
    ON stake_address_balance (address);

CREATE INDEX if not exists idx_stake_addr_balance_block_time
    ON stake_address_balance (block_time);

CREATE INDEX if not exists idx_stake_addr_balance_epoch
    ON stake_address_balance (epoch);

-- transaction_witness

CREATE INDEX if not exists idx_transaction_witness_tx_hash
    ON transaction_witness(tx_hash);

-- metadata

CREATE INDEX if not exists idx_txn_metadata_tx_hash
    ON transaction_metadata(tx_hash);

CREATE INDEX if not exists idx_txn_metadata_label
    ON transaction_metadata(label);

-- scripts

CREATE INDEX if not exists idx_txn_scripts_tx_hash
    ON transaction_scripts (tx_hash);

