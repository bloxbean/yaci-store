#!/usr/bin/env python3
"""
================================================================================
  compare_gov_action_proposal_status.py
  Compare governance action proposal status between DB Sync and Yaci Store
================================================================================

USAGE:
------

1. Install required dependencies:
   pip3 install psycopg2-binary

2. Run the script:

   # Using a config file
   python3 compare_gov_action_proposal_status.py --epoch 515 --config config.json

   # Compare a specific epoch
   python3 compare_gov_action_proposal_status.py --epoch 515

   # Compare an epoch range (510 to 520)
   python3 compare_gov_action_proposal_status.py --start-epoch 510 --end-epoch 520
   python3 compare_gov_action_proposal_status.py --start-epoch 492 --end-epoch 1048 --config config.json

   # Custom database connections (CLI args override config file)
   python3 compare_gov_action_proposal_status.py --epoch 515 \\
       --dbsync-url "postgresql://10.4.10.135:5678/cexplorer" \\
       --dbsync-user dbsync --dbsync-password dbsync \\
       --store-url "postgresql://10.4.10.112:5432/yaci_store" \\
       --store-user yaci --store-password dbpass \\
       --store-schema yaci_store

   # Output to log file only, suppress console output
   python3 compare_gov_action_proposal_status.py --epoch 515 --quiet

   # Limit the number of mismatches printed
   python3 compare_gov_action_proposal_status.py --epoch 515 --max-mismatches 50
================================================================================
"""

import argparse
import os
import sys
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from common import Logger, connect, add_common_args, resolve_config, normalize_hash


# Yaci-Store has only 3 statuses: ACTIVE, RATIFIED, EXPIRED.
# The gov_action_proposal_status table is a snapshot at the start of each epoch
# (ProposalStateProcessor writes for currentEpoch = stakeSnapshotEvent.epoch + 1).
#
# Lifecycle of proposal P submitted at epoch X (per ProposalCollectionService logic):
#   - epoch <= X       : no row
#   - epoch X+1..R     : status = ACTIVE (CONTINUE)
#   - epoch R          : status = RATIFIED or EXPIRED (only once)
#   - epoch >= R+1     : no row (RATIFIED/EXPIRED from the prior snapshot is used
#                        for drop calculation, not re-written)
#
# Meanwhile dbsync stores 1 row per proposal with epoch columns:
#   ratified_epoch  = epoch ratified
#   enacted_epoch   = ratified_epoch + 1 (epoch enacted)
#   expired_epoch   = epoch expired
#   dropped_epoch   = epoch dropped
#
# Mapping (same epoch, NO offset):
#   yaci epoch E, status=RATIFIED  <->  ratified_epoch == E
#   yaci epoch E, status=EXPIRED   <->  expired_epoch  == E
#   yaci epoch E, status=ACTIVE    <->  submit_epoch < E and every end column > E or NULL
#   yaci has no row at epoch E     <->  submit_epoch >= E, or
#                                       (any end column <= E - 1 with RATIFIED/EXPIRED),
#                                       or dropped_epoch <= E, or enacted_epoch <= E
def derive_dbsync_status(epoch, ratified_epoch, enacted_epoch, dropped_epoch, expired_epoch):
    """
    Return ("STATUS", note) for yaci-store epoch E.
    None -> yaci-store should not have a row at this epoch.
    """
    if expired_epoch is not None and epoch == expired_epoch:
        return ("EXPIRED", f"expired_epoch={expired_epoch}")
    if ratified_epoch is not None and epoch == ratified_epoch:
        return ("RATIFIED", f"ratified_epoch={ratified_epoch}")

    # Already ended before this epoch -> yaci should have no row
    if expired_epoch is not None and epoch > expired_epoch:
        return (None, f"already expired at {expired_epoch}")
    if ratified_epoch is not None and epoch > ratified_epoch:
        return (None, f"already ratified at {ratified_epoch} (enacted at {enacted_epoch})")
    if enacted_epoch is not None and epoch >= enacted_epoch:
        return (None, f"already enacted at {enacted_epoch}")
    if dropped_epoch is not None and epoch >= dropped_epoch:
        return (None, f"dropped at {dropped_epoch}")

    return ("ACTIVE", None)


def compare_proposal_status(epoch, dbsync_url, store_url, store_schema, logger, max_mismatches):
    # Fetch every proposal that YACI-STORE COULD have a row for at epoch E:
    #   - submit_epoch < E (first row appears at submit+1)
    #   - not ended before E:
    #       expired_epoch  IS NULL OR expired_epoch  >= E
    #       ratified_epoch IS NULL OR ratified_epoch >= E   (still RATIFIED at E, not after)
    #       enacted_epoch  IS NULL OR enacted_epoch  >  E   (enacted_epoch=E means already enacted)
    #       dropped_epoch  IS NULL OR dropped_epoch  >  E
    dbsync_query = """
        SELECT encode(tx.hash, 'hex') AS tx_hash,
               gap.index,
               gap.type::text,
               gap.ratified_epoch,
               gap.enacted_epoch,
               gap.dropped_epoch,
               gap.expired_epoch,
               gap.expiration,
               b.epoch_no AS submit_epoch
        FROM gov_action_proposal gap
        JOIN tx ON tx.id = gap.tx_id
        JOIN block b ON b.id = tx.block_id
        WHERE b.epoch_no < %(epoch)s
          AND COALESCE(gap.expired_epoch,  2147483647) >= %(epoch)s
          AND COALESCE(gap.ratified_epoch, 2147483647) >= %(epoch)s
          AND COALESCE(gap.enacted_epoch,  2147483647) >  %(epoch)s
          AND COALESCE(gap.dropped_epoch,  2147483647) >  %(epoch)s
    """

    store_query = """
        SELECT gov_action_tx_hash, gov_action_index, type, status
        FROM gov_action_proposal_status
        WHERE epoch = %s
    """

    dbsync_results = {}  # (tx_hash, index) -> dict
    store_results = {}   # (tx_hash, index) -> dict

    try:
        conn = connect(dbsync_url)
        with conn.cursor() as cur:
            cur.execute(dbsync_query, {"epoch": epoch})
            for row in cur.fetchall():
                (tx_hash, index, gtype, ratified_epoch, enacted_epoch,
                 dropped_epoch, expired_epoch, expiration, submit_epoch) = row
                h = normalize_hash(tx_hash)
                status, note = derive_dbsync_status(
                    epoch, ratified_epoch, enacted_epoch, dropped_epoch, expired_epoch
                )
                dbsync_results[(h, int(index))] = {
                    "type": gtype,
                    "status": status,
                    "note": note,
                    "ratified_epoch": ratified_epoch,
                    "enacted_epoch": enacted_epoch,
                    "dropped_epoch": dropped_epoch,
                    "expired_epoch": expired_epoch,
                    "expiration": expiration,
                    "submit_epoch": submit_epoch,
                }
        conn.close()
    except Exception as e:
        logger.error(f"DB Sync query error for epoch {epoch}", e)
        return -1

    try:
        conn = connect(store_url, store_schema)
        with conn.cursor() as cur:
            cur.execute(store_query, (epoch,))
            for row in cur.fetchall():
                tx_hash, gov_index, gtype, status = row
                h = normalize_hash(tx_hash)
                store_results[(h, int(gov_index))] = {
                    "type": str(gtype) if gtype is not None else None,
                    "status": str(status) if status is not None else None,
                }
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

    # 1) Iterate dbsync -> find missing / wrong status in yaci-store
    for key, dbs in dbsync_results.items():
        if check_limit():
            break
        tx_hash, index = key
        expected_status = dbs["status"]

        if expected_status is None:
            # Proposal already ended before this epoch - yaci-store should not have a row.
            if key in store_results:
                mismatch_count += 1
                logger.log(f"  Proposal present in Yaci Store but DB Sync shows it has ended ({dbs['note']})")
                logger.log(f"    tx_hash={tx_hash}, index={index}, type={dbs['type']}")
                logger.log(f"    Yaci Store status : {store_results[key]['status']}")
                logger.log(f"    DB Sync epochs    : ratified={dbs['ratified_epoch']}, enacted={dbs['enacted_epoch']}, "
                           f"dropped={dbs['dropped_epoch']}, expired={dbs['expired_epoch']}")
            continue

        if key not in store_results:
            mismatch_count += 1
            logger.log(f"  Proposal present in DB Sync but MISSING in Yaci Store")
            logger.log(f"    tx_hash={tx_hash}, index={index}, type={dbs['type']}")
            logger.log(f"    DB Sync status    : {expected_status} ({dbs['note'] or 'derived'})")
            logger.log(f"    DB Sync epochs    : submit={dbs['submit_epoch']}, ratified={dbs['ratified_epoch']}, "
                       f"enacted={dbs['enacted_epoch']}, dropped={dbs['dropped_epoch']}, "
                       f"expired={dbs['expired_epoch']}, expiration={dbs['expiration']}")
            continue

        store_status = store_results[key]["status"]
        if store_status != expected_status:
            mismatch_count += 1
            logger.log(f"  Mismatch status proposal tx_hash={tx_hash}, index={index}, type={dbs['type']}")
            logger.log(f"    DB Sync status    : {expected_status} ({dbs['note'] or 'derived'})")
            logger.log(f"    Yaci Store status : {store_status}")
            logger.log(f"    DB Sync epochs    : ratified={dbs['ratified_epoch']}, enacted={dbs['enacted_epoch']}, "
                       f"dropped={dbs['dropped_epoch']}, expired={dbs['expired_epoch']}")

    # 2) Iterate yaci-store -> find rows extra to dbsync
    for key, sres in store_results.items():
        if check_limit():
            break
        if key in dbsync_results:
            continue
        tx_hash, index = key
        mismatch_count += 1
        logger.log(f"  Proposal present in Yaci Store but MISSING in DB Sync")
        logger.log(f"    tx_hash={tx_hash}, index={index}, type={sres['type']}")
        logger.log(f"    Yaci Store status : {sres['status']}")

    return mismatch_count


def main():
    parser = argparse.ArgumentParser(
        description="Compare governance action proposal status between DB Sync and Yaci Store.",
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

    parser.add_argument("--max-mismatches", type=int, default=None,
                        help="Limit mismatches printed per epoch (0 = unlimited)")

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
    log_file = os.path.join("logs", f"gov_action_proposal_status_compare_{ts}.log")
    logger = Logger(log_file, quiet=args.quiet)

    logger.log(f"===== Starting gov action proposal status comparison =====")
    logger.log(f"Log file: {os.path.abspath(log_file)}")
    logger.log(f"Epoch range: {start_epoch} -> {end_epoch}")
    logger.log(f"DB Sync URL: {args.dbsync_url}")
    logger.log(f"Yaci Store URL: {args.store_url} (schema: {args.store_schema})")
    logger.log()

    total_mismatches = 0
    epochs_with_mismatch = 0
    total_epochs = end_epoch - start_epoch + 1

    for epoch in range(start_epoch, end_epoch + 1):
        logger.log(f"############ Epoch {epoch} - gov action proposal status ############")

        count = compare_proposal_status(
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
    logger.log(f"SUMMARY (gov action proposal status):")
    logger.log(f"  Epochs compared     : {total_epochs}")
    logger.log(f"  Epochs w/ mismatch  : {epochs_with_mismatch}/{total_epochs}")
    logger.log(f"  Total mismatches    : {total_mismatches}")
    logger.log("=" * 50)

    if total_mismatches > 0:
        logger.log(f"\nSee details at: {os.path.abspath(log_file)}")


if __name__ == "__main__":
    main()
