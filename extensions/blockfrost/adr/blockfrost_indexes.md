# Blockfrost Index Manifest

**Created:** 2026-05-26

## Scope

This file summarizes PostgreSQL indexes referenced by the Blockfrost compatibility module ADRs.

Duplicate references across module ADRs are consolidated into a single row when the proposed
index name, table, and columns are the same.

## Index Inventory

| No. | Module | Source ADR | Table | Proposed Index Name | Proposed Columns | Condition | Existing / Coverage | Notes |
|---:|---|---|---|---|---|---|---|---|
| 1 | account | `account_gaps.md` | `address_utxo` | `idx_address_utxo_owner_stake_addr` | `(owner_stake_addr)` | None | `index.yml` | Already provided by `index.yml`. |
| 2 | account | `account_gaps.md` | `withdrawal` | `idx_withdrawal_address` | `(address)` | None | `index.yml` | Already provided by `index.yml`. |
| 3 | account | `account_gaps.md` | `withdrawal` | `idx_withdrawal_address_slot` | `(address, slot)` | None | `index.yml` | Already provided by `index.yml`. |
| 4 | account | `account_gaps.md` | `instant_reward` | `idx_instant_reward_address_slot` | `(address, slot)` | None | `index.yml` | Already provided by `index.yml`. |
| 5 | account | `account_gaps.md` | `reward_rest` | `idx_reward_rest_address_slot` | `(address, slot)` | None | `index.yml` | Already provided by `index.yml`. |
| 6 | address | `address_gaps.md` | `address_utxo` | `idx_address_utxo_owner_addr` | `(owner_addr)` | None | `index.yml` | Already provided by `index.yml`. |
| 7 | address | `address_gaps.md` | `address_utxo` | `idx_address_utxo_owner_addr_full` | `(owner_addr_full)` | None | None | Needed for Byron-length addresses. |
| 8 | asset | `asset_gaps.md` | `address_utxo` | `idx_address_utxo_amounts` | `(amounts)` | None | `extra-index.yml` | Already provided by `extra-index.yml` as a GIN index on JSONB `amounts`. |
| 9 | asset | `asset_gaps.md` | `assets` | `idx_assets_unit_slot` | `(unit, slot)` | None | None | Proposed from asset ADR. |
| 10 | asset | `asset_gaps.md` | `assets` | `idx_assets_unit_policy` | `(unit, policy)` | None | None | Proposed from asset ADR. |
| 11 | asset | `asset_gaps.md` | `assets` | `idx_assets_unit_qty` | `(unit, quantity)` | None | None | Proposed from asset ADR. |
| 12 | asset | `asset_gaps.md` | `assets` | `idx_assets_policy` | `(policy)` | None | `index.yml` | Already provided by `index.yml`. |
| 13 | asset | `asset_gaps.md` | `assets_mint` | `idx_assets_mint_unit_slot` | `(unit, slot)` | None | N/A | `assets_mint` is not present in the current tree; re-check when the owning module lands. |
| 14 | asset | `asset_gaps.md` | `assets_mint` | `idx_assets_mint_slot_tx_hash` | `(slot, tx_hash)` | None | N/A | `assets_mint` is not present in the current tree; re-check when the owning module lands. |
| 15 | asset, transaction | `asset_gaps.md`, `transaction_gaps.md` | `transaction` | `idx_transaction_tx_hash_tx_index` | `(tx_hash, tx_index)` | None | Covered by `transaction(tx_hash)` PK | Do not add unless a query-plan check proves the composite is needed. |
| 16 | block | `block_gaps.md` | `transaction` | `idx_transaction_block_tx_index_tx_hash` | `(block_hash, tx_index, tx_hash)` | None | None | Proposed from block ADR. |
| 17 | block | `block_gaps.md` | `address_utxo` | `idx_address_utxo_block` | `(block_hash)` | None | None | Proposed from block ADR. |
| 18 | block | `block_gaps.md` | `tx_input` | `idx_tx_input_spent_at_block_spent_tx_hash` | `(spent_at_block, spent_tx_hash)` | None | Prefix exists: `idx_tx_input_block(spent_at_block)` | Proposed composite is not an exact duplicate. |
| 19 | epoch | `epoch_gaps.md` | `block` | `idx_block_epoch_slot` | `(epoch, slot)` | None | Prefix exists: `idx_block_epoch(epoch)` | Proposed composite is not an exact duplicate. |
| 20 | governance | `governance_gaps.md` | `drep_registration` | `idx_drep_registration_drep_hash` | `(drep_hash)` | None | Flyway: `idx_drep_registration_drep_hash` | Already provided by governance store migration. |
| 21 | governance | `governance_gaps.md` | `voting_procedure` | `idx_voting_procedure_drep_hash_type` | `(drep_hash, voter_type)` | None | N/A | `drep_hash` is not present on current `voting_procedure`; re-check with governance PR/schema. |
| 22 | pools | `pools_gaps.md` | `pool_registration` | `idx_pool_registration_pool_id_slot` | `(pool_id, slot)` | None | Prefix exists: `idx_pool_registration_pool_id(pool_id)` | Proposed from pools ADR. |
| 23 | pools | `pools_gaps.md` | `pool_retirement` | `idx_pool_retirement_pool_id_slot` | `(pool_id, slot)` | None | Prefix exists: `idx_pool_retirement_pool_id(pool_id)` | Proposed from pools ADR. |
| 24 | pools | `pools_gaps.md` | `pool_status` | `idx_pool_status_pool_id` | `(pool_id)` | None | N/A | `pool_status` is not present in the current tree; re-check table name before applying. |
| 25 | pools | `pools_gaps.md` | `epoch_stake` | `idx_epoch_stake_pool_epoch` | `(pool_id, epoch)` | None | Reverse order exists: `idx_epoch_stake_epoch_pool_id(epoch, pool_id)` | Column order differs; needs query-plan validation. |
| 26 | pools | `pools_gaps.md` | `block` | `idx_block_slot_leader_epoch` | `(slot_leader, epoch)` | None | Prefix exists: `idx_block_slot_leader(slot_leader)` | Proposed from pools ADR. |
| 27 | pools | `pools_gaps.md` | `voting_procedure` | `idx_gov_action_pool_id` | `(pool_hash)` | None | N/A | `pool_hash` is not present on current `voting_procedure`; if later valid, prefer `idx_voting_procedure_pool_hash`. |
| 28 | scripts | `scripts_gaps.md` | `transaction_scripts` | `idx_transaction_scripts_purpose_not_null` | `(script_hash, slot)` | `WHERE purpose IS NOT NULL` | None | Partial PostgreSQL index; runtime YAML excludes `mysql` and `h2`. |
| 29 | transaction | `transaction_gaps.md` | `transaction_cbor` | `idx_transaction_cbor_tx_hash` | `(tx_hash)` | None | Covered by `transaction_cbor(tx_hash)` PK | Do not add as a separate index. |
| 30 | transaction | `transaction_gaps.md` | `tx_input` | `idx_tx_input_tx_hash` | `(tx_hash)` | None | None | Current PK is `(output_index, tx_hash)`, so `tx_hash` is not the leading column. |
| 31 | transaction | `transaction_gaps.md` | `delegation` | `idx_delegation_tx_hash` | `(tx_hash)` | None | Flyway: `idx_delegation_txhash` | Same table and column with a different existing index name. |
| 32 | transaction | `transaction_gaps.md` | `stake_registration` | `idx_stake_registration_tx_hash` | `(tx_hash)` | None | Flyway: `idx_stake_registration_stake_txhash` | Same table and column with a different existing index name. |
| 33 | transaction | `transaction_gaps.md` | `pool_registration` | `idx_pool_registration_tx_hash` | `(tx_hash)` | None | Flyway: `idx_pool_registration_txhash` | Same table and column with a different existing index name. |
| 34 | transaction | `transaction_gaps.md` | `drep_registration` | `idx_drep_registration_tx_hash` | `(tx_hash)` | None | Covered by `drep_registration(tx_hash, cert_index)` PK | Do not add as a separate index. |

## Existing Or Covered Indexes

These proposed indexes should not be added to `blockfrost-index.yml` unless new query-plan evidence
shows that a separate Blockfrost-specific index is still needed.

| Proposed Index Name | Existing Source | Reason |
|---|---|---|
| `idx_address_utxo_owner_stake_addr` | `index.yml` | Exact index from `index.yml`. |
| `idx_withdrawal_address` | `index.yml` | Exact index from `index.yml`. |
| `idx_withdrawal_address_slot` | `index.yml` | Exact index from `index.yml`. |
| `idx_instant_reward_address_slot` | `index.yml` | Exact index from `index.yml`. |
| `idx_reward_rest_address_slot` | `index.yml` | Exact index from `index.yml`. |
| `idx_address_utxo_owner_addr` | `index.yml` | Exact index from `index.yml`. |
| `idx_address_utxo_amounts` | `extra-index.yml` | Exact optional GIN index. |
| `idx_assets_policy` | `index.yml` | Exact index from `index.yml`. |
| `idx_drep_registration_drep_hash` | Flyway governance migration | Exact migration index. |
| `idx_transaction_tx_hash_tx_index` | `transaction(tx_hash)` primary key | `tx_hash` lookup is already covered. |
| `idx_transaction_cbor_tx_hash` | `transaction_cbor(tx_hash)` primary key | `tx_hash` lookup is already covered. |
| `idx_delegation_tx_hash` | Flyway `idx_delegation_txhash` | Same table and column with a different index name. |
| `idx_stake_registration_tx_hash` | Flyway `idx_stake_registration_stake_txhash` | Same table and column with a different index name. |
| `idx_pool_registration_tx_hash` | Flyway `idx_pool_registration_txhash` | Same table and column with a different index name. |
| `idx_drep_registration_tx_hash` | `drep_registration(tx_hash, cert_index)` primary key | `tx_hash` lookup is already covered by the leading PK column. |

## Modules Without Additional Indexes

| Module | Notes |
|---|---|
| metadata | `metadata_gaps.md` states no additional indexes are required |
| network | `network_gaps.md` states no additional indexes are required |
