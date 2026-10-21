-- The MIR export omits both the operational surrogate id and the block number.
--
-- The id is derived as a deterministic UUIDv5 over the natural key (tx_hash, cert_index, credential)
-- so a re-import of the same snapshot reproduces exactly the same ids, and the block number is
-- recovered by joining the block export on block_hash. One MIR certificate pays many credentials
-- (mainnet: 208,618 rows from 1,062 certificates), so (tx_hash, cert_index) alone is not unique.
--
-- id is cast to the target uuid type. The epoch cast dates from when the importer let DuckDB replace
-- the file's int32 epoch with the BIGINT value of its epoch=N directory; scans now read the file's
-- own columns, and the cast stays as a guard.
--
-- Parameters: ${files}, ${dep.block}, ${cutSlot}, ${uuidNamespace}
WITH src AS (
    SELECT * FROM ${files} WHERE slot <= ${cutSlot}
),
blocks AS (
    SELECT hash, number FROM ${dep.block}
),
keyed AS (
    SELECT s.*,
           sha1(from_hex(${uuidNamespace})
                || CAST(COALESCE(CAST(s.tx_hash AS VARCHAR), '') || chr(31)
                        || COALESCE(CAST(s.cert_index AS VARCHAR), '') || chr(31)
                        || COALESCE(CAST(s.credential AS VARCHAR), '') AS BLOB)) AS h
    FROM src s
)
SELECT
    CAST(substr(k.h, 1, 8) || '-'
         || substr(k.h, 9, 4) || '-'
         || '5' || substr(k.h, 14, 3) || '-'
         || substr('89ab', ((position(substr(k.h, 17, 1) IN '0123456789abcdef') - 1) % 4) + 1, 1)
         || substr(k.h, 18, 3) || '-'
         || substr(k.h, 21, 12) AS UUID) AS id,
    k.tx_hash                           AS tx_hash,
    k.cert_index                        AS cert_index,
    k.pot                               AS pot,
    k.credential                        AS credential,
    k.address                           AS address,
    k.amount                            AS amount,
    CAST(k.epoch AS INTEGER)            AS epoch,
    k.slot                              AS slot,
    k.block_hash                        AS block_hash,
    b.number                            AS block,
    CAST(epoch(k.block_time) AS BIGINT) AS block_time
FROM keyed k
LEFT JOIN blocks b ON b.hash = k.block_hash
