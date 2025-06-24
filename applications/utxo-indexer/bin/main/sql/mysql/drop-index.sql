-- set search_path to mainnet;

-- utxo store
drop index idx_address_utxo_owner_addr on address_utxo;
drop index idx_address_utxo_owner_stake_addr on address_utxo;
drop index idx_address_utxo_owner_paykey_hash on address_utxo;
drop index idx_address_utxo_owner_stakekey_hash on address_utxo;
drop index idx_address_utxo_epoch on address_utxo;

