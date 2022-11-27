create table transaction
(
    tx_hash                 varchar(255)   not null
        primary key,
    auxiliary_datahash      varchar(255)   null,
    block                   bigint         null,
    collateral_inputs       json           null,
    collateral_return       json           null,
    fee                     bigint         null,
    inputs                  json           null,
    invalid                 bit            null,
    network_id              int            null,
    outputs                 json           null,
    reference_inputs        json           null,
    required_signers        json           null,
    script_datahash         varchar(255)   null,
    slot                    bigint         null,
    total_collateral        bigint         null,
    ttl                     bigint         null,
    validity_interval_start bigint         null,
    collateral_return_json  json           null
);

