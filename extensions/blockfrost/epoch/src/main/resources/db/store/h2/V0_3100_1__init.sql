-- Tables used by blockfrost epoch module for jOOQ code generation
-- These are read-only references to tables from other modules

-- epoch table (from epoch-aggr)
drop table if exists epoch;
create table epoch
(
    number              bigint       not null primary key,
    block_count         int          null,
    transaction_count   bigint       null,
    total_output        numeric(38)  null,
    total_fees          bigint       null,
    start_time          bigint       null,
    end_time            bigint       null,
    max_slot            bigint       null,
    create_datetime     timestamp,
    update_datetime     timestamp
);

-- block table (from blocks store)
drop table if exists block;
create table block
(
    hash                varchar(64)  not null primary key,
    number              bigint       null,
    body_hash           varchar(64)  null,
    body_size           bigint       null,
    epoch               int          null,
    total_output        numeric(38)  null,
    total_fees          bigint       null,
    block_time          bigint       null,
    era                 smallint     null,
    issuer_vkey         varchar(64)  null,
    leader_vrf          json         null,
    nonce_vrf           json         null,
    prev_hash           varchar(64)  null,
    protocol_version    varchar(64)  null,
    slot                bigint       null,
    vrf_result          json         null,
    vrf_vkey            varchar(64)  null,
    no_of_txs           int,
    slot_leader         varchar(56),
    epoch_slot          int,
    op_cert_hot_vkey    varchar(64)  null,
    op_cert_seq_number  bigint       null,
    op_cert_kes_period  bigint       null,
    op_cert_sigma       varchar(256) null,
    create_datetime     timestamp,
    update_datetime     timestamp
);

CREATE INDEX idx_block_number
    ON block(number);

CREATE INDEX idx_block_epoch
    ON block(epoch);

CREATE INDEX idx_block_slot_leader
    ON block(slot_leader);

-- epoch_stake table (from adapot)
drop table if exists epoch_stake;
create table epoch_stake
(
    epoch              integer,
    address            varchar(255),
    amount             numeric(38),
    pool_id            varchar(56),
    delegation_epoch   integer,
    active_epoch       integer,
    create_datetime    timestamp,
    primary key (epoch, address)
);

create index epoch_stake_active_epoch_address_index
    on epoch_stake (active_epoch, address);

