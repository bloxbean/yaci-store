drop table if exists transaction;
create table transaction
(
    tx_hash                 varchar(255) not null
        primary key,
    auxiliary_datahash      varchar(255),
    block_hash            varchar(255),
    collateral_inputs       json,
    collateral_return       json,
    fee                     bigint,
    inputs                  json,
    invalid                 boolean,
    network_id              integer,
    outputs                 json,
    reference_inputs        json,
    required_signers        json,
    script_datahash         varchar(255),
    slot                    bigint,
    total_collateral        bigint,
    ttl                     bigint,
    validity_interval_start bigint,
    collateral_return_json  json,
    block                   bigint,
    block_time              bigint,
    update_datetime         timestamp
);

CREATE INDEX idx_transaction_slot
    ON transaction(slot);

drop table if exists transaction_witness;
create table transaction_witness
(
    tx_hash varchar(128) not null,
    idx   integer not null,
    pub_key varchar(128),
    signature varchar(255),
    pub_keyhash varchar(128),
    type varchar(40),
    additional_data json,
    slot bigint,
    primary key (tx_hash, idx)
);

CREATE INDEX idx_transaction_witness_slot
    ON transaction_witness(slot);
