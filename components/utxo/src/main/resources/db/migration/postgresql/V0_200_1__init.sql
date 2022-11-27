create table address_utxo
(
    output_index          integer      not null,
    tx_hash               varchar(255) not null,
    amounts               jsonb,
    block                 bigint,
    block_hash            varchar(255),
    data_hash             varchar(255),
    inline_datum          oid,
    owner_addr            varchar(255),
    owner_stake_addr      varchar(255),
    reference_script_hash oid,
    spent                 boolean,
    spent_at_slot         bigint,
    spent_tx_hash         varchar(255) null,
    is_collateral_return  boolean,
    primary key (output_index, tx_hash)
);

create table invalid_transaction
(
    tx_hash     varchar(255) not null
        primary key,
    transaction jsonb
);
