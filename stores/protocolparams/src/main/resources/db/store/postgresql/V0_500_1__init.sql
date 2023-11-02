drop table if exists protocol_params;
create table protocol_params
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
    tx_hash            varchar(255) not null,
    key_hash           varchar(255) not null,
    params             jsonb,
    target_epoch       integer,
    epoch              integer,
    slot               bigint,
    era                integer,
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
    slot   bigint,
    block              bigint,
    block_time         bigint,
    update_datetime    timestamp,
        primary key ( epoch )
);

CREATE INDEX idx_epoch_param_slot
    ON epoch_param(slot);
