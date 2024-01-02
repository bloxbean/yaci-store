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
    description     varchar,
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
    block              bigint,
    block_time         bigint,
    update_datetime    timestamp,
    primary key (tx_hash, index)
);

CREATE INDEX idx_voting_procedure_txhash
    ON voting_procedure (tx_hash);

CREATE INDEX idx_voting_procedure_gov_action_tx_hash
    ON voting_procedure (gov_action_tx_hash);
