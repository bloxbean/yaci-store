SELECT
    COALESCE(d1.drep_hash, d2.drep_hash) AS drep_hash,
    d1.amount AS drep_dist_amount,
    d2.amount AS local_drep_dist_amount
FROM
    (SELECT drep_hash, amount FROM drep_dist WHERE epoch = 208) d1
        FULL OUTER JOIN
    (SELECT drep_hash, amount FROM local_drep_dist WHERE epoch = 208) d2
    ON d1.drep_hash = d2.drep_hash
WHERE
    d1.amount IS DISTINCT FROM d2.amount;
