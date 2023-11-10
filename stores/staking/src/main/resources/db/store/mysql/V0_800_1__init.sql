drop table if exists stake_registration;
create table stake_registration
(
    tx_hash         varchar(64) not null,
    cert_index      int          not null,
    credential      varchar(56) not null,
    type            varchar(50),
    address         varchar(255), -- bech32 stake address
    epoch           int,
    slot            bigint,
    block_hash      varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (tx_hash, cert_index)
);

CREATE INDEX idx_stake_registration_slot
    ON stake_registration (slot);

CREATE INDEX idx_stake_registration_stake_credential
    ON stake_registration (credential);

CREATE INDEX idx_stake_registration_stake_txhash
    ON stake_registration (tx_hash);

CREATE INDEX idx_stake_registration_type
    ON stake_registration (type);

CREATE INDEX idx_stake_registration_stake_address
    ON stake_registration (address);

drop table if exists delegation;
create table delegation
(
    tx_hash         varchar(64) not null,
    cert_index      int          not null,
    credential      varchar(56) not null,
    pool_id         varchar(56), -- pool hash
    address         varchar(255), -- bech32 stake address
    epoch           int,
    slot            bigint,
    block_hash      varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (tx_hash, cert_index)
);

CREATE INDEX idx_delegation_slot
    ON delegation (slot);

CREATE INDEX idx_delegation_credential
    ON delegation (credential);

CREATE INDEX idx_delegation_txhash
    ON delegation (tx_hash);

CREATE INDEX idx_delegation_address
    ON delegation (address);

drop table if exists pool_registration;
create table pool_registration
(
    tx_hash         varchar(64) not null,
    cert_index      int          not null,
    pool_id         varchar(56), -- pool hash
    vrf_key         varchar(64),
    pledge          numeric(20, 0),
    cost            numeric(20, 0),
    margin          decimal(10, 8),
    reward_account  varchar(255),
    pool_owners     json,
    relays          json,
    metadata_url    longtext,
    metadata_hash   varchar(64),
    epoch           int,
    slot            bigint,
    block_hash      varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (tx_hash, cert_index)
);

CREATE INDEX idx_pool_registration_slot
    ON pool_registration (slot);

CREATE INDEX idx_pool_registration_txhash
    ON pool_registration (tx_hash);

CREATE INDEX idx_pool_registration_pool_id
    ON pool_registration (pool_id);

CREATE INDEX idx_pool_registration_reward_account
    ON pool_registration (reward_account);

drop table if exists pool_retirement;
create table pool_retirement
(
    tx_hash          varchar(64) not null,
    cert_index       int          not null,
    pool_id          varchar(56), -- pool hash
    retirement_epoch int,
    epoch            int,
    slot             bigint,
    block_hash      varchar(64),
    block           bigint,
    block_time      bigint,
    update_datetime  timestamp,
    primary key (tx_hash, cert_index)
);

CREATE INDEX idx_pool_retirement_slot
    ON pool_retirement (slot);

CREATE INDEX idx_pool_retirement_txhash
    ON pool_retirement (tx_hash);

CREATE INDEX idx_pool_retirement_pool_id
    ON pool_retirement (pool_id);

CREATE INDEX idx_pool_retirement_retirement_epoch
    ON pool_retirement (retirement_epoch);
