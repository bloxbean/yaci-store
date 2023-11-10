drop table if exists local_protocol_params;
create table local_protocol_params
(
    id   bigint
        primary key,
    params jsonb,
    create_datetime  timestamp,
    update_datetime  timestamp
);

drop table if exists protocol_param_proposal;
create table protocol_params_proposal
(
    tx_hash            varchar(64) not null,
    key_hash           varchar(56) not null,
    params             jsonb,
    target_epoch       integer,
    epoch              integer,
    slot               bigint,
    era                smallint,
    block              bigint,
    block_time         bigint,
    update_datetime    timestamp,
    primary key (tx_hash, key_hash)
);

CREATE INDEX idx_protocol_params_proposal_slot
    ON protocol_params_proposal(slot);

drop table if exists epoch_param;
create table epoch_param
(
    epoch  integer not null,
    params jsonb,
    cost_model_hash varchar(64) null ,
    slot   bigint,
    block              bigint,
    block_time         bigint,
    update_datetime    timestamp,
    primary key ( epoch )
);

CREATE INDEX idx_epoch_param_slot
    ON epoch_param(slot);

drop table if exists cost_model;
create table cost_model
(
    hash  varchar(64) not null,
    costs jsonb,
    slot   bigint,
    block              bigint,
    block_time         bigint,
    update_datetime    timestamp,
    primary key ( hash )
);
