drop table if exists script;
create table script
(
    script_hash   varchar(255) not null
        primary key,
    plutus_script json,
    native_script json,
    create_datetime  timestamp,
    update_datetime  timestamp
);

drop table if exists transaction_scripts;
create table transaction_scripts
(
    id              bigint       not null auto_increment
        primary key,
    slot                  bigint,
    block                 bigint,
    block_hash            varchar(255),
    tx_hash               varchar(255) not null,
    script_hash           varchar(255),
    script_type           integer,
    redeemer              clob,
    datum                 clob,
    datum_hash            varchar(255),
    create_datetime       timestamp,
    update_datetime       timestamp
);

CREATE INDEX if not exists idx_txn_scripts_tx_hash
    ON transaction_scripts(tx_hash);

drop table if exists datum;
create table datum (
    hash   varchar(256) not null
        primary key,
    datum clob,
    created_at_tx varchar(256),
    create_datetime timestamp,
    update_datetime timestamp
);

CREATE INDEX if not exists idx_datum_hash
    ON datum(hash);
