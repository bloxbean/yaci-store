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
    payment_credential varchar(56),
    stake_address      varchar(255),
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

CREATE INDEX idx_address_stake_address
    ON address_balance (stake_address);

CREATE INDEX idx_address_balance_policy_asset
    ON address_balance (policy, asset_name);

-- stake_balance
drop table if exists stake_address_balance;
create table stake_address_balance
(
    address          varchar(255),
    unit             varchar(255),
    slot             bigint,
    quantity         numeric(38)  null,
    policy           varchar(56),
    asset_name       varchar(255),
    stake_credential varchar(56),
    block_hash       varchar(64),
    block            bigint,
    block_time       bigint,
    epoch            integer,
    update_datetime  timestamp,
    primary key (address, unit, slot)
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

CREATE INDEX idx_stake_addr_balance_unit
    ON stake_address_balance (unit);

CREATE INDEX idx_stake_addr_balance_policy
    ON stake_address_balance (policy);

CREATE INDEX idx_stake_addr_balance_policy_asset
    ON stake_address_balance (policy, asset_name);
