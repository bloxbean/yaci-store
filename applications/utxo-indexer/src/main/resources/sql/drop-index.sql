-- set search_path to mainnet;

-- utxo store
drop index idx_address_utxo_owner_addr;
drop index idx_address_utxo_owner_stake_addr;
drop index idx_address_utxo_owner_paykey_hash;
drop index idx_address_utxo_owner_stakekey_hash;
drop index idx_address_utxo_epoch;

drop index idx_utxo_amount_owner_addr;
drop index idx_utxo_amount_unit;
drop index idx_utxo_amount_policy;
drop index idx_utxo_amount_asset_name;
