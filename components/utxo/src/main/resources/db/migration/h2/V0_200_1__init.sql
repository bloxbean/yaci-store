create table address_utxo
(
    output_index          int          not null,
    tx_hash               varchar(255) not null,
    slot                  bigint       not null,
    block                 bigint,
    block_hash            varchar(255),
    amounts               json         null,
    data_hash             varchar(255) null,
    inline_datum          clob     null,
    owner_addr            varchar(255) null,
    owner_stake_addr      varchar(255) null,
    reference_script_hash clob         null,
    spent                 bit          null,
    spent_at_slot         bigint       null,
    spent_tx_hash         varchar(255) null,
    is_collateral_return  bit          null,
    create_datetime       timestamp,
    update_datetime       timestamp,
    primary key (output_index, tx_hash)
);

CREATE INDEX idx_address_utxo_slot
    ON address_utxo(slot);

create table invalid_transaction
(
    tx_hash     varchar(255) not null
        primary key,
    slot            bigint not null,
    block_hash      varchar(255),
    transaction     json         null,
    create_datetime timestamp,
    update_datetime timestamp
);

CREATE INDEX idx_invalid_transaction_slot
    ON invalid_transaction(slot);
