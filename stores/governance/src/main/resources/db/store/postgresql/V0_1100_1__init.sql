drop table if exists gov_action_proposal;
create table gov_action_proposal
(
    tx_hash         varchar(64) not null,
    index           int         not null,
    deposit         bigint,
    return_address  varchar,
    anchor_url      varchar,
    anchor_hash     varchar,
    type            varchar(50),
    description     varchar,
    block           bigint,
    block_time      bigint,
    update_datetime timestamp,
    primary key (tx_hash, index)
);

drop table if exists voting_procedure;
create table voting_procedure
(
    tx_hash          varchar(64) not null,
    index            int         not null,
    voter_type       varchar(50),
    voter_hash       varchar,
    transaction_id   varchar(64),
    gov_action_index int,
    vote             varchar(10),
    anchor_url       varchar,
    anchor_hash      varchar,
    block            bigint,
    block_time       bigint,
    update_datetime  timestamp,
    primary key (tx_hash, index)
);

