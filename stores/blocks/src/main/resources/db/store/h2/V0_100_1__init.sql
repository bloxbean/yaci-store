drop table if exists block;
create table block
(
    hash       varchar(64) not null
        primary key,
    number            bigint       null,
    body_hash  varchar(64) null,
    body_size  bigint       null,
    epoch            int       null,
    total_output     numeric(38)  null,
    total_fees       bigint       null,
    block_time       bigint       null,
    era              smallint     null,
    issuer_vkey      varchar(64)  null,
    leader_vrf       json         null,
    nonce_vrf        json         null,
    prev_hash        varchar(64) null,
    protocol_version varchar(64) null,
    slot             bigint       null,
    vrf_result       json         null,
    vrf_vkey         varchar(64) null,
    no_of_txs        int,
    slot_leader      varchar(56),
    epoch_slot       int,
    op_cert_hot_vkey   varchar(64) null,
    op_cert_seq_number bigint null,
    op_cert_kes_period bigint null,
    op_cert_sigma      varchar(256) null,
    create_datetime  timestamp,
    update_datetime  timestamp
);

CREATE INDEX idx_block_number
    ON block(number);

CREATE INDEX idx_block_epoch
    ON block(epoch);

CREATE INDEX idx_block_slot_leader
    ON block(slot_leader);

CREATE INDEX idx_block_slot
    ON block(slot);

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

drop table if exists rollback;
create table rollback
(
    id bigint not null auto_increment
        primary key,
    rollback_to_block_hash varchar(64),
    rollback_to_slot       bigint,
    current_block_hash     varchar(64),
    current_slot           bigint,
    current_block          bigint,
    create_datetime        timestamp,
    update_datetime        timestamp
);
