drop table if exists local_gov_action_proposal_status;
create table local_gov_action_proposal_status
(
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    status             varchar(20),
    epoch              int,
    slot               bigint,
    update_datetime    timestamp,
    primary key (gov_action_tx_hash, gov_action_index, epoch)
);

drop table if exists local_committee;
create table local_committee
(
    threshold          double precision,
    epoch              int,
    slot               bigint,
    update_datetime    timestamp,
    primary key (epoch)
);

drop table if exists local_treasury_withdrawal;
create table local_treasury_withdrawal
(
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    address            varchar(255),
    amount             bigint,
    epoch              int,
    slot               bigint,
    update_datetime    timestamp,
    primary key (gov_action_tx_hash, gov_action_index, address)
);

drop table if exists local_constitution;
create table local_constitution
(
    anchor_url      longtext,
    anchor_hash     varchar(64),
    script          varchar(64),
    epoch           int,
    slot            bigint,
    update_datetime timestamp,
    primary key (epoch)
);

drop table if exists local_committee_member;
create table local_committee_member
(
    hash            varchar(56) not null,
    cred_type       varchar(40),
    expired_epoch   int,
    epoch           int,
    slot            bigint,
    update_datetime timestamp,
    primary key (hash, epoch)
);

drop table if exists local_hard_fork_initiation;
create table local_hard_fork_initiation
(
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    major_version      int,
    minor_version      int,
    epoch              int,
    slot               bigint,
    update_datetime    timestamp,
    primary key (gov_action_tx_hash, gov_action_index)
);