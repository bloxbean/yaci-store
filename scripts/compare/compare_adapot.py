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
       --dbsync-url "postgresql://dbsync:dbsync@10.4.10.135:5678/cexplorer" \
       --store-url "postgresql://yaci:dbpass@10.4.10.112:5432/yaci_store" \
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
import sys
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import Logger, connect, add_common_args, resolve_config


# ============================================================
# Compare: adapot (treasury + reserves)
# ============================================================
def compare_adapot(epoch, dbsync_url, store_url, store_schema, logger):
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
                return 0
        conn.close()
    except Exception as e:
        logger.error(f"DB Sync query error for epoch {epoch}", e)
        return -1

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
                return 0
        conn.close()
    except Exception as e:
        logger.error(f"Yaci Store query error for epoch {epoch}", e)
        return -1

    # --- Compare ---
    mismatch_count = 0

    if dbsync_treasury != store_treasury:
        mismatch_count += 1
        logger.log(f"  Mismatch TREASURY: DB Sync={dbsync_treasury}, Yaci Store={store_treasury}")

    if dbsync_reserves != store_reserves:
        mismatch_count += 1
        logger.log(f"  Mismatch RESERVES: DB Sync={dbsync_reserves}, Yaci Store={store_reserves}")

    return mismatch_count


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

    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_file = os.path.join("logs", f"adapot_compare_{ts}.log")
    logger = Logger(log_file, quiet=args.quiet)

    logger.log("===== Starting AdaPot comparison =====")
    logger.log(f"Log file: {os.path.abspath(log_file)}")
    logger.log(f"Epoch range: {start_epoch} -> {end_epoch}")
    logger.log(f"DB Sync URL: {args.dbsync_url}")
    logger.log(f"Yaci Store URL: {args.store_url} (schema: {args.store_schema})")
    logger.log()

    total_mismatches = 0
    epochs_with_mismatch = 0
    total_epochs = end_epoch - start_epoch + 1

    for epoch in range(start_epoch, end_epoch + 1):
        logger.log(f"############ Epoch {epoch} - adapot ############")

        count = compare_adapot(epoch, args.dbsync_url, args.store_url, args.store_schema, logger)

        if count < 0:
            logger.log(f"  Database connection error, skipping epoch {epoch}")
        elif count == 0:
            logger.log(f"  OK - Treasury and Reserves match between DB Sync and Yaci Store")
        else:
            epochs_with_mismatch += 1
            total_mismatches += count
            logger.log(f"  MISMATCH: {count} mismatch(es)")

        logger.log()

    logger.log("=" * 50)
    logger.log("SUMMARY (adapot):")
    logger.log(f"  Epochs compared     : {total_epochs}")
    logger.log(f"  Epochs w/ mismatch  : {epochs_with_mismatch}/{total_epochs}")
    logger.log(f"  Total mismatches    : {total_mismatches}")
    logger.log("=" * 50)

    if total_mismatches > 0:
        logger.log(f"\nSee details at: {os.path.abspath(log_file)}")


if __name__ == "__main__":
    main()
