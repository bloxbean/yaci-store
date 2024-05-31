drop table if exists adapot;
create table adapot
(
    epoch           int,
    slot            bigint,
    epoch_boundary  boolean,
    deposits        numeric(38),
    fees            numeric(38),
    utxo            numeric(38),
    treasury        numeric(38),
    reserves        numeric(38),
    rewards         numeric(38),
    block_hash      varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (epoch, slot, epoch_boundary)
);

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

drop table if exists instant_reward;
create table instant_reward
(
    id              uuid not null primary key,
    address         varchar(255),
    amount          numeric(38),
    type            varchar(50),
    tx_hash         varchar(64),
    slot            bigint,
    earned_epoch    integer,
    spendable_epoch integer,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp
);

drop table if exists reward;
create table reward
(
    address         varchar(255),
    earned_epoch    integer,
    amount          numeric(38),
    type            varchar(50),
    pool_id         varchar(56),
    spendable_epoch integer,
    slot            bigint,
    update_datetime timestamp,
    primary key (address, earned_epoch)
);
