drop table if exists transaction_metadata;
create table transaction_metadata
(
    id bigint not null auto_increment
       primary key,
    slot                  bigint,
    tx_hash               varchar(64) not null,
    label                 varchar(255),
    body                  clob,
    cbor                  clob,
    block                 bigint,
    block_time            bigint,
    update_datetime       timestamp
);

CREATE INDEX idx_txn_metadata_tx_hash
    ON transaction_metadata(tx_hash);

CREATE INDEX idx_txn_metadata_label
    ON transaction_metadata(label);
