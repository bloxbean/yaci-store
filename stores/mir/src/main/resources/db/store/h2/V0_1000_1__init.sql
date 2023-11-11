drop table if exists mir;
create table mir
(
    id              uuid not null primary key,
    tx_hash         varchar(64) not null,
    cert_index      int          not null,
    pot             varchar(30),
    credential      varchar(56),
    address         varchar(255), -- bech32 stake address
    amount          numeric(38),
    epoch           int,
    slot            bigint,
    block_hash      varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime timestamp
);

CREATE INDEX idx_mir_slot
    ON mir (slot);

CREATE INDEX idx_mir_pot
    ON mir (pot);

CREATE INDEX idx_mir_credential
    ON mir (credential);

CREATE INDEX idx_mir_txhash
    ON mir (tx_hash);

CREATE INDEX idx_mir_address
    ON mir (address);
