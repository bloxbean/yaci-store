drop table if exists adapot;
create table adapot
(
    epoch                   int primary key,
    slot                    bigint,
    deposits_stake          numeric(38),
    fees                    numeric(38),
    utxo                    numeric(38),
    treasury                numeric(38),
    reserves                numeric(38),
    circulation             numeric(38),
    distributed_rewards     numeric(38),
    undistributed_rewards   numeric(38),
    rewards_pot             numeric(38),
    pool_rewards_pot        numeric(38),
    update_datetime         timestamp
);

create index idx_adapot_slot
    on adapot (slot);

drop table if exists adapot_jobs;
create table adapot_jobs
(
    epoch  integer primary key ,
    slot   bigint,
    type   varchar(30) not null,
    status varchar(30)   not null,
    total_time bigint,
    reward_calc_time bigint,
    update_reward_time bigint,
    stake_snapshot_time bigint,
    error_message text
);

create index idx_adapot_jobs_slot
    on adapot_jobs (slot);

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

drop table if exists instant_reward;
create table instant_reward
(
    address         varchar(255),
    type            varchar(50),
    amount          numeric(38),
    earned_epoch    integer,
    spendable_epoch integer,
    slot            bigint,
    create_datetime timestamp,
    primary key (address, type, earned_epoch)
);

create index idx_instant_reward_slot
    on instant_reward (slot);

drop table if exists reward;
create table reward
(
    address         varchar(255),
    earned_epoch    integer,
    type            varchar(50),
    pool_id         varchar(56),
    amount          numeric(38),
    spendable_epoch integer,
    slot            bigint,
    update_datetime timestamp,
    primary key (address, earned_epoch, type, pool_id)
);

create index idx_reward_slot
    on reward (slot);

drop table if exists reward_rest;
create table reward_rest
(
    id              char(36) PRIMARY KEY,
    address         varchar(255),
    type            varchar(50),
    amount          numeric(38),
    earned_epoch    integer,
    spendable_epoch integer,
    slot            bigint,
    create_datetime timestamp
);

create index idx_reward_rest_slot
    on reward_rest (slot);

drop table if exists unclaimed_reward_rest;
create table unclaimed_reward_rest
(
    id              char(36) PRIMARY KEY,
    address         varchar(255),
    type            varchar(50),
    amount          numeric(38),
    earned_epoch    integer,
    spendable_epoch integer,
    slot            bigint,
    create_datetime timestamp
);

create index idx_unclaimed_reward_rest_slot
    on unclaimed_reward_rest (slot);
