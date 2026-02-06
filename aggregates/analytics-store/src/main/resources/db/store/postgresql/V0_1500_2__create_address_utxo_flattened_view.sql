DROP VIEW IF EXISTS address_utxo_flattened;

CREATE VIEW address_utxo_flattened AS
SELECT
    au.tx_hash,
    au.output_index,
    elem->>'unit' as asset_unit,
    NULLIF(elem->>'policy_id', '') as policy_id,
    NULLIF(elem->>'asset_name', '') as asset_name,
    (elem->>'quantity')::NUMERIC(38,0) as quantity,
    au.owner_addr,
    au.owner_stake_addr,
    au.owner_payment_credential,
    au.owner_stake_credential,
    au.inline_datum,
    au.data_hash,
    au.script_ref,
    au.reference_script_hash,
    au.is_collateral_return,
    au.epoch,
    au.slot,
    au.block_hash,
    au.block_time
FROM address_utxo au
         CROSS JOIN LATERAL jsonb_array_elements(au.amounts) as elem;
