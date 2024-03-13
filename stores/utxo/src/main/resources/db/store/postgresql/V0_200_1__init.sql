-- address_utxo
drop table if exists address_utxo;
create table address_utxo
(
    output_index          smallint      not null,
    tx_hash               varchar(64) not null,
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

CREATE INDEX idx_address_utxo_slot
    ON address_utxo(slot);

CREATE INDEX idx_address_utxo_owner_addr
    ON address_utxo(owner_addr);

CREATE INDEX idx_address_utxo_owner_stake_addr
    ON address_utxo(owner_stake_addr);

CREATE INDEX idx_address_utxo_owner_paykey_hash
    ON address_utxo(owner_payment_credential);

CREATE INDEX idx_address_utxo_owner_stakekey_hash
    ON address_utxo(owner_stake_credential);

CREATE INDEX idx_reference_script_hash
    ON address_utxo(reference_script_hash);

CREATE INDEX idx_address_utxo_epoch
    ON address_utxo(epoch);


-- tx_input
drop table if exists tx_input;
create table tx_input
(
    output_index          smallint      not null,
    tx_hash                     varchar(64) not null,
    spent_at_slot               bigint,
    spent_at_block              bigint,
    spent_at_block_hash         varchar(64),
    spent_block_time            bigint,
    spent_epoch                 integer,
    spent_tx_hash               varchar(64) null,
    primary key (output_index, tx_hash)
);

CREATE INDEX idx_tx_input_slot
    ON tx_input(spent_at_slot);

CREATE INDEX idx_tx_input_block
    ON tx_input(spent_at_block);

