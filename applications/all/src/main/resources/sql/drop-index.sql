-- set search_path to mainnet;

-- transaction store
drop index idx_transaction_block;
drop index idx_transaction_block_hash;

drop index idx_withdrawal_address;
drop index idx_withdrawal_tx_hash;

-- utxo store
drop index idx_address_utxo_owner_addr;
drop index idx_address_utxo_owner_stake_addr;
drop index idx_address_utxo_owner_paykey_hash;
drop index idx_address_utxo_owner_stakekey_hash;
drop index idx_address_utxo_epoch;


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


-- stake address balance

drop index idx_stake_addr_balance_stake_addr;
drop index idx_stake_addr_balance_block_time;
drop index idx_stake_addr_balance_epoch;
-- transaction_witness
drop index idx_transaction_witness_tx_hash;

-- metadata
drop index idx_txn_metadata_tx_hash;
drop index idx_txn_metadata_label;

-- scripts
drop index idx_txn_scripts_tx_hash;

-- governance
drop index idx_gov_action_proposal_txhash;
drop index idx_gov_action_proposal_return_address;
drop index idx_gov_action_proposal_type;
drop index idx_voting_procedure_txhash;
drop index idx_voting_procedure_gov_action_tx_hash;
drop index idx_voting_procedure_gov_action_tx_hash_gov_action_index;
drop index idx_delegation_vote_address;
drop index idx_delegation_vote_drep_id;