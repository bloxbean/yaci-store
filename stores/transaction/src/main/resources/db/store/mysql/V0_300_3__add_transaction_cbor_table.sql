drop table if exists transaction_cbor;

create table transaction_cbor
(
    tx_hash         varchar(64) not null primary key,
    cbor_data       longblob not null,
    cbor_size       integer,
    slot            bigint not null,
    create_datetime timestamp,
    update_datetime timestamp
);

CREATE INDEX idx_transaction_cbor_slot ON transaction_cbor(slot);


