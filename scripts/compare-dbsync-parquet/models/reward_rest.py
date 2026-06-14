"""Reward rest multiset comparison against DB Sync Parquet."""

from models.helpers import run_epoch_diff, run_epoch_model


def parquet_sql(reward_type):
    return f"""
        SELECT CAST(stake_address AS VARCHAR) AS stake_address,
               CAST(type AS VARCHAR) AS type,
               CAST(earned_epoch AS BIGINT) AS earned_epoch,
               CAST(amount AS HUGEINT) AS amount,
               CAST(spendable_epoch AS BIGINT) AS spendable_epoch
        FROM {{parquet_source}}
        WHERE CAST(earned_epoch AS BIGINT) = {{epoch}}
          AND CAST(type AS VARCHAR) = '{reward_type}'
    """


STORE_QUERY = """
    SELECT address AS stake_address, type, earned_epoch, amount, spendable_epoch
    FROM reward_rest
    WHERE type = %s
      AND earned_epoch = %s
"""

DIFF_SQL = """
    WITH p_agg AS (
        SELECT stake_address, type, earned_epoch, amount, spendable_epoch, count(*) AS row_count
        FROM p
        GROUP BY stake_address, type, earned_epoch, amount, spendable_epoch
    ),
    s_agg AS (
        SELECT CAST(stake_address AS VARCHAR) AS stake_address,
               CAST(type AS VARCHAR) AS type,
               CAST(earned_epoch AS BIGINT) AS earned_epoch,
               CAST(amount AS HUGEINT) AS amount,
               CAST(spendable_epoch AS BIGINT) AS spendable_epoch,
               count(*) AS row_count
        FROM s
        GROUP BY stake_address, type, earned_epoch, amount, spendable_epoch
    )
    SELECT *
    FROM (
        SELECT COALESCE(p.stake_address, s.stake_address) AS stake_address,
               COALESCE(p.type, s.type) AS type,
               COALESCE(p.earned_epoch, s.earned_epoch) AS earned_epoch,
               COALESCE(p.amount, s.amount) AS amount,
               COALESCE(p.spendable_epoch, s.spendable_epoch) AS spendable_epoch,
               CASE
                   WHEN p.stake_address IS NULL THEN 'ONLY_IN_YACI'
                   WHEN s.stake_address IS NULL THEN 'ONLY_IN_PARQUET'
                   WHEN p.row_count IS DISTINCT FROM s.row_count THEN 'COUNT_MISMATCH'
                   ELSE NULL
               END AS issue,
               p.row_count AS parquet_count,
               s.row_count AS yaci_count
        FROM p_agg p
        FULL OUTER JOIN s_agg s
          ON p.stake_address = s.stake_address
         AND p.type = s.type
         AND p.earned_epoch = s.earned_epoch
         AND p.amount = s.amount
         AND p.spendable_epoch IS NOT DISTINCT FROM s.spendable_epoch
    ) diff
    WHERE issue IS NOT NULL
    ORDER BY stake_address, type, earned_epoch, amount, issue
"""


def compare_epoch_for_type(reward_type):
    def compare_epoch(ctx, epoch):
        return run_epoch_diff(
            ctx,
            "reward_rest",
            epoch,
            "reward_rest",
            parquet_sql(reward_type),
            STORE_QUERY,
            (reward_type, epoch),
            DIFF_SQL,
            f"reward_rest_{reward_type}_epoch_{epoch}",
        )

    return compare_epoch


def run(ctx, epochs, reward_type):
    label = f"reward_rest (type={reward_type})"
    return run_epoch_model(ctx, label, "reward_rest", epochs, compare_epoch_for_type(reward_type))
