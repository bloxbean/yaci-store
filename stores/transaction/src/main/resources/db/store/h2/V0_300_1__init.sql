drop table if exists transaction;
create table transaction
(
    tx_hash                 varchar(64) not null
        primary key,
    auxiliary_datahash      varchar(64),
    block_hash              varchar(64),
    collateral_inputs       json,
    collateral_return       json,
    fee                     bigint,
    inputs                  json,
    invalid                 boolean,
    network_id              smallint,
    outputs                 json,
    reference_inputs        json,
    required_signers        json,
    script_datahash         varchar(64),
    slot                    bigint,
    total_collateral        bigint,
    ttl                     bigint,
    validity_interval_start bigint,
    collateral_return_json  json,
    block_index             integer,
    epoch                   integer,
    block                   bigint,
    block_time              bigint,
    update_datetime         timestamp
);

CREATE INDEX idx_transaction_slot
    ON transaction(slot);


drop table if exists transaction_witness;
create table transaction_witness
(
    tx_hash varchar(64) not null,
    idx   integer not null,
    pub_key varchar(128),
    signature varchar(128),
    pub_keyhash varchar(56),
    type varchar(40),
    additional_data json,
    slot bigint,
    primary key (tx_hash, idx)
);

CREATE INDEX idx_transaction_witness_slot
    ON transaction_witness(slot);


drop table if exists withdrawal;
create table withdrawal
(
    tx_hash         varchar(64),
    address         varchar(255),
    amount          numeric(38),
    epoch           integer,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (address, tx_hash)
);

CREATE INDEX idx_withdrawal_slot
    ON withdrawal(slot);

-- invalid_transaction
drop table if exists invalid_transaction;
create table invalid_transaction
(
    tx_hash     varchar(64) not null
        primary key,
    slot            bigint not null,
    block_hash      varchar(64),
    transaction     json         null,
    create_datetime timestamp,
    update_datetime timestamp
);

CREATE INDEX idx_invalid_transaction_slot
    ON invalid_transaction(slot);
