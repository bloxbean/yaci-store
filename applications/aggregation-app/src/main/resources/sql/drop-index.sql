-- utxo store
drop index idx_address_utxo_owner_addr;
drop index idx_address_utxo_owner_stake_addr;
drop index idx_address_utxo_owner_paykey_hash;
drop index idx_address_utxo_owner_stakekey_hash;
drop index idx_address_utxo_epoch;


-- account balance
drop index idx_address_balance_address;
drop index idx_address_balance_block_time;
drop index idx_address_balance_epoch;
drop index idx_address_balance_unit;


-- stake address balance

drop index idx_stake_addr_balance_stake_addr;
drop index idx_stake_addr_balance_block_time;
drop index idx_stake_addr_balance_epoch;
