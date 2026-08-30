"""Shared utilities for the standalone governance voting-stats verifier."""

import csv
import json
import os
import sys
import time
from urllib.parse import quote, urlparse, urlunparse


DEFAULT_STORE_URL = "postgresql://localhost:5432/yaci_store"
DEFAULT_STORE_SCHEMA = "yaci_store"
TOOL_DIR = os.path.dirname(os.path.abspath(__file__))


def load_config(config_path):
    with open(config_path, "r", encoding="utf-8") as config_file:
        return json.load(config_file)


def resolve_path(path, base_dir):
    if path is None:
        return None
    path = os.path.expanduser(path)
    if os.path.isabs(path):
        return path
    return os.path.abspath(os.path.join(base_dir, path))


def apply_credentials(url, user, password):
    if not url or (user is None and password is None):
        return url
    parsed = urlparse(url)
    new_user = user if user is not None else (parsed.username or "")
    new_password = password if password is not None else (parsed.password or "")

    host = parsed.hostname or ""
    if parsed.port:
        host = f"{host}:{parsed.port}"

    userinfo = ""
    if new_user or new_password:
        userinfo = quote(new_user, safe="")
        if new_password:
            userinfo += ":" + quote(new_password, safe="")
        userinfo += "@"
    return urlunparse(parsed._replace(netloc=f"{userinfo}{host}"))


def redact_url(url):
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


class Logger:
    def __init__(self, log_file, quiet=False):
        self.log_file = log_file
        self.quiet = quiet
        os.makedirs(os.path.dirname(log_file), exist_ok=True)
        with open(log_file, "w", encoding="utf-8") as output:
            output.write("")

    def log(self, message=""):
        if not self.quiet:
            print(message)
        with open(self.log_file, "a", encoding="utf-8") as output:
            output.write(message + "\n")

    def error(self, message, exc=None):
        error_message = f"ERROR: {message}"
        if exc:
            error_message += f"\n  {type(exc).__name__}: {exc}"
        print(error_message, file=sys.stderr)
        with open(self.log_file, "a", encoding="utf-8") as output:
            output.write(error_message + "\n")


class MismatchCsvWriter:
    def __init__(self, output_dir, sample_name, fieldnames, max_rows=0):
        self.output_dir = output_dir
        self.sample_name = sample_name
        self.fieldnames = fieldnames
        self.max_rows = int(max_rows or 0)
        self.rows_written = 0
        self.path = None
        self._file = None
        self._writer = None

    def ensure_open(self):
        if self._writer is not None or self.output_dir is None:
            return
        os.makedirs(self.output_dir, exist_ok=True)
        self.path = os.path.join(self.output_dir, f"{self.sample_name}.csv")
        self._file = open(self.path, "w", encoding="utf-8", newline="")
        self._writer = csv.DictWriter(
            self._file,
            fieldnames=self.fieldnames,
            extrasaction="ignore",
        )
        self._writer.writeheader()

    def write(self, row):
        if self.max_rows and self.rows_written >= self.max_rows:
            return False
        self.ensure_open()
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


def render_summary(
    results,
    tool_name,
    started_at,
    finished_at,
    command,
    epoch_scope,
    report_dir,
    log_file,
):
    counts = status_counts(results)
    total_mismatches = sum(result["total_mismatches"] for result in results)
    total_duration = (finished_at - started_at).total_seconds()

    lines = [
        "=" * 100,
        f"FINAL RESULT SUMMARY ({tool_name})",
        "=" * 100,
        f"  Started at        : {started_at.isoformat(timespec='seconds')}",
        f"  Finished at       : {finished_at.isoformat(timespec='seconds')}",
        f"  Command           : {command}",
        f"  Epoch scope       : {epoch_scope}",
        f"  Total runtime     : {total_duration:.1f}s",
        f"  Comparators run   : {len(results)}",
        (
            "  Status counts     : "
            f"OK={counts['OK']}, MISMATCH={counts['MISMATCH']}, "
            f"ERROR={counts['ERROR']}"
        ),
        f"  Total mismatches  : {total_mismatches}",
        "",
        (
            f"  {'Comparator':<40} {'Status':<9} {'Epochs':>8} "
            f"{'Bad epochs':>11} {'Mismatches':>11} {'Errors':>7} {'Time(s)':>8}"
        ),
        (
            f"  {'-'*40} {'-'*9} {'-'*8} {'-'*11} "
            f"{'-'*11} {'-'*7} {'-'*8}"
        ),
    ]
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
    lines.extend(
        (
            "",
            f"  Report directory  : {report_dir}",
            f"  Log file          : {log_file}",
            "=" * 100,
        )
    )
    return "\n".join(lines)


def summary_payload(
    results,
    started_at,
    finished_at,
    command,
    epoch_scope,
    report_dir,
    log_file,
    settings=None,
):
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
    with open(summary_log, "w", encoding="utf-8") as output:
        output.write(summary_text + "\n")
    with open(summary_json, "w", encoding="utf-8") as output:
        json.dump(payload, output, indent=2)
        output.write("\n")
    return summary_log, summary_json


def write_json(path, payload):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as output:
        json.dump(payload, output, indent=2)
        output.write("\n")


def run_report_dir(args, prefix, run_id):
    if args.report_dir:
        return args.report_dir
    return os.path.join(args.reports_dir, f"{prefix}_{run_id}")


def connect(url, schema=None):
    try:
        import psycopg2
    except ImportError:
        print("ERROR: psycopg2 library is not installed.")
        print("Run: pip3 install psycopg2-binary")
        sys.exit(1)

    connection = psycopg2.connect(url)
    if schema:
        with connection.cursor() as cursor:
            cursor.execute(f"SET search_path TO {schema}")
    return connection
