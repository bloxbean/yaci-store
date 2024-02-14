drop table if exists adapot;
create table adapot
(
    epoch           int,
    slot            bigint,
    epoch_boundary  boolean,
    deposits        numeric(38),
    fees            numeric(38),
    utxo            numeric(38),
    treasury        numeric(38),
    reserves        numeric(38),
    rewards         numeric(38),
    block_hash      varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (epoch, slot, epoch_boundary)
);
