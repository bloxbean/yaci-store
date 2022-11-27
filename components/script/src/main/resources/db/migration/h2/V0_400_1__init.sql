create table script
(
    script_hash   varchar(255) not null
        primary key,
    plutus_script json,
    native_script json
);

create table transaction_scripts
(
    tx_hash               varchar(255) not null,
    script_hash           varchar(255),
    script_type           integer,
    block                 bigint,
    block_hash            varchar(255),
    primary key (tx_hash, script_hash)
)
