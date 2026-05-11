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
    python3 compare_all.py --start-epoch 740 --end-epoch 902

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
import re
import shlex
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


def new_child_summary():
    return {
        "log_file": None,
        "epochs_compared": None,
        "epochs_with_mismatch": None,
        "epochs_total": None,
        "total_mismatches": None,
        "reported_errors": 0,
    }


def parse_child_output_line(line, summary):
    stripped = line.strip()

    if stripped.startswith("Log file:"):
        summary["log_file"] = stripped.split(":", 1)[1].strip()
        return

    if stripped.startswith("ERROR:") or "Database connection error" in stripped:
        summary["reported_errors"] += 1
        return

    match = re.match(r"Epochs compared\s*:\s*(\d+)", stripped)
    if match:
        summary["epochs_compared"] = int(match.group(1))
        return

    match = re.match(r"Epochs w/ mismatch\s*:\s*(\d+)\s*/\s*(\d+)", stripped)
    if match:
        summary["epochs_with_mismatch"] = int(match.group(1))
        summary["epochs_total"] = int(match.group(2))
        return

    match = re.match(r"Total mismatches\s*:\s*(\d+)", stripped)
    if match:
        summary["total_mismatches"] = int(match.group(1))


def format_count(value):
    return "n/a" if value is None else str(value)


def format_mismatch_epochs(result):
    summary = result["summary"]
    mismatch_epochs = summary["epochs_with_mismatch"]
    total_epochs = summary["epochs_total"] or summary["epochs_compared"]
    if mismatch_epochs is None or total_epochs is None:
        return "n/a"
    return f"{mismatch_epochs}/{total_epochs}"


def result_status(result):
    summary = result["summary"]
    if result["rc"] != 0 or summary["reported_errors"] > 0:
        return "ERROR"
    if summary["total_mismatches"] is None:
        return "UNKNOWN"
    if summary["total_mismatches"] > 0:
        return "MISMATCH"
    return "OK"


def format_epoch_scope(args):
    if args.epoch is not None:
        return f"epoch {args.epoch}"
    return f"epochs {args.start_epoch} -> {args.end_epoch}"


def default_summary_file(started_at):
    ts = started_at.strftime("%Y%m%d_%H%M%S")
    return os.path.join(HERE, "logs", f"compare_all_summary_{ts}.log")


def resolve_summary_file(path):
    if os.path.isabs(path):
        return path
    return os.path.abspath(path)


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
    cmd = [sys.executable, "-u", os.path.join(HERE, script)]
    cmd += build_epoch_args(args)
    cmd += build_passthrough_args(args)
    cmd += extra_args

    print(f"\n========================================================")
    print(f"  Running: {label}")
    if not args.quiet:
        print(f"  Command: {' '.join(cmd)}")
    print(f"========================================================")

    started = datetime.now()
    summary = new_child_summary()
    process = subprocess.Popen(
        cmd,
        cwd=HERE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
    for line in process.stdout:
        if not args.quiet:
            print(line, end="")
        parse_child_output_line(line, summary)

    rc = process.wait()
    duration = (datetime.now() - started).total_seconds()
    return {
        "label": label,
        "rc": rc,
        "duration": duration,
        "summary": summary,
    }


def calculate_summary_counts(results):
    failed = sum(1 for result in results if result_status(result) == "ERROR")
    mismatched = sum(1 for result in results if result_status(result) == "MISMATCH")
    unknown = sum(1 for result in results if result_status(result) == "UNKNOWN")
    total_mismatches = sum(
        result["summary"]["total_mismatches"]
        for result in results
        if result["summary"]["total_mismatches"] is not None
    )
    known_mismatch_counts = sum(
        1 for result in results if result["summary"]["total_mismatches"] is not None
    )

    return failed, mismatched, unknown, total_mismatches, known_mismatch_counts


def build_overall_summary_text(results, total_duration, started_at, finished_at, command, epoch_scope):
    failed, mismatched, unknown, total_mismatches, known_mismatch_counts = calculate_summary_counts(results)
    lines = []

    lines.append("=" * 100)
    lines.append("FINAL RESULT SUMMARY (compare_all)")
    lines.append("=" * 100)
    lines.append(f"  Started at        : {started_at.isoformat(timespec='seconds')}")
    lines.append(f"  Finished at       : {finished_at.isoformat(timespec='seconds')}")
    lines.append(f"  Command           : {command}")
    lines.append(f"  Epoch scope       : {epoch_scope}")
    lines.append(f"  Total runtime     : {total_duration:.1f}s")
    lines.append(f"  Comparators run   : {len(results)}")
    lines.append(f"  Status counts     : OK={sum(1 for r in results if result_status(r) == 'OK')}, "
                 f"MISMATCH={mismatched}, ERROR={failed}, UNKNOWN={unknown}")
    if known_mismatch_counts == len(results):
        lines.append(f"  Total mismatches  : {total_mismatches}")
    else:
        lines.append(f"  Total mismatches  : {total_mismatches} known ({len(results) - known_mismatch_counts} unknown)")
    lines.append("")
    lines.append(f"  {'Comparator':<40} {'Status':<9} {'Epochs':>8} {'Bad epochs':>11} {'Mismatches':>11} {'RC':>4} {'Time(s)':>8}")
    lines.append(f"  {'-'*40} {'-'*9} {'-'*8} {'-'*11} {'-'*11} {'-'*4} {'-'*8}")
    for result in results:
        summary = result["summary"]
        lines.append(
            f"  {result['label']:<40} "
            f"{result_status(result):<9} "
            f"{format_count(summary['epochs_compared']):>8} "
            f"{format_mismatch_epochs(result):>11} "
            f"{format_count(summary['total_mismatches']):>11} "
            f"{result['rc']:>4} "
            f"{result['duration']:>8.1f}"
        )

    lines.append("")
    lines.append("  Logs:")
    for result in results:
        log_file = result["summary"]["log_file"] or "n/a"
        lines.append(f"    {result['label']}: {log_file}")
    lines.append("=" * 100)

    return "\n".join(lines), failed, mismatched, unknown


def write_summary_file(summary_file, summary_text):
    os.makedirs(os.path.dirname(summary_file), exist_ok=True)
    with open(summary_file, "w") as f:
        f.write(summary_text + "\n")


def print_overall_summary(results, total_duration, started_at, finished_at, command, epoch_scope, summary_file):
    summary_text, failed, mismatched, unknown = build_overall_summary_text(
        results,
        total_duration,
        started_at,
        finished_at,
        command,
        epoch_scope,
    )

    print("\n" + summary_text)
    write_summary_file(summary_file, summary_text)
    print(f"\nFinal summary written to: {summary_file}")
    return failed, mismatched, unknown


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
    parser.add_argument("--quiet", action="store_true", help="Suppress child output and print only compare_all progress/summary")
    parser.add_argument("--max-mismatches", type=int, default=None,
                        help="Limit mismatches printed per epoch in each child script")
    parser.add_argument(
        "--summary-file",
        metavar="FILE",
        help="Write the final summary to this file (default: logs/compare_all_summary_<timestamp>.log)",
    )

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
    command = shlex.join([sys.executable] + sys.argv)
    epoch_scope = format_epoch_scope(args)
    summary_file = resolve_summary_file(args.summary_file) if args.summary_file else default_summary_file(started_at)
    results = []

    for key, script in selected:
        if key == "reward_rest":
            for rtype in reward_types:
                label = f"reward_rest (type={rtype})"
                results.append(run_one(label, script, ["--reward-type", rtype], args))
        else:
            results.append(run_one(key, script, [], args))

    finished_at = datetime.now()
    total_duration = (finished_at - started_at).total_seconds()

    failed, mismatched, unknown = print_overall_summary(
        results,
        total_duration,
        started_at,
        finished_at,
        command,
        epoch_scope,
        summary_file,
    )
    sys.exit(1 if failed or mismatched or unknown else 0)


if __name__ == "__main__":
    main()
