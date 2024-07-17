-- set search_path to mainnet;

-- transaction store
drop index idx_transaction_block on transaction;
drop index idx_transaction_block_hash on transaction;

drop index idx_withdrawal_address on withdrawal;
drop index idx_withdrawal_tx_hash on withdrawal;

-- utxo store
drop index idx_address_utxo_owner_addr on address_utxo;
drop index idx_address_utxo_owner_stake_addr on address_utxo;
drop index idx_address_utxo_owner_paykey_hash on address_utxo;
drop index idx_address_utxo_owner_stakekey_hash on address_utxo;
drop index idx_address_utxo_epoch on address_utxo;

-- assets store
drop index idx_assets_tx_hash on assets;
drop index idx_assets_policy  on assets;
drop index idx_assets_policy_assetname  on assets;
drop index idx_assets_unit  on assets;
drop index idx_assets_fingerprint  on assets;

-- account balance
drop index idx_address_balance_address on address_balance;
drop index idx_address_balance_block_time on address_balance;
drop index idx_address_balance_epoch on address_balance;
drop index idx_address_balance_unit on address_balance;


-- stake address balance

drop index idx_stake_addr_balance_stake_addr on stake_address_balance;
drop index idx_stake_addr_balance_block_time on stake_address_balance;
drop index idx_stake_addr_balance_epoch on stake_address_balance;

-- transaction_witness
drop index idx_transaction_witness_tx_hash on transaction_witness;

-- metadata
drop index idx_txn_metadata_tx_hash on transaction_metadata;
drop index idx_txn_metadata_label on transaction_metadata;

-- scripts
drop index idx_txn_scripts_tx_hash on transaction_scripts;

-- governance
drop index idx_gov_action_proposal_txhash on gov_action_proposal;
drop index idx_gov_action_proposal_return_address on gov_action_proposal;
drop index idx_gov_action_proposal_type on gov_action_proposal;
drop index idx_voting_procedure_txhash on voting_procedure;
drop index idx_voting_procedure_gov_action_tx_hash on voting_procedure;
drop index idx_voting_procedure_gov_action_tx_hash_gov_action_index on voting_procedure;
drop index idx_delegation_vote_address on delegation_vote;
drop index idx_delegation_vote_drep_id on delegation_vote;