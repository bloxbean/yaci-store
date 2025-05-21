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
