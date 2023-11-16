drop table if exists script;
create table script
(
    script_hash     varchar(56) not null
        primary key,
    script_type     varchar(30),
    content   json,
    create_datetime timestamp,
    update_datetime timestamp
);

drop table if exists transaction_scripts;
create table transaction_scripts
(
    id              binary(16) not null primary key,
    slot            bigint,
    block_hash      varchar(64),
    tx_hash         varchar(64) not null,
    script_hash     varchar(56),
    script_type     smallint,
    datum_hash      varchar(64),
    redeemer_cbor   longtext,
    unit_mem        bigint,
    unit_steps      bigint,
    purpose         varchar(20),
    redeemer_index  smallint,
    redeemer_datahash varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime timestamp
);

CREATE INDEX idx_txn_scripts_tx_hash
    ON transaction_scripts (tx_hash);

drop table if exists datum;
create table datum
(
    hash varchar(64) not null
        primary key,
    datum longtext,
    created_at_tx varchar(64),
    create_datetime timestamp,
    update_datetime timestamp
);

CREATE INDEX idx_datum_hash
    ON datum(hash);
