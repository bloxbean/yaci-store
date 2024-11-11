drop table if exists latest_voting_procedure;
create table latest_voting_procedure
(
    id                      uuid        not null,
    tx_hash                 varchar(64)  not null,
    idx                     int         not null,
    voter_type              varchar(50),
    voter_hash              varchar(56),
    gov_action_tx_hash      varchar(64),
    gov_action_index        int,
    vote_in_prev_aggr_slot  varchar(10),
    vote                    varchar(10),
    anchor_url              varchar,
    anchor_hash             varchar(64),
    epoch                   int,
    slot                    bigint,
    block                   bigint,
    block_time              bigint,
    update_datetime         timestamp,
    repeat_vote             boolean,
    primary key (voter_hash, gov_action_tx_hash, gov_action_index)
    );

CREATE INDEX idx_latest_voting_procedure_slot
    ON latest_voting_procedure (slot);

CREATE INDEX idx_latest_voting_procedure_epoch
    ON latest_voting_procedure (epoch);

CREATE INDEX idx_latest_voting_procedure_gov_action_tx_hash_gov_action_index
    ON latest_voting_procedure (gov_action_tx_hash, gov_action_index);

drop table if exists committee_vote;
create table committee_vote
(
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    yes_cnt            int,
    no_cnt             int,
    abstain_cnt        int,
    voter_hash         varchar(56),
    vote               varchar(10),
    status             varchar(64),
    slot               bigint,
    update_datetime    timestamp,
    primary key (gov_action_tx_hash, gov_action_index, voter_hash, slot)
    );

CREATE INDEX idx_committee_vote_slot
    ON committee_vote (slot);

CREATE INDEX idx_committee_vote_gov_action_tx_hash_gov_action_index
    ON committee_vote (gov_action_tx_hash, gov_action_index);

drop table if exists drep_dist;
create table drep_dist
(
    drep_hash         varchar(56),
    drep_id           varchar(255),
    amount            bigint,
    epoch             int,
    slot              bigint,
    update_datetime   timestamp,
    primary key (drep_id)
    );

CREATE INDEX idx_drep_dist_slot
    ON drep_dist (slot);

CREATE INDEX idx_drep_dist_drep_id
    ON drep_dist (drep_id);

CREATE INDEX idx_drep_dist_epoch
    ON drep_dist (epoch);

drop table if exists drep;
create table drep
(
    drep_id           varchar(255),
    drep_hash         varchar(56),
    tx_hash           varchar(64) not null,
    cert_index        int         not null,
    tx_index          int         not null,
    cert_type         varchar(40),
    status            varchar(50),
    amount            numeric(38),
    epoch             int,
    active_epoch      int,
    inactive_epoch    int,
    retire_epoch      int,
    registration_slot bigint,
    slot              bigint,
    block_hash        varchar(64),
    block             bigint,
    block_time        bigint,
    update_datetime   timestamp,
    primary key (drep_id, tx_hash, cert_index, slot)
);

CREATE INDEX idx_drep_slot
    ON drep (slot);

CREATE INDEX idx_drep_drep_id
    ON drep (drep_id);

CREATE INDEX idx_drep_epoch
    ON drep (epoch);

CREATE INDEX idx_drep_retire_epoch
    ON drep (retire_epoch);
