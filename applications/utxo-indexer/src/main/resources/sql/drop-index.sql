-- set search_path to mainnet;

-- utxo store
drop index idx_address_utxo_owner_addr;
drop index idx_address_utxo_owner_stake_addr;
drop index idx_address_utxo_owner_paykey_hash;
drop index idx_address_utxo_owner_stakekey_hash;
drop index idx_address_utxo_epoch;
