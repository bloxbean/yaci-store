drop table if exists block_cbor;

create table block_cbor
(
    block_hash      varchar(64) not null primary key,
    cbor_data       blob not null,
    cbor_size       integer,
    slot            bigint not null,
    create_datetime timestamp default current_timestamp
);

CREATE INDEX idx_block_cbor_slot ON block_cbor(slot);


