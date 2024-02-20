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

drop table if exists reward;
create table reward
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

drop table if exists reward_account;
create table reward_account
(
    address         varchar(255) not null,
    slot            bigint       not null,
    amount          numeric(38),
    withdrawable    numeric(38),
    epoch           integer,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (address, slot)
);

drop table if exists withdrawal;
create table withdrawal
(
    tx_hash         varchar(64),
    address         varchar(255),
    amount          numeric(38),
    epoch           integer,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (address, tx_hash)
);

drop view if exists reward_account_view;
CREATE VIEW reward_account_view AS
SELECT ra.*
FROM reward_account ra
         INNER JOIN (SELECT address, MAX(slot) AS max_slot
                     FROM reward_account
                     GROUP BY address) max_ra ON ra.address = max_ra.address AND ra.slot = max_ra.max_slot;
