-- set search_path  to mainnet;

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
