drop table if exists mir;
create table mir
(
    id              bigint  not null auto_increment
        primary key,
    tx_hash         varchar(255) not null,
    cert_index      int          not null,
    pot             varchar(50),
    credential      varchar(255),
    address         varchar(255), -- bech32 stake address
    amount          numeric(38),
    epoch           int,
    slot            bigint,
    block_hash      varchar(255),
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
