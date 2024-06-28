drop table if exists gov_action_proposal;
create table gov_action_proposal
(
    tx_hash         varchar(64) not null,
    idx           int         not null,
    deposit         bigint,
    return_address  varchar(255),
    anchor_url      varchar,
    anchor_hash     varchar(64),
    type            varchar(50),
    details         jsonb,
    epoch           int,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (tx_hash, idx)
);

CREATE INDEX idx_gov_action_proposal_slot
    ON gov_action_proposal (slot);

CREATE INDEX idx_gov_action_proposal_txhash
    ON gov_action_proposal (tx_hash);

CREATE INDEX idx_gov_action_proposal_return_address
    ON gov_action_proposal (return_address);

CREATE INDEX idx_gov_action_proposal_type
    ON gov_action_proposal (type);

drop table if exists voting_procedure;
create table voting_procedure
(
    id                 uuid        not null,
    tx_hash            varchar(64) not null,
    idx                int         not null,
    voter_type         varchar(50),
    voter_hash         varchar(56),
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    vote               varchar(10),
    anchor_url         varchar,
    anchor_hash        varchar(64),
    epoch              int,
    slot               bigint,
    block              bigint,
    block_time         bigint,
    update_datetime    timestamp,
    primary key (tx_hash, voter_hash, gov_action_tx_hash, gov_action_index)
);

CREATE INDEX idx_voting_procedure_slot
    ON voting_procedure (slot);

CREATE INDEX idx_voting_procedure_txhash
    ON voting_procedure (tx_hash);

CREATE INDEX idx_voting_procedure_gov_action_tx_hash
    ON voting_procedure (gov_action_tx_hash);

CREATE INDEX idx_voting_procedure_gov_action_tx_hash_gov_action_index
    ON voting_procedure (gov_action_tx_hash, gov_action_index);

CREATE TABLE committee_registration
(
    tx_hash         varchar(64) not null,
    cert_index      int         NOT NULL,
    cold_key        varchar,
    hot_key         varchar,
    cred_type       varchar(40),
    epoch           int,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    PRIMARY KEY (tx_hash, cert_index)
);

CREATE INDEX idx_committee_registration_slot
    ON committee_registration (slot);

CREATE TABLE committee_deregistration
(
    tx_hash         varchar(64) not null,
    cert_index      int         NOT NULL,
    anchor_url      varchar,
    anchor_hash     varchar(64),
    cold_key        varchar     NOT NULL,
    cred_type       varchar(40),
    epoch           int,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    PRIMARY KEY (tx_hash, cert_index)
);

CREATE INDEX idx_committee_deregistration_slot
    ON committee_deregistration (slot);

CREATE TABLE delegation_vote
(
    tx_hash         varchar(64) not null,
    cert_index      int         NOT NULL,
    address         varchar(255), -- bech32 stake address
    drep_hash       varchar(56),
    drep_id         varchar(255),
    drep_type       varchar(40),
    epoch           int,
    credential      varchar(56),
    cred_type       varchar(40),
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    PRIMARY KEY (tx_hash, cert_index)
);

CREATE INDEX idx_delegation_vote_slot
    ON delegation_vote (slot);

CREATE INDEX idx_delegation_vote_address
    ON delegation_vote (address);

CREATE INDEX idx_delegation_vote_drep_id
    ON delegation_vote (drep_id);

CREATE TABLE drep_registration
(
    tx_hash         varchar(64) NOT NULL,
    cert_index      int         NOT NULL,
    type            varchar(50),
    deposit         bigint NULL,
    drep_hash       varchar(56),
    drep_id         varchar(255),
    anchor_url      varchar,
    anchor_hash     varchar(64),
    cred_type       varchar(40),
    epoch           int,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    PRIMARY KEY (tx_hash, cert_index)
);

CREATE INDEX idx_drep_registration_slot
    ON drep_registration (slot);

CREATE INDEX idx_drep_registration_type
    ON drep_registration (type);

CREATE TABLE committee_member
(
    hash            varchar(56) NOT NULL,
    cred_type       varchar(40),
    start_epoch     int,
    expired_epoch   int,
    slot            bigint,
    update_datetime timestamp,
    PRIMARY KEY (hash, slot)
);

CREATE INDEX idx_committee_member_slot
    ON drep_registration (slot);

CREATE TABLE constitution
(
    anchor_url      varchar,
    anchor_hash     varchar(64),
    script          varchar(64),
    slot            bigint,
    update_datetime timestamp,
    PRIMARY KEY (anchor_hash, slot)
);

CREATE INDEX idx_constitution_slot
    ON constitution (slot);