drop table if exists gov_action_proposal;
create table gov_action_proposal
(
    tx_hash         varchar(64) not null,
    idx             int         not null,
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

drop table if exists committee_registration;
create table committee_registration
(
    tx_hash         varchar(64) not null,
    cert_index      int         not null,
    cold_key        varchar,
    hot_key         varchar,
    cred_type       varchar(40),
    epoch           int,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (tx_hash, cert_index)
);

CREATE INDEX idx_committee_registration_slot
    ON committee_registration (slot);

drop table if exists committee_deregistration;
create table committee_deregistration
(
    tx_hash         varchar(64) not null,
    cert_index      int         not null,
    anchor_url      varchar,
    anchor_hash     varchar(64),
    cold_key        varchar     not null,
    cred_type       varchar(40),
    epoch           int,
    slot            bigint,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (tx_hash, cert_index)
);

CREATE INDEX idx_committee_deregistration_slot
    ON committee_deregistration (slot);

drop table if exists delegation_vote;
create table delegation_vote
(
    tx_hash         varchar(64) not null,
    cert_index      int         not null,
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
    primary key (tx_hash, cert_index)
);

CREATE INDEX idx_delegation_vote_slot
    ON delegation_vote (slot);

CREATE INDEX idx_delegation_vote_address
    ON delegation_vote (address);

CREATE INDEX idx_delegation_vote_drep_id
    ON delegation_vote (drep_id);

drop table if exists drep_registration;
create table drep_registration
(
    tx_hash         varchar(64) not null,
    cert_index      int         not null,
    type            varchar(50),
    deposit         bigint,
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
    primary key (tx_hash, cert_index)
);

CREATE INDEX idx_drep_registration_slot
    ON drep_registration (slot);

CREATE INDEX idx_drep_registration_type
    ON drep_registration (type);

drop table if exists committee_member;
create table committee_member
(
    hash            varchar(56) not null,
    cred_type       varchar(40),
    start_epoch     int,
    expired_epoch   int,
    slot            bigint,
    update_datetime timestamp,
    primary key (hash, slot)
);

CREATE INDEX idx_committee_member_slot
    ON committee_member (slot);

drop table if exists constitution;
create table constitution
(
    active_epoch    int,
    anchor_url      varchar,
    anchor_hash     varchar(64),
    script          varchar(64),
    slot            bigint,
    update_datetime timestamp,
    primary key (active_epoch)
);

CREATE INDEX idx_constitution_slot
    ON constitution (slot);

drop table if exists committee;
create table committee
(
    gov_action_tx_hash       varchar(64),
    gov_action_index         int,
    threshold_numerator      bigint,
    threshold_denominator    bigint,
    threshold                double precision,
    epoch                    int,
    slot                     bigint,
    update_datetime          timestamp,
    primary key (epoch));
)

CREATE INDEX idx_committee_slot
    ON committee (slot);

DROP TABLE if exists local_gov_action_proposal_status;
CREATE TABLE local_gov_action_proposal_status
(
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    status             varchar(20),
    create_datetime    timestamp,
    update_datetime    timestamp,
    PRIMARY KEY (gov_action_tx_hash, gov_action_index)
);

DROP TABLE IF EXISTS committee;
CREATE TABLE committee
(
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    quorum_numerator   int,
    quorum_denominator int,
    slot               bigint,
    update_datetime    timestamp,
    PRIMARY KEY (gov_action_tx_hash, gov_action_index)
);

DROP TABLE IF EXISTS treasury_withdrawal;
CREATE TABLE treasury_withdrawal
(
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    stake_address      varchar(255),
    amount             bigint,
    slot               bigint,
    update_datetime    timestamp,
    PRIMARY KEY (gov_action_tx_hash, gov_action_index)
)
