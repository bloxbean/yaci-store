-- address_utxo_unspent: the outputs of address_utxo that are not spent (no tx_input row).
-- Filled only when store.utxo.unspent-table-enabled=true, by UnspentUtxoTableProcessor at
-- each commit; see UnspentUtxoTableService. Same columns and key as address_utxo.
-- On mainnet it holds about 11M rows against address_utxo's ~355M, so reads that need an
-- address's or an asset's current outputs no longer anti-join their whole history.
create table address_utxo_unspent
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
    tx_index              int,
    primary key (output_index, tx_hash)
);

CREATE INDEX idx_address_utxo_unspent_slot ON address_utxo_unspent(slot);

-- Read indexes (owner address, credentials, amounts) are optional, as for address_utxo:
-- see index.yml, extra-index.yml and blockfrost-index.yml in components/dbutils.
