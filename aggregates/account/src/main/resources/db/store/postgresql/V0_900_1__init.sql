drop table if exists address_balance;
create table address_balance
(
    address            varchar(500),
    unit               varchar(255),
    slot               bigint,
    quantity           numeric(38)  null,
    addr_full          text,
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

-- stake_balance
drop table if exists stake_address_balance;
create table stake_address_balance
(
    address          varchar(255),
    slot             bigint,
    quantity         numeric(38)  null,
    block            bigint,
    block_time       bigint,
    epoch            integer,
    update_datetime  timestamp,
    primary key (address, slot)
);

CREATE INDEX idx_stake_addr_balance_slot
    ON stake_address_balance (slot);

CREATE INDEX idx_stake_addr_balance_block
    ON stake_address_balance (block);

CREATE INDEX idx_stake_address_balance_epoch
    ON stake_address_balance(epoch);

-- address_tx_amount

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

drop table if exists account_config;
create table account_config
(
    config_id varchar(100),
    status varchar(50),
    slot bigint,
    block  bigint,
    block_hash varchar(64),
    primary key (config_id)
);

drop table if exists address_balance_current;
create table address_balance_current
(
    address            varchar(500),
    unit               varchar(255),
    quantity           numeric(38)  null,
    addr_full          text,
    slot               bigint,
    block              bigint,
    block_time         bigint,
    epoch              integer,
    update_datetime    timestamp,
    primary key (address, unit)
);

-- stake_balance
drop table if exists stake_address_balance_current;
create table stake_address_balance_current
(
    address          varchar(255),
    quantity         numeric(38)  null,
    slot             bigint,
    block            bigint,
    block_time       bigint,
    epoch            integer,
    update_datetime  timestamp,
    primary key (address)
);
