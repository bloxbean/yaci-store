-- address_utxo
drop table if exists address_utxo;
create table address_utxo
(
    tx_hash               varchar(64) not null,
    output_index          smallint    not null,
    slot                  bigint,
    block_hash            varchar(64),
    epoch                 integer,
    lovelace_amount       bigint       null,
    amounts               jsonb,
    data_hash             varchar(64),
    inline_datum          text,
    owner_addr            varchar(500),
    owner_addr_full       text,
    owner_stake_addr      varchar(255),
    owner_payment_credential varchar(56),
    owner_stake_credential  varchar(56),
    script_ref            text,
    reference_script_hash varchar(56) null,
    is_collateral_return  boolean,
    block                 bigint,
    block_time            bigint,
    update_datetime       timestamp,
    primary key (output_index, tx_hash)
);

CREATE INDEX idx_address_utxo_slot
    ON address_utxo(slot);

CREATE INDEX idx_address_utxo_owner_addr
    ON address_utxo(owner_addr);

CREATE INDEX idx_address_utxo_owner_stake_addr
    ON address_utxo(owner_stake_addr);

CREATE INDEX idx_address_utxo_owner_paykey_hash
    ON address_utxo(owner_payment_credential);

CREATE INDEX idx_address_utxo_owner_stakekey_hash
    ON address_utxo(owner_stake_credential);

CREATE INDEX idx_reference_script_hash
    ON address_utxo(reference_script_hash);

CREATE INDEX idx_address_utxo_epoch
    ON address_utxo(epoch);


-- tx_input
drop table if exists tx_input;
create table tx_input
(
    output_index          smallint      not null,
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

-- address

drop table if exists address;
create table address
(
    address            varchar(500) unique not null,
    addr_full          text,
    payment_credential varchar(56),
    stake_address      varchar(255),
    stake_credential   varchar(56),
    update_datetime    timestamp,
    primary key (address)
) PARTITION BY HASH (address);

DO $$
DECLARE
partition_num INTEGER := 0;
    max_partitions INTEGER := 100; -- number of partitions
    sql_command TEXT;
BEGIN
    WHILE partition_num < max_partitions LOOP
        sql_command := format('CREATE TABLE address_p%s PARTITION OF address FOR VALUES WITH (MODULUS %s, REMAINDER %s);', partition_num::TEXT, max_partitions::TEXT, partition_num::TEXT);
EXECUTE sql_command;
partition_num := partition_num + 1;
END LOOP;
END$$;

CREATE INDEX idx_address_stake_address
    ON address (stake_address);
