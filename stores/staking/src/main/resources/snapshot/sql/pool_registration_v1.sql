-- pool_registration carries block_hash but not the block number, which the operational table stores.
-- Recover it by joining the block export, and rename vrf_key_hash to the operational vrf_key.
--
-- margin_numerator and margin_denominator are not exported (only the computed margin is), so they
-- are left to their column defaults. That is declared in the specification.
--
-- Parameters: ${files}, ${dep.block}, ${cutSlot}
WITH src AS (
    SELECT * FROM read_parquet(${files}) WHERE slot <= ${cutSlot}
),
blocks AS (
    SELECT hash, number FROM read_parquet(${dep.block})
)
SELECT
    s.tx_hash                           AS tx_hash,
    s.cert_index                        AS cert_index,
    s.tx_index                          AS tx_index,
    s.pool_id                           AS pool_id,
    s.vrf_key_hash                      AS vrf_key,
    s.pledge                            AS pledge,
    s.cost                              AS cost,
    s.margin                            AS margin,
    s.reward_account                    AS reward_account,
    s.pool_owners                       AS pool_owners,
    s.relays                            AS relays,
    s.metadata_url                      AS metadata_url,
    s.metadata_hash                     AS metadata_hash,
    s.epoch                             AS epoch,
    s.slot                              AS slot,
    s.block_hash                        AS block_hash,
    b.number                            AS block,
    CAST(epoch(s.block_time) AS BIGINT) AS block_time
FROM src s
LEFT JOIN blocks b ON b.hash = s.block_hash
