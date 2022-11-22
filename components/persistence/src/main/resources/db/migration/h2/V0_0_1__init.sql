create table address_utxo
(
    output_index          int          not null,
    tx_hash               varchar(255) not null,
    amounts               json         null,
    block                 bigint       null,
    block_hash            varchar(255) null,
    data_hash             varchar(255) null,
    inline_datum          clob     null,
    owner_addr            varchar(255) null,
    owner_stake_addr      varchar(255) null,
    reference_script_hash clob     null,
    spent                 bit          null,
    spent_at_slot         bit          null,
    primary key (output_index, tx_hash)
);

create table block
(
    block_hash       varchar(255) not null
        primary key,
    block            bigint       null,
    block_body_hash  varchar(255) null,
    block_body_size  bigint       null,
    epoch            bigint       null,
    era              int          null,
    issuer_vkey      varchar(255) null,
    leader_vrf       json         null,
    nonce_vrf        json         null,
    prev_hash        varchar(255) null,
    protocol_version varchar(255) null,
    slot             bigint       null,
    vrf_result       json         null,
    vrf_vkey         varchar(255) null
);

create table error
(
    id         int auto_increment
        primary key,
    block      bigint       null,
    block_hash varchar(255) null,
    error_code varchar(255) null,
    reason     clob     null,
    tx_hash    varchar(255) null
);

create table invalid_transaction
(
    tx_hash     varchar(255) not null
        primary key,
    transaction json         null
);

create table rollback
(
    block_hash varchar(255) not null
        primary key,
    block      bigint       null,
    epoch      bigint       null
);

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
    validity_interval_start bigint         null
);

