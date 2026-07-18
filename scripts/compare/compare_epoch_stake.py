#!/usr/bin/env python3
"""
================================================================================
  compare_epoch_stake.py - Compare Epoch Stake data between DB Sync and Yaci Store
================================================================================

USAGE:
------

1. Install required dependencies:
   pip3 install psycopg2-binary

2. Run the script:

   # Using a config file
   python3 compare_epoch_stake.py --epoch 800 --config config.json

   # Compare epoch_stake for a specific epoch
   python3 compare_epoch_stake.py --epoch 800

   # Compare across an epoch range (740 to 902)
   python3 compare_epoch_stake.py --start-epoch 740 --end-epoch 902

   # Compare in descending order (high -> low, like the original Java version)
   python3 compare_epoch_stake.py --start-epoch 902 --end-epoch 740 --reverse

   # Add delay between epochs (avoid DB overload, original Java used 5s)
   python3 compare_epoch_stake.py --start-epoch 740 --end-epoch 902 --delay 5

   # Custom database connections (CLI args override config file)
   python3 compare_epoch_stake.py --epoch 800 \
       --dbsync-url "postgresql://dbsync:dbsync@10.4.10.135:5678/cexplorer" \
       --store-url "postgresql://yaci:dbpass@localhost:5432/yaci_store" \
       --store-schema yaci_store

   # Output to log file only, suppress console output
   python3 compare_epoch_stake.py --epoch 800 --quiet

   # Limit the number of mismatches printed
   python3 compare_epoch_stake.py --epoch 800 --max-mismatches 50

   # Include zero-amount rows when comparing raw table contents
   python3 compare_epoch_stake.py --epoch 800 --include-zero-amount

3. Output:
   - Prints to console (and writes to log file in logs/ directory)
   - Log file named: logs/epoch_stake_compare_<timestamp>.log
   - Compares by key: address + pool_id, value: amount
   - By default, ignores amount=0 rows to match DB Sync's current active-stake semantics

4. Notes:
   - DB Sync: epoch_stake table, epoch_no = epoch
   - Yaci Store: epoch_stake table, epoch = epoch - 2 (offset preserved from original query)
   - The script keeps this offset logic from the original Java
   - DB Sync >= 13.7.0.3 deletes legacy amount=0 epoch_stake rows; the comparator
     filters them by default unless --include-zero-amount is used
   - epoch_stake data can be very large; use --delay to avoid DB overload
   - Config file (JSON) provides defaults, CLI args override

PROBLEM SOLVED:
---------------
This script replaces EpochStakeDataComparator.java, with advantages:
  - No build/compile needed, runs directly with Python
  - Flexible configuration via config file or command-line arguments
  - Supports delay between epochs (avoid DB overload)
  - Supports descending iteration (reverse)
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


MISMATCH_FIELDS = ["epoch", "issue", "address", "pool_id", "dbsync_amount", "yaci_store_amount"]


# ============================================================
# Compare: epoch_stake
# ============================================================
def compare_epoch_stake(epoch, dbsync_url, store_url, store_schema, logger,
                        max_mismatches, include_zero_amount, mismatch_dir):
    amount_filter = "" if include_zero_amount else "AND es.amount <> 0"
    dbsync_query = f"""
        SELECT sa.view, es.amount, encode(ph.hash_raw, 'hex') as pool_id
        FROM epoch_stake es
        INNER JOIN stake_address sa ON sa.id = es.addr_id
        INNER JOIN pool_hash ph ON ph.id = es.pool_id
        WHERE es.epoch_no = %s
        {amount_filter}
        ORDER BY sa.view, encode(ph.hash_raw, 'hex'), amount
    """
    dbsync_zero_count_query = """
        SELECT count(*)
        FROM epoch_stake es
        WHERE es.epoch_no = %s
          AND es.amount = 0
    """
    # Yaci Store uses epoch - 2 (preserved offset logic from the original Java)
    store_amount_filter = "" if include_zero_amount else "AND amount <> 0"
    store_query = f"""
        SELECT address, amount, pool_id
        FROM epoch_stake
        WHERE epoch = %s - 2
        {store_amount_filter}
        ORDER BY address, pool_id, amount
    """
    store_zero_count_query = """
        SELECT count(*)
        FROM epoch_stake
        WHERE epoch = %s - 2
          AND amount = 0
    """

    dbsync_map = {}
    store_map = {}
    ignored_dbsync_zero_amount = 0
    ignored_store_zero_amount = 0

    # --- Fetch DB Sync ---
    try:
        conn = connect(dbsync_url)
        with conn.cursor() as cur:
            if not include_zero_amount:
                cur.execute(dbsync_zero_count_query, (epoch,))
                ignored_dbsync_zero_amount = int(cur.fetchone()[0])
            cur.execute(dbsync_query, (epoch,))
            for row in cur.fetchall():
                address, amount, pool_id = row
                key = f"{address}_{pool_id}"
                dbsync_map[key] = {"address": address, "amount": int(amount), "pool_id": pool_id}
        conn.close()
    except Exception as e:
        logger.error(f"DB Sync query error for epoch {epoch}", e)
        return -1, None

    # --- Fetch Yaci Store ---
    try:
        conn = connect(store_url, store_schema)
        with conn.cursor() as cur:
            if not include_zero_amount:
                cur.execute(store_zero_count_query, (epoch,))
                ignored_store_zero_amount = int(cur.fetchone()[0])
            cur.execute(store_query, (epoch,))
            for row in cur.fetchall():
                address, amount, pool_id = row
                key = f"{address}_{pool_id}"
                store_map[key] = {"address": address, "amount": int(amount), "pool_id": pool_id}
        conn.close()
    except Exception as e:
        logger.error(f"Yaci Store query error for epoch {epoch}", e)
        return -1, None

    # --- Compare ---
    writer = MismatchCsvWriter(mismatch_dir, f"epoch_stake_epoch_{epoch}", MISMATCH_FIELDS, max_mismatches)
    recorder = MismatchRecorder(logger, writer, max_mismatches)

    if ignored_dbsync_zero_amount or ignored_store_zero_amount:
        logger.log(
            "  Ignored zero-amount epoch_stake row(s): "
            f"DB Sync={ignored_dbsync_zero_amount}, Yaci Store={ignored_store_zero_amount}"
        )

    for key, db_data in dbsync_map.items():
        if key in store_map:
            store_data = store_map[key]
            if db_data["amount"] != store_data["amount"]:
                recorder.record(
                    {
                        "epoch": epoch,
                        "issue": "AMOUNT_MISMATCH",
                        "address": db_data["address"],
                        "pool_id": db_data["pool_id"],
                        "dbsync_amount": db_data["amount"],
                        "yaci_store_amount": store_data["amount"],
                    },
                    [
                        f"  Mismatch key: {key}",
                        f"    DB Sync    : amount={db_data['amount']}",
                        f"    Yaci Store : amount={store_data['amount']}",
                    ],
                )
        else:
            recorder.record(
                {
                    "epoch": epoch,
                    "issue": "ONLY_IN_DBSYNC",
                    "address": db_data["address"],
                    "pool_id": db_data["pool_id"],
                    "dbsync_amount": db_data["amount"],
                    "yaci_store_amount": None,
                },
                [f"  Key {key} present in DB Sync but MISSING in Yaci Store"],
            )

    for key in store_map:
        if key not in dbsync_map:
            store_data = store_map[key]
            recorder.record(
                {
                    "epoch": epoch,
                    "issue": "ONLY_IN_YACI",
                    "address": store_data["address"],
                    "pool_id": store_data["pool_id"],
                    "dbsync_amount": None,
                    "yaci_store_amount": store_data["amount"],
                },
                [f"  Key {key} present in Yaci Store but MISSING in DB Sync"],
            )

    return recorder.finish()


# ============================================================
# Main
# ============================================================
def main():
    parser = argparse.ArgumentParser(
        description="Compare Epoch Stake data between DB Sync and Yaci Store.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --epoch 800
  %(prog)s --epoch 800 --config config.json
  %(prog)s --start-epoch 740 --end-epoch 902
  %(prog)s --start-epoch 902 --end-epoch 740 --reverse
  %(prog)s --start-epoch 740 --end-epoch 902 --delay 5
        """,
    )

    epoch_group = parser.add_mutually_exclusive_group(required=True)
    epoch_group.add_argument("--epoch", type=int, help="Single epoch to compare")
    epoch_group.add_argument("--start-epoch", type=int, help="Start epoch (use with --end-epoch)")
    parser.add_argument("--end-epoch", type=int, help="End epoch (use with --start-epoch)")

    parser.add_argument("--reverse", action="store_true", help="Iterate from high epoch to low (like the original Java)")
    parser.add_argument("--delay", type=float, default=None, help="Delay (seconds) between epochs to avoid DB overload (original Java used 5s)")
    parser.add_argument("--max-mismatches", type=int, default=None, help="Limit mismatches printed per epoch (0 = unlimited)")
    parser.add_argument(
        "--include-zero-amount",
        action="store_true",
        help="Include amount=0 rows in the comparison instead of using DB Sync's current active-stake semantics",
    )

    add_common_args(parser)

    args = parser.parse_args()
    args = resolve_config(args)

    if args.epoch is not None:
        start_epoch, end_epoch = args.epoch, args.epoch
    else:
        start_epoch, end_epoch = args.start_epoch, args.end_epoch
        if end_epoch is None:
            parser.error("--end-epoch is required when using --start-epoch")

    # Build epoch list
    if args.reverse or start_epoch > end_epoch:
        hi, lo = max(start_epoch, end_epoch), min(start_epoch, end_epoch)
        epochs = list(range(hi, lo - 1, -1))
    else:
        epochs = list(range(start_epoch, end_epoch + 1))

    started_at = datetime.now()
    run_id = started_at.strftime("%Y%m%d_%H%M%S")
    command = shlex.join([sys.executable] + sys.argv)
    report_dir = run_report_dir(args, "compare_epoch_stake", run_id)
    mismatch_dir = os.path.join(report_dir, "mismatches")
    log_file = os.path.join(args.logs_dir, f"epoch_stake_compare_{run_id}.log")
    os.makedirs(mismatch_dir, exist_ok=True)
    logger = Logger(log_file, quiet=args.quiet)

    logger.log("===== Starting Epoch Stake comparison =====")
    logger.log(f"Log file: {os.path.abspath(log_file)}")
    logger.log(f"Report directory: {os.path.abspath(report_dir)}")
    logger.log(f"Epochs: {epochs[0]} -> {epochs[-1]} ({len(epochs)} epochs)")
    logger.log(f"DB Sync URL: {redact_url(args.dbsync_url)}")
    logger.log(f"Yaci Store URL: {redact_url(args.store_url)} (schema: {args.store_schema})")
    logger.log(
        "Zero-amount rows: "
        + ("included" if args.include_zero_amount else "ignored (DB Sync active-stake semantics)")
    )
    if args.delay > 0:
        logger.log(f"Delay between epochs: {args.delay}s")
    logger.log()

    result = new_result("epoch_stake")
    result["epochs_compared"] = len(epochs)
    result["log_file"] = os.path.abspath(log_file)
    result_started = time.time()

    for i, epoch in enumerate(epochs):
        logger.log(f"############ Epoch {epoch} - epoch_stake ############")

        count, mismatch_file = compare_epoch_stake(
            epoch,
            args.dbsync_url,
            args.store_url,
            args.store_schema,
            logger,
            args.max_mismatches,
            args.include_zero_amount,
            mismatch_dir,
        )

        if count < 0:
            result["errors"] += 1
            logger.log(f"  Database connection error, skipping epoch {epoch}")
        elif count == 0:
            logger.log(f"  OK - All epoch_stake data matches")
        else:
            result["epochs_with_mismatch"] += 1
            result["total_mismatches"] += count
            if mismatch_file:
                result["mismatch_files"].append(mismatch_file)
            logger.log(f"  MISMATCH: {count} mismatch(es)")
            if mismatch_file:
                logger.log(f"  Sample: {mismatch_file}")

        logger.log()

        # Delay between epochs (skip after last)
        if args.delay > 0 and i < len(epochs) - 1:
            time.sleep(args.delay)

    result = finish_result(result, result_started)
    finished_at = datetime.now()
    epoch_scope_text = f"epochs {epochs[0]} -> {epochs[-1]}" if len(epochs) > 1 else f"epoch {epochs[0]}"
    summary_text = render_summary(
        [result],
        "compare_epoch_stake",
        started_at,
        finished_at,
        command,
        epoch_scope_text,
        report_dir,
        os.path.abspath(log_file),
    )
    payload = summary_payload(
        [result],
        started_at,
        finished_at,
        command,
        {"start_epoch": start_epoch, "end_epoch": end_epoch, "epochs": epochs},
        report_dir,
        os.path.abspath(log_file),
        {
            "dbsync_url": redact_url(args.dbsync_url),
            "store_url": redact_url(args.store_url),
            "store_schema": args.store_schema,
            "include_zero_amount": args.include_zero_amount,
            "max_mismatches": args.max_mismatches,
            "delay": args.delay,
            "reverse": args.reverse,
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
