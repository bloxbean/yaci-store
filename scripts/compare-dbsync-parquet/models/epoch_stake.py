"""Epoch stake comparison against DB Sync Parquet."""

from models.helpers import run_epoch_diff, run_epoch_model


def parquet_sql(include_zero_amount):
    amount_filter = "" if include_zero_amount else "AND CAST(amount AS HUGEINT) <> 0"
    return f"""
        SELECT CAST(epoch_no AS BIGINT) AS epoch_no,
               CAST(stake_address AS VARCHAR) AS stake_address,
               lower(CAST(pool_id AS VARCHAR)) AS pool_id,
               CAST(amount AS HUGEINT) AS amount
        FROM {{parquet_source}}
        WHERE CAST(epoch_no AS BIGINT) = {{epoch}}
          {amount_filter}
    """


def store_query(include_zero_amount):
    amount_filter = "" if include_zero_amount else "AND amount <> 0"
    return f"""
        SELECT address AS stake_address, pool_id, amount
        FROM epoch_stake
        WHERE epoch = %s - 2
          {amount_filter}
    """


DIFF_SQL = """
    WITH p_agg AS (
        SELECT stake_address, pool_id, sum(amount) AS amount, count(*) AS row_count
        FROM p
        GROUP BY stake_address, pool_id
    ),
    s_agg AS (
        SELECT CAST(stake_address AS VARCHAR) AS stake_address,
               lower(CAST(pool_id AS VARCHAR)) AS pool_id,
               sum(CAST(amount AS HUGEINT)) AS amount,
               count(*) AS row_count
        FROM s
        GROUP BY stake_address, pool_id
    )
    SELECT *
    FROM (
        SELECT COALESCE(p.stake_address, s.stake_address) AS stake_address,
               COALESCE(p.pool_id, s.pool_id) AS pool_id,
               CASE
                   WHEN p.stake_address IS NULL THEN 'ONLY_IN_YACI'
                   WHEN s.stake_address IS NULL THEN 'ONLY_IN_PARQUET'
                   WHEN p.amount IS DISTINCT FROM s.amount THEN 'AMOUNT_MISMATCH'
                   WHEN p.row_count IS DISTINCT FROM s.row_count THEN 'ROW_COUNT_MISMATCH'
                   ELSE NULL
               END AS issue,
               p.amount AS parquet_amount,
               s.amount AS yaci_amount,
               p.row_count AS parquet_row_count,
               s.row_count AS yaci_row_count
        FROM p_agg p
        FULL OUTER JOIN s_agg s
          ON p.stake_address = s.stake_address
         AND p.pool_id = s.pool_id
    ) diff
    WHERE issue IS NOT NULL
    ORDER BY stake_address, pool_id, issue
"""


def compare_epoch(ctx, epoch):
    return run_epoch_diff(
        ctx,
        "epoch_stake",
        epoch,
        "epoch_stake",
        parquet_sql(ctx.cfg["include_zero_amount"]),
        store_query(ctx.cfg["include_zero_amount"]),
        (epoch,),
        DIFF_SQL,
        f"epoch_stake_epoch_{epoch}",
    )


def run(ctx, epochs):
    return run_epoch_model(ctx, "epoch_stake", "epoch_stake", epochs, compare_epoch)
