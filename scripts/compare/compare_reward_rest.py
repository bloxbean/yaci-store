#!/usr/bin/env python3
"""
================================================================================
  compare_reward_rest.py - Compare Reward Rest data between DB Sync and Yaci Store
================================================================================

USAGE:
------

1. Install required dependencies:
   pip3 install psycopg2-binary

2. Run the script:

   # Using a config file
   python3 compare_reward_rest.py --epoch 1075 --config config.json

   # Compare reward_rest for a single epoch, default type = proposal_refund
   python3 compare_reward_rest.py --epoch 1075

   # Compare with a specific reward type
   python3 compare_reward_rest.py --epoch 1075 --reward-type treasury
   python3 compare_reward_rest.py --epoch 1075 --reward-type reserves
   python3 compare_reward_rest.py --epoch 1075 --reward-type proposal_refund

   # Compare across an epoch range
   python3 compare_reward_rest.py --start-epoch 1075 --end-epoch 1080

   # Custom database connections (CLI args override config file)
   python3 compare_reward_rest.py --epoch 1075 \
       --dbsync-url "postgresql://dbsync:dbsync@localhost:5678/cexplorer" \
       --store-url "postgresql://yaci:dbpass@localhost:5432/yaci_store" \
       --store-schema yaci_store

   # Output to log file only, suppress console output
   python3 compare_reward_rest.py --epoch 1075 --quiet

   # Limit the number of mismatches printed
   python3 compare_reward_rest.py --epoch 1075 --max-mismatches 50

3. Output:
   - Prints to console (and writes to log file in logs/ directory)
   - Log file named: logs/reward_rest_compare_<type>_<timestamp>.log
   - Compares using multiset: (address, type, earned_epoch, amount, spendable_epoch)
   - Includes cases where a record appears multiple times (count mismatch)

4. Notes:
   - DB Sync: reward_rest table, joined with stake_address to get view (bech32 address)
   - Yaci Store: reward_rest table, address field is already bech32
   - Valid reward types: treasury, reserves, proposal_refund
   - Uses multiset comparison (counts occurrences), not just presence/absence
   - Config file (JSON) provides defaults, CLI args override

PROBLEM SOLVED:
---------------
This script replaces RewardRestDataComparator.java, with advantages:
  - No build/compile needed, runs directly with Python
  - Flexible configuration via config file or command-line arguments
  - Pass reward type via --reward-type instead of modifying code
================================================================================
"""

import argparse
import os
import shlex
import sys
import time
from collections import Counter
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


MISMATCH_FIELDS = [
    "epoch",
    "issue",
    "address",
    "type",
    "earned_epoch",
    "amount",
    "spendable_epoch",
    "dbsync_count",
    "yaci_store_count",
]


# ============================================================
# Compare: reward_rest
# ============================================================
def compare_reward_rest(epoch, reward_type, dbsync_url, store_url, store_schema,
                        logger, max_mismatches, mismatch_dir):
    dbsync_query = """
        SELECT sa.view, rr.type, rr.earned_epoch, rr.amount, rr.spendable_epoch
        FROM reward_rest rr
        INNER JOIN stake_address sa ON sa.id = rr.addr_id
        WHERE rr.type = %s::rewardtype AND rr.earned_epoch = %s
        ORDER BY rr.earned_epoch, sa.view, rr.amount
    """
    store_query = """
        SELECT address, type, earned_epoch, amount, spendable_epoch
        FROM reward_rest
        WHERE type = %s AND earned_epoch = %s
        ORDER BY earned_epoch, address, amount
    """

    dbsync_counter = Counter()
    store_counter = Counter()

    # --- Fetch DB Sync ---
    try:
        conn = connect(dbsync_url)
        with conn.cursor() as cur:
            cur.execute(dbsync_query, (reward_type, epoch))
            for row in cur.fetchall():
                address, rtype, earned_epoch, amount, spendable_epoch = row
                key = (address, rtype, int(earned_epoch), int(amount), int(spendable_epoch) if spendable_epoch is not None else None)
                dbsync_counter[key] += 1
        conn.close()
    except Exception as e:
        logger.error(f"DB Sync query error for epoch {epoch}", e)
        return -1, None

    # --- Fetch Yaci Store ---
    try:
        conn = connect(store_url, store_schema)
        with conn.cursor() as cur:
            cur.execute(store_query, (reward_type, epoch))
            for row in cur.fetchall():
                address, rtype, earned_epoch, amount, spendable_epoch = row
                key = (address, rtype, int(earned_epoch), int(amount), int(spendable_epoch) if spendable_epoch is not None else None)
                store_counter[key] += 1
        conn.close()
    except Exception as e:
        logger.error(f"Yaci Store query error for epoch {epoch}", e)
        return -1, None

    # --- Compare ---
    sample_name = f"reward_rest_{reward_type}_epoch_{epoch}"
    writer = MismatchCsvWriter(mismatch_dir, sample_name, MISMATCH_FIELDS, max_mismatches)
    recorder = MismatchRecorder(logger, writer, max_mismatches)

    def fmt_entry(entry):
        return f"address={entry[0]}, type={entry[1]}, earned_epoch={entry[2]}, amount={entry[3]}, spendable_epoch={entry[4]}"

    def row_for(issue, entry, db_count, store_count):
        return {
            "epoch": epoch,
            "issue": issue,
            "address": entry[0],
            "type": entry[1],
            "earned_epoch": entry[2],
            "amount": entry[3],
            "spendable_epoch": entry[4],
            "dbsync_count": db_count,
            "yaci_store_count": store_count,
        }

    for entry, db_count in dbsync_counter.items():
        store_count = store_counter.get(entry)
        if store_count is None:
            recorder.record(
                row_for("ONLY_IN_DBSYNC", entry, db_count, 0),
                [f"  Only in DB Sync: {fmt_entry(entry)} (count={db_count})"],
            )
        elif store_count != db_count:
            recorder.record(
                row_for("COUNT_MISMATCH", entry, db_count, store_count),
                [
                    f"  Count mismatch: {fmt_entry(entry)}",
                    f"    DB Sync count={db_count}, Yaci Store count={store_count}",
                ],
            )

    for entry, store_count in store_counter.items():
        if entry not in dbsync_counter:
            recorder.record(
                row_for("ONLY_IN_YACI", entry, 0, store_count),
                [f"  Only in Yaci Store: {fmt_entry(entry)} (count={store_count})"],
            )

    return recorder.finish()


# ============================================================
# Main
# ============================================================
def main():
    parser = argparse.ArgumentParser(
        description="Compare Reward Rest data between DB Sync and Yaci Store.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --epoch 1075
  %(prog)s --epoch 1075 --config config.json
  %(prog)s --epoch 1075 --reward-type treasury
  %(prog)s --start-epoch 1075 --end-epoch 1080
        """,
    )

    epoch_group = parser.add_mutually_exclusive_group(required=True)
    epoch_group.add_argument("--epoch", type=int, help="Single epoch to compare")
    epoch_group.add_argument("--start-epoch", type=int, help="Start epoch (use with --end-epoch)")
    parser.add_argument("--end-epoch", type=int, help="End epoch (use with --start-epoch)")

    parser.add_argument("--reward-type", default="proposal_refund",
                        help="Reward type to compare: treasury, reserves, proposal_refund (default: proposal_refund)")
    parser.add_argument("--max-mismatches", type=int, default=None, help="Limit mismatches printed per epoch (0 = unlimited)")

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
    report_dir = run_report_dir(args, f"compare_reward_rest_{args.reward_type}", run_id)
    mismatch_dir = os.path.join(report_dir, "mismatches")
    log_file = os.path.join(args.logs_dir, f"reward_rest_compare_{args.reward_type}_{run_id}.log")
    os.makedirs(mismatch_dir, exist_ok=True)
    logger = Logger(log_file, quiet=args.quiet)

    logger.log("===== Starting Reward Rest comparison =====")
    logger.log(f"Log file: {os.path.abspath(log_file)}")
    logger.log(f"Report directory: {os.path.abspath(report_dir)}")
    logger.log(f"Epoch range: {start_epoch} -> {end_epoch}")
    logger.log(f"Reward type: {args.reward_type}")
    logger.log(f"DB Sync URL: {redact_url(args.dbsync_url)}")
    logger.log(f"Yaci Store URL: {redact_url(args.store_url)} (schema: {args.store_schema})")
    logger.log()

    total_epochs = end_epoch - start_epoch + 1
    label = f"reward_rest (type={args.reward_type})"
    result = new_result(label)
    result["epochs_compared"] = total_epochs
    result["log_file"] = os.path.abspath(log_file)
    result_started = time.time()

    for epoch in range(start_epoch, end_epoch + 1):
        logger.log(f"############ Epoch {epoch} - reward_rest (type={args.reward_type}) ############")

        count, mismatch_file = compare_reward_rest(
            epoch,
            args.reward_type,
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
            logger.log(f"  OK - All reward_rest data matches")
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
        "compare_reward_rest",
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
            "reward_type": args.reward_type,
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
