-- pool_retirement carries block_hash but not the block number; recover it from the block export.
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
    s.retirement_epoch                  AS retirement_epoch,
    s.epoch                             AS epoch,
    s.slot                              AS slot,
    s.block_hash                        AS block_hash,
    b.number                            AS block,
    CAST(epoch(s.block_time) AS BIGINT) AS block_time
FROM src s
LEFT JOIN blocks b ON b.hash = s.block_hash
