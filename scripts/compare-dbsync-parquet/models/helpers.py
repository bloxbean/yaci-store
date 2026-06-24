"""Shared model-comparison helpers."""

import os
import time

from common.postgres import copy_query_to_csv


def normalize_hash_expr(column):
    return (
        "lower(CASE "
        f"WHEN {column} IS NULL THEN NULL "
        f"WHEN lower(CAST({column} AS VARCHAR)) LIKE '0x%' THEN substr(lower(CAST({column} AS VARCHAR)), 3) "
        f"ELSE lower(CAST({column} AS VARCHAR)) END)"
    )


def run_epoch_diff(ctx, job_key, epoch, dataset, parquet_select_sql, store_query,
                   store_params, diff_sql, sample_name):
    parquet_path = ctx.parquet_files[dataset]
    parquet_source = ctx.duck.parquet_source(parquet_path)
    store_csv = os.path.join(ctx.temp_dir, f"{sample_name}_store.csv")

    ctx.duck.create_view("p", parquet_select_sql.format(parquet_source=parquet_source, epoch=epoch))
    copy_query_to_csv(ctx.store_conn, store_query, store_params, store_csv)
    ctx.duck.create_csv_view("s", store_csv)

    mismatch_count = ctx.duck.diff_count(diff_sql)
    mismatch_file = None
    if mismatch_count:
        mismatch_file = os.path.join(ctx.mismatch_dir, f"{sample_name}.csv")
        ctx.duck.write_diff_sample(diff_sql, mismatch_file, ctx.cfg["max_mismatches"])
    return mismatch_count, mismatch_file


def new_result(label):
    return {
        "label": label,
        "status": "OK",
        "epochs_compared": 0,
        "epochs_with_mismatch": 0,
        "total_mismatches": 0,
        "errors": 0,
        "mismatch_files": [],
        "duration_seconds": 0.0,
    }


def finish_result(result, started_at):
    result["duration_seconds"] = round(time.time() - started_at, 3)
    if result["errors"]:
        result["status"] = "ERROR"
    elif result["total_mismatches"]:
        result["status"] = "MISMATCH"
    else:
        result["status"] = "OK"
    return result


def run_epoch_model(ctx, label, job_key, epochs, compare_epoch_func):
    started_at = time.time()
    result = new_result(label)
    result["epochs_compared"] = len(epochs)

    for epoch in epochs:
        ctx.logger.log(f"############ Epoch {epoch} - {label} ############")
        try:
            mismatch_count, mismatch_file = compare_epoch_func(ctx, epoch)
        except Exception as e:
            result["errors"] += 1
            ctx.logger.error(f"{label} comparison error for epoch {epoch}", e)
            ctx.logger.log()
            continue

        if mismatch_count == 0:
            ctx.logger.log("  OK - data matches")
        else:
            result["epochs_with_mismatch"] += 1
            result["total_mismatches"] += mismatch_count
            if mismatch_file:
                result["mismatch_files"].append(mismatch_file)
            ctx.logger.log(f"  MISMATCH: {mismatch_count} mismatch(es)")
            if mismatch_file:
                ctx.logger.log(f"  Sample: {mismatch_file}")
        ctx.logger.log()

    return finish_result(result, started_at)
