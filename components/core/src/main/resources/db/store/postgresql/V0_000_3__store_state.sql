-- How far a derived table or a backfill has been kept, one row per state_id (e.g. 'utxo.unspent_table').
-- The meaning of status, the block and data belongs to the module that owns the state_id.
drop table if exists store_state;
create table store_state
(
    state_id        varchar(100) not null,
    status          varchar(50),
    slot            bigint,
    block           bigint,
    block_hash      varchar(64),
    data            text,
    update_datetime timestamp,
    primary key (state_id)
);
