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

CREATE INDEX idx_utxo_amount_unit
    ON utxo_amount(unit);

CREATE INDEX idx_utxo_amount_policy
    ON utxo_amount(policy);

CREATE INDEX idx_utxo_amount_asset_name
    ON utxo_amount(asset_name);

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
