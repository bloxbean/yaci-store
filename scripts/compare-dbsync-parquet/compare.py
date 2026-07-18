#!/usr/bin/env python3
"""Compare Yaci Store PostgreSQL data with DB Sync reference Parquet files."""

import argparse
import json
import os
import shlex
import shutil
import sys
import tempfile
import time
from datetime import datetime

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from common.config import redact_url, resolve_config
from common.duckdb_utils import DuckDbContext
from common.logger import Logger
from common.parquet import required_datasets, resolve_parquet_files, validate_schemas
from common.postgres import connect
from models import adapot, drep, epoch_stake, gov_action_proposal_status, reward_rest


VALID_MODELS = [
    "adapot",
    "epoch_stake",
    "reward_rest",
    "drep_amount",
    "drep_active_until",
    "gov_action_proposal_status",
]


class CompareContext:
    def __init__(self, cfg, logger, duck, store_conn, parquet_files,
                 report_dir, mismatch_dir, temp_dir):
        self.cfg = cfg
        self.logger = logger
        self.duck = duck
        self.store_conn = store_conn
        self.parquet_files = parquet_files
        self.report_dir = report_dir
        self.mismatch_dir = mismatch_dir
        self.temp_dir = temp_dir


def add_args(parser):
    epoch_group = parser.add_mutually_exclusive_group()
    epoch_group.add_argument("--epoch", type=int, help="Single epoch to compare")
    epoch_group.add_argument("--start-epoch", type=int, help="Start epoch")
    parser.add_argument("--end-epoch", type=int, help="End epoch")

    parser.add_argument("--config", metavar="FILE", help="Path to JSON config file")
    parser.add_argument("--models", nargs="+", choices=VALID_MODELS, help="Comparison models to run")
    parser.add_argument(
        "--reward-types",
        help="Comma-separated reward_rest types: treasury,reserves,proposal_refund",
    )
    parser.add_argument(
        "--include-zero-amount",
        action="store_true",
        default=None,
        help="Include amount=0 epoch_stake rows",
    )
    parser.add_argument("--max-mismatches", type=int, default=None, help="Mismatch sample limit per epoch")
    parser.add_argument("--dbsync-parquet-dir", help="Directory containing DB Sync Parquet files")
    parser.add_argument("--store-url", help="Yaci Store PostgreSQL connection URL")
    parser.add_argument("--store-user", help="Yaci Store username override")
    parser.add_argument("--store-password", help="Yaci Store password override")
    parser.add_argument("--store-schema", help="Yaci Store schema name")
    parser.add_argument("--duckdb-memory-limit", help="DuckDB memory limit, for example 2GB")
    parser.add_argument("--reports-dir", help="Directory for structured reports")
    parser.add_argument("--logs-dir", help="Directory for text logs")
    parser.add_argument(
        "--quiet",
        action="store_true",
        default=None,
        help="Write to log file only, do not print detailed output to console",
    )


def epoch_list(cfg):
    return list(range(cfg["start_epoch"], cfg["end_epoch"] + 1))


def format_epoch_scope(cfg):
    if cfg["start_epoch"] == cfg["end_epoch"]:
        return f"epoch {cfg['start_epoch']}"
    return f"epochs {cfg['start_epoch']} -> {cfg['end_epoch']}"


def new_error_result(label, error, started_at):
    return {
        "label": label,
        "status": "ERROR",
        "epochs_compared": 0,
        "epochs_with_mismatch": 0,
        "total_mismatches": 0,
        "errors": 1,
        "error_message": f"{type(error).__name__}: {error}",
        "mismatch_files": [],
        "duration_seconds": round(time.time() - started_at, 3),
    }


def run_selected_models(ctx, epochs):
    results = []
    for model in ctx.cfg["models"]:
        started_at = time.time()
        try:
            if model == "adapot":
                results.append(adapot.run(ctx, epochs))
            elif model == "epoch_stake":
                results.append(epoch_stake.run(ctx, epochs))
            elif model == "reward_rest":
                for reward_type in ctx.cfg["reward_types"]:
                    results.append(reward_rest.run(ctx, epochs, reward_type))
            elif model == "drep_amount":
                results.append(drep.run_amount(ctx, epochs))
            elif model == "drep_active_until":
                results.append(drep.run_active_until(ctx, epochs))
            elif model == "gov_action_proposal_status":
                results.append(gov_action_proposal_status.run(ctx, epochs))
            else:
                raise ValueError(f"unsupported model: {model}")
        except Exception as e:
            ctx.logger.error(f"{model} comparison failed", e)
            results.append(new_error_result(model, e, started_at))
    return results


def status_counts(results):
    return {
        "OK": sum(1 for result in results if result["status"] == "OK"),
        "MISMATCH": sum(1 for result in results if result["status"] == "MISMATCH"),
        "ERROR": sum(1 for result in results if result["status"] == "ERROR"),
    }


def render_summary(results, cfg, started_at, finished_at, command, report_dir, log_file):
    counts = status_counts(results)
    total_mismatches = sum(result["total_mismatches"] for result in results)
    total_duration = (finished_at - started_at).total_seconds()

    lines = []
    lines.append("=" * 100)
    lines.append("FINAL RESULT SUMMARY (compare-dbsync-parquet)")
    lines.append("=" * 100)
    lines.append(f"  Started at        : {started_at.isoformat(timespec='seconds')}")
    lines.append(f"  Finished at       : {finished_at.isoformat(timespec='seconds')}")
    lines.append(f"  Command           : {command}")
    lines.append(f"  Epoch scope       : {format_epoch_scope(cfg)}")
    lines.append(f"  Total runtime     : {total_duration:.1f}s")
    lines.append(f"  Comparators run   : {len(results)}")
    lines.append(
        "  Status counts     : "
        f"OK={counts['OK']}, MISMATCH={counts['MISMATCH']}, ERROR={counts['ERROR']}"
    )
    lines.append(f"  Total mismatches  : {total_mismatches}")
    lines.append("")
    lines.append(
        f"  {'Comparator':<40} {'Status':<9} {'Epochs':>8} "
        f"{'Bad epochs':>11} {'Mismatches':>11} {'Errors':>7} {'Time(s)':>8}"
    )
    lines.append(
        f"  {'-'*40} {'-'*9} {'-'*8} {'-'*11} {'-'*11} {'-'*7} {'-'*8}"
    )
    for result in results:
        bad_epochs = f"{result['epochs_with_mismatch']}/{result['epochs_compared']}"
        lines.append(
            f"  {result['label']:<40} "
            f"{result['status']:<9} "
            f"{result['epochs_compared']:>8} "
            f"{bad_epochs:>11} "
            f"{result['total_mismatches']:>11} "
            f"{result['errors']:>7} "
            f"{result['duration_seconds']:>8.1f}"
        )
    lines.append("")
    lines.append(f"  Report directory  : {report_dir}")
    lines.append(f"  Log file          : {log_file}")
    lines.append("=" * 100)
    return "\n".join(lines)


def summary_payload(results, cfg, parquet_files, started_at, finished_at, command,
                    report_dir, log_file):
    counts = status_counts(results)
    return {
        "started_at": started_at.isoformat(timespec="seconds"),
        "finished_at": finished_at.isoformat(timespec="seconds"),
        "command": command,
        "epoch_scope": {
            "start_epoch": cfg["start_epoch"],
            "end_epoch": cfg["end_epoch"],
        },
        "store": {
            "url": redact_url(cfg["store_url"]),
            "schema": cfg["store_schema"],
        },
        "parquet_files": parquet_files,
        "settings": {
            "models": cfg["models"],
            "reward_types": cfg["reward_types"],
            "include_zero_amount": cfg["include_zero_amount"],
            "max_mismatches": cfg["max_mismatches"],
            "duckdb_memory_limit": cfg["duckdb_memory_limit"],
        },
        "status_counts": counts,
        "total_mismatches": sum(result["total_mismatches"] for result in results),
        "results": results,
        "report_dir": report_dir,
        "log_file": log_file,
    }


def write_report_files(report_dir, summary_text, payload):
    os.makedirs(report_dir, exist_ok=True)
    summary_log = os.path.join(report_dir, "summary.log")
    summary_json = os.path.join(report_dir, "summary.json")
    with open(summary_log, "w", encoding="utf-8") as f:
        f.write(summary_text + "\n")
    with open(summary_json, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2)
        f.write("\n")
    return summary_log, summary_json


def exit_code(results):
    counts = status_counts(results)
    if counts["ERROR"]:
        return 2
    if counts["MISMATCH"]:
        return 1
    return 0


def main():
    parser = argparse.ArgumentParser(
        description="Compare Yaci Store PostgreSQL data with DB Sync reference Parquet files.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 compare.py --config config.json
  python3 compare.py --epoch 624 --models adapot drep_amount --config config.json
  python3 compare.py --start-epoch 510 --end-epoch 520 --config config.json
        """,
    )
    add_args(parser)
    args = parser.parse_args()

    try:
        cfg = resolve_config(args, VALID_MODELS)
    except (OSError, json.JSONDecodeError, ValueError) as e:
        parser.error(str(e))

    started_at = datetime.now()
    run_id = started_at.strftime("%Y%m%d_%H%M%S")
    command = shlex.join([sys.executable] + sys.argv)

    report_dir = os.path.join(cfg["reports_dir"], f"compare_dbsync_parquet_{run_id}")
    mismatch_dir = os.path.join(report_dir, "mismatches")
    log_file = os.path.join(cfg["logs_dir"], f"compare_dbsync_parquet_{run_id}.log")
    os.makedirs(mismatch_dir, exist_ok=True)

    logger = Logger(log_file, quiet=cfg["quiet"])
    duck = None
    store_conn = None
    temp_dir = tempfile.mkdtemp(prefix="compare_dbsync_parquet_")

    try:
        datasets = required_datasets(cfg["models"])
        duck = DuckDbContext(cfg["duckdb_memory_limit"])
        parquet_files = resolve_parquet_files(cfg, datasets)
        validate_schemas(duck, parquet_files)

        logger.log("===== Starting DB Sync Parquet comparison =====")
        logger.log(f"Log file: {os.path.abspath(log_file)}")
        logger.log(f"Report directory: {os.path.abspath(report_dir)}")
        logger.log(f"Epoch scope: {format_epoch_scope(cfg)}")
        logger.log(f"Yaci Store URL: {redact_url(cfg['store_url'])} (schema: {cfg['store_schema']})")
        logger.log("Parquet files:")
        for dataset, path in parquet_files.items():
            logger.log(f"  {dataset}: {path}")
        logger.log()

        store_conn = connect(cfg["store_url"], cfg["store_schema"])
        ctx = CompareContext(
            cfg,
            logger,
            duck,
            store_conn,
            parquet_files,
            report_dir,
            mismatch_dir,
            temp_dir,
        )

        results = run_selected_models(ctx, epoch_list(cfg))
        finished_at = datetime.now()
        summary_text = render_summary(
            results,
            cfg,
            started_at,
            finished_at,
            command,
            report_dir,
            log_file,
        )
        payload = summary_payload(
            results,
            cfg,
            parquet_files,
            started_at,
            finished_at,
            command,
            report_dir,
            log_file,
        )
        summary_log, summary_json = write_report_files(report_dir, summary_text, payload)

        logger.log(summary_text)
        logger.log()
        logger.log(f"Summary log written to: {summary_log}")
        logger.log(f"Summary JSON written to: {summary_json}")
        sys.exit(exit_code(results))
    finally:
        if store_conn is not None:
            store_conn.close()
        if duck is not None:
            duck.close()
        shutil.rmtree(temp_dir, ignore_errors=True)


if __name__ == "__main__":
    main()
