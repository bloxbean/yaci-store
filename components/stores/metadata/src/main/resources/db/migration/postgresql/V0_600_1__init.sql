create table transaction_metadata
(
    id bigint not null
       primary key generated ALWAYS AS IDENTITY,
    slot                  bigint,
    tx_hash               varchar(255) not null,
    label                 varchar(255),
    body                  text,
    create_datetime       timestamp,
    update_datetime       timestamp
);

CREATE INDEX idx_txn_metadata_tx_hash
    ON transaction_metadata(tx_hash);

CREATE INDEX idx_txn_metadata_label
    ON transaction_metadata(label);
