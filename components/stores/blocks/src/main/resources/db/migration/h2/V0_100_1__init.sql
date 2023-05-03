drop table if exists block;
create table block
(
    hash       varchar(255) not null
        primary key,
    number            bigint       null,
    body_hash  varchar(255) null,
    body_size  bigint       null,
    epoch            bigint       null,
    total_output     numeric(38)  null,
    total_fees       bigint       null,
    block_time       bigint       null,
    era              int          null,
    issuer_vkey      varchar(255) null,
    leader_vrf       json         null,
    nonce_vrf        json         null,
    prev_hash        varchar(255) null,
    protocol_version varchar(255) null,
    slot             bigint       null,
    vrf_result       json         null,
    vrf_vkey         varchar(255) null,
    no_of_txs        int,
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
    id bigint not null auto_increment
        primary key,
    rollback_to_block_hash varchar(255),
    rollback_to_slot       bigint,
    current_block_hash     varchar(255),
    current_slot           bigint,
    current_block          bigint,
    create_datetime        timestamp,
    update_datetime        timestamp
);
