-- Integration-test fixture: the subset of the operational schema the import path touches.
--
-- Copied verbatim from the Flyway migrations that own these tables, so the built-in specifications
-- are exercised against the same column names, types and nullability the real schema has. The real
-- preprod qualification run is what proves the full schema; this keeps the automated test honest
-- about the tables it does cover.

create table block
(
    hash               varchar(64) not null primary key,
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

create table address_utxo
(
    tx_hash               varchar(64) not null,
    output_index          smallint    not null,
    slot                  bigint,
    block_hash            varchar(64),
    epoch                 integer,
    lovelace_amount       bigint       null,
    amounts               jsonb,
    data_hash             varchar(64),
    inline_datum          text,
    owner_addr            varchar(500),
    owner_addr_full       text,
    owner_stake_addr      varchar(255),
    owner_payment_credential varchar(56),
    owner_stake_credential  varchar(56),
    script_ref            text,
    reference_script_hash varchar(56) null,
    is_collateral_return  boolean,
    block                 bigint,
    block_time            bigint,
    update_datetime       timestamp,
    primary key (output_index, tx_hash)
);

create table adapot
(
    epoch                   int primary key,
    slot                    bigint,
    deposits_stake          numeric(38),
    fees                    numeric(38),
    utxo                    numeric(38),
    treasury                numeric(38),
    reserves                numeric(38),
    circulation             numeric(38),
    distributed_rewards     numeric(38),
    undistributed_rewards   numeric(38),
    rewards_pot             numeric(38),
    pool_rewards_pot        numeric(38),
    update_datetime         timestamp
);

create table adapot_jobs
(
    epoch  integer primary key,
    slot   bigint,
    block  bigint,
    type   varchar(30) not null,
    status varchar(30) not null,
    total_time bigint,
    reward_calc_time bigint,
    update_reward_time bigint,
    stake_snapshot_time bigint,
    drep_distr_snapshot_time bigint,
    error_message text,
    extra_info jsonb,
    block_hash varchar(64)
);

create table cursor_
(
    id          integer not null,
    block_hash  varchar(64),
    slot        bigint,
    block_number bigint,
    era         int,
    prev_block_hash varchar(64),
    create_datetime  timestamp,
    update_datetime  timestamp,
    primary key (id, block_hash)
);

create table era
(
    era        int not null primary key,
    start_slot bigint not null,
    block     bigint not null,
    block_hash varchar(64) not null
);

-- Exercises sequence reset after a bulk load that writes explicit key values.
create table rollback
(
    id                    bigint not null primary key,
    rollback_to_block_hash varchar(64),
    rollback_to_slot      bigint,
    current_block_hash    varchar(64),
    current_slot          bigint,
    current_block         bigint,
    create_datetime       timestamp,
    update_datetime       timestamp
);

create sequence rollback_id_seq owned by rollback.id;

create table account_config
(
    config_id  varchar(50) not null primary key,
    status     varchar(50),
    slot       bigint,
    block      bigint,
    block_hash varchar(64)
);
