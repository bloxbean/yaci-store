#!/usr/bin/env python3
"""
================================================================================
  compare_adapot.py - Compare AdaPot data between DB Sync and Yaci Store
================================================================================

USAGE:
------

1. Install required dependencies:
   pip3 install psycopg2-binary

2. Run the script:

   # Using a config file
   python3 compare_adapot.py --epoch 800 --config config.json

   # Compare adapot for a specific epoch
   python3 compare_adapot.py --epoch 800

   # Compare across an epoch range (740 to 902)
   python3 compare_adapot.py --start-epoch 740 --end-epoch 902

   # Custom database connections (CLI args override config file)
   python3 compare_adapot.py --epoch 800 \
       --dbsync-url "postgresql://dbsync:dbsync@localhost:5678/cexplorer" \
       --store-url "postgresql://yaci:dbpass@localhost:5432/yaci_store" \
       --store-schema yaci_store

   # Output to log file only, suppress console output
   python3 compare_adapot.py --epoch 800 --quiet

3. Output:
   - Prints to console (and writes to log file in logs/ directory)
   - Log file named: logs/adapot_compare_<timestamp>.log
   - Compares 2 fields: treasury and reserves

4. Notes:
   - DB Sync stores adapot in the ada_pots table (last record by slot_no)
   - Yaci Store stores adapot in the adapot table
   - Config file (JSON) provides defaults, CLI args override

PROBLEM SOLVED:
---------------
This script replaces AdapotDataComparator.java, with advantages:
  - No build/compile needed, runs directly with Python
  - Flexible configuration via config file or command-line arguments
  - Automatic timestamped logging
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
    redact_url,
    render_summary,
    resolve_config,
    run_report_dir,
    summary_payload,
    write_json,
    write_report_files,
)


MISMATCH_FIELDS = ["epoch", "issue", "dbsync_value", "yaci_store_value"]


# ============================================================
# Compare: adapot (treasury + reserves)
# ============================================================
def compare_adapot(epoch, dbsync_url, store_url, store_schema, logger, mismatch_dir, max_mismatches):
    dbsync_query = """
        SELECT treasury, reserves
        FROM ada_pots
        WHERE epoch_no = %s
        ORDER BY slot_no DESC
        LIMIT 1
    """
    store_query = "SELECT treasury, reserves FROM adapot WHERE epoch = %s"

    dbsync_treasury = None
    dbsync_reserves = None
    store_treasury = None
    store_reserves = None

    # --- Fetch DB Sync ---
    try:
        conn = connect(dbsync_url)
        with conn.cursor() as cur:
            cur.execute(dbsync_query, (epoch,))
            row = cur.fetchone()
            if row:
                dbsync_treasury, dbsync_reserves = int(row[0]), int(row[1])
            else:
                logger.log(f"  No data in DB Sync for epoch {epoch}")
                return 0, None
        conn.close()
    except Exception as e:
        logger.error(f"DB Sync query error for epoch {epoch}", e)
        return -1, None

    # --- Fetch Yaci Store ---
    try:
        conn = connect(store_url, store_schema)
        with conn.cursor() as cur:
            cur.execute(store_query, (epoch,))
            row = cur.fetchone()
            if row:
                store_treasury, store_reserves = int(row[0]), int(row[1])
            else:
                logger.log(f"  No data in Yaci Store for epoch {epoch}")
                return 0, None
        conn.close()
    except Exception as e:
        logger.error(f"Yaci Store query error for epoch {epoch}", e)
        return -1, None

    # --- Compare ---
    writer = MismatchCsvWriter(mismatch_dir, f"adapot_epoch_{epoch}", MISMATCH_FIELDS, max_mismatches)
    recorder = MismatchRecorder(logger, writer, max_mismatches)

    if dbsync_treasury != store_treasury:
        recorder.record(
            {
                "epoch": epoch,
                "issue": "TREASURY",
                "dbsync_value": dbsync_treasury,
                "yaci_store_value": store_treasury,
            },
            [f"  Mismatch TREASURY: DB Sync={dbsync_treasury}, Yaci Store={store_treasury}"],
        )

    if dbsync_reserves != store_reserves:
        recorder.record(
            {
                "epoch": epoch,
                "issue": "RESERVES",
                "dbsync_value": dbsync_reserves,
                "yaci_store_value": store_reserves,
            },
            [f"  Mismatch RESERVES: DB Sync={dbsync_reserves}, Yaci Store={store_reserves}"],
        )

    return recorder.finish()


# ============================================================
# Main
# ============================================================
def main():
    parser = argparse.ArgumentParser(
        description="Compare AdaPot data (treasury, reserves) between DB Sync and Yaci Store.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --epoch 800
  %(prog)s --epoch 800 --config config.json
  %(prog)s --start-epoch 740 --end-epoch 902
        """,
    )

    epoch_group = parser.add_mutually_exclusive_group(required=True)
    epoch_group.add_argument("--epoch", type=int, help="Single epoch to compare")
    epoch_group.add_argument("--start-epoch", type=int, help="Start epoch (use with --end-epoch)")
    parser.add_argument("--end-epoch", type=int, help="End epoch (use with --start-epoch)")
    parser.add_argument("--max-mismatches", type=int, default=None, help="Limit mismatch samples printed per epoch (0 = unlimited)")

    add_common_args(parser)

    args = parser.parse_args()
    args = resolve_config(args)

    if args.epoch is not None:
        start_epoch, end_epoch = args.epoch, args.epoch
    else:
        start_epoch, end_epoch = args.start_epoch, args.end_epoch
        if end_epoch is None:
            parser.error("--end-epoch is required when using --start-epoch")
        if end_epoch < start_epoch:
            parser.error("--end-epoch must be >= --start-epoch")

    started_at = datetime.now()
    run_id = started_at.strftime("%Y%m%d_%H%M%S")
    command = shlex.join([sys.executable] + sys.argv)
    report_dir = run_report_dir(args, "compare_adapot", run_id)
    mismatch_dir = os.path.join(report_dir, "mismatches")
    log_file = os.path.join(args.logs_dir, f"adapot_compare_{run_id}.log")
    os.makedirs(mismatch_dir, exist_ok=True)
    logger = Logger(log_file, quiet=args.quiet)

    logger.log("===== Starting AdaPot comparison =====")
    logger.log(f"Log file: {os.path.abspath(log_file)}")
    logger.log(f"Report directory: {os.path.abspath(report_dir)}")
    logger.log(f"Epoch range: {start_epoch} -> {end_epoch}")
    logger.log(f"DB Sync URL: {redact_url(args.dbsync_url)}")
    logger.log(f"Yaci Store URL: {redact_url(args.store_url)} (schema: {args.store_schema})")
    logger.log()

    total_epochs = end_epoch - start_epoch + 1
    result = new_result("adapot")
    result["epochs_compared"] = total_epochs
    result["log_file"] = os.path.abspath(log_file)
    result_started = time.time()

    for epoch in range(start_epoch, end_epoch + 1):
        logger.log(f"############ Epoch {epoch} - adapot ############")

        count, mismatch_file = compare_adapot(
            epoch,
            args.dbsync_url,
            args.store_url,
            args.store_schema,
            logger,
            mismatch_dir,
            args.max_mismatches,
        )

        if count < 0:
            result["errors"] += 1
            logger.log(f"  Database connection error, skipping epoch {epoch}")
        elif count == 0:
            logger.log(f"  OK - Treasury and Reserves match between DB Sync and Yaci Store")
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
        "compare_adapot",
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
