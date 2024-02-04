drop table if exists adapot;
create table adapot
(
    epoch           int,
    slot            bigint,
    deposit         numeric(38),
    fees            numeric(38),
    block_hash      varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (epoch, slot)
);

drop table if exists deposit;
create table deposit
(
    tx_hash         varchar(64) not null,
    cert_index      int         not null,
    credential      varchar(56),
    cred_type       varchar(50),
    deposit_type    varchar(50),
    amount          numeric(38),
    epoch           int,
    slot            bigint,
    block_hash      varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (tx_hash, cert_index)
);
