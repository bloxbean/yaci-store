create table if not exists script
(
    script_hash     varchar(255) not null
        primary key,
    plutus_script   json,
    native_script   json,
    create_datetime timestamp,
    update_datetime timestamp
);

create table if not exists transaction_scripts
(
    id              bigint       not null auto_increment
        primary key,
    slot                  bigint,
    block                 bigint,
    block_hash            varchar(255),
    tx_hash               varchar(255) not null,
    script_hash           varchar(255),
    script_type           integer,
    redeemer              longtext,
    datum                 longtext,
    datum_hash            varchar(255),
    create_datetime       timestamp,
    update_datetime       timestamp
);

CREATE INDEX if not exists idx_txn_scripts_tx_hash
    ON transaction_scripts(tx_hash);
