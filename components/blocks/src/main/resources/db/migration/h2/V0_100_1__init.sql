create table block
(
    block_hash       varchar(255) not null
        primary key,
    block            bigint       null,
    block_body_hash  varchar(255) null,
    block_body_size  bigint       null,
    epoch            bigint       null,
    era              int          null,
    issuer_vkey      varchar(255) null,
    leader_vrf       json         null,
    nonce_vrf        json         null,
    prev_hash        varchar(255) null,
    protocol_version varchar(255) null,
    slot             bigint       null,
    vrf_result       json         null,
    vrf_vkey         varchar(255) null,
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
