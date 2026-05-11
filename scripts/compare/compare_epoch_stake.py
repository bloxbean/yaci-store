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

3. Output:
   - Prints to console (and writes to log file in logs/ directory)
   - Log file named: logs/epoch_stake_compare_<timestamp>.log
   - Compares by key: address + pool_id, value: amount

4. Notes:
   - DB Sync: epoch_stake table, epoch_no = epoch
   - Yaci Store: epoch_stake table, epoch = epoch - 2 (offset preserved from original query)
   - The script keeps this offset logic from the original Java
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
import sys
import time
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import Logger, connect, add_common_args, resolve_config


# ============================================================
# Compare: epoch_stake
# ============================================================
def compare_epoch_stake(epoch, dbsync_url, store_url, store_schema, logger, max_mismatches):
    dbsync_query = """
        SELECT sa.view, es.amount, encode(ph.hash_raw, 'hex') as pool_id
        FROM epoch_stake es
        INNER JOIN stake_address sa ON sa.id = es.addr_id
        INNER JOIN pool_hash ph ON ph.id = es.pool_id
        WHERE es.epoch_no = %s
        ORDER BY sa.view, encode(ph.hash_raw, 'hex'), amount
    """
    # Yaci Store uses epoch - 2 (preserved offset logic from the original Java)
    store_query = """
        SELECT address, amount, pool_id
        FROM epoch_stake
        WHERE epoch = %s - 2
        ORDER BY address, pool_id, amount
    """

    dbsync_map = {}
    store_map = {}

    # --- Fetch DB Sync ---
    try:
        conn = connect(dbsync_url)
        with conn.cursor() as cur:
            cur.execute(dbsync_query, (epoch,))
            for row in cur.fetchall():
                address, amount, pool_id = row
                key = f"{address}_{pool_id}"
                dbsync_map[key] = {"address": address, "amount": int(amount), "pool_id": pool_id}
        conn.close()
    except Exception as e:
        logger.error(f"DB Sync query error for epoch {epoch}", e)
        return -1

    # --- Fetch Yaci Store ---
    try:
        conn = connect(store_url, store_schema)
        with conn.cursor() as cur:
            cur.execute(store_query, (epoch,))
            for row in cur.fetchall():
                address, amount, pool_id = row
                key = f"{address}_{pool_id}"
                store_map[key] = {"address": address, "amount": int(amount), "pool_id": pool_id}
        conn.close()
    except Exception as e:
        logger.error(f"Yaci Store query error for epoch {epoch}", e)
        return -1

    # --- Compare ---
    mismatch_count = 0
    truncated = False

    def check_limit():
        nonlocal truncated
        if max_mismatches and mismatch_count >= max_mismatches:
            if not truncated:
                logger.log(f"  ... (reached limit of {max_mismatches} mismatches, skipping the rest)")
                truncated = True
            return True
        return False

    for key, db_data in dbsync_map.items():
        if check_limit():
            break
        if key in store_map:
            store_data = store_map[key]
            if db_data["amount"] != store_data["amount"]:
                mismatch_count += 1
                logger.log(f"  Mismatch key: {key}")
                logger.log(f"    DB Sync    : amount={db_data['amount']}")
                logger.log(f"    Yaci Store : amount={store_data['amount']}")
        else:
            mismatch_count += 1
            logger.log(f"  Key {key} present in DB Sync but MISSING in Yaci Store")

    for key in store_map:
        if check_limit():
            break
        if key not in dbsync_map:
            mismatch_count += 1
            logger.log(f"  Key {key} present in Yaci Store but MISSING in DB Sync")

    return mismatch_count


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

    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_file = os.path.join("logs", f"epoch_stake_compare_{ts}.log")
    logger = Logger(log_file, quiet=args.quiet)

    logger.log("===== Starting Epoch Stake comparison =====")
    logger.log(f"Log file: {os.path.abspath(log_file)}")
    logger.log(f"Epochs: {epochs[0]} -> {epochs[-1]} ({len(epochs)} epochs)")
    logger.log(f"DB Sync URL: {args.dbsync_url}")
    logger.log(f"Yaci Store URL: {args.store_url} (schema: {args.store_schema})")
    if args.delay > 0:
        logger.log(f"Delay between epochs: {args.delay}s")
    logger.log()

    total_mismatches = 0
    epochs_with_mismatch = 0

    for i, epoch in enumerate(epochs):
        logger.log(f"############ Epoch {epoch} - epoch_stake ############")

        count = compare_epoch_stake(
            epoch, args.dbsync_url, args.store_url, args.store_schema, logger, args.max_mismatches
        )

        if count < 0:
            logger.log(f"  Database connection error, skipping epoch {epoch}")
        elif count == 0:
            logger.log(f"  OK - All epoch_stake data matches")
        else:
            epochs_with_mismatch += 1
            total_mismatches += count
            logger.log(f"  MISMATCH: {count} mismatch(es)")

        logger.log()

        # Delay between epochs (skip after last)
        if args.delay > 0 and i < len(epochs) - 1:
            time.sleep(args.delay)

    logger.log("=" * 50)
    logger.log("SUMMARY (epoch_stake):")
    logger.log(f"  Epochs compared     : {len(epochs)}")
    logger.log(f"  Epochs w/ mismatch  : {epochs_with_mismatch}/{len(epochs)}")
    logger.log(f"  Total mismatches    : {total_mismatches}")
    logger.log("=" * 50)

    if total_mismatches > 0:
        logger.log(f"\nSee details at: {os.path.abspath(log_file)}")


if __name__ == "__main__":
    main()
