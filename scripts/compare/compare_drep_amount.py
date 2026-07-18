#!/usr/bin/env python3
"""
================================================================================
  compare_drep_amount.py - Compare drep distribution AMOUNT between DB Sync and Yaci Store
================================================================================

USAGE:
------

1. Install required dependencies:
   pip3 install psycopg2-binary

2. Run the script:

   # Using a config file
   python3 compare_drep_amount.py --epoch 515 --config config.json

   # Compare a specific epoch
   python3 compare_drep_amount.py --epoch 515

   # Compare an epoch range (510 to 520)
   python3 compare_drep_amount.py --start-epoch 510 --end-epoch 520

   # Custom database connections (CLI args override config file)
   python3 compare_drep_amount.py --epoch 515 \\
       --dbsync-url "postgresql://localhost:5678/cexplorer" \\
       --dbsync-user dbsync --dbsync-password dbsync \\
       --store-url "postgresql://localhost:5432/yaci_store" \\
       --store-user yaci --store-password dbpass \\
       --store-schema yaci_store

   # Output to log file only, suppress console output
   python3 compare_drep_amount.py --epoch 515 --quiet

   # Limit the number of mismatches printed
   python3 compare_drep_amount.py --epoch 515 --max-mismatches 50
================================================================================
"""

import argparse
import os
import shlex
import sys
import time
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import (
    Logger,
    MismatchCsvWriter,
    MismatchRecorder,
    add_common_args,
    connect,
    exit_code,
    finish_result,
    new_result,
    normalize_hash,
    redact_url,
    render_summary,
    resolve_config,
    run_report_dir,
    summary_payload,
    write_json,
    write_report_files,
)


MISMATCH_FIELDS = ["epoch", "issue", "drep_hash", "drep_id", "dbsync_amount", "yaci_store_amount"]


def compare_amount(epoch, dbsync_url, store_url, store_schema, logger, max_mismatches, mismatch_dir):
    dbsync_query = """
        SELECT dh.raw, d.amount, dh.view
        FROM drep_distr d
        INNER JOIN drep_hash dh ON dh.id = d.hash_id
        WHERE d.epoch_no = %s
    """
    store_query = """
        SELECT drep_hash, drep_id, amount, drep_type
        FROM drep_dist
        WHERE epoch = %s
    """

    dbsync_results = {}
    store_results = {}
    drep_hash_to_id = {}

    dbsync_abstain = 0
    store_abstain = 0
    dbsync_no_confidence = 0
    store_no_confidence = 0

    try:
        conn = connect(dbsync_url)
        with conn.cursor() as cur:
            cur.execute(dbsync_query, (epoch,))
            for row in cur.fetchall():
                raw, amount, view = row
                h = normalize_hash(raw)
                amt = int(amount)
                if view and "abstain" in view:
                    dbsync_abstain += amt
                elif view and "no_confidence" in view:
                    dbsync_no_confidence += amt
                else:
                    dbsync_results[h] = amt
        conn.close()
    except Exception as e:
        logger.error(f"DB Sync query error for epoch {epoch}", e)
        return -1, None

    try:
        conn = connect(store_url, store_schema)
        with conn.cursor() as cur:
            cur.execute(store_query, (epoch,))
            for row in cur.fetchall():
                drep_hash, drep_id, amount, drep_type = row
                h = normalize_hash(drep_hash)
                amt = int(amount)
                if drep_type == "ABSTAIN":
                    store_abstain = amt
                elif drep_type == "NO_CONFIDENCE":
                    store_no_confidence = amt
                else:
                    store_results[h] = amt
                if drep_id:
                    drep_hash_to_id[h] = drep_id
        conn.close()
    except Exception as e:
        logger.error(f"Yaci Store query error for epoch {epoch}", e)
        return -1, None

    writer = MismatchCsvWriter(mismatch_dir, f"drep_amount_epoch_{epoch}", MISMATCH_FIELDS, max_mismatches)
    recorder = MismatchRecorder(logger, writer, max_mismatches)

    for h, amt_dbsync in dbsync_results.items():
        if h in store_results:
            amt_store = store_results[h]
            if amt_dbsync != amt_store:
                lines = [
                    f"  Mismatch hash: {h}",
                    f"    DB Sync amount : {amt_dbsync}",
                    f"    Yaci Store amt : {amt_store}",
                ]
                if h in drep_hash_to_id:
                    lines.append(f"    DRep ID: {drep_hash_to_id[h]}")
                recorder.record(
                    {
                        "epoch": epoch,
                        "issue": "AMOUNT_MISMATCH",
                        "drep_hash": h,
                        "drep_id": drep_hash_to_id.get(h),
                        "dbsync_amount": amt_dbsync,
                        "yaci_store_amount": amt_store,
                    },
                    lines,
                )
        else:
            lines = [f"  Hash {h} present in DB Sync but MISSING in Yaci Store (amount: {amt_dbsync})"]
            if h in drep_hash_to_id:
                lines.append(f"    DRep ID: {drep_hash_to_id[h]}")
            recorder.record(
                {
                    "epoch": epoch,
                    "issue": "ONLY_IN_DBSYNC",
                    "drep_hash": h,
                    "drep_id": drep_hash_to_id.get(h),
                    "dbsync_amount": amt_dbsync,
                    "yaci_store_amount": None,
                },
                lines,
            )

    for h in store_results:
        if h not in dbsync_results:
            lines = [f"  Hash {h} present in Yaci Store but MISSING in DB Sync (amount: {store_results[h]})"]
            if h in drep_hash_to_id:
                lines.append(f"    DRep ID: {drep_hash_to_id[h]}")
            recorder.record(
                {
                    "epoch": epoch,
                    "issue": "ONLY_IN_YACI",
                    "drep_hash": h,
                    "drep_id": drep_hash_to_id.get(h),
                    "dbsync_amount": None,
                    "yaci_store_amount": store_results[h],
                },
                lines,
            )

    if dbsync_abstain != store_abstain:
        recorder.record(
            {
                "epoch": epoch,
                "issue": "SPECIAL_AMOUNT_MISMATCH",
                "drep_hash": "ABSTAIN",
                "drep_id": None,
                "dbsync_amount": dbsync_abstain,
                "yaci_store_amount": store_abstain,
            },
            [f"  Mismatch ABSTAIN amount: DB Sync={dbsync_abstain}, Yaci Store={store_abstain}"],
        )

    if dbsync_no_confidence != store_no_confidence:
        recorder.record(
            {
                "epoch": epoch,
                "issue": "SPECIAL_AMOUNT_MISMATCH",
                "drep_hash": "NO_CONFIDENCE",
                "drep_id": None,
                "dbsync_amount": dbsync_no_confidence,
                "yaci_store_amount": store_no_confidence,
            },
            [
                "  Mismatch NO_CONFIDENCE amount: "
                f"DB Sync={dbsync_no_confidence}, Yaci Store={store_no_confidence}"
            ],
        )

    return recorder.finish()


def main():
    parser = argparse.ArgumentParser(
        description="Compare drep distribution AMOUNT between DB Sync and Yaci Store.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --epoch 515
  %(prog)s --epoch 515 --config config.json
  %(prog)s --start-epoch 510 --end-epoch 520
        """,
    )

    epoch_group = parser.add_mutually_exclusive_group(required=True)
    epoch_group.add_argument("--epoch", type=int, help="Single epoch to compare")
    epoch_group.add_argument("--start-epoch", type=int, help="Start epoch (use with --end-epoch)")
    parser.add_argument("--end-epoch", type=int, help="End epoch (use with --start-epoch)")

    parser.add_argument("--max-mismatches", type=int, default=None, help="Limit mismatches printed per epoch (0 = unlimited)")

    add_common_args(parser)

    args = parser.parse_args()
    args = resolve_config(args)

    if args.epoch is not None:
        start_epoch = args.epoch
        end_epoch = args.epoch
    else:
        start_epoch = args.start_epoch
        end_epoch = args.end_epoch
        if end_epoch is None:
            parser.error("--end-epoch is required when using --start-epoch")
        if end_epoch < start_epoch:
            parser.error("--end-epoch must be >= --start-epoch")

    started_at = datetime.now()
    run_id = started_at.strftime("%Y%m%d_%H%M%S")
    command = shlex.join([sys.executable] + sys.argv)
    report_dir = run_report_dir(args, "compare_drep_amount", run_id)
    mismatch_dir = os.path.join(report_dir, "mismatches")
    log_file = os.path.join(args.logs_dir, f"drep_compare_amount_{run_id}.log")
    os.makedirs(mismatch_dir, exist_ok=True)
    logger = Logger(log_file, quiet=args.quiet)

    logger.log(f"===== Starting drep amount comparison =====")
    logger.log(f"Log file: {os.path.abspath(log_file)}")
    logger.log(f"Report directory: {os.path.abspath(report_dir)}")
    logger.log(f"Epoch range: {start_epoch} -> {end_epoch}")
    logger.log(f"DB Sync URL: {redact_url(args.dbsync_url)}")
    logger.log(f"Yaci Store URL: {redact_url(args.store_url)} (schema: {args.store_schema})")
    logger.log()

    total_epochs = end_epoch - start_epoch + 1
    result = new_result("drep_amount")
    result["epochs_compared"] = total_epochs
    result["log_file"] = os.path.abspath(log_file)
    result_started = time.time()

    for epoch in range(start_epoch, end_epoch + 1):
        logger.log(f"############ Epoch {epoch} - amount ############")

        count, mismatch_file = compare_amount(
            epoch,
            args.dbsync_url,
            args.store_url,
            args.store_schema,
            logger,
            args.max_mismatches,
            mismatch_dir,
        )

        if count < 0:
            result["errors"] += 1
            logger.log(f"  Database connection error, skipping epoch {epoch}")
        elif count == 0:
            logger.log(f"  OK - All data matches between DB Sync and Yaci Store")
        else:
            result["epochs_with_mismatch"] += 1
            result["total_mismatches"] += count
            if mismatch_file:
                result["mismatch_files"].append(mismatch_file)
            logger.log(f"  MISMATCH: {count} mismatch(es)")
            if mismatch_file:
                logger.log(f"  Sample: {mismatch_file}")

        logger.log()

    result = finish_result(result, result_started)
    finished_at = datetime.now()
    summary_text = render_summary(
        [result],
        "compare_drep_amount",
        started_at,
        finished_at,
        command,
        f"epochs {start_epoch} -> {end_epoch}" if start_epoch != end_epoch else f"epoch {start_epoch}",
        report_dir,
        os.path.abspath(log_file),
    )
    payload = summary_payload(
        [result],
        started_at,
        finished_at,
        command,
        {"start_epoch": start_epoch, "end_epoch": end_epoch},
        report_dir,
        os.path.abspath(log_file),
        {
            "dbsync_url": redact_url(args.dbsync_url),
            "store_url": redact_url(args.store_url),
            "store_schema": args.store_schema,
            "max_mismatches": args.max_mismatches,
        },
    )
    payload["result"] = result

    logger.log(summary_text)
    if args.result_json:
        write_json(args.result_json, payload)
    else:
        summary_log, summary_json = write_report_files(report_dir, summary_text, payload)
        logger.log()
        logger.log(f"Summary log written to: {summary_log}")
        logger.log(f"Summary JSON written to: {summary_json}")
    sys.exit(exit_code([result]))


if __name__ == "__main__":
    main()
