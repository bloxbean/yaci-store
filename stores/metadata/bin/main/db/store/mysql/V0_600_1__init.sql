drop table if exists transaction_metadata;
create table transaction_metadata
(
    id                    binary(16) not null primary key,
    slot                  bigint,
    tx_hash               varchar(64) not null,
    label                 varchar(255),
    body                  longtext,
    cbor                  longtext,
    block                 bigint,
    block_time            bigint,
    update_datetime       timestamp
);

CREATE INDEX idx_txn_metadata_slot
    ON transaction_metadata(slot);
