drop table if exists address_balance;
create table address_balance
(
    address            varchar(500),
    unit               varchar(255),
    slot               bigint,
    quantity           numeric(38)  null,
    addr_full          clob null,
    policy             varchar(56),
    asset_name         varchar(255),
    block_hash         varchar(64),
    block              bigint,
    block_time         bigint,
    epoch              integer,
    update_datetime    timestamp,
    primary key (address, unit, slot)
);

CREATE INDEX idx_address_balance_address
    ON address_balance (address);

CREATE INDEX idx_address_balance_slot
    ON address_balance (slot);

CREATE INDEX idx_address_balance_block
    ON address_balance (block);

CREATE INDEX idx_address_balance_block_time
    ON address_balance (block_time);

CREATE INDEX idx_address_balance_epoch
    ON address_balance (epoch);

CREATE INDEX idx_address_balance_unit
    ON address_balance (unit);

CREATE INDEX idx_address_balance_policy
    ON address_balance (policy);

CREATE INDEX idx_address_balance_policy_asset
    ON address_balance (policy, asset_name);

-- stake_balance
drop table if exists stake_address_balance;
create table stake_address_balance
(
    address          varchar(255),
    slot             bigint,
    quantity         numeric(38)  null,
    stake_credential varchar(56),
    block_hash       varchar(64),
    block            bigint,
    block_time       bigint,
    epoch            integer,
    update_datetime  timestamp,
    primary key (address, slot)
);

CREATE INDEX idx_stake_addr_balance_stake_addr
    ON stake_address_balance (address);

CREATE INDEX idx_stake_addr_balance_slot
    ON stake_address_balance (slot);

CREATE INDEX idx_stake_addr_balance_block
    ON stake_address_balance (block);

CREATE INDEX idx_stake_addr_balance_block_time
    ON stake_address_balance (block_time);

CREATE INDEX idx_stake_addr_balance_epoch
    ON stake_address_balance (epoch);

-- address
drop table if exists address;
create table address
(
    id                 bigint not null auto_increment,
    address            varchar(500) unique not null,
    addr_full          text,
    payment_credential varchar(56),
    stake_address      varchar(255),
    update_datetime    timestamp,
    primary key (id)
);

CREATE INDEX idx_address_stake_address
    ON address (stake_address);

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
