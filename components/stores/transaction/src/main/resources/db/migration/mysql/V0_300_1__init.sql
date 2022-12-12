create table transaction
(
    tx_hash                 varchar(255) not null
        primary key,
    auxiliary_datahash      varchar(255),
    block_hash            varchar(255),
    block                   bigint,
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
    create_datetime         timestamp,
    update_datetime         timestamp
);

CREATE INDEX idx_transaction_slot
    ON transaction(slot);
