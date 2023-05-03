drop table if exists block;
create table block
(
    hash       varchar(255) not null
        primary key,
    number            bigint,
    body_hash  varchar(255),
    body_size  bigint,
    epoch            bigint,
    total_output     numeric(38)  null,
    total_fees       bigint       null,
    block_time       bigint       null,
    era              integer,
    issuer_vkey      varchar(255),
    leader_vrf       jsonb,
    nonce_vrf        jsonb,
    prev_hash        varchar(255),
    protocol_version varchar(255),
    slot             bigint,
    vrf_result       jsonb,
    vrf_vkey         varchar(255),
    no_of_txs        integer,
    create_datetime  timestamp,
    update_datetime  timestamp
);

create table epoch
(
    number bigint       not null primary key,
    block_count         int             null,
    transaction_count   bigint          null,
    total_output        numeric(38)     null,
    total_fees          bigint          null,
    start_time          bigint          null,
    end_time            bigint          null,
    max_slot            bigint          null,
    create_datetime     timestamp,
    update_datetime     timestamp
);

CREATE INDEX idx_block_number
    ON block(number);

drop table if exists rollback;
create table rollback
(
    id bigint not null
        primary key generated ALWAYS AS IDENTITY,
    rollback_to_block_hash varchar(255),
    rollback_to_slot       bigint,
    current_block_hash     varchar(255),
    current_slot           bigint,
    current_block          bigint,
    create_datetime        timestamp,
    update_datetime        timestamp
);
