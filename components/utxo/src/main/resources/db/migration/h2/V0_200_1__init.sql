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
    reference_script_hash clob         null,
    spent                 bit          null,
    spent_at_slot         bigint       null,
    spent_tx_hash         varchar(255) null,
    is_collateral_return  bit          null,
    primary key (output_index, tx_hash)
);

create table invalid_transaction
(
    tx_hash     varchar(255) not null
        primary key,
    transaction json         null
);
