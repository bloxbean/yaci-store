# Blockfrost Index Manifest

**Created:** 2026-05-26

## Scope

This file summarizes PostgreSQL indexes referenced by the Blockfrost compatibility module ADRs.

Duplicate references across module ADRs are consolidated into a single row when the proposed
index name, table, and columns are the same.

## Index Inventory

| No. | Module | Table | Proposed Index Name | Proposed Columns | Notes |
|---:|---|---|---|---|---|
| 1 | account | `address_utxo` | `idx_address_utxo_owner_stake_addr` | `(owner_stake_addr)` | Listed in `account_gaps.md` as already in `index.yml`; verify before applying. |
| 2 | account | `withdrawal` | `idx_withdrawal_address` | `(address)` | Listed in `account_gaps.md` as already in `index.yml`; verify before applying. |
| 3 | account | `withdrawal` | `idx_withdrawal_address_slot` | `(address, slot)` | Listed in `account_gaps.md` as already in `index.yml`; verify before applying. |
| 4 | account | `instant_reward` | `idx_instant_reward_address_slot` | `(address, slot)` | Listed in `account_gaps.md` as already in `index.yml`; verify before applying. |
| 5 | account | `reward_rest` | `idx_reward_rest_address_slot` | `(address, slot)` | Listed in `account_gaps.md` as already in `index.yml`; verify before applying. |
| 6 | address | `address_utxo` | `idx_address_utxo_owner_addr` | `(owner_addr)` | Proposed from `address_gaps.md`; prevents full table scans for address endpoints. |
| 7 | address | `address_utxo` | `idx_address_utxo_owner_addr_full` | `(owner_addr_full)` | Proposed from `address_gaps.md`; needed for Byron-length addresses. |
| 8 | asset | `address_utxo` | `idx_address_utxo_amounts` | `(amounts)` | Proposed from `asset_gaps.md`; GIN index on JSONB `amounts`. |
| 9 | asset | `assets` | `idx_assets_unit_slot` | `(unit, slot)` | Proposed from `asset_gaps.md`. |
| 10 | asset | `assets` | `idx_assets_unit_policy` | `(unit, policy)` | Proposed from `asset_gaps.md`. |
| 11 | asset | `assets` | `idx_assets_unit_qty` | `(unit, quantity)` | Proposed from `asset_gaps.md`. |
| 12 | asset | `assets` | `idx_assets_policy` | `(policy)` | Proposed from `asset_gaps.md`. |
| 13 | asset | `assets_mint` | `idx_assets_mint_unit_slot` | `(unit, slot)` | Proposed from `asset_gaps.md`. |
| 14 | asset | `assets_mint` | `idx_assets_mint_slot_tx_hash` | `(slot, tx_hash)` | Proposed from `asset_gaps.md`. |
| 15 | asset, transaction | `transaction` | `idx_transaction_tx_hash_tx_index` | `(tx_hash, tx_index)` | Listed in both `asset_gaps.md` and `transaction_gaps.md`; keep as one proposed index. |
| 16 | block | `transaction` | `idx_transaction_block_tx_index_tx_hash` | `(block_hash, tx_index, tx_hash)` | Proposed from `block_gaps.md`. |
| 17 | block | `address_utxo` | `idx_address_utxo_block` | `(block_hash)` | Proposed from `block_gaps.md`. |
| 18 | block | `tx_input` | `idx_tx_input_spent_at_block_spent_tx_hash` | `(spent_at_block, spent_tx_hash)` | Proposed from `block_gaps.md`. |
| 19 | epoch | `block` | `idx_block_epoch_slot` | `(epoch, slot)` | Proposed from `epoch_gaps.md` for block query performance. |
| 20 | governance | `drep_registration` | `idx_drep_registration_drep_hash` | `(drep_hash)` | Proposed from `governance_gaps.md`. |
| 21 | governance | `voting_procedure` | `idx_voting_procedure_drep_hash_type` | `(drep_hash, voter_type)` | Proposed from `governance_gaps.md`. |
| 22 | pools | `pool_registration` | `idx_pool_registration_pool_id_slot` | `(pool_id, slot)` | Proposed from `pools_gaps.md`. |
| 23 | pools | `pool_retirement` | `idx_pool_retirement_pool_id_slot` | `(pool_id, slot)` | Proposed from `pools_gaps.md`. |
| 24 | pools | `pool_status` | `idx_pool_status_pool_id` | `(pool_id)` | Proposed from `pools_gaps.md`. |
| 25 | pools | `epoch_stake` | `idx_epoch_stake_pool_epoch` | `(pool_id, epoch)` | Proposed from `pools_gaps.md`. |
| 26 | pools | `block` | `idx_block_slot_leader_epoch` | `(slot_leader, epoch)` | Proposed from `pools_gaps.md`. |
| 27 | pools | `voting_procedure` | `idx_gov_action_pool_id` | `(pool_hash)` | Proposed from `pools_gaps.md`; index name references governance action but table is `voting_procedure`. |
| 28 | scripts | `transaction_scripts` | `idx_transaction_scripts_purpose_not_null` | `(script_hash, slot)` | Proposed from `scripts_gaps.md`; partial index with `WHERE purpose IS NOT NULL`. |
| 29 | transaction | `transaction_cbor` | `idx_transaction_cbor_tx_hash` | `(tx_hash)` | Proposed from `transaction_gaps.md`. |
| 30 | transaction | `tx_input` | `idx_tx_input_tx_hash` | `(tx_hash)` | Proposed from `transaction_gaps.md`. |
| 31 | transaction | `delegation` | `idx_delegation_tx_hash` | `(tx_hash)` | Proposed from `transaction_gaps.md`. |
| 32 | transaction | `stake_registration` | `idx_stake_registration_tx_hash` | `(tx_hash)` | Proposed from `transaction_gaps.md`. |
| 33 | transaction | `pool_registration` | `idx_pool_registration_tx_hash` | `(tx_hash)` | Proposed from `transaction_gaps.md`. |
| 34 | transaction | `drep_registration` | `idx_drep_registration_tx_hash` | `(tx_hash)` | Proposed from `transaction_gaps.md`. |

## Modules Without Additional Indexes

| Module | Notes |
|---|---|
| metadata | `metadata_gaps.md` states no additional indexes are required |
| network | `network_gaps.md` states no additional indexes are required |
