# I. Assets store

## 1. assets

| **Column name** | **Type**     | **Description**                     |
|:----------------|:-------------|:------------------------------------|
| **id**          | uuid         | Unique identifier of the asset      |
| slot            | bigint       | Slot number                         |
| tx_hash         | varchar(64)  | Transaction hash                    |
| policy          | varchar(56)  | Policy id                           |
| asset_name      | varchar(255) | Name of the asset                   |
| unit            | varchar(255) | Unit of the asset                   |
| fingerprint     | varchar(255) | Fingerprint of the asset            |
| quantity        | bigint       | Quantity of the asset               |
| mint_type       | varchar(4)   | Mint type (FR or CW)<br>(MINT/BURN) |
| block           | bigint       | Block number                        |
| block_time      | bigint       | Block time                          |
| update_datetime | timestamp    | Datetime of update                  |

# II. Block store

## 1. block

| **Column name**           | **Type**             | **Description**                                     |
|:--------------------------|:---------------------|:----------------------------------------------------|
| **hash**                  | varchar(64) not null | Unique identifier for the block (primary key)       |
| number                    | bigint               | Block number within the chain                       |
| body_hash                 | varchar(64)          | Hash of the block body data                         |
| body_size                 | integer              | Size of the block body data in bytes                |
| epoch                     | integer              | Epoch (era) the block belongs to                    |
| total_output (null)       | numeric(38)          | Total output created in the block (nullable)        |
| total_fees (null)         | bigint               | Total fees collected in the block (nullable)        |
| block_time (null)         | bigint               | Block creation timestamp (nullable)                 |
| era                       | smallint             | Era number                                          |
| issuer_vkey               | varchar(64)          | Public key of the block issuer                      |
| leader_vrf (null)         | jsonb                | Leader verification result (JSON, nullable)         |
| nonce_vrf (null)          | jsonb                | Nonce verification result (JSON, nullable)          |
| prev_hash                 | varchar(64)          | Hash of the previous block                          |
| protocol_version          | varchar(64)          | Blockchain protocol version used                    |
| slot                      | bigint               | Slot number                                         |
| vrf_result (null)         | jsonb                | VRF verification result (JSON, nullable)            |
| vrf_vkey                  | varchar(64)          | Public key used for VRF verification                |
| no_of_txs                 | integer              | Number of transactions in the block                 |
| slot_leader               | varchar(56)          | Public key of the slot leader                       |
| epoch_slot                | integer              | Slot number within the epoch (redundant with slot)  |
| op_cert_hot_vkey (null)   | varchar(64)          | Hot verification public key (nullable)              |
| op_cert_seq_number (null) | bigint               | Sequence number for operator certificate (nullable) |
| op_cert_kes_period (null) | bigint               | KES key period for operator certificate (nullable)  |
| op_cert_sigma (null)      | varchar(256)         | Signature for operator certificate (nullable)       |
| create_datetime           | timestamp            | Date and time the record was created                |
| update_datetime           | timestamp            | Date and time the record was last updated           |

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

| **Column name** | **Type**    | **Description**                              |
|:----------------|:------------|:---------------------------------------------|
| **epoch**       | integer     | Epoch number (not null)                      |
| params          | jsonb       | JSON document containing epoch parameters    |
| cost_model_hash | varchar(64) | Hash of the associated cost model (nullable) |
| slot            | bigint      | Slot number within the epoch                 |
| block           | bigint      | Block number                                 |
| block_time      | bigint      | Block creation timestamp                     |
| update_datetime | timestamp   | Date and time the record was last updated    |

## 4. cost_model

| **Column name** | **Type**    | **Description**                           |
|:----------------|:------------|:------------------------------------------|
| **hash**        | varchar(64) | Unique cost model hash (not null)         |
| costs           | jsonb       | JSON document containing cost details     |
| slot            | bigint      | Slot number within the epoch              |
| block           | bigint      | Block number                              |
| block_time      | bigint      | Block creation timestamp                  |
| update_datetime | timestamp   | Date and time the record was last updated |

# IV Epoch aggregation

## 1. epoch

| Column Name       | Data Type   | Description                                                                   |
|-------------------|-------------|-------------------------------------------------------------------------------|
| **number**        | bigint      | Unique epoch identifier (not null, primary key)                               |
| block_count       | int         | Number of blocks produced in the epoch (nullable)                             |
| transaction_count | bigint      | Number of transactions included in the epoch (nullable)                       |
| total_output      | numeric(38) | Total amount of ADA distributed as outputs in the epoch (nullable)            |
| total_fees        | bigint      | Total amount of ADA collected in transaction fees during the epoch (nullable) |
| start_time        | bigint      | Unix timestamp representing the start time of the epoch (nullable)            |
| end_time          | bigint      | Unix timestamp representing the end time of the epoch (nullable)              |
| max_slot          | bigint      | Maximum slot number reached within the epoch (nullable)                       |
| create_datetime   | timestamp   | Date and time the record was created                                          |
| update_datetime   | timestamp   | Date and time the record was last updated                                     |

# V. Governance store

## 1. gov_action_proposal

| Column Name     | Data Type    | Description                                                                 |
|-----------------|--------------|-----------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | Unique transaction hash (not null, part of primary key)                     |
| **idx**         | int          | Proposal index within the transaction (not null, part of primary key)       |
| deposit         | bigint       | Amount of ADA required to deposit a vote on the proposal (nullable)         |
| return_address  | varchar(255) | Address to which remaining ADA will be returned after voting (nullable)     |
| anchor_url      | varchar      | URL for additional information about the proposal (nullable)                |
| anchor_hash     | varchar(64)  | Hash of the off-chain data pointed to by anchor_url (nullable)              |
| type            | varchar(50)  | Type of governance action proposed (e.g., updateProposal, withdrawProposal) |
| details         | jsonb        | JSON document containing details about the proposal (nullable)              |
| epoch           | int          | Epoch number when the proposal was submitted (nullable)                     |
| slot            | bigint       | Slot number within the epoch when the proposal was submitted (nullable)     |
| block           | bigint       | Block number where the proposal transaction is included (nullable)          |
| block_time      | bigint       | Unix timestamp representing the time the block was produced (nullable)      |
| update_datetime | timestamp    | Date and time the record was last updated                                   |

## 2. voting_procedure

| Column Name            | Data Type   | Description                                                                       |
|------------------------|-------------|-----------------------------------------------------------------------------------|
| **tx_hash**            | varchar(64) | Transaction hash associated with the vote (not null, part of primary key)         |
| **voter_hash**         | varchar(56) | Hash identifying the voter (not null, part of primary key)                        |
| **gov_action_tx_hash** | varchar(64) | Transaction hash of the corresponding governance action proposal (nullable)       |
| **gov_action_index**   | int         | Index of the proposal within the governance action transaction (nullable)         |
| id                     | uuid        | Unique identifier (not null, primary key)                                         |
| idx                    | int         | Index of the voting option within the transaction (not null, part of primary key) |
| voter_type             | varchar(50) | Type of voter (e.g., stake pool, delegation certificate)                          |
| vote                   | varchar(10) | Cast vote (e.g., "yes", "no", "abstain")                                          |
| anchor_url             | varchar     | URL for additional information about the vote (nullable)                          |
| anchor_hash            | varchar(64) | Hash of the off-chain data pointed to by anchor_url (nullable)                    |
| epoch                  | int         | Epoch number when the vote was cast (nullable)                                    |
| slot                   | bigint      | Slot number within the epoch when the vote was cast (nullable)                    |
| block                  | bigint      | Block number where the voting transaction is included (nullable)                  |
| block_time             | bigint      | Unix timestamp representing the time the block was produced (nullable)            |
| update_datetime        | timestamp   | Date and time the record was last updated                                         |

## 3. committee_registration

| Column Name     | Data Type   | Description                                                             |
|-----------------|-------------|-------------------------------------------------------------------------|
| **tx_hash**     | varchar(64) | Unique transaction hash (not null, primary key)                         |
| **cert_index**  | int         | Index of the certificate within the transaction (not null, primary key) |
| cold_key        | varchar     | Public key for the cold wallet (nullable)                               |
| hot_key         | varchar     | Public key for the hot wallet (nullable)                                |
| cred_type       | varchar(40) | Type of credential used for registration (nullable)                     |
| epoch           | int         | Epoch number when the registration occurred (nullable)                  |
| slot            | bigint      | Slot number within the epoch when the registration occurred (nullable)  |
| block           | bigint      | Block number where the registration transaction is included (nullable)  |
| block_time      | bigint      | Unix timestamp representing the time the block was produced (nullable)  |
| update_datetime | timestamp   | Date and time the record was last updated                               |

## 4. committee_deregistration

| Column Name     | Data Type   | Description                                                              |
|-----------------|-------------|--------------------------------------------------------------------------|
| **tx_hash**     | varchar(64) | Unique transaction hash (not null, primary key)                          |
| **cert_index**  | int         | Index of the certificate within the transaction (not null, primary key)  |
| anchor_url      | varchar     | URL for additional information about the deregistration (nullable)       |
| anchor_hash     | varchar(64) | Hash of the off-chain data pointed to by anchor_url (nullable)           |
| cold_key        | varchar(64) | Public key for the cold wallet (not null)                                |
| cred_type       | varchar(40) | Type of credential used for deregistration (nullable)                    |
| epoch           | int         | Epoch number when the deregistration occurred (nullable)                 |
| slot            | bigint      | Slot number within the epoch when the deregistration occurred (nullable) |
| block           | bigint      | Block number where the deregistration transaction is included (nullable) |
| block_time      | bigint      | Unix timestamp representing the time the block was produced (nullable)   |
| update_datetime | timestamp   | Date and time the record was last updated                                |

## 5. delegation_vote

| Column Name     | Data Type    | Description                                                               |
|-----------------|--------------|---------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | Unique transaction hash (not null, primary key)                           |
| **cert_index**  | int          | Index of the certificate within the transaction (not null, primary key)   |
| address         | varchar(255) | Bech32 encoded stake address of the delegator (nullable)                  |
| drep_hash       | varchar(56)  | Hash of the delegation epoch reward pool (nullable)                       |
| drep_id         | varchar(255) | Unique identifier for the delegation epoch reward pool (nullable)         |
| drep_type       | varchar(40)  | Type of the delegation epoch reward pool (nullable)                       |
| epoch           | int          | Epoch number for which the vote is cast (nullable)                        |
| credential      | varchar(56)  | Hash of the credential used for voting (nullable)                         |
| cred_type       | varchar(40)  | Type of credential used for voting (nullable)                             |
| slot            | bigint       | Slot number within the epoch when the vote was cast (nullable)            |
| block           | bigint       | Block number where the delegation vote transaction is included (nullable) |
| block_time      | bigint       | Unix timestamp representing the time the block was produced (nullable)    |
| update_datetime | timestamp    | Date and time the record was last updated                                 |

## 6. drep_registration

| Column Name     | Data Type    | Description                                                             |
|-----------------|--------------|-------------------------------------------------------------------------|
| **tx_hash**     | varchar(64)  | Unique transaction hash (not null, primary key)                         |
| **cert_index**  | int          | Index of the certificate within the transaction (not null, primary key) |
| type            | varchar(50)  | Type of DREP registration (e.g., stake pool registration, withdrawal)   |
| deposit         | bigint       | Amount of ADA deposited for specific registration types (nullable)      |
| drep_hash       | varchar(56)  | Hash of the delegation epoch reward pool (nullable)                     |
| drep_id         | varchar(255) | Unique identifier for the delegation epoch reward pool (nullable)       |
| anchor_url      | varchar      | URL for additional information about the registration (nullable)        |
| anchor_hash     | varchar(64)  | Hash of the off-chain data pointed to by anchor_url (nullable)          |
| cred_type       | varchar(40)  | Type of credential used for registration (nullable)                     |
| epoch           | int          | Epoch number when the registration occurred (nullable)                  |
| slot            | bigint       | Slot number within the epoch when the registration occurred (nullable)  |
| block           | bigint       | Block number where the registration transaction is included (nullable)  |
| block_time      | bigint       | Unix timestamp representing the time the block was produced (nullable)  |
| update_datetime | timestamp    | Date and time the record was last updated                               |

## 7. committee_member

| Column Name     | Data Type   | Description                                                                                             |
|-----------------|-------------|---------------------------------------------------------------------------------------------------------|
| **hash**        | varchar(56) | Unique identifier for the committee member (not null, primary key)                                      |
| **slot**        | bigint      | Slot number within the blockchain where the committee member record was updated (not null, primary key) |
| cred_type       | varchar(40) | Type of credential used for committee membership (nullable)                                             |
| expired_epoch   | int         | Epoch number when the committee membership expires (nullable)                                           |
| update_datetime | timestamp   | Date and time the record was last updated                                                               |

# VI. Live store

| **Column name** | **Type** | **Description** |
|:----------------|:---------|:----------------|
|                 |          |                 |

# VII. Metadata store

## 1. transaction_metadata

| **Column name** | **Type**     | **Description**                                                          |
|:----------------|:-------------|:-------------------------------------------------------------------------|
| **id**          | uuid         | Unique identifier (primary key)                                          |
| slot            | bigint       | Slot number of the block containing the transaction                      |
| tx_hash         | varchar(64)  | Unique hash identifier of the transaction                                |
| label           | varchar(255) | Optional human-readable label associated with the transaction            |
| body            | text         | Transaction data in plain text format (optional)                         |
| cbor            | text         | Transaction data in CBOR ( Concise Binary Object Representation ) format |
| block           | bigint       | Block number containing the transaction                                  |
| block_time      | bigint       | Timestamp (in epoch seconds) of the block containing the transaction     |
| update_datetime | timestamp    | Date and time the record was last updated                                |

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

# XIII. Core table

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


