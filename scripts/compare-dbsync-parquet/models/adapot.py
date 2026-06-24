"""AdaPot comparison against DB Sync Parquet."""

from models.helpers import run_epoch_diff, run_epoch_model


PARQUET_SQL = """
    SELECT CAST(epoch_no AS BIGINT) AS epoch,
           CAST(treasury AS HUGEINT) AS treasury,
           CAST(reserves AS HUGEINT) AS reserves
    FROM {parquet_source}
    WHERE CAST(epoch_no AS BIGINT) = {epoch}
"""

STORE_QUERY = """
    SELECT epoch, treasury, reserves
    FROM adapot
    WHERE epoch = %s
"""

DIFF_SQL = """
    SELECT *
    FROM (
        SELECT COALESCE(p.epoch, CAST(s.epoch AS BIGINT)) AS epoch,
               CASE
                   WHEN p.epoch IS NULL THEN 'ONLY_IN_YACI'
                   WHEN s.epoch IS NULL THEN 'ONLY_IN_PARQUET'
                   WHEN p.treasury IS DISTINCT FROM CAST(s.treasury AS HUGEINT)
                     OR p.reserves IS DISTINCT FROM CAST(s.reserves AS HUGEINT)
                        THEN 'VALUE_MISMATCH'
                   ELSE NULL
               END AS issue,
               p.treasury AS parquet_treasury,
               CAST(s.treasury AS HUGEINT) AS yaci_treasury,
               p.reserves AS parquet_reserves,
               CAST(s.reserves AS HUGEINT) AS yaci_reserves
        FROM p
        FULL OUTER JOIN s ON p.epoch = CAST(s.epoch AS BIGINT)
    ) diff
    WHERE issue IS NOT NULL
    ORDER BY epoch, issue
"""


def compare_epoch(ctx, epoch):
    return run_epoch_diff(
        ctx,
        "adapot",
        epoch,
        "adapot",
        PARQUET_SQL,
        STORE_QUERY,
        (epoch,),
        DIFF_SQL,
        f"adapot_epoch_{epoch}",
    )


def run(ctx, epochs):
    return run_epoch_model(ctx, "adapot", "adapot", epochs, compare_epoch)
