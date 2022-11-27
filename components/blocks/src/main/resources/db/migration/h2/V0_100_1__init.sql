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
    vrf_vkey         varchar(255) null
);
