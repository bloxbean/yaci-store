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

| **Column name** | **Type**    | **Description**                                            |
|:----------------|:------------|:-----------------------------------------------------------|
| **script_hash** | varchar(56) | Unique identifier for the script (primary key)             |
| script_type     | varchar(30) | Type of the script (e.g., locking script, spending script) |
| content         | jsonb       | JSON document containing the script details                |
| create_datetime | timestamp   | Date and time the record was created                       |
| update_datetime | timestamp   | Date and time the record was last updated                  |

## 2. transaction_scripts

| **Column name**   | **Type**    | **Description**                                                   |
|:------------------|:------------|:------------------------------------------------------------------|
| id                | uuid        | Unique identifier (primary key)                                   |
| slot              | bigint      | Slot number of the block containing the transaction               |
| block_hash        | varchar(64) | Hash of the block containing the transaction                      |
| tx_hash           | varchar(64) | Unique identifier for the transaction (not null)                  |
| script_hash       | varchar(56) | Hash of the transaction script                                    |
| script_type       | smallint    | Type of the transaction script (numerical code)                   |
| datum_hash        | varchar(64) | Hash of the transaction data                                      |
| redeemer_cbor     | text        | CBOR encoded redemption script                                    |
| unit_mem          | bigint      | Memory units consumed by the transaction                          |
| unit_steps        | bigint      | Computation steps used by the transaction                         |
| purpose           | varchar(20) | Purpose of the transaction (e.g., payment, delegation)            |
| redeemer_index    | smallint    | Index of the redeemer within the transaction                      |
| redeemer_datahash | varchar(64) | Hash of the transaction redeemer data                             |
| block             | bigint      | Block number (unique identifier for the block)                    |
| block_time        | bigint      | Timestamp of the block containing the transaction (in epoch time) |
| update_datetime   | timestamp   | Date and time the record was last updated                         |

## 3. datum

| **Column name** | **Type**    | **Description**                              |
|:----------------|:------------|:---------------------------------------------|
| **hash**        | varchar(64) | Unique identifier (primary key)              |
| datum           | text        | The actual data content                      |
| created_at_tx   | varchar(64) | Transaction hash where the datum was created |
| create_datetime | timestamp   | Date and time the record was created         |
| update_datetime | timestamp   | Date and time the record was last updated    |

# X. Staking store

## 1. stake_registration

| **Column name** | **Type**     | **Description**                                             |
|:----------------|:-------------|:------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | Unique identifier for the transaction (not null)            |
| **cert_index**  | int          | Index of the certificate within the transaction (not null)  |
| credential      | varchar(56)  | Stake credential (not null)                                 |
| type            | varchar(50)  | Type of stake registration (e.g., delegation, registration) |
| address         | varchar(255) | Bech32 encoded stake address                                |
| epoch           | int          | Epoch number associated with the registration               |
| slot            | bigint       | Slot number within the epoch                                |
| block_hash      | varchar(64)  | Hash of the block containing the transaction                |
| block           | bigint       | Block number (unique identifier for the block)              |
| block_time      | bigint       | Timestamp of the block in milliseconds since epoch          |
| update_datetime | timestamp    | Date and time the record was last updated                   |

## 2. delegation

| **Column name** | **Type**     | **Description**                                             |
|:----------------|:-------------|:------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | Unique identifier for the transaction (not null)            |
| **cert_index**  | int          | Index of the certificate within the transaction (not null)  |
| credential      | varchar(56)  | Stake credential (not null)                                 |
| type            | varchar(50)  | Type of stake registration (e.g., delegation, registration) |
| address         | varchar(255) | Bech32 encoded stake address                                |
| epoch           | int          | Epoch number associated with the registration               |
| slot            | bigint       | Slot number within the epoch                                |
| block_hash      | varchar(64)  | Hash of the block containing the transaction                |
| block           | bigint       | Block number (unique identifier for the block)              |
| block_time      | bigint       | Timestamp of the block in milliseconds since epoch          |
| update_datetime | timestamp    | Date and time the record was last updated                   |

## 3. pool_registration

| **Column name** | **Type**       | **Description**                                                     |
|:----------------|:---------------|:--------------------------------------------------------------------|
| **tx_hash**     | varchar(64)    | Unique transaction hash (not null)                                  |
| **cert_index**  | int            | Index of the certificate within the transaction (not null)          |
| pool_id         | varchar(56)    | Hash of the registered stake pool                                   |
| vrf_key         | varchar(64)    | Verifiable random function key for the pool                         |
| pledge          | numeric(20,0)  | Amount of ADA pledged to the pool                                   |
| cost            | numeric(20, 0) | Operational cost of the pool per epoch                              |
| margin          | decimal(10, 8) | Stake pool deposit margin                                           |
| reward_account  | varchar(255)   | Address of the reward account for the pool                          |
| pool_owners     | jsonb          | JSON document containing information about pool owners              |
| relays          | jsonb          | JSON document containing information about pool relays              |
| metadata_url    | text           | URL to the pool's metadata                                          |
| metadata_hash   | varchar(64)    | Hash of the pool's metadata                                         |
| epoch           | int            | Cardano epoch number when the registration occurred                 |
| slot            | bigint         | Slot number within the epoch when the registration occurred         |
| block_hash      | varchar(64)    | Hash of the block containing the registration transaction           |
| block           | bigint         | Block number within the Cardano blockchain                          |
| block_time      | bigint         | Unix timestamp of the block containing the registration transaction |
| update_datetime | timestamp      | Date and time the record was last updated                           |

## 4.pool_retirement

| **Column name**  | **Type**    | **Description**                                            |
|:-----------------|:------------|:-----------------------------------------------------------|
| **tx_hash**      | varchar(64) | Unique transaction hash (not null)                         |
| **cert_index**   | int         | Index of the certificate within the transaction (not null) |
| pool_id          | varchar(56) | Hash of the retiring stake pool                            |
| retirement_epoch | int         | Epoch at which the pool retires                            |
| epoch            | int         | Current epoch                                              |
| slot             | bigint      | Slot number within the epoch                               |
| block_hash       | varchar(64) | Hash of the block containing the retirement transaction    |
| block            | bigint      | Block number (unique identifier for the block)             |
| block_time       | bigint      | Timestamp of the block in seconds since epoch              |
| update_datetime  | timestamp   | Date and time the record was last updated                  |

# XI. Transaction store

## 1. transaction

| **Column name**  | **Type**    | **Description**                                            |
|:-----------------|:------------|:-----------------------------------------------------------|
| **tx_hash**      | varchar(64) | Unique transaction hash (not null)                         |
| **cert_index**   | int         | Index of the certificate within the transaction (not null) |
| pool_id          | varchar(56) | Hash of the retiring stake pool                            |
| retirement_epoch | int         | Epoch at which the pool retires                            |
| epoch            | int         | Current epoch                                              |
| slot             | bigint      | Slot number within the epoch                               |
| block_hash       | varchar(64) | Hash of the block containing the retirement transaction    |
| block            | bigint      | Block number (unique identifier for the block)             |
| block_time       | bigint      | Timestamp of the block in seconds since epoch              |
| update_datetime  | timestamp   | Date and time the record was last updated                  |

## 2. transaction_witness

| **Column name** | **Type**     | **Description**                                                            |
|:----------------|:-------------|:---------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | Unique hash identifier of the transaction (not null)                       |
| **idx**         | integer      | Index of the witness within the transaction (not null)                     |
| pub_key         | varchar(128) | Public key used for signing the transaction (optional)                     |
| signature       | varchar(128) | Signature of the transaction data (optional)                               |
| pub_keyhash     | varchar(56)  | Hash of the public key (optional)                                          |
| type            | varchar(40)  | Type of witness (e.g., stake witness, voting witness)                      |
| additional_data | jsonb        | Additional data specific to the witness type (optional)                    |
| slot            | bigint       | Slot number where the transaction is included in the blockchain (optional) |

## 3. withdrawal

| **Column name** | **Type**     | **Description**                                                           |
|:----------------|:-------------|:--------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | Unique transaction hash for the withdrawal                                |
| **address**     | varchar(255) | Wallet address from which funds were withdrawn                            |
| amount          | numeric(38)  | Amount of cryptocurrency withdrawn (supports decimals)                    |
| epoch           | integer      | Epoch number during which the withdrawal occurred                         |
| slot            | bigint       | Slot number within the epoch when the withdrawal occurred                 |
| block           | bigint       | Block number on the blockchain containing the withdrawal transaction      |
| block_time      | bigint       | Timestamp (epoch time) of the block containing the withdrawal transaction |
| update_datetime | timestamp    | Date and time the record was last updated                                 |

## 4. invalid_transaction

| **Column name** | **Type**    | **Description**                                                                   |
|:----------------|:------------|:----------------------------------------------------------------------------------|
| tx_hash         | varchar(64) | Unique transaction hash (not null, primary key)                                   |
| slot            | bigint      | Slot number of the block where the invalid transaction was encountered (not null) |
| block_hash      | varchar(64) | Hash of the block containing the invalid transaction                              |
| transaction     | jsonb       | JSON document containing the raw invalid transaction data                         |
| create_datetime | timestamp   | Date and time the record was created                                              |
| update_datetime | timestamp   | Date and time the record was last updated                                         |

# XII. Utxo store

## 1. address_utxo

| **Column name**  | **Type**    | **Description**                                                                 |
|:-----------------|:------------|:--------------------------------------------------------------------------------|
| **tx_hash**      | varchar(64) | Unique identifier for the transaction containing the UTXO (not null)            |
| **output_index** | smallint    | Index of the output within the transaction. Primary key with tx_hash (not null) |
| slot             | bigint      | Slot number in which the transaction was included in a block.                   |
| block_hash       | varchar(64) | Unique identifier for the block containing the transaction                      |
| epoch            | integer     | Epoch number when the transaction was included in a block                       |
| lovelace_amount  | bigint      | Amount of lovelace in the UTXO (null if lovelace amount is zero)                |
| amounts          | jsonb       | Object containing the amount of each multi-asset coin in the UTXO.              |

## 2. tx_input

| **Column name**     | **Type**    | **Description**                                                     |
|:--------------------|:------------|:--------------------------------------------------------------------|
| **output_index**    | smallint    | Index of the output within the spending transaction (not null)      |
| **tx_hash**         | varchar(64) | Unique identifier for the transaction that spent the UTXO.          |
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

| **Column Name** | **Data Type** | **Description**                                                           |
|-----------------|---------------|---------------------------------------------------------------------------|
| **address**     | varchar(500)  | Bech32 encoded stake address or public key hash                           |
| **unit**        | varchar(255)  | Optional unit for the quantity (e.g., lovelace for ADA)                   |
| **slot**        | bigint        | Slot number within the epoch when the balance was recorded                |
| quantity        | numeric(38)   | Numeric representation of the asset amount                                |
| addr_full       | text          | Full address details in Cardano format                                    |
| policy          | varchar(56)   | Policy ID (fingerprint) of the off-chain asset definition                 |
| asset_name      | varchar(255)  | Optional human-readable name of the asset                                 |
| block_hash      | varchar(64)   | Hash of the block where the transaction affecting the balance is included |
| block           | bigint        | Block number within the Cardano blockchain                                |
| block_time      | bigint        | Unix timestamp representing the time the block was produced               |
| epoch           | integer       | Epoch number when the balance was recorded                                |
| update_datetime | timestamp     | Date and time the record was last updated                                 |

## 2. stake_address_balance

| **Column Name**  | **Data Type** | **Description**                                                                    |
|------------------|---------------|------------------------------------------------------------------------------------|
| **address**      | varchar(255)  | Bech32 encoded stake address (not null, primary key)                               |
| **slot**         | bigint        | Slot number within the epoch when the balance was recorded (not null, primary key) |
| quantity         | numeric(38)   | Numeric representation of the ADA balance                                          |
| stake_credential | varchar(56)   | Stake credential associated with the address                                       |
| block_hash       | varchar(64)   | Hash of the block where the transaction affecting the balance is included          |
| block            | bigint        | Block number within the Cardano blockchain                                         |
| block_time       | bigint        | Unix timestamp representing the time the block was produced                        |
| epoch            | integer       | Epoch number when the balance was recorded                                         |
| update_datetime  | timestamp     | Date and time the record was last updated                                          |

## 3. address_tx_amount

| **Column Name** | **Data Type** | **Description**                                                              |
|-----------------|---------------|------------------------------------------------------------------------------|
| **address**     | varchar(500)  | Bech32 encoded stake address or public key hash                              |
| **unit**        | varchar(255)  | Optional unit for the quantity (e.g., lovelace for ADA)                      |
| **tx_hash**     | varchar(64)   | Unique transaction hash (not null, part of primary key)                      |
| slot            | bigint        | Slot number within the epoch when the transaction occurred                   |
| quantity        | numeric(38)   | Numeric representation of the asset amount involved in the transaction       |
| addr_full       | text          | Full address details in Cardano format                                       |
| stake_address   | varchar(255)  | Bech32 encoded stake address associated with the transaction                 |
| block           | bigint        | Block number within the Cardano blockchain where the transaction is included |
| block_time      | bigint        | Unix timestamp representing the time the block was produced                  |
| epoch           | integer       | Epoch number when the transaction occurred                                   |

## 4. account_config

| **Column Name** | **Data Type** | **Description**                                                                                          |
|-----------------|---------------|----------------------------------------------------------------------------------------------------------|
| **config_id**   | varchar(100)  | Unique identifier for the account configuration (primary key)                                            |
| status          | varchar(50)   | Current status of the account configuration (e.g., active, inactive)                                     |
| slot            | bigint        | Slot number within the epoch when the account configuration was updated                                  |
| block           | bigint        | Block number within the Cardano blockchain where the transaction affecting the configuration is included |
| block_hash      | varchar(64)   | Hash of the block containing the transaction affecting the configuration                                 |

# XIV. Core table

## 1. cursor_

| **Column name** | **Type**    | **Description**                                                               |
|:----------------|:------------|:------------------------------------------------------------------------------|
| **id**          | integer     | Unique identifier (not null)<br>Id của service dùng cursor_ (all, account,..) |
| **block_hash**  | varchar(64) | Hash of the block associated with the cursor position                         |
| slot            | bigint      | Slot number                                                                   |
| block_number    | bigint      | Block number (unique identifier for the block)                                |
| era             | int         | Era identifier                                                                |
| prev_block_hash | varchar(64) | Hash of the previous block                                                    |
| create_datetime | timestamp   | Date and time the record was created                                          |
| update_datetime | timestamp   | Date and time the record was last updated                                     |

## 2. era

Byron(1),

Shelley(2),

Allegra(3),

Mary(4),

Alonzo(5),

Babbage(6),

Conway(7);

| **Column name** | **Type**    | **Description**                                           |
|:----------------|:------------|:----------------------------------------------------------|
| **era**         | int         | Era identifier (unique integer value, primary key)        |
| start_slot      | bigint      | Slot number at which the era begins                       |
| block           | bigint      | Block number that marks the start of the era              |
| block_hash      | varchar(64) | Hash of the block that starts the era (unique identifier) |


