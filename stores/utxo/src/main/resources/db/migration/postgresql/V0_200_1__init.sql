drop table if exists address_utxo;
create table address_utxo
(
    output_index          integer      not null,
    tx_hash               varchar(255) not null,
    slot                  bigint,
    block                 bigint,
    block_hash            varchar(255),
    epoch                 integer,
    lovelace_amount       bigint       null,
    amounts               jsonb,
    data_hash             varchar(255),
    inline_datum          text,
    owner_addr            varchar(500),
    owner_addr_full       text,
    owner_stake_addr      varchar(255),
    owner_payment_credential varchar(255),
    owner_stake_credential  varchar(255),
    script_ref            text,
    reference_script_hash varchar(255) null,
    spent                 boolean,
    spent_at_slot         bigint,
    spent_epoch           integer,
    spent_tx_hash         varchar(255) null,
    is_collateral_return  boolean,
    create_datetime       timestamp,
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

CREATE INDEX idx_address_utxo_spent_epoch
    ON address_utxo(spent_epoch);

drop table if exists invalid_transaction;
create table invalid_transaction
(
    tx_hash         varchar(255) not null
        primary key,
    slot            bigint not null,
    block_hash      varchar(255),
    transaction     jsonb,
    create_datetime timestamp,
    update_datetime timestamp
);

CREATE INDEX idx_invalid_transaction_slot
    ON invalid_transaction(slot);
