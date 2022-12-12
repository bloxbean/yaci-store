create table script
(
    script_hash   varchar(255) not null
        primary key,
    plutus_script jsonb,
    native_script jsonb,
    create_datetime  timestamp,
    update_datetime  timestamp
);

create table transaction_scripts
(
    id bigint not null
       primary key generated ALWAYS AS IDENTITY,
    slot                  bigint,
    block                 bigint,
    block_hash            varchar(255),
    tx_hash               varchar(255) not null,
    script_hash           varchar(255),
    script_type           integer,
    redeemer              text,
    datum                 text,
    datum_hash            varchar(255),
    create_datetime       timestamp,
    update_datetime       timestamp
);

CREATE INDEX idx_txn_scripts_tx_hash
    ON transaction_scripts(tx_hash);
