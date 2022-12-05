create table block
(
    block_hash       varchar(255) not null
        primary key,
    block            bigint,
    block_body_hash  varchar(255),
    block_body_size  bigint,
    epoch            bigint,
    era              integer,
    issuer_vkey      varchar(255),
    leader_vrf       jsonb,
    nonce_vrf        jsonb,
    prev_hash        varchar(255),
    protocol_version varchar(255),
    slot             bigint,
    vrf_result       jsonb,
    vrf_vkey         varchar(255),
    create_datetime  timestamp,
    update_datetime  timestamp
);

create table rollback
(
    id bigint not null
        primary key generated ALWAYS AS IDENTITY,
    rollback_to_block_hash varchar(255),
--     rollback_to_block      bigint,
    rollback_to_slot       bigint,
    current_block_hash     varchar(255),
    current_slot           bigint,
    current_block          bigint,
    create_datetime        timestamp,
    update_datetime        timestamp
)
