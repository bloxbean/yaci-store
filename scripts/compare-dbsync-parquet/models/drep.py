"""DRep amount and active_until comparisons against DB Sync Parquet."""

from models.helpers import normalize_hash_expr, run_epoch_diff, run_epoch_model


PARQUET_SPECIAL_TYPE = """
    CASE
        WHEN lower(coalesce(CAST(drep_id AS VARCHAR), '')) LIKE '%abstain%' THEN 'ABSTAIN'
        WHEN lower(coalesce(CAST(drep_id AS VARCHAR), '')) LIKE '%no_confidence%'
          OR lower(coalesce(CAST(drep_id AS VARCHAR), '')) LIKE '%no-confidence%' THEN 'NO_CONFIDENCE'
        ELSE NULL
    END
"""

STORE_SPECIAL_TYPE = """
    CASE
        WHEN CAST(drep_type AS VARCHAR) = 'ABSTAIN' THEN 'ABSTAIN'
        WHEN CAST(drep_type AS VARCHAR) = 'NO_CONFIDENCE' THEN 'NO_CONFIDENCE'
        ELSE NULL
    END
"""


PARQUET_SQL = f"""
    SELECT CAST(epoch_no AS BIGINT) AS epoch_no,
           {normalize_hash_expr('drep_hash')} AS drep_hash,
           CAST(drep_id AS VARCHAR) AS drep_id,
           CAST(amount AS HUGEINT) AS amount,
           CAST(active_until AS BIGINT) AS active_until,
           {PARQUET_SPECIAL_TYPE} AS special_type
    FROM {{parquet_source}}
    WHERE CAST(epoch_no AS BIGINT) = {{epoch}}
"""

STORE_QUERY = """
    SELECT drep_hash, drep_id, amount, active_until, drep_type
    FROM drep_dist
    WHERE epoch = %s
"""

AMOUNT_DIFF_SQL = f"""
    WITH p_agg AS (
        SELECT CASE WHEN special_type IS NOT NULL THEN special_type ELSE drep_hash END AS compare_key,
               coalesce(special_type, 'NORMAL') AS key_type,
               any_value(drep_id) AS drep_id,
               sum(amount) AS amount
        FROM p
        WHERE special_type IS NOT NULL OR drep_hash IS NOT NULL
        GROUP BY 1, 2
    ),
    s_agg AS (
        SELECT CASE
                   WHEN {STORE_SPECIAL_TYPE} IS NOT NULL THEN {STORE_SPECIAL_TYPE}
                   ELSE {normalize_hash_expr('drep_hash')}
               END AS compare_key,
               coalesce({STORE_SPECIAL_TYPE}, 'NORMAL') AS key_type,
               any_value(CAST(drep_id AS VARCHAR)) AS drep_id,
               sum(CAST(amount AS HUGEINT)) AS amount
        FROM s
        WHERE {STORE_SPECIAL_TYPE} IS NOT NULL OR drep_hash IS NOT NULL
        GROUP BY 1, 2
    )
    SELECT *
    FROM (
        SELECT COALESCE(p.compare_key, s.compare_key) AS compare_key,
               COALESCE(p.key_type, s.key_type) AS key_type,
               CASE
                   WHEN p.compare_key IS NULL THEN 'ONLY_IN_YACI'
                   WHEN s.compare_key IS NULL THEN 'ONLY_IN_PARQUET'
                   WHEN p.amount IS DISTINCT FROM s.amount THEN 'AMOUNT_MISMATCH'
                   ELSE NULL
               END AS issue,
               p.amount AS parquet_amount,
               s.amount AS yaci_amount,
               p.drep_id AS parquet_drep_id,
               s.drep_id AS yaci_drep_id
        FROM p_agg p
        FULL OUTER JOIN s_agg s
          ON p.compare_key = s.compare_key
         AND p.key_type = s.key_type
    ) diff
    WHERE issue IS NOT NULL
    ORDER BY key_type, compare_key, issue
"""

ACTIVE_UNTIL_DIFF_SQL = f"""
    WITH p_agg AS (
        SELECT drep_hash,
               any_value(drep_id) AS drep_id,
               max(active_until) AS active_until
        FROM p
        WHERE special_type IS NULL
          AND drep_hash IS NOT NULL
          AND active_until IS NOT NULL
        GROUP BY drep_hash
    ),
    s_agg AS (
        SELECT {normalize_hash_expr('drep_hash')} AS drep_hash,
               any_value(CAST(drep_id AS VARCHAR)) AS drep_id,
               max(CAST(active_until AS BIGINT)) AS active_until
        FROM s
        WHERE CAST(drep_type AS VARCHAR) NOT IN ('ABSTAIN', 'NO_CONFIDENCE')
          AND drep_hash IS NOT NULL
        GROUP BY drep_hash
    )
    SELECT *
    FROM (
        SELECT COALESCE(p.drep_hash, s.drep_hash) AS drep_hash,
               CASE
                   WHEN p.drep_hash IS NULL THEN 'ONLY_IN_YACI'
                   WHEN s.drep_hash IS NULL THEN 'ONLY_IN_PARQUET'
                   WHEN p.active_until IS DISTINCT FROM s.active_until THEN 'ACTIVE_UNTIL_MISMATCH'
                   ELSE NULL
               END AS issue,
               p.active_until AS parquet_active_until,
               s.active_until AS yaci_active_until,
               p.drep_id AS parquet_drep_id,
               s.drep_id AS yaci_drep_id
        FROM p_agg p
        FULL OUTER JOIN s_agg s ON p.drep_hash = s.drep_hash
    ) diff
    WHERE issue IS NOT NULL
    ORDER BY drep_hash, issue
"""


def compare_amount_epoch(ctx, epoch):
    return run_epoch_diff(
        ctx,
        "drep_amount",
        epoch,
        "drep_distr",
        PARQUET_SQL,
        STORE_QUERY,
        (epoch,),
        AMOUNT_DIFF_SQL,
        f"drep_amount_epoch_{epoch}",
    )


def compare_active_until_epoch(ctx, epoch):
    return run_epoch_diff(
        ctx,
        "drep_active_until",
        epoch,
        "drep_distr",
        PARQUET_SQL,
        STORE_QUERY,
        (epoch,),
        ACTIVE_UNTIL_DIFF_SQL,
        f"drep_active_until_epoch_{epoch}",
    )


def run_amount(ctx, epochs):
    return run_epoch_model(ctx, "drep_amount", "drep_amount", epochs, compare_amount_epoch)


def run_active_until(ctx, epochs):
    return run_epoch_model(
        ctx,
        "drep_active_until",
        "drep_active_until",
        epochs,
        compare_active_until_epoch,
    )
