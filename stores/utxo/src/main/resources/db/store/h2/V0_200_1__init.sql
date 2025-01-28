-- address_utxo
drop table if exists address_utxo;
create table address_utxo
(
    tx_hash               varchar(64) not null,
    output_index          int          not null,
    slot                  bigint,
    block_hash            varchar(64),
    epoch                 int,
    lovelace_amount       bigint       null,
    amounts               json         null,
    data_hash             varchar(64) null,
    inline_datum          clob     null,
    owner_addr            varchar(500) null,
    owner_addr_full       clob         null,
    owner_stake_addr      varchar(255) null,
    owner_payment_credential varchar(56),
    owner_stake_credential  varchar(56),
    script_ref            clob         null,
    reference_script_hash varchar(56) null,
    is_collateral_return  bit          null,
    block                 bigint,
    block_time            bigint,
    update_datetime       timestamp,
    primary key (output_index, tx_hash)
);

CREATE INDEX idx_address_utxo_slot
    ON address_utxo(slot);

CREATE INDEX idx_reference_script_hash
    ON address_utxo(reference_script_hash);

-- tx_input
drop table if exists tx_input;
create table tx_input
(
    output_index                int      not null,
    tx_hash                     varchar(64) not null,
    spent_at_slot               bigint,
    spent_at_block              bigint,
    spent_at_block_hash         varchar(64),
    spent_block_time            bigint,
    spent_epoch                 integer,
    spent_tx_hash               varchar(64) null,
    primary key (output_index, tx_hash)
);

CREATE INDEX idx_tx_input_slot
    ON tx_input(spent_at_slot);

CREATE INDEX idx_tx_input_block
    ON tx_input(spent_at_block);

-- address
drop table if exists address;
create table address
(
    id                 bigint not null auto_increment,
    address            varchar(500) unique not null,
    addr_full          text,
    payment_credential varchar(56),
    stake_address      varchar(255),
    stake_credential   varchar(56),
    update_datetime    timestamp,
    primary key (id)
);

CREATE INDEX idx_address_stake_address
    ON address (stake_address);
