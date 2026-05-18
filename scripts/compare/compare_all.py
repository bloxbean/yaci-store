#!/usr/bin/env python3
"""
================================================================================
  compare_all.py - Run every comparator in scripts/compare/ in one go
================================================================================

Runs each individual compare_*.py script sequentially against the same epoch
(or epoch range), collects structured child results, and writes one combined
report directory.

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
import json
import os
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from common import (  # noqa: E402
    exit_code,
    load_config,
    redact_url,
    render_summary,
    resolve_path,
    summary_payload,
    write_report_files,
)


COMPARATORS = [
    ("adapot", "compare_adapot.py"),
    ("epoch_stake", "compare_epoch_stake.py"),
    ("reward_rest", "compare_reward_rest.py"),
    ("drep_amount", "compare_drep_amount.py"),
    ("drep_active_until", "compare_drep_active_until.py"),
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


def format_epoch_scope(args):
    if args.epoch is not None:
        return f"epoch {args.epoch}"
    return f"epochs {args.start_epoch} -> {args.end_epoch}"


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
    if args.quiet:
        out += ["--quiet"]
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


def safe_label(label):
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", label).strip("_")


def load_child_result(label, rc, duration, result_json, fallback_summary):
    if os.path.exists(result_json):
        with open(result_json, "r", encoding="utf-8") as f:
            payload = json.load(f)
        result = payload.get("result")
        if result is None and payload.get("results"):
            result = payload["results"][0]
        if result is not None:
            result["return_code"] = rc
            result["process_duration_seconds"] = round(duration, 3)
            return result

    errors = 1 if rc != 0 or fallback_summary["reported_errors"] else 0
    total_mismatches = fallback_summary["total_mismatches"] or 0
    status = "ERROR" if errors else ("MISMATCH" if total_mismatches else "OK")
    return {
        "label": label,
        "status": status,
        "epochs_compared": fallback_summary["epochs_compared"] or 0,
        "epochs_with_mismatch": fallback_summary["epochs_with_mismatch"] or 0,
        "total_mismatches": total_mismatches,
        "errors": errors,
        "mismatch_files": [],
        "log_file": fallback_summary["log_file"],
        "duration_seconds": round(duration, 3),
        "return_code": rc,
        "structured_result_missing": True,
    }


def run_one(label, script, extra_args, args, report_dir, child_results_dir):
    result_json = os.path.join(child_results_dir, f"{safe_label(label)}.json")
    cmd = [sys.executable, "-u", os.path.join(HERE, script)]
    cmd += build_epoch_args(args)
    cmd += build_passthrough_args(args)
    cmd += extra_args
    cmd += [
        "--report-dir",
        report_dir,
        "--result-json",
        result_json,
        "--logs-dir",
        args.logs_dir,
    ]

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
    return load_child_result(label, rc, duration, result_json, summary)


def resolve_output_dirs(args):
    reports_dir = os.path.join(HERE, "reports")
    logs_dir = os.path.join(HERE, "logs")
    file_cfg = {}

    if args.config:
        config_dir = os.path.dirname(os.path.abspath(args.config))
        file_cfg = load_config(args.config)
        if "reports_dir" in file_cfg:
            reports_dir = resolve_path(file_cfg["reports_dir"], config_dir)
        if "logs_dir" in file_cfg:
            logs_dir = resolve_path(file_cfg["logs_dir"], config_dir)

    if args.reports_dir:
        reports_dir = resolve_path(args.reports_dir, os.getcwd())
    if args.logs_dir:
        logs_dir = resolve_path(args.logs_dir, os.getcwd())

    return reports_dir, logs_dir, file_cfg


def resolve_summary_file(path, logs_dir, run_id):
    if path:
        return resolve_path(path, os.getcwd())
    return os.path.join(logs_dir, f"compare_all_summary_{run_id}.log")


def write_summary_file(summary_file, summary_text):
    os.makedirs(os.path.dirname(summary_file), exist_ok=True)
    with open(summary_file, "w", encoding="utf-8") as f:
        f.write(summary_text + "\n")


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

    parser.add_argument("--config", metavar="FILE", help="Path to JSON config file")
    parser.add_argument("--dbsync-url", help="DB Sync connection URL")
    parser.add_argument("--dbsync-user", help="DB Sync username")
    parser.add_argument("--dbsync-password", help="DB Sync password")
    parser.add_argument("--store-url", help="Yaci Store connection URL")
    parser.add_argument("--store-user", help="Yaci Store username")
    parser.add_argument("--store-password", help="Yaci Store password")
    parser.add_argument("--store-schema", help="Yaci Store schema name")
    parser.add_argument("--reports-dir", help="Directory for structured reports")
    parser.add_argument("--logs-dir", help="Directory for text logs")
    parser.add_argument("--quiet", action="store_true", help="Suppress child output and print only compare_all progress/summary")
    parser.add_argument("--max-mismatches", type=int, default=None,
                        help="Limit mismatch samples printed/written per epoch in each child script")
    parser.add_argument(
        "--summary-file",
        metavar="FILE",
        help="Also write the final text summary to this file (default: logs/compare_all_summary_<timestamp>.log)",
    )

    args = parser.parse_args()

    if args.only and args.skip:
        parser.error("--only and --skip are mutually exclusive")
    if args.epoch is None:
        if args.start_epoch is None or args.end_epoch is None:
            parser.error("Provide either --epoch or both --start-epoch and --end-epoch")
        if args.end_epoch < args.start_epoch:
            parser.error("--end-epoch must be >= --start-epoch")

    try:
        args.reports_dir, args.logs_dir, file_cfg = resolve_output_dirs(args)
    except (OSError, json.JSONDecodeError) as e:
        parser.error(str(e))

    selected = select_comparators(args)
    reward_types = [t.strip() for t in args.reward_types.split(",") if t.strip()]

    started_at = datetime.now()
    run_id = started_at.strftime("%Y%m%d_%H%M%S")
    command = shlex.join([sys.executable] + sys.argv)
    epoch_scope = format_epoch_scope(args)
    report_dir = os.path.join(args.reports_dir, f"compare_all_{run_id}")
    summary_file = resolve_summary_file(args.summary_file, args.logs_dir, run_id)
    os.makedirs(os.path.join(report_dir, "mismatches"), exist_ok=True)

    print(f"Structured report directory: {report_dir}")
    print(f"Text summary file: {summary_file}")

    child_results_dir = tempfile.mkdtemp(prefix="compare_all_child_results_")
    results = []
    try:
        for key, script in selected:
            if key == "reward_rest":
                for rtype in reward_types:
                    label = f"reward_rest (type={rtype})"
                    results.append(
                        run_one(label, script, ["--reward-type", rtype], args, report_dir, child_results_dir)
                    )
            else:
                results.append(run_one(key, script, [], args, report_dir, child_results_dir))
    finally:
        shutil.rmtree(child_results_dir, ignore_errors=True)

    finished_at = datetime.now()
    summary_text = render_summary(
        results,
        "compare_all",
        started_at,
        finished_at,
        command,
        epoch_scope,
        report_dir,
        summary_file,
    )
    payload = summary_payload(
        results,
        started_at,
        finished_at,
        command,
        {
            "start_epoch": args.epoch if args.epoch is not None else args.start_epoch,
            "end_epoch": args.epoch if args.epoch is not None else args.end_epoch,
        },
        report_dir,
        summary_file,
        {
            "selected": [key for key, _ in selected],
            "reward_types": reward_types,
            "dbsync_url": redact_url(args.dbsync_url or file_cfg.get("dbsync_url")),
            "store_url": redact_url(args.store_url or file_cfg.get("store_url")),
            "store_schema": args.store_schema or file_cfg.get("store_schema", "yaci_store"),
            "max_mismatches": (
                args.max_mismatches
                if args.max_mismatches is not None
                else file_cfg.get("max_mismatches", 0)
            ),
        },
    )
    summary_log, summary_json = write_report_files(report_dir, summary_text, payload)
    write_summary_file(summary_file, summary_text)

    print("\n" + summary_text)
    print()
    print(f"Summary log written to: {summary_log}")
    print(f"Summary JSON written to: {summary_json}")
    print(f"Text summary written to: {summary_file}")
    sys.exit(exit_code(results))


if __name__ == "__main__":
    main()
