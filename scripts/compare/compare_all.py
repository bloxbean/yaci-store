#!/usr/bin/env python3
"""
================================================================================
  compare_all.py - Run every comparator in scripts/compare/ in one go
================================================================================

Runs each individual compare_*.py script sequentially against the same epoch
(or epoch range) and prints a combined summary at the end. Each child script
still writes its own timestamped log under logs/.

USAGE:
------
    # Single epoch
    python3 compare_all.py --epoch 800 --config config.json

    # Epoch range
    python3 compare_all.py --start-epoch 740 --end-epoch 902 --config config.json

    # Skip specific comparators
    python3 compare_all.py --epoch 800 --skip epoch_stake,reward_rest

    # Run only a subset
    python3 compare_all.py --epoch 800 --only adapot,drep_amount

    # Pick reward_rest types to run (default: all three)
    python3 compare_all.py --epoch 1075 --reward-types treasury,reserves
================================================================================
"""

import argparse
import os
import subprocess
import sys
from datetime import datetime

HERE = os.path.dirname(os.path.abspath(__file__))

# (key, script filename, extra args builder)
COMPARATORS = [
    ("adapot",                   "compare_adapot.py"),
    ("epoch_stake",              "compare_epoch_stake.py"),
    ("reward_rest",              "compare_reward_rest.py"),
    ("drep_amount",              "compare_drep_amount.py"),
    ("drep_active_until",        "compare_drep_active_until.py"),
    ("gov_action_proposal_status", "compare_gov_action_proposal_status.py"),
]

REWARD_TYPES_DEFAULT = ["treasury", "reserves", "proposal_refund"]


def build_epoch_args(args):
    if args.epoch is not None:
        return ["--epoch", str(args.epoch)]
    return ["--start-epoch", str(args.start_epoch), "--end-epoch", str(args.end_epoch)]


def build_passthrough_args(args):
    """Forward CLI flags that every child script understands."""
    out = []
    if args.config:
        out += ["--config", args.config]
    if args.dbsync_url:
        out += ["--dbsync-url", args.dbsync_url]
    if args.dbsync_user:
        out += ["--dbsync-user", args.dbsync_user]
    if args.dbsync_password:
        out += ["--dbsync-password", args.dbsync_password]
    if args.store_url:
        out += ["--store-url", args.store_url]
    if args.store_user:
        out += ["--store-user", args.store_user]
    if args.store_password:
        out += ["--store-password", args.store_password]
    if args.store_schema:
        out += ["--store-schema", args.store_schema]
    if args.quiet:
        out += ["--quiet"]
    if args.max_mismatches is not None:
        out += ["--max-mismatches", str(args.max_mismatches)]
    return out


def select_comparators(args):
    keys = [k for k, _ in COMPARATORS]
    if args.only:
        wanted = [k.strip() for k in args.only.split(",") if k.strip()]
        unknown = [k for k in wanted if k not in keys]
        if unknown:
            sys.exit(f"Unknown comparator key(s) in --only: {unknown}. Valid: {keys}")
        return [(k, s) for k, s in COMPARATORS if k in wanted]
    if args.skip:
        skip = {k.strip() for k in args.skip.split(",") if k.strip()}
        unknown = [k for k in skip if k not in keys]
        if unknown:
            sys.exit(f"Unknown comparator key(s) in --skip: {unknown}. Valid: {keys}")
        return [(k, s) for k, s in COMPARATORS if k not in skip]
    return list(COMPARATORS)


def run_one(label, script, extra_args, args):
    cmd = [sys.executable, os.path.join(HERE, script)]
    cmd += build_epoch_args(args)
    cmd += build_passthrough_args(args)
    cmd += extra_args

    print(f"\n========================================================")
    print(f"  Running: {label}")
    print(f"  Command: {' '.join(cmd)}")
    print(f"========================================================")

    started = datetime.now()
    rc = subprocess.call(cmd, cwd=HERE)
    duration = (datetime.now() - started).total_seconds()
    return rc, duration


def main():
    parser = argparse.ArgumentParser(
        description="Run every Yaci Store ↔ DB Sync comparator in one command.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Comparator keys:
  adapot, epoch_stake, reward_rest, drep_amount, drep_active_until,
  gov_action_proposal_status
        """,
    )

    epoch_group = parser.add_mutually_exclusive_group(required=True)
    epoch_group.add_argument("--epoch", type=int, help="Single epoch to compare")
    epoch_group.add_argument("--start-epoch", type=int, help="Start epoch (use with --end-epoch)")
    parser.add_argument("--end-epoch", type=int, help="End epoch (use with --start-epoch)")

    parser.add_argument("--only", help="Comma-separated comparator keys to run (mutually exclusive with --skip)")
    parser.add_argument("--skip", help="Comma-separated comparator keys to skip")
    parser.add_argument(
        "--reward-types",
        default=",".join(REWARD_TYPES_DEFAULT),
        help=f"Comma-separated reward types passed to compare_reward_rest.py (default: {','.join(REWARD_TYPES_DEFAULT)})",
    )

    # Pass-through flags (mirror common.add_common_args)
    parser.add_argument("--config", metavar="FILE", help="Path to JSON config file")
    parser.add_argument("--dbsync-url", help="DB Sync connection URL")
    parser.add_argument("--dbsync-user", help="DB Sync username")
    parser.add_argument("--dbsync-password", help="DB Sync password")
    parser.add_argument("--store-url", help="Yaci Store connection URL")
    parser.add_argument("--store-user", help="Yaci Store username")
    parser.add_argument("--store-password", help="Yaci Store password")
    parser.add_argument("--store-schema", help="Yaci Store schema name")
    parser.add_argument("--quiet", action="store_true", help="Pass --quiet to each child script")
    parser.add_argument("--max-mismatches", type=int, default=None,
                        help="Limit mismatches printed per epoch in each child script")

    args = parser.parse_args()

    if args.only and args.skip:
        parser.error("--only and --skip are mutually exclusive")
    if args.epoch is None:
        if args.start_epoch is None or args.end_epoch is None:
            parser.error("Provide either --epoch or both --start-epoch and --end-epoch")
        if args.end_epoch < args.start_epoch:
            parser.error("--end-epoch must be >= --start-epoch")

    selected = select_comparators(args)
    reward_types = [t.strip() for t in args.reward_types.split(",") if t.strip()]

    started_at = datetime.now()
    results = []  # list of (label, returncode, duration_seconds)

    for key, script in selected:
        if key == "reward_rest":
            for rtype in reward_types:
                label = f"reward_rest (type={rtype})"
                rc, dur = run_one(label, script, ["--reward-type", rtype], args)
                results.append((label, rc, dur))
        else:
            rc, dur = run_one(key, script, [], args)
            results.append((key, rc, dur))

    total_duration = (datetime.now() - started_at).total_seconds()

    print("\n" + "=" * 60)
    print("OVERALL SUMMARY (compare_all)")
    print("=" * 60)
    print(f"  Total runtime : {total_duration:.1f}s")
    print(f"  Comparators   : {len(results)}")
    print()
    print(f"  {'Comparator':<40} {'RC':>4} {'Time(s)':>10}")
    print(f"  {'-'*40} {'-'*4} {'-'*10}")
    failed = 0
    for label, rc, dur in results:
        status = "OK" if rc == 0 else f"RC={rc}"
        if rc != 0:
            failed += 1
        print(f"  {label:<40} {status:>4} {dur:>10.1f}")
    print()
    print(f"  Failures      : {failed}/{len(results)}")
    print("=" * 60)

    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()