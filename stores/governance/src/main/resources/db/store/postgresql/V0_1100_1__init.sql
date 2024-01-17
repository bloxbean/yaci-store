drop table if exists gov_action_proposal;
create table gov_action_proposal
(
    tx_hash         varchar(64) not null,
    index           int         not null,
    deposit         bigint,
    return_address  varchar(255),
    anchor_url      varchar(256),
    anchor_hash     varchar(64),
    type            varchar(50),
    details         jsonb,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (tx_hash, index)
);

CREATE INDEX idx_gov_action_proposal_txhash
    ON gov_action_proposal (tx_hash);

CREATE INDEX idx_gov_action_proposal_return_address
    ON gov_action_proposal (return_address);

drop table if exists voting_procedure;
create table voting_procedure
(
    tx_hash            varchar(64) not null,
    index              int         not null,
    voter_type         varchar(50),
    voter_hash         varchar(56),
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    vote               varchar(10),
    anchor_url         varchar,
    anchor_hash        varchar(64),
    slot               bigint,
    block              bigint,
    block_time         bigint,
    update_datetime    timestamp,
    primary key (tx_hash, index)
);

CREATE TABLE committee_registration
(
    tx_hash         varchar(64) not null,
    cert_index      int         NOT NULL,
    cold_key        varchar,
    hot_key         varchar,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    PRIMARY KEY (tx_hash, cert_index)
);

CREATE TABLE committee_de_registration
(
    tx_hash         varchar(64) not null,
    cert_index      int         NOT NULL,
    anchor_url      varchar(256),
    anchor_hash     varchar(64),
    cold_key        varchar     NOT NULL,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    PRIMARY KEY (tx_hash, cert_index)
);

CREATE TABLE delegation_vote
(
    tx_hash         varchar(64) not null,
    cert_index      int         NOT NULL,
    address         varchar(255), -- bech32 stake address
    drep_hash       varchar(56),
    drep_view       varchar(255),
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    PRIMARY KEY (tx_hash, cert_index)
);

CREATE TABLE drep_registration
(
    tx_hash         varchar(64) NOT NULL,
    cert_index      int         NOT NULL,
    type            varchar(50),
    deposit         bigint NULL,
    drep_hash       varchar(56),
    drep_view       varchar(255),
    anchor_url      varchar,
    anchor_hash     varchar(64),
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    PRIMARY KEY (tx_hash, cert_index)
);

CREATE INDEX idx_voting_procedure_txhash
    ON voting_procedure (tx_hash);

CREATE INDEX idx_voting_procedure_gov_action_tx_hash
    ON voting_procedure (gov_action_tx_hash);
