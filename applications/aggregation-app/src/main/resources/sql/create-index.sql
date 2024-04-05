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
