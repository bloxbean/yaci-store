drop table if exists assets;
create table assets
(
    id                    binary(16) not null primary key,
    slot                  bigint,
    tx_hash               varchar(64) not null,
    policy                varchar(56),
    asset_name            varchar(255),
    unit                  varchar(255),
    fingerprint           varchar(255),
    quantity              bigint,
    mint_type             varchar(4),
    block                 bigint,
    block_time            bigint,
    update_datetime       timestamp
);

CREATE INDEX idx_assets_slot
    ON assets(slot);
