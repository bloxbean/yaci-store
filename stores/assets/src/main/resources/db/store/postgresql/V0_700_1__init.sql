drop table if exists assets;
create table assets
(
    id bigint not null
       primary key generated ALWAYS AS IDENTITY,
    slot                  bigint,
    tx_hash               varchar(255) not null,
    policy                varchar(255),
    asset_name            varchar(255),
    unit                  varchar(255),
    fingerprint           varchar(255),
    quantity              bigint,
    mint_type             varchar(4),
    block                 bigint,
    block_time            bigint,
    update_datetime       timestamp
);
