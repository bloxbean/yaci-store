"""Governance action proposal status comparison against DB Sync Parquet."""

from models.helpers import normalize_hash_expr, run_epoch_diff, run_epoch_model


PARQUET_SQL = f"""
    SELECT {normalize_hash_expr('tx_hash')} AS tx_hash,
           CAST("index" AS BIGINT) AS gov_action_index,
           CAST(type AS VARCHAR) AS type,
           CASE
               WHEN CAST(expired_epoch AS BIGINT) = {{epoch}} THEN 'EXPIRED'
               WHEN CAST(ratified_epoch AS BIGINT) = {{epoch}} THEN 'RATIFIED'
               ELSE 'ACTIVE'
           END AS status,
           CAST(submit_epoch AS BIGINT) AS submit_epoch,
           CAST(ratified_epoch AS BIGINT) AS ratified_epoch,
           CAST(enacted_epoch AS BIGINT) AS enacted_epoch,
           CAST(dropped_epoch AS BIGINT) AS dropped_epoch,
           CAST(expired_epoch AS BIGINT) AS expired_epoch,
           CAST(expiration AS BIGINT) AS expiration
    FROM {{parquet_source}}
    WHERE CAST(submit_epoch AS BIGINT) < {{epoch}}
      AND coalesce(CAST(expired_epoch AS BIGINT), 2147483647) >= {{epoch}}
      AND coalesce(CAST(ratified_epoch AS BIGINT), 2147483647) >= {{epoch}}
      AND coalesce(CAST(enacted_epoch AS BIGINT), 2147483647) > {{epoch}}
      AND coalesce(CAST(dropped_epoch AS BIGINT), 2147483647) > {{epoch}}
"""

STORE_QUERY = """
    SELECT gov_action_tx_hash AS tx_hash,
           gov_action_index,
           type,
           status
    FROM gov_action_proposal_status
    WHERE epoch = %s
"""

DIFF_SQL = f"""
    WITH s_norm AS (
        SELECT {normalize_hash_expr('tx_hash')} AS tx_hash,
               CAST(gov_action_index AS BIGINT) AS gov_action_index,
               CAST(type AS VARCHAR) AS type,
               CAST(status AS VARCHAR) AS status
        FROM s
    )
    SELECT *
    FROM (
        SELECT COALESCE(p.tx_hash, s.tx_hash) AS tx_hash,
               COALESCE(p.gov_action_index, s.gov_action_index) AS gov_action_index,
               CASE
                   WHEN p.tx_hash IS NULL THEN 'ONLY_IN_YACI'
                   WHEN s.tx_hash IS NULL THEN 'ONLY_IN_PARQUET'
                   WHEN p.status IS DISTINCT FROM s.status THEN 'STATUS_MISMATCH'
                   ELSE NULL
               END AS issue,
               p.status AS parquet_status,
               s.status AS yaci_status,
               p.type AS parquet_type,
               s.type AS yaci_type,
               p.submit_epoch,
               p.ratified_epoch,
               p.enacted_epoch,
               p.dropped_epoch,
               p.expired_epoch,
               p.expiration
        FROM p
        FULL OUTER JOIN s_norm s
          ON p.tx_hash = s.tx_hash
         AND p.gov_action_index = s.gov_action_index
    ) diff
    WHERE issue IS NOT NULL
    ORDER BY tx_hash, gov_action_index, issue
"""


def compare_epoch(ctx, epoch):
    return run_epoch_diff(
        ctx,
        "gov_action_proposal_status",
        epoch,
        "gov_action_proposal",
        PARQUET_SQL,
        STORE_QUERY,
        (epoch,),
        DIFF_SQL,
        f"gov_action_proposal_status_epoch_{epoch}",
    )


def run(ctx, epochs):
    return run_epoch_model(
        ctx,
        "gov_action_proposal_status",
        "gov_action_proposal_status",
        epochs,
        compare_epoch,
    )
