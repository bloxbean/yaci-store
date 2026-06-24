"""Shared utilities for compare scripts."""

import argparse
import csv
import json
import os
import sys
import time
from urllib.parse import urlparse, urlunparse, quote


# ============================================================
# Default connection config
# ============================================================
DEFAULT_DBSYNC_URL = "postgresql://dbsync:dbsync@10.4.10.135:5678/cexplorer"
DEFAULT_STORE_URL = "postgresql://yaci:dbpass@10.4.10.112:5432/yaci_store"
DEFAULT_STORE_SCHEMA = "yaci_store"
TOOL_DIR = os.path.dirname(os.path.abspath(__file__))
PATH_KEYS = {"reports_dir", "logs_dir", "report_dir"}


# ============================================================
# Config loading
# ============================================================
def load_config(config_path):
    """Load config from a JSON file. Returns a dict."""
    with open(config_path, "r", encoding="utf-8") as f:
        return json.load(f)


def resolve_path(path, base_dir):
    if path is None:
        return None
    path = os.path.expanduser(path)
    if os.path.isabs(path):
        return path
    return os.path.abspath(os.path.join(base_dir, path))


def add_common_args(parser):
    """Add common arguments (config file, DB connections, output) to an ArgumentParser."""
    parser.add_argument("--config", metavar="FILE", help="Path to JSON config file (CLI args override file values)")
    parser.add_argument("--dbsync-url", help="DB Sync connection URL")
    parser.add_argument("--dbsync-user", help="DB Sync username (override URL userinfo)")
    parser.add_argument("--dbsync-password", help="DB Sync password (override URL userinfo)")
    parser.add_argument("--store-url", help="Yaci Store connection URL")
    parser.add_argument("--store-user", help="Yaci Store username (override URL userinfo)")
    parser.add_argument("--store-password", help="Yaci Store password (override URL userinfo)")
    parser.add_argument("--store-schema", help="Yaci Store schema name")
    parser.add_argument("--reports-dir", help="Directory for structured reports")
    parser.add_argument("--logs-dir", help="Directory for text logs")
    parser.add_argument("--quiet", action="store_true", default=None, help="Write to log file only, do not print to console")
    parser.add_argument("--report-dir", help=argparse.SUPPRESS)
    parser.add_argument("--result-json", help=argparse.SUPPRESS)


def resolve_config(args):
    """
    Merge config from: defaults -> config file -> CLI args.
    CLI args always win. Config file overrides defaults.
    Modifies args in-place and returns it.
    """
    # Start with defaults
    defaults = {
        "dbsync_url": DEFAULT_DBSYNC_URL,
        "dbsync_user": None,
        "dbsync_password": None,
        "store_url": DEFAULT_STORE_URL,
        "store_user": None,
        "store_password": None,
        "store_schema": DEFAULT_STORE_SCHEMA,
        "reports_dir": os.path.join(TOOL_DIR, "reports"),
        "logs_dir": os.path.join(TOOL_DIR, "logs"),
        "report_dir": None,
        "result_json": None,
        "quiet": False,
        "max_mismatches": 0,
        "delay": 0,
    }

    # Layer config file on top of defaults
    if args.config:
        config_dir = os.path.dirname(os.path.abspath(args.config))
        file_cfg = load_config(args.config)
        for key in PATH_KEYS:
            if key in file_cfg:
                file_cfg[key] = resolve_path(file_cfg[key], config_dir)
        defaults.update(file_cfg)

    # Map CLI arg names (with hyphens) to config keys (with underscores)
    cli_mapping = {
        "dbsync_url": "dbsync_url",
        "dbsync_user": "dbsync_user",
        "dbsync_password": "dbsync_password",
        "store_url": "store_url",
        "store_user": "store_user",
        "store_password": "store_password",
        "store_schema": "store_schema",
        "reports_dir": "reports_dir",
        "logs_dir": "logs_dir",
        "report_dir": "report_dir",
        "result_json": "result_json",
        "quiet": "quiet",
        "max_mismatches": "max_mismatches",
        "delay": "delay",
    }

    for attr, cfg_key in cli_mapping.items():
        cli_val = getattr(args, attr, None)
        if cli_val is None:
            # CLI arg not provided -> use config/default
            setattr(args, attr, defaults.get(cfg_key, None))
        elif cfg_key in PATH_KEYS or cfg_key == "result_json":
            setattr(args, attr, resolve_path(cli_val, os.getcwd()))

    # Apply user/pass overrides to URLs if provided
    args.dbsync_url = apply_credentials(args.dbsync_url, args.dbsync_user, args.dbsync_password)
    args.store_url = apply_credentials(args.store_url, args.store_user, args.store_password)
    args.max_mismatches = int(args.max_mismatches or 0)
    args.reports_dir = resolve_path(args.reports_dir, os.getcwd())
    args.logs_dir = resolve_path(args.logs_dir, os.getcwd())
    args.report_dir = resolve_path(args.report_dir, os.getcwd())
    args.result_json = resolve_path(args.result_json, os.getcwd())

    return args


def apply_credentials(url, user, password):
    """Override the userinfo component of a postgresql URL with user/password if provided."""
    if not url or (user is None and password is None):
        return url
    parsed = urlparse(url)
    existing_user = parsed.username or ""
    existing_pass = parsed.password or ""
    new_user = user if user is not None else existing_user
    new_pass = password if password is not None else existing_pass

    host = parsed.hostname or ""
    if parsed.port:
        host = f"{host}:{parsed.port}"

    userinfo = ""
    if new_user or new_pass:
        userinfo = quote(new_user, safe="")
        if new_pass:
            userinfo += ":" + quote(new_pass, safe="")
        userinfo += "@"

    netloc = f"{userinfo}{host}"
    return urlunparse(parsed._replace(netloc=netloc))


def redact_url(url):
    """Return a connection URL safe for logs and JSON reports."""
    if not url:
        return url
    parsed = urlparse(url)
    if parsed.password is None:
        return url

    user = quote(parsed.username or "", safe="")
    host = parsed.hostname or ""
    if parsed.port:
        host = f"{host}:{parsed.port}"
    return urlunparse(parsed._replace(netloc=f"{user}:****@{host}"))


# ============================================================
# Logger
# ============================================================
class Logger:
    def __init__(self, log_file, quiet=False):
        self.log_file = log_file
        self.quiet = quiet
        os.makedirs(os.path.dirname(log_file), exist_ok=True)
        with open(log_file, "w", encoding="utf-8") as f:
            f.write("")

    def log(self, message=""):
        if not self.quiet:
            print(message)
        with open(self.log_file, "a", encoding="utf-8") as f:
            f.write(message + "\n")

    def error(self, message, exc=None):
        err_msg = f"ERROR: {message}"
        if exc:
            err_msg += f"\n  {type(exc).__name__}: {exc}"
        print(err_msg, file=sys.stderr)
        with open(self.log_file, "a", encoding="utf-8") as f:
            f.write(err_msg + "\n")


# ============================================================
# Reporting helpers
# ============================================================
class MismatchCsvWriter:
    def __init__(self, mismatch_dir, sample_name, fieldnames, max_rows=0):
        self.mismatch_dir = mismatch_dir
        self.sample_name = sample_name
        self.fieldnames = fieldnames
        self.max_rows = int(max_rows or 0)
        self.rows_written = 0
        self.path = None
        self._file = None
        self._writer = None

    def _ensure_open(self):
        if self._writer is not None or self.mismatch_dir is None:
            return
        os.makedirs(self.mismatch_dir, exist_ok=True)
        self.path = os.path.join(self.mismatch_dir, f"{self.sample_name}.csv")
        self._file = open(self.path, "w", encoding="utf-8", newline="")
        self._writer = csv.DictWriter(self._file, fieldnames=self.fieldnames, extrasaction="ignore")
        self._writer.writeheader()

    def write(self, row):
        if self.max_rows and self.rows_written >= self.max_rows:
            return False
        self._ensure_open()
        if self._writer is None:
            return False
        self._writer.writerow({key: row.get(key) for key in self.fieldnames})
        self.rows_written += 1
        return True

    def close(self):
        if self._file is not None:
            self._file.close()
            self._file = None
            self._writer = None


class MismatchRecorder:
    def __init__(self, logger, csv_writer=None, max_mismatches=0):
        self.logger = logger
        self.csv_writer = csv_writer
        self.max_mismatches = int(max_mismatches or 0)
        self.count = 0
        self.samples_emitted = 0
        self.truncated_logged = False

    def _can_emit_sample(self):
        return self.max_mismatches <= 0 or self.samples_emitted < self.max_mismatches

    def record(self, row, log_lines):
        self.count += 1
        if self._can_emit_sample():
            for line in log_lines:
                self.logger.log(line)
            if self.csv_writer is not None:
                self.csv_writer.write(row)
            self.samples_emitted += 1
            return

        if not self.truncated_logged:
            self.logger.log(
                f"  ... (reached limit of {self.max_mismatches} mismatch samples, "
                "continuing count only)"
            )
            self.truncated_logged = True

    def finish(self):
        if self.csv_writer is not None:
            self.csv_writer.close()
            return self.count, self.csv_writer.path
        return self.count, None


def new_result(label):
    return {
        "label": label,
        "status": "OK",
        "epochs_compared": 0,
        "epochs_with_mismatch": 0,
        "total_mismatches": 0,
        "errors": 0,
        "mismatch_files": [],
        "log_file": None,
        "duration_seconds": 0.0,
    }


def finish_result(result, started_at):
    result["duration_seconds"] = round(time.time() - started_at, 3)
    if result["errors"]:
        result["status"] = "ERROR"
    elif result["total_mismatches"]:
        result["status"] = "MISMATCH"
    else:
        result["status"] = "OK"
    return result


def status_counts(results):
    return {
        "OK": sum(1 for result in results if result["status"] == "OK"),
        "MISMATCH": sum(1 for result in results if result["status"] == "MISMATCH"),
        "ERROR": sum(1 for result in results if result["status"] == "ERROR"),
    }


def render_summary(results, tool_name, started_at, finished_at, command, epoch_scope,
                   report_dir, log_file):
    counts = status_counts(results)
    total_mismatches = sum(result["total_mismatches"] for result in results)
    total_duration = (finished_at - started_at).total_seconds()

    lines = []
    lines.append("=" * 100)
    lines.append(f"FINAL RESULT SUMMARY ({tool_name})")
    lines.append("=" * 100)
    lines.append(f"  Started at        : {started_at.isoformat(timespec='seconds')}")
    lines.append(f"  Finished at       : {finished_at.isoformat(timespec='seconds')}")
    lines.append(f"  Command           : {command}")
    lines.append(f"  Epoch scope       : {epoch_scope}")
    lines.append(f"  Total runtime     : {total_duration:.1f}s")
    lines.append(f"  Comparators run   : {len(results)}")
    lines.append(
        "  Status counts     : "
        f"OK={counts['OK']}, MISMATCH={counts['MISMATCH']}, ERROR={counts['ERROR']}"
    )
    lines.append(f"  Total mismatches  : {total_mismatches}")
    lines.append("")
    lines.append(
        f"  {'Comparator':<40} {'Status':<9} {'Epochs':>8} "
        f"{'Bad epochs':>11} {'Mismatches':>11} {'Errors':>7} {'Time(s)':>8}"
    )
    lines.append(
        f"  {'-'*40} {'-'*9} {'-'*8} {'-'*11} {'-'*11} {'-'*7} {'-'*8}"
    )
    for result in results:
        bad_epochs = f"{result['epochs_with_mismatch']}/{result['epochs_compared']}"
        lines.append(
            f"  {result['label']:<40} "
            f"{result['status']:<9} "
            f"{result['epochs_compared']:>8} "
            f"{bad_epochs:>11} "
            f"{result['total_mismatches']:>11} "
            f"{result['errors']:>7} "
            f"{result['duration_seconds']:>8.1f}"
        )
    lines.append("")
    lines.append(f"  Report directory  : {report_dir}")
    lines.append(f"  Log file          : {log_file}")
    lines.append("=" * 100)
    return "\n".join(lines)


def summary_payload(results, started_at, finished_at, command, epoch_scope, report_dir,
                    log_file, settings=None):
    return {
        "started_at": started_at.isoformat(timespec="seconds"),
        "finished_at": finished_at.isoformat(timespec="seconds"),
        "command": command,
        "epoch_scope": epoch_scope,
        "settings": settings or {},
        "status_counts": status_counts(results),
        "total_mismatches": sum(result["total_mismatches"] for result in results),
        "results": results,
        "report_dir": report_dir,
        "log_file": log_file,
    }


def write_report_files(report_dir, summary_text, payload):
    os.makedirs(report_dir, exist_ok=True)
    summary_log = os.path.join(report_dir, "summary.log")
    summary_json = os.path.join(report_dir, "summary.json")
    with open(summary_log, "w", encoding="utf-8") as f:
        f.write(summary_text + "\n")
    with open(summary_json, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2)
        f.write("\n")
    return summary_log, summary_json


def write_json(path, payload):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2)
        f.write("\n")


def exit_code(results):
    counts = status_counts(results)
    if counts["ERROR"]:
        return 2
    if counts["MISMATCH"]:
        return 1
    return 0


def run_report_dir(args, prefix, run_id):
    if args.report_dir:
        return args.report_dir
    return os.path.join(args.reports_dir, f"{prefix}_{run_id}")


# ============================================================
# DB helpers
# ============================================================
def normalize_hash(h):
    """Normalize drep/pool hash: lowercase hex, strip 0x prefix, accept bytes."""
    if h is None:
        return None
    if isinstance(h, (bytes, bytearray, memoryview)):
        h = bytes(h).hex()
    h = str(h)
    if h.startswith("0x") or h.startswith("0X"):
        h = h[2:]
    return h.lower()


def connect(url, schema=None):
    try:
        import psycopg2
    except ImportError:
        print("ERROR: psycopg2 library is not installed.")
        print("Run: pip3 install psycopg2-binary")
        sys.exit(1)

    conn = psycopg2.connect(url)
    if schema:
        with conn.cursor() as cur:
            cur.execute(f"SET search_path TO {schema}")
    return conn
