-- Rebuild operational address_utxo rows from the flattened analytics export.
--
-- The analytics view explodes the JSONB `amounts` array into one row per asset, so this transform
-- regroups by (tx_hash, output_index), rebuilds `amounts`, derives `lovelace_amount`, and recovers
-- the block number by joining the block export on block_hash.
--
-- The lovelace entry is emitted first and the remaining assets follow in unit order, matching the
-- operational writer, so a re-import produces byte-identical JSON.
--
-- owner_addr_full is not present in this export and is therefore left to its column default. That
-- limitation is declared in the specification and recorded in the manifest.
--
-- Parameters supplied by the importer:
--   ${files}    read_parquet list for this batch (whole partition directories)
--   ${dep.block} read_parquet list of the block files covering this batch's slot range
--   ${cutSlot}  consistency point slot
WITH flattened AS (
    SELECT *
    FROM read_parquet(${files})
    WHERE slot <= ${cutSlot}
),
grouped AS (
    SELECT
        tx_hash,
        output_index,
        any_value(owner_addr)               AS owner_addr,
        any_value(owner_stake_addr)         AS owner_stake_addr,
        any_value(owner_payment_credential) AS owner_payment_credential,
        any_value(owner_stake_credential)   AS owner_stake_credential,
        any_value(inline_datum)             AS inline_datum,
        any_value(data_hash)                AS data_hash,
        any_value(script_ref)               AS script_ref,
        any_value(reference_script_hash)    AS reference_script_hash,
        any_value(is_collateral_return)     AS is_collateral_return,
        any_value(epoch)                    AS epoch,
        any_value(slot)                     AS slot,
        any_value(block_hash)               AS block_hash,
        any_value(block_time)               AS block_time,
        sum(CASE WHEN asset_unit = 'lovelace' THEN quantity ELSE 0 END) AS lovelace_amount,
        -- list(... ORDER BY ...) is a real aggregate, so the element order is deterministic;
        -- json_group_array is a macro and cannot take an ORDER BY.
        CAST(list(struct_pack(unit := asset_unit,
                              quantity := quantity,
                              policy_id := policy_id,
                              asset_name := asset_name)
                  ORDER BY CASE WHEN asset_unit = 'lovelace' THEN 0 ELSE 1 END, asset_unit)
             AS JSON) AS amounts
    FROM flattened
    GROUP BY tx_hash, output_index
),
blocks AS (
    SELECT hash, number FROM read_parquet(${dep.block})
)
SELECT
    g.tx_hash                                AS tx_hash,
    CAST(g.output_index AS SMALLINT)         AS output_index,
    g.slot                                   AS slot,
    g.block_hash                             AS block_hash,
    g.epoch                                  AS epoch,
    CAST(g.lovelace_amount AS BIGINT)        AS lovelace_amount,
    g.amounts                                AS amounts,
    g.data_hash                              AS data_hash,
    g.inline_datum                           AS inline_datum,
    g.owner_addr                             AS owner_addr,
    g.owner_stake_addr                       AS owner_stake_addr,
    g.owner_payment_credential               AS owner_payment_credential,
    g.owner_stake_credential                 AS owner_stake_credential,
    g.script_ref                             AS script_ref,
    g.reference_script_hash                  AS reference_script_hash,
    g.is_collateral_return                   AS is_collateral_return,
    b.number                                 AS block,
    CAST(epoch(g.block_time) AS BIGINT)      AS block_time
FROM grouped g
LEFT JOIN blocks b ON b.hash = g.block_hash
