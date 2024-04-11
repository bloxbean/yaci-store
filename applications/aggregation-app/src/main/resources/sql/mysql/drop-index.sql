-- utxo store
drop index idx_address_utxo_owner_addr on address_utxo;
drop index idx_address_utxo_owner_stake_addr on address_utxo;
drop index idx_address_utxo_owner_paykey_hash on address_utxo;
drop index idx_address_utxo_owner_stakekey_hash on address_utxo;
drop index idx_address_utxo_epoch on address_utxo;

drop index idx_utxo_amount_unit on utxo_amount;
drop index idx_utxo_amount_policy on utxo_amount;
drop index idx_utxo_amount_asset_name on utxo_amount;

-- account balance
drop index idx_address_balance_address on address_balance;
drop index idx_address_balance_block_time on address_balance;
drop index idx_address_balance_epoch on address_balance;
drop index idx_address_balance_unit on address_balance;


-- stake address balance

drop index idx_stake_addr_balance_stake_addr on stake_address_balance;
drop index idx_stake_addr_balance_block_time on stake_address_balance;
drop index idx_stake_addr_balance_epoch on stake_address_balance;
