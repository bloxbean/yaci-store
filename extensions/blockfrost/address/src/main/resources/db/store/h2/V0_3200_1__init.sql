-- Tables used by blockfrost address module for jOOQ code generation
-- These are read-only references to tables from other modules

-- address_tx_amount table (from account)
drop table if exists address_tx_amount;
create table address_tx_amount
(
    address            varchar(500),
    unit               varchar(255),
    tx_hash            varchar(64),
    slot               bigint,
    quantity           numeric(38) null,
    addr_full          text,
    stake_address      varchar(255),
    block              bigint,
    block_time         bigint,
    epoch              integer,
    primary key (address, unit, tx_hash)
);

CREATE INDEX idx_address_tx_amount_slot
    ON address_tx_amount(slot);

-- transaction table (from transaction store)
drop table if exists transaction;
create table transaction
(
    tx_hash                 varchar(64) not null primary key,
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
    tx_index                integer,
    treasury_donation       bigint,
    epoch                   integer,
    block                   bigint,
    block_time              bigint,
    update_datetime         timestamp
);

CREATE INDEX idx_transaction_slot
    ON transaction(slot);

CREATE INDEX idx_transaction_block
    ON transaction(block);

-- address_utxo table (from utxo store)
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

CREATE INDEX idx_address_utxo_owner_addr
    ON address_utxo(owner_addr);

-- tx_input table (from utxo store)
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

-- address_balance table (from account)
drop table if exists address_balance;
create table address_balance
(
    address            varchar(500),
    unit               varchar(255),
    slot               bigint,
    quantity           numeric(38)  null,
    addr_full          clob null,
    block              bigint,
    block_time         bigint,
    epoch              integer,
    update_datetime    timestamp,
    primary key (address, unit, slot)
);

CREATE INDEX idx_address_balance_slot
    ON address_balance (slot);

CREATE INDEX idx_address_balance_block
    ON address_balance (block);

-- address_balance_current table (from account)
drop table if exists address_balance_current;
create table address_balance_current
(
    address            varchar(500),
    unit               varchar(255),
    quantity           numeric(38)  null,
    addr_full          clob null,
    slot               bigint,
    block              bigint,
    block_time         bigint,
    epoch              integer,
    update_datetime    timestamp,
    primary key (address, unit)
);

CREATE INDEX idx_address_balance_current_address
    ON address_balance_current (address);
