set search_path to mainet;

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
