drop table if exists address_balance;
create table address_balance
(
    address            varchar(500),
    unit               varchar(255),
    slot               bigint,
    quantity           numeric(38)  null,
    addr_full          text,
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
) PARTITION BY HASH (address);

DO $$
DECLARE
partition_num INTEGER := 0;
    max_partitions INTEGER := 100; -- number of partitions
    sql_command TEXT;
BEGIN
    WHILE partition_num < max_partitions LOOP
        sql_command := format('CREATE TABLE address_tx_amount_p%s PARTITION OF address_tx_amount FOR VALUES WITH (MODULUS %s, REMAINDER %s);', partition_num::TEXT, max_partitions::TEXT, partition_num::TEXT);
EXECUTE sql_command;
partition_num := partition_num + 1;
END LOOP;
END$$;


CREATE INDEX idx_address_tx_amount_slot
    ON address_tx_amount(slot);


drop table if exists address_balance_snapshot;
create table address_balance_snapshot
(
    address         character varying(500) not null,
    unit            character varying(255) not null,
    slot            bigint                 not null,
    quantity        numeric(38, 0),
    addr_full       text,
    block           bigint,
    block_time      bigint,
    epoch           integer,
    update_datetime timestamp without time zone,
    primary key (address, unit, slot)
) PARTITION BY HASH (address);

DO $$
DECLARE
partition_num INTEGER := 0;
    max_partitions INTEGER := 100; -- number of partitions
    sql_command TEXT;
BEGIN
    WHILE partition_num < max_partitions LOOP
        sql_command := format('CREATE TABLE address_balance_snapshot_p%s PARTITION OF address_balance_snapshot FOR VALUES WITH (MODULUS %s, REMAINDER %s);', partition_num::TEXT, max_partitions::TEXT, partition_num::TEXT);
EXECUTE sql_command;
partition_num := partition_num + 1;
END LOOP;
END$$;

