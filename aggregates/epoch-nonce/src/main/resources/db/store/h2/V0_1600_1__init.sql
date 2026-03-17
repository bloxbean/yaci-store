drop table if exists epoch_nonce;
create table epoch_nonce
(
    epoch                    integer not null primary key,
    nonce                    varchar(64) not null,
    evolving_nonce           varchar(64),
    candidate_nonce          varchar(64),
    lab_nonce                varchar(64),
    last_epoch_block_nonce   varchar(64),
    slot                     bigint,
    block                    bigint,
    block_time               bigint,
    update_datetime          timestamp
);

create index idx_epoch_nonce_slot on epoch_nonce(slot);
