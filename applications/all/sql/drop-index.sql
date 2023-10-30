set search_path to mainnet;

-- transaction store
drop index idx_transaction_block;
drop index idx_transaction_block_hash;

-- utxo store
drop index idx_address_utxo_owner_addr;
drop index idx_address_utxo_owner_stake_addr;
drop index idx_address_utxo_owner_paykey_hash;
drop index idx_address_utxo_owner_stakekey_hash;
drop index idx_address_utxo_epoch;
drop index idx_address_utxo_spent_epoch;

-- assets store
drop index idx_assets_tx_hash;
drop index idx_assets_policy;
drop index idx_assets_policy_assetname;
drop index idx_assets_unit;
drop index idx_assets_fingerprint;

-- account balance
drop index idx_address_balance_address;
drop index idx_address_balance_block_time;
drop index idx_address_balance_epoch;
drop index idx_address_balance_unit;
drop index idx_address_balance_policy;
drop index idx_address_stake_address;
drop index idx_address_balance_policy_asset;

-- stake address balance

drop index idx_stake_addr_balance_stake_addr;
drop index idx_stake_addr_balance_block_time;
drop index idx_stake_addr_balance_epoch;
drop index idx_stake_addr_balance_unit;
drop index idx_stake_addr_balance_policy;
drop index idx_stake_addr_balance_policy_asset;