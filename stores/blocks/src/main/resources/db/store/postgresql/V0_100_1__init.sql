drop table if exists block;
create table block
(
    hash               varchar(64) not null
        primary key,
    number             bigint,
    body_hash          varchar(64),
    body_size          integer,
    epoch              integer,
    total_output       numeric(38)  null,
    total_fees         bigint       null,
    block_time         bigint       null,
    era                smallint,
    issuer_vkey        varchar(64),
    leader_vrf         jsonb,
    nonce_vrf          jsonb,
    prev_hash          varchar(64),
    protocol_version   varchar(64),
    slot               bigint,
    vrf_result         jsonb,
    vrf_vkey           varchar(64),
    no_of_txs          integer,
    slot_leader        varchar(56),
    epoch_slot         integer,
    op_cert_hot_vkey   varchar(64) null,
    op_cert_seq_number bigint null,
    op_cert_kes_period bigint null,
    op_cert_sigma      varchar(256) null,
    create_datetime    timestamp,
    update_datetime    timestamp
);

CREATE INDEX idx_block_number
    ON block(number);

CREATE INDEX idx_block_epoch
    ON block(epoch);

CREATE INDEX idx_block_slot_leader
    ON block(slot_leader);

CREATE INDEX idx_block_slot
    ON block(slot);


drop table if exists rollback;
create table rollback
(
    id bigint not null
        primary key generated ALWAYS AS IDENTITY,
    rollback_to_block_hash varchar(64),
    rollback_to_slot       bigint,
    current_block_hash     varchar(64),
    current_slot           bigint,
    current_block          bigint,
    create_datetime        timestamp,
    update_datetime        timestamp
);
