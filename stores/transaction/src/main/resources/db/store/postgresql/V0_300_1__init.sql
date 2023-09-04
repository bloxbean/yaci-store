drop table if exists transaction;
create table transaction
(
    tx_hash                 varchar(255) not null
        primary key,
    auxiliary_datahash      varchar(255),
    block_hash            varchar(255),
    collateral_inputs       jsonb,
    collateral_return       jsonb,
    fee                     bigint,
    inputs                  jsonb,
    invalid                 boolean,
    network_id              integer,
    outputs                 jsonb,
    reference_inputs        jsonb,
    required_signers        jsonb,
    script_datahash         varchar(255),
    slot                    bigint,
    total_collateral        bigint,
    ttl                     bigint,
    validity_interval_start bigint,
    collateral_return_json  jsonb,
    block                   bigint,
    block_time              bigint,
    update_datetime         timestamp
);

CREATE INDEX idx_transaction_slot
    ON transaction(slot);
