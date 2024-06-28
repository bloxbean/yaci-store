# I. Assets store

## 1. assets

| **Column name** | **Type**     | **Description**                |
|:----------------|:-------------|:-------------------------------|
| **id**          | uuid         | Unique identifier of the asset |
| slot            | bigint       | Slot number                    |
| tx_hash         | varchar(64)  | Transaction hash               |
| policy          | varchar(56)  | Policy id                      |
| asset_name      | varchar(255) | Name of the asset              |
| unit            | varchar(255) | Unit of the asset              |
| fingerprint     | varchar(255) | Fingerprint of the asset       |
| quantity        | bigint       | Quantity of the asset          |
| mint_type       | varchar(4)   | Mint type (MINT/BURN)          |
| block           | bigint       | Block number                   |
| block_time      | bigint       | Block time                     |
| update_datetime | timestamp    | Datetime of update             |

# II. Block store

## 1. block

| **Column name**    | **Type**             | **Description**                                                              |
|:-------------------|:---------------------|:-----------------------------------------------------------------------------|
| **hash**           | varchar(64) not null | Block hash (primary key)                                                     |
| number             | bigint               | Block number within the chain                                                |
| body_hash          | varchar(64)          | Hash of the block body data                                                  |
| body_size          | integer              | Size of the block body data in bytes                                         |
| epoch              | integer              | Epoch no of the block                                                        |
| total_output       | numeric(38)          | Total output created in the block                                            |
| total_fees         | bigint               | Total fees collected in the block                                            |
| block_time         | bigint               | Block creation timestamp                                                     |
| era                | smallint             | Era number                                                                   |
| issuer_vkey        | varchar(64)          | Public key of the block issuer                                               |
| leader_vrf         | jsonb                | Leader verification result (JSON, nullable)                                  |
| nonce_vrf          | jsonb                | Nonce verification result (JSON, nullable)                                   |
| prev_hash          | varchar(64)          | Hash of the previous block                                                   |
| protocol_version   | varchar(64)          | Blockchain protocol version used                                             |
| slot               | bigint               | Slot number                                                                  |
| vrf_result         | jsonb                | VRF verification result (JSON, nullable)                                     |
| vrf_vkey           | varchar(64)          | The VRF key of the creator of this block                                     |
| no_of_txs          | integer              | The number of transactions in this block                                     |
| slot_leader        | varchar(56)          | The hash of of the block producer identifier                                 |
| epoch_slot         | integer              | The slot number within an epoch (resets to zero at the start of each epoch). |
| op_cert_hot_vkey   | varchar(64)          | The hash of the operational certificate of the block producer                |
| op_cert_seq_number | bigint               | The value of the counter used to produce the operational certificate         |
| op_cert_kes_period | bigint               | KES key period for operator certificate                                      |
| op_cert_sigma      | varchar(256)         | Signature for operator certificate                                           |
| create_datetime    | timestamp            | Date and time the record was created                                         |
| update_datetime    | timestamp            | Date and time the record was last updated                                    |

## 2. rollback

| **Column name**        | **Type**    | **Description**                                |
|:-----------------------|:------------|:-----------------------------------------------|
| **id**                 | bigint      | Unique identifier (auto-incrementing)          |
| rollback_to_block_hash | varchar(64) | Hash of the block to rollback to               |
| rollback_to_slot       | bigint      | Slot number of the block to rollback to        |
| current_block_hash     | varchar(64) | Hash of the current block                      |
| current_slot           | bigint      | Slot number of the current block               |
| current_block          | bigint      | Block number (unique identifier for the block) |
| create_datetime        | timestamp   | Date and time the record was created           |
| update_datetime        | timestamp   | Date and time the record was last updated      |

# III. Epoch store

## 1. local_protocol_params

| **Column name** | **Type**  | **Description**                              |
|:----------------|:----------|:---------------------------------------------|
| **id**          | bigint    | Unique identifier (primary key)              |
| params          | jsonb     | JSON document containing protocol parameters |
| create_datetime | timestamp | Date and time the record was created         |
| update_datetime | timestamp | Date and time the record was last updated    |

## 2. protocol_params_proposal

| **Column name** | **Type**    | **Description**                                       |
|:----------------|:------------|:------------------------------------------------------|
| **tx_hash**     | varchar(64) | Unique transaction hash (not null)                    |
| **key_hash**    | varchar(56) | Unique key hash (not null)                            |
| params          | jsonb       | JSON document containing proposed protocol parameters |
| target_epoch    | integer     | Target epoch for the protocol parameter change        |
| epoch           | integer     | Epoch number                                          |
| slot            | bigint      | Slot number                                           |
| era             | smallint    | Era identifier                                        |
| block           | bigint      | Block number                                          |
| block_time      | bigint      | Block creation timestamp                              |
| update_datetime | timestamp   | Date and time the record was last updated             |

## 3. epoch_param

| **Column name** | **Type**    | **Description**                                                       |
|:----------------|:------------|:----------------------------------------------------------------------|
| **epoch**       | integer     | The first epoch for which these parameters are valid. (not null)      |
| params          | jsonb       | JSON document containing epoch parameters                             |
| cost_model_hash | varchar(64) | The hash of cost model. It ensures uniqueness of entries. New in v13. |
| slot            | bigint      | Slot number                                                           |
| block           | bigint      | Block number                                                          |
| block_time      | bigint      | Block creation timestamp                                              |
| update_datetime | timestamp   | Date and time the record was last updated                             |

## 4. cost_model

| **Column name** | **Type**    | **Description**                                                                  |
|:----------------|:------------|:---------------------------------------------------------------------------------|
| **hash**        | varchar(64) | The hash of cost model. It ensures uniqueness of entries. New in v13. (not null) |
| costs           | jsonb       | The actual costs formatted as json                                               |
| slot            | bigint      | Slot number                                                                      |
| block           | bigint      | Block number                                                                     |
| block_time      | bigint      | Block creation timestamp                                                         |
| update_datetime | timestamp   | Date and time the record was last updated                                        |

# IV Epoch aggregation

## 1. epoch

| **Column name**   | **Type**    | **Description**                                                      |
|-------------------|-------------|----------------------------------------------------------------------|
| **number**        | bigint      | The epoch number                                                     |
| block_count       | int         | The number of blocks in this epoch                                   |
| transaction_count | bigint      | The number of transactions in this epoch                             |
| total_output      | numeric(38) | The sum of the transaction output values (in Lovelace) in this epoch |
| total_fees        | bigint      | The sum of the fees (in Lovelace) in this epoch                      |
| start_time        | bigint      | The epoch start time                                                 |
| end_time          | bigint      | The epoch end time                                                   |
| max_slot          | bigint      | The slot of the last block in the epoch                              |
| create_datetime   | timestamp   | Date and time the record was created                                 |
| update_datetime   | timestamp   | Date and time the record was last updated                            |

# V. Governance store

## 1. gov_action_proposal

| **Column name** | **Type**     | **Description**                                                                                                                 |
|-----------------|--------------|---------------------------------------------------------------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | The hash of the tx that includes this certificate                                                                               |
| **idx**         | int          | The index of this proposal procedure within its transaction                                                                     |
| deposit         | bigint       | The deposit amount payed for this proposal (in lovelace)                                                                        |
| return_address  | varchar(255) | The StakeAddress index of the reward address to receive the deposit when it is repaid                                           |
| anchor_url      | varchar      | URL for additional information about the proposal                                                                               |
| anchor_hash     | varchar(64)  | Hash of the off-chain data pointed to by anchor_url                                                                             |
| type            | varchar(50)  | Can be one of ParameterChange, HardForkInitiation, TreasuryWithdrawals, NoConfidence, NewCommittee, NewConstitution, InfoAction |
| details         | jsonb        | JSON document describing the content of  governance action                                                                      |
| epoch           | int          | Epoch number                                                                                                                    |
| slot            | bigint       | Slot number                                                                                                                     |
| block           | bigint       | Block number                                                                                                                    |
| block_time      | bigint       | Block time                                                                                                                      |
| update_datetime | timestamp    | Date and time the record was last updated                                                                                       |

## 2. voting_procedure

| **Column name**        | **Type**    | **Description**                                                         |
|------------------------|-------------|-------------------------------------------------------------------------|
| **tx_hash**            | varchar(64) | Transaction hash of the tx that includes this VotingProcedure           |
| **voter_hash**         | varchar(56) | Hash identifying the voter (not null, part of primary key)              |
| **gov_action_tx_hash** | varchar(64) | Transaction hash of the governance action                               |
| **gov_action_index**   | int         | The index of this proposal procedure within its transaction             |
| id                     | uuid        | Unique identifier                                                       |
| idx                    | int         | The index of this VotingProcedure within this transaction               |
| voter_type             | varchar(50) | The role of the voter. Can be one of ConstitutionalCommittee, DRep, SPO |
| vote                   | varchar(10) | The Vote. Can be one of Yes, No, Abstain                                |
| anchor_url             | varchar     | URL for additional information about the vote                           |
| anchor_hash            | varchar(64) | Hash of the off-chain data pointed to by anchor_url                     |
| epoch                  | int         | Epoch number                                                            |
| slot                   | bigint      | Slot number                                                             |
| block                  | bigint      | Block number                                                            |
| block_time             | bigint      | Block time                                                              |
| update_datetime        | timestamp   | Date and time the record was last updated                               |

## 3. committee_registration

| **Column name** | **Type**    | **Description**                                                            |
|-----------------|-------------|----------------------------------------------------------------------------|
| **tx_hash**     | varchar(64) | The hash of the tx that includes this certificate                          |
| **cert_index**  | int         | The index of this registration within the certificates of this transaction |
| cold_key        | varchar     | The reference to the registered cold key hash id                           |
| hot_key         | varchar     | The reference to the registered hot key hash id                            |
| cred_type       | varchar(40) | Type of credential used for registration (ADDR_KEYHASH, SCRIPTHASH)        |
| epoch           | int         | Epoch number                                                               |
| slot            | bigint      | Slot number                                                                |
| block           | bigint      | Block number                                                               |
| block_time      | bigint      | Block time                                                                 |
| update_datetime | timestamp   | Date and time the record was last updated                                  |

## 4. committee_deregistration

| **Column name** | **Type**    | **Description**                                                            |
|-----------------|-------------|----------------------------------------------------------------------------|
| **tx_hash**     | varchar(64) | The hash of the tx that includes this certificate                          |
| **cert_index**  | int         | The index of this registration within the certificates of this transaction |
| anchor_url      | varchar     | URL for additional information about the deregistration                    |
| anchor_hash     | varchar(64) | Hash of the off-chain data pointed to by anchor_url                        |
| cold_key        | varchar(64) | Public key for the cold wallet (not null)                                  |
| cred_type       | varchar(40) | Type of credential used for registration (ADDR_KEYHASH, SCRIPTHASH)        |
| epoch           | int         | Epoch number                                                               |
| slot            | bigint      | Slot number                                                                |
| block           | bigint      | Block number                                                               |
| block_time      | bigint      | Block time                                                                 |
| update_datetime | timestamp   | Date and time the record was last updated                                  |

## 5. delegation_vote

| **Column name** | **Type**     | **Description**                                                            |
|-----------------|--------------|----------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | The hash of the tx that includes this certificate                          |
| **cert_index**  | int          | The index of this registration within the certificates of this transaction |
| address         | varchar(255) | Bech32 encoded stake address of the delegator                              |
| drep_hash       | varchar(56)  | Drep hash for the pool being delegated to                                  |
| drep_id         | varchar(255) | Unique identifier for  a delegated representative (Bech32)                 |
| drep_type       | varchar(40)  | Type of drep (ADDR_KEYHASH, SCRIPTHASH, ABSTAIN, NO_CONFIDENCE)            |
| epoch           | int          | Epoch number                                                               |
| credential      | varchar(56)  | Hash of the credential used for voting                                     |
| cred_type       | varchar(40)  | Type of credential used for registration (ADDR_KEYHASH, SCRIPTHASH)        |
| slot            | bigint       | Slot number                                                                |
| block           | bigint       | Block number                                                               |
| block_time      | bigint       | Block time                                                                 |
| update_datetime | timestamp    | Date and time the record was last updated                                  |

## 6. drep_registration

| **Column name** | **Type**     | **Description**                                                         |
|-----------------|--------------|-------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | Unique transaction hash (not null, primary key)                         |
| **cert_index**  | int          | Index of the certificate within the transaction (not null, primary key) |
| type            | varchar(50)  | Type of DREP registration (e.g., stake pool registration, withdrawal)   |
| deposit         | bigint       | Amount of ADA deposited for specific registration types                 |
| drep_hash       | varchar(56)  | Drep hash for the pool being delegated to                               |
| drep_id         | varchar(255) | Unique identifier for  a delegated representative (Bech32)              |
| anchor_url      | varchar      | URL for additional information about the registration                   |
| anchor_hash     | varchar(64)  | Hash of the off-chain data pointed to by anchor_url                     |
| cred_type       | varchar(40)  | Type of credential used for registration (ADDR_KEYHASH, SCRIPTHASH)     |
| epoch           | int          | Epoch number                                                            |
| slot            | bigint       | Slot number                                                             |
| block           | bigint       | Block number                                                            |
| block_time      | bigint       | Block time                                                              |
| update_datetime | timestamp    | Date and time the record was last updated                               |

## 7. committee_member

| **Column name** | **Type**    | **Description**                                                     |
|-----------------|-------------|---------------------------------------------------------------------|
| **hash**        | varchar(56) | The cold key of committee member                                    |
| **slot**        | bigint      | Slot number                                                         |
| cred_type       | varchar(40) | Type of credential used for registration (ADDR_KEYHASH, SCRIPTHASH) |
| expired_epoch   | int         | The epoch this member expires                                       |
| update_datetime | timestamp   | Date and time the record was last updated                           |

# VI. Live store

| **Column name** | **Type** | **Description** |
|:----------------|:---------|:----------------|
|                 |          |                 |

# VII. Metadata store

## 1. transaction_metadata

| **Column name** | **Type**     | **Description**                                    |
|:----------------|:-------------|:---------------------------------------------------|
| **id**          | uuid         | Unique identifier (primary key)                    |
| slot            | bigint       | Slot number                                        |
| tx_hash         | varchar(64)  | The hash identifier of the transaction             |
| label           | varchar(255) | The metadata key (a Word64/unsigned 64 bit number) |
| body            | text         | The JSON payload if it can be decoded as JSON      |
| cbor            | text         | The raw bytes of the payload                       |
| block           | bigint       | Block number                                       |
| block_time      | bigint       | Block time                                         |
| update_datetime | timestamp    | Date and time the record was last updated          |

# VIII. Mir store

## 1. mir

| **Column name** | **Type**     | **Description**                                                              |
|:----------------|:-------------|:-----------------------------------------------------------------------------|
| **id**          | uuid         | Unique identifier (primary key)                                              |
| tx_hash         | varchar(64)  | Transaction hash (unique identifier for the transaction)                     |
| cert_index      | int          | Index of the certificate within the transaction                              |
| pot             | varchar(30)  | Pot type (e.g., stake, delegation)                                           |
| credential      | varchar(56)  | Credential associated with the MIR (stake address or delegation certificate) |
| address         | varchar(255) | Bech32 stake address associated with the MIR                                 |
| amount          | numeric(38)  | Amount of ADA associated with the MIR                                        |
| epoch           | int          | Cardano epoch number                                                         |
| slot            | bigint       | Slot number within the epoch                                                 |
| block_hash      | varchar(64)  | Hash of the block containing the transaction                                 |
| block           | bigint       | Block number (unique identifier for the block)                               |
| block_time      | bigint       | Unix timestamp of the block creation time                                    |
| update_datetime | timestamp    | Date and time the record was last updated                                    |

# IX. Script store

## 1. script

| **Column name** | **Type**    | **Description**                                                           |
|:----------------|:------------|:--------------------------------------------------------------------------|
| **script_hash** | varchar(56) | The Hash of the Script                                                    |
| script_type     | varchar(30) | The type of the script. This is currenttly either 'timelock' or 'plutus'. |
| content         | jsonb       | JSON representation of the timelock script, null for other script type    |
| create_datetime | timestamp   | Date and time the record was created                                      |
| update_datetime | timestamp   | Date and time the record was last updated                                 |

## 2. transaction_scripts

| **Column name**   | **Type**    | **Description**                                                                                                          |
|:------------------|:------------|:-------------------------------------------------------------------------------------------------------------------------|
| **id**            | uuid        | Unique identifier (primary key)                                                                                          |
| slot              | bigint      | Slot number                                                                                                              |
| block_hash        | varchar(64) | Hash of the block                                                                                                        |
| tx_hash           | varchar(64) | The hash identifier of the transaction                                                                                   |
| script_hash       | varchar(56) | The script hash this redeemer is used for                                                                                |
| script_type       | smallint    | Type of the script (NATIVE_SCRIPT(0), PLUTUS_V1(1), PLUTUS_V2(2), PLUTUS_V3(3))                                          |
| datum_hash        | varchar(64) | The Hash of the datum                                                                                                    |
| redeemer_cbor     | text        | Redeemer encoded in CBOR format                                                                                          |
| unit_mem          | bigint      | The budget in Memory to run a script                                                                                     |
| unit_steps        | bigint      | The budget in Cpu steps to run a script                                                                                  |
| purpose           | varchar(20) | What kind pf validation this redeemer is used for. It can be one of 'spend', 'mint', 'cert', 'reward', voting, proposing |
| redeemer_index    | smallint    | The index of the redeemer pointer in the transaction                                                                     |
| redeemer_datahash | varchar(64) | The Hash of the Plutus Data                                                                                              |
| block             | bigint      | Block number                                                                                                             |
| block_time        | bigint      | Block time                                                                                                               |
| update_datetime   | timestamp   | Date and time the record was last updated                                                                                |

## 3. datum

| **Column name** | **Type**    | **Description**                                           |
|:----------------|:------------|:----------------------------------------------------------|
| **hash**        | varchar(64) | The Hash of the datum                                     |
| datum           | text        | The actual data in CBOR format                            |
| created_at_tx   | varchar(64) | Transaction hash where this script first became available |
| create_datetime | timestamp   | Date and time the record was created                      |
| update_datetime | timestamp   | Date and time the record was last updated                 |

# X. Staking store

## 1. stake_registration

| **Column name** | **Type**     | **Description**                                                                                                                                                                                                                                                                                                                                                                                              |
|:----------------|:-------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | The hash identifier of the transaction                                                                                                                                                                                                                                                                                                                                                                       |
| **cert_index**  | int          | The index of this stake registration within the certificates of this transaction                                                                                                                                                                                                                                                                                                                             |
| credential      | varchar(56)  | Stake credential (not null)                                                                                                                                                                                                                                                                                                                                                                                  |
| type            | varchar(50)  | Type of stake registration (STAKE_REGISTRATION,STAKE_DEREGISTRATION,STAKE_DELEGATION,POOL_REGISTRATION,POOL_RETIREMENT,GENESIS_KEY_DELEGATION,MOVE_INSTATENEOUS_REWARDS_CERT,REG_CERT,UNREG_CERT,VOTE_DELEG_CERT,STAKE_VOTE_DELEG_CERT,STAKE_REG_DELEG_CERT,VOTE_REG_DELEG_CERT,STAKE_VOTE_REG_DELEG_CERT,AUTH_COMMITTEE_HOT_CERT,RESIGN_COMMITTEE_COLD_CERT,REG_DREP_CERT,UNREG_DREP_CERT,UPDATE_DREP_CERT) |
| address         | varchar(255) | The Bech32 encoded version of the stake address                                                                                                                                                                                                                                                                                                                                                              |
| epoch           | int          | The epoch in which the registration took place                                                                                                                                                                                                                                                                                                                                                               |
| slot            | bigint       | Slot number                                                                                                                                                                                                                                                                                                                                                                                                  |
| block_hash      | varchar(64)  | Hash of the block                                                                                                                                                                                                                                                                                                                                                                                            |
| block           | bigint       | Block number                                                                                                                                                                                                                                                                                                                                                                                                 |
| block_time      | bigint       | Block time                                                                                                                                                                                                                                                                                                                                                                                                   |
| update_datetime | timestamp    | Date and time the record was last updated                                                                                                                                                                                                                                                                                                                                                                    |

## 2. delegation

| **Column Name** | **Data Type** | **Description**                                                                  |
|-----------------|---------------|----------------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)   | The hash identifier of the transaction                                           |
| **cert_index**  | int           | The index of this stake registration within the certificates of this transaction |
| credential      | varchar(56)   | Delegation credential                                                            |
| pool_id         | varchar(56)   | The raw bytes of the pool hash                                                   |
| address         | varchar(255)  | The Bech32 encoded version of the stake address                                  |
| epoch           | int           | The epoch number where this delegation becomes active                            |
| slot            | bigint        | The slot number of the block that contained this delegation                      |
| block_hash      | varchar(64)   | Hash of the block                                                                |
| block           | bigint        | Block number                                                                     |
| block_time      | bigint        | Block time                                                                       |
| update_datetime | timestamp     | Date and time the record was last updated                                        |

## 3. pool_registration

| **Column name** | **Type**       | **Description**                                                                                    |
|:----------------|:---------------|:---------------------------------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)    | The hash identifier of the transaction                                                             |
| **cert_index**  | int            | The index of this stake registration within the certificates of this transaction                   |
| pool_id         | varchar(56)    | The raw bytes of the pool hash                                                                     |
| vrf_key         | varchar(64)    | The hash of the pool's VRF key                                                                     |
| pledge          | numeric(20,0)  | The amount (in Lovelace) the pool owner pledges to the pool                                        |
| cost            | numeric(20, 0) | The fixed per epoch fee (in ADA) this pool charges                                                 |
| margin          | decimal(10, 8) | The margin (as a percentage) this pool charges                                                     |
| reward_account  | varchar(255)   | Address of the reward account for the pool                                                         |
| pool_owners     | jsonb          | The Bech32 encoded version for the pool owner's stake address                                      |
| relays          | jsonb          | JSON document containing information about pool relays (dns_name, dns_srv_name, ipv4, ipv6, port ) |
| metadata_url    | text           | The URL for the location of the off-chain data                                                     |
| metadata_hash   | varchar(64)    | The expected hash for the off-chain data                                                           |
| epoch           | int            | Epoch number                                                                                       |
| slot            | bigint         | Slot number                                                                                        |
| block_hash      | varchar(64)    | Hash of the block                                                                                  |
| block           | bigint         | Block number                                                                                       |
| block_time      | bigint         | Block time                                                                                         |
| update_datetime | timestamp      | Date and time the record was last updated                                                          |

## 4.pool_retirement

| **Column name**  | **Type**    | **Description**                                                                  |
|:-----------------|:------------|:---------------------------------------------------------------------------------|
| **tx_hash**      | varchar(64) | The hash identifier of the transaction                                           |
| **cert_index**   | int         | The index of this stake registration within the certificates of this transaction |
| pool_id          | varchar(56) | The raw bytes of the pool hash                                                   |
| retirement_epoch | int         | The epoch where this pool retires                                                |
| epoch            | int         | Current epoch                                                                    |
| slot             | bigint      | Slot number                                                                      |
| block_hash       | varchar(64) | Hash of the block                                                                |
| block            | bigint      | Block number                                                                     |
| block_time       | bigint      | Block time                                                                       |
| update_datetime  | timestamp   | Date and time the record was last updated                                        |

# XI. Transaction store

## 1. transaction

| **Column Name**         | **Data Type** | **Description**                                                                            |
|-------------------------|---------------|--------------------------------------------------------------------------------------------|
| **tx_hash**             | varchar(64)   | The hash identifier of the transaction                                                     |
| auxiliary_datahash      | varchar(64)   | Hash of the transaction's auxiliary data                                                   |
| block_hash              | varchar(64)   | The hash identifier of the block                                                           |
| collateral_inputs       | jsonb         | JSON document containing details about collateral inputs                                   |
| collateral_return       | jsonb         | JSON document containing details about collateral returned                                 |
| fee                     | bigint        | The fees paid for this transaction                                                         |
| inputs                  | jsonb         | JSON document containing details about transaction inputs                                  |
| invalid                 | boolean       | Flag indicating whether the transaction is valid or invalid                                |
| network_id              | smallint      | The network identifier (0 for testnet, 1 for mainnet)                                      |
| outputs                 | jsonb         | JSON document containing details about transaction outputs                                 |
| reference_inputs        | jsonb         | JSON document containing details about reference inputs (nullable)                         |
| required_signers        | jsonb         | JSON document containing information about required signers for the transaction (nullable) |
| script_datahash         | varchar(64)   | Hash of the transaction script data (nullable)                                             |
| slot                    | bigint        | Slot number within the epoch when the transaction was included (nullable)                  |
| total_collateral        | bigint        | Total amount of collateral involved in the transaction (nullable)                          |
| ttl                     | bigint        | Transaction time-to-live (nullable)                                                        |
| validity_interval_start | bigint        | Start time of the transaction's validity interval (nullable)                               |
| collateral_return_json  | jsonb         | JSON document containing details about collateral return (alternative format, nullable)    |
| block                   | bigint        | Block number within the Cardano blockchain where the transaction is included (nullable)    |
| block_time              | bigint        | Unix timestamp representing the time the block was produced (nullable)                     |
| update_datetime         | timestamp     | Date and time the record was last updated                                                  |

## 2. transaction_witness

| **Column name** | **Type**     | **Description**                                                                                                                   |
|:----------------|:-------------|:----------------------------------------------------------------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | The hash identifier of the transaction                                                                                            |
| **idx**         | integer      | Index of the witness within the transaction                                                                                       |
| pub_key         | varchar(128) | Public key used for signing the transaction                                                                                       |
| signature       | varchar(128) | Signature of the transaction data                                                                                                 |
| pub_keyhash     | varchar(56)  | Hash of the public key                                                                                                            |
| type            | varchar(40)  | Type of witness (BOOTSTRAP_WITNESS,VKEY_WITNESS,BYRON_PK_WITNESS,BYRON_REDEEM_WITNESS,BYRON_SCRIPT_WITNESS,BYRON_UNKNOWN_WITNESS) |
| additional_data | jsonb        | Additional data specific to the witness type                                                                                      |
| slot            | bigint       | Slot number                                                                                                                       |

## 3. withdrawal

| **Column name** | **Type**     | **Description**                                   |
|:----------------|:-------------|:--------------------------------------------------|
| **tx_hash**     | varchar(64)  | The hash identifier of the transaction            |
| **address**     | varchar(255) | The stake address for which the withdrawal is for |
| amount          | numeric(38)  | The withdrawal amount (in Lovelace)               |
| epoch           | integer      | Epoch number                                      |
| slot            | bigint       | Slot number                                       |
| block           | bigint       | Block number                                      |
| block_time      | bigint       | Block time                                        |
| update_datetime | timestamp    | Date and time the record was last updated         |

## 4. invalid_transaction

| **Column name** | **Type**    | **Description**                                           |
|:----------------|:------------|:----------------------------------------------------------|
| **tx_hash**     | varchar(64) | The hash identifier of the transaction                    |
| slot            | bigint      | Slot number                                               |
| block_hash      | varchar(64) | Hash of the block containing the invalid transaction      |
| transaction     | jsonb       | JSON document containing the raw invalid transaction data |
| create_datetime | timestamp   | Date and time the record was created                      |
| update_datetime | timestamp   | Date and time the record was last updated                 |

# XII. Utxo store

## 1. address_utxo

| **Column name**  | **Type**    | **Description**                                                              |
|:-----------------|:------------|:-----------------------------------------------------------------------------|
| **tx_hash**      | varchar(64) | The hash identifier of the transaction that contains this transaction output |
| **output_index** | smallint    | The index of this transaction output with the transaction                    |
| slot             | bigint      | Slot number                                                                  |
| block_hash       | varchar(64) | Hash of the block                                                            |
| epoch            | integer     | Epoch number                                                                 |
| lovelace_amount  | bigint      | The output value (in Lovelace) of the transaction output                     |
| amounts          | jsonb       | Object containing the amount of each multi-asset coin in the UTXO.           |

## 2. tx_input

| **Column name**     | **Type**    | **Description**                                                     |
|:--------------------|:------------|:--------------------------------------------------------------------|
| **tx_hash**         | varchar(64) | The hash identifier of the transaction                              |
| **output_index**    | smallint    | The index within the transaction outputs                            |
| spend_at_slot       | bigint      | Slot number in which the UTXO was spent                             |
| spent_at_block      | bigint      | Block number in which the UTXO was spent                            |
| spent_at_block_hash | varchar(64) | Unique identifier for the block containing the spending transaction |
| spent_block_time    | bigint      | Unix timestamp of the block containing the spending transaction     |
| spent_epoch         | integer     | Epoch number when the UTXO was spent                                |
| spent_tx_hash       | varchar(64) | Unique identifier for the spending transaction                      |

## 3. address

| **Column name**    | **Type**     | **Description**                                       |
|:-------------------|:-------------|:------------------------------------------------------|
| **id**             | bigserial    | Unique identifier for the address (auto-incrementing) |
| address            | varchar(500) | Bech32 address in the Cardano blockchain.             |
| addr_full          | text         | Full address information (might include more details) |
| payment_credential | varchar(56)  | Bech32 payment credential for the address             |
| stake_address      | varchar(255) | Bech32 stake address associated with the address      |
| stake_credential   | varchar(56)  | Bech32 stake credential associated with the address   |
| update_datetime    | timestamp    | Timestamp of the last update to this record.          |

# XIII. Account aggregation

## 1. address_balance

| **Column Name** | **Data Type** | **Description**                                           |
|-----------------|---------------|-----------------------------------------------------------|
| **address**     | varchar(500)  | Bech32 encoded address                                    |
| **unit**        | varchar(255)  | The unit for the quantity (e.g., lovelace for ADA)        |
| **slot**        | bigint        | Slot number                                               |
| quantity        | numeric(38)   | Numeric representation of the asset amount                |
| addr_full       | text          | Full address details in Cardano format                    |
| policy          | varchar(56)   | Policy ID (fingerprint) of the off-chain asset definition |
| asset_name      | varchar(255)  | Optional human-readable name of the asset                 |
| block_hash      | varchar(64)   | Hash of the block                                         |
| block           | bigint        | Block number                                              |
| block_time      | bigint        | Block time                                                |
| epoch           | integer       | Epoch number                                              |
| update_datetime | timestamp     | Date and time the record was last updated                 |

## 2. stake_address_balance

| **Column Name**  | **Data Type** | **Description**                                             |
|------------------|---------------|-------------------------------------------------------------|
| **address**      | varchar(255)  | Bech32 encoded stake address                                |
| **slot**         | bigint        | Slot number                                                 |
| quantity         | numeric(38)   | Numeric representation of the lovelace                      |
| stake_credential | varchar(56)   | Stake credential associated with the address                |
| block_hash       | varchar(64)   | Hash of the block                                           |
| block            | bigint        | Block number                                                |
| block_time       | bigint        | Unix timestamp representing the time the block was produced |
| epoch            | integer       | Epoch number                                                |
| update_datetime  | timestamp     | Date and time the record was last updated                   |

## 3. address_tx_amount

| **Column Name** | **Data Type** | **Description**                                                        |
|-----------------|---------------|------------------------------------------------------------------------|
| **address**     | varchar(500)  | Bech32 encoded address                                                 |
| **unit**        | varchar(255)  | Optional unit for the quantity (e.g., lovelace for ADA)                |
| **tx_hash**     | varchar(64)   | The hash identifier of the transaction                                 |
| slot            | bigint        | Slot number                                                            |
| quantity        | numeric(38)   | Numeric representation of the asset amount involved in the transaction |
| addr_full       | text          | Full address details in Cardano format                                 |
| stake_address   | varchar(255)  | Bech32 encoded stake address associated with the transaction           |
| block           | bigint        | Block number                                                           |
| block_time      | bigint        | Unix timestamp representing the time the block was produced            |
| epoch           | integer       | Epoch number when the transaction occurred                             |

## 4. account_config

| **Column Name** | **Data Type** | **Description**                                                |
|-----------------|---------------|----------------------------------------------------------------|
| **config_id**   | varchar(100)  | Unique identifier for the account configuration                |
| status          | varchar(50)   | Current status of the account configuration (BALANCE_SNAPSHOT) |
| slot            | bigint        | Slot number                                                    |
| block           | bigint        | Block number                                                   |
| block_hash      | varchar(64)   | Hash of the block                                              |

# XIV. Core table

## 1. cursor_

| **Column name** | **Type**    | **Description**                                    |
|:----------------|:------------|:---------------------------------------------------|
| **id**          | integer     | Unique identifier<br>Id of the service use cursor_ |
| **block_hash**  | varchar(64) | Hash of the block                                  |
| slot            | bigint      | Slot number                                        |
| block_number    | bigint      | Block number                                       |
| era             | int         | Era identifier                                     |
| prev_block_hash | varchar(64) | Hash of the previous block                         |
| create_datetime | timestamp   | Date and time the record was created               |
| update_datetime | timestamp   | Date and time the record was last updated          |

## 2. era

Byron(1),

Shelley(2),

Allegra(3),

Mary(4),

Alonzo(5),

Babbage(6),

Conway(7);

| **Column name** | **Type**    | **Description**                              |
|:----------------|:------------|:---------------------------------------------|
| **era**         | int         | Era identifier                               |
| start_slot      | bigint      | Slot number at which the era begins          |
| block           | bigint      | Block number that marks the start of the era |
| block_hash      | varchar(64) | Hash of the block that starts the era        |

