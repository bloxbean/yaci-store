#!/usr/bin/env python3
"""
================================================================================
  compare_drep_active_until.py - Compare drep ACTIVE_UNTIL between DB Sync and Yaci Store
================================================================================

USAGE:
------

1. Install required dependencies:
   pip3 install psycopg2-binary

2. Run the script:

   # Using a config file
   python3 compare_drep_active_until.py --epoch 624 --config config.json

   # Compare a specific epoch
   python3 compare_drep_active_until.py --epoch 624

   # Compare an epoch range (620 to 630)
   python3 compare_drep_active_until.py --start-epoch 620 --end-epoch 630

   # Custom database connections (CLI args override config file)
   python3 compare_drep_active_until.py --epoch 624 \\
       --dbsync-url "postgresql://10.4.10.135:5678/cexplorer" \\
       --dbsync-user dbsync --dbsync-password dbsync \\
       --store-url "postgresql://10.4.10.112:5432/yaci_store" \\
       --store-user yaci --store-password dbpass \\
       --store-schema yaci_store

   # Output to log file only, suppress console output
   python3 compare_drep_active_until.py --epoch 624 --quiet

   # Limit the number of mismatches printed
   python3 compare_drep_active_until.py --epoch 624 --max-mismatches 50
================================================================================
"""

import argparse
import os
import sys
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import Logger, connect, add_common_args, resolve_config, normalize_hash


def compare_active_until(epoch, dbsync_url, store_url, store_schema, logger, max_mismatches):
    dbsync_query = """
        SELECT dh.raw, d.active_until
        FROM drep_distr d
        INNER JOIN drep_hash dh ON dh.id = d.hash_id
        WHERE d.epoch_no = %s AND d.active_until IS NOT NULL
    """
    store_query = """
        SELECT drep_hash, active_until
        FROM drep_dist
        WHERE epoch = %s AND drep_type NOT IN ('ABSTAIN', 'NO_CONFIDENCE')
    """

    dbsync_map = {}
    store_map = {}

    try:
        conn = connect(dbsync_url)
        with conn.cursor() as cur:
            cur.execute(dbsync_query, (epoch,))
            for row in cur.fetchall():
                raw, active_until = row
                h = normalize_hash(raw)
                if h:
                    dbsync_map[h] = active_until
        conn.close()
    except Exception as e:
        logger.error(f"DB Sync query error for epoch {epoch}", e)
        return -1

    try:
        conn = connect(store_url, store_schema)
        with conn.cursor() as cur:
            cur.execute(store_query, (epoch,))
            for row in cur.fetchall():
                drep_hash, active_until = row
                h = normalize_hash(drep_hash)
                if h:
                    store_map[h] = active_until
        conn.close()
    except Exception as e:
        logger.error(f"Yaci Store query error for epoch {epoch}", e)
        return -1

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

    for h, db_val in dbsync_map.items():
        if check_limit():
            break
        if h in store_map:
            store_val = store_map[h]
            if db_val != store_val:
                mismatch_count += 1
                logger.log(f"  Mismatch hash: {h}")
                logger.log(f"    DB Sync   : active_until = {db_val}")
                logger.log(f"    Yaci Store: active_until = {store_val}")
        else:
            mismatch_count += 1
            logger.log(f"  Hash {h} present in DB Sync but MISSING in Yaci Store")

    for h in store_map:
        if check_limit():
            break
        if h not in dbsync_map:
            mismatch_count += 1
            logger.log(f"  Hash {h} present in Yaci Store but MISSING in DB Sync")

    return mismatch_count


def main():
    parser = argparse.ArgumentParser(
        description="Compare drep ACTIVE_UNTIL between DB Sync and Yaci Store.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --epoch 624
  %(prog)s --epoch 624 --config config.json
  %(prog)s --start-epoch 620 --end-epoch 630
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

    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_file = os.path.join("logs", f"drep_compare_active_until_{ts}.log")
    logger = Logger(log_file, quiet=args.quiet)

    logger.log(f"===== Starting drep active_until comparison =====")
    logger.log(f"Log file: {os.path.abspath(log_file)}")
    logger.log(f"Epoch range: {start_epoch} -> {end_epoch}")
    logger.log(f"DB Sync URL: {args.dbsync_url}")
    logger.log(f"Yaci Store URL: {args.store_url} (schema: {args.store_schema})")
    logger.log()

    total_mismatches = 0
    epochs_with_mismatch = 0
    total_epochs = end_epoch - start_epoch + 1

    for epoch in range(start_epoch, end_epoch + 1):
        logger.log(f"############ Epoch {epoch} - active_until ############")

        count = compare_active_until(
            epoch, args.dbsync_url, args.store_url, args.store_schema, logger, args.max_mismatches
        )

        if count < 0:
            logger.log(f"  Database connection error, skipping epoch {epoch}")
        elif count == 0:
            logger.log(f"  OK - All data matches between DB Sync and Yaci Store")
        else:
            epochs_with_mismatch += 1
            total_mismatches += count
            logger.log(f"  MISMATCH: {count} mismatch(es)")

        logger.log()

    logger.log("=" * 50)
    logger.log(f"SUMMARY (active_until):")
    logger.log(f"  Epochs compared     : {total_epochs}")
    logger.log(f"  Epochs w/ mismatch  : {epochs_with_mismatch}/{total_epochs}")
    logger.log(f"  Total mismatches    : {total_mismatches}")
    logger.log("=" * 50)

    if total_mismatches > 0:
        logger.log(f"\nSee details at: {os.path.abspath(log_file)}")


if __name__ == "__main__":
    main()
