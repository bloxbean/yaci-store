#!/usr/bin/env python3
"""
Full CIP-26 regression sweep: enumerate every subject currently in yaci-store's
`cip26_metadata` table, then byte-compare each subject's `standards.cip26` block
between yaci-store and a CF V2 API source.

DB-enumerated (fast and complete) — requires SSH+Postgres access to the host
running yaci-store. For a setup that needs no DB access, use
`verify_against_cf_registry.py` instead (slower, registry-enumerated).

Targets the V2 endpoints only on both sides. CF V1 is intentionally not exercised
because yaci-store does not persist off-chain signatures.

Usage examples
--------------
  # Default: yaci on :8081, CF mirror on :8082 (no rate limit), 16 workers
  python sweep_cip26_full.py

  # Against public CF mainnet (rate-limited — throttle to ~1 req/1.5s)
  python sweep_cip26_full.py --cf-url https://tokens.cardano.org \\
                              --cf-throttle-ms 1500 --workers 1

  # Smoke test on first 100 subjects
  python sweep_cip26_full.py --limit 100

Exit codes
----------
  0  all subjects agree
  1  one or more diverged
  2  setup error (DB enumeration failed, no subjects returned)
"""
from __future__ import annotations

import argparse
import concurrent.futures
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request


def enumerate_subjects(args) -> list[str]:
    """SELECT subject FROM cip26_metadata via ssh+psql."""
    sql = "SELECT subject FROM cip26_metadata;"
    cmd = [
        "ssh", "-o", "ConnectTimeout=5", args.db_host,
        f'PGPASSWORD={args.db_password} psql -h {args.db_pg_host} -U {args.db_user} '
        f'-d {args.db_name} -At -c "{sql}"',
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        print(f"[setup] DB enumeration failed: {result.stderr}", file=sys.stderr)
        sys.exit(2)
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def http_get(url: str, timeout: int = 15) -> tuple[int, str]:
    try:
        with urllib.request.urlopen(url, timeout=timeout) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception:
        return 0, ""


def extract_cip26(body: str) -> dict | None:
    try:
        return json.loads(body).get("subject", {}).get("standards", {}).get("cip26")
    except Exception:
        return None


def compare_one(subject: str, args) -> tuple[str, str, int, int, list[str] | None]:
    if args.cf_throttle_ms > 0:
        time.sleep(args.cf_throttle_ms / 1000.0)

    yaci_url = f"{args.yaci_url}/api/v1/tokens/subject/{subject}?show_cips_details=true"
    cf_url = f"{args.cf_url}/api/v2/subjects/{subject}?show_cips_details=true"

    yc, yb = http_get(yaci_url)
    cc, cb = http_get(cf_url)

    if yc != 200 or cc != 200:
        return subject, "skip", yc, cc, None

    y_cip = extract_cip26(yb)
    c_cip = extract_cip26(cb)

    if y_cip == c_cip:
        return subject, "agree", yc, cc, None

    diffs = []
    keys = sorted(set(y_cip or {}) | set(c_cip or {}))
    for k in keys:
        yv = (y_cip or {}).get(k)
        cv = (c_cip or {}).get(k)
        if yv != cv:
            diffs.append(f"{k}: yaci={yv!r:.120}  cf={cv!r:.120}")
    return subject, "diverge", yc, cc, diffs


def main() -> int:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--yaci-url", default="http://127.0.0.1:8081",
                        help="yaci-store base URL (default: %(default)s)")
    parser.add_argument("--cf-url", default="http://127.0.0.1:8082",
                        help="CF V2 API base URL — mirror or https://tokens.cardano.org (default: %(default)s)")
    parser.add_argument("--workers", type=int, default=16,
                        help="parallel HTTP workers (default: %(default)s; drop to 1 against public CF)")
    parser.add_argument("--limit", type=int, default=None,
                        help="limit number of subjects (smoke test)")
    parser.add_argument("--cf-throttle-ms", type=int, default=0,
                        help="sleep N ms before each CF GET (public CF needs ~1500)")
    parser.add_argument("--db-host", default="mczeladka",
                        help="SSH host to enumerate from (default: %(default)s)")
    parser.add_argument("--db-pg-host", default="localhost",
                        help="Postgres host on the SSH target (default: %(default)s)")
    parser.add_argument("--db-name", default="yaci_store")
    parser.add_argument("--db-user", default="postgres")
    parser.add_argument("--db-password", default="postgres")
    parser.add_argument("--show-diff-limit", type=int, default=20,
                        help="how many diverged subjects to print in detail (default: %(default)s)")
    args = parser.parse_args()

    print(f"[setup] yaci={args.yaci_url}  cf={args.cf_url}  workers={args.workers}")
    subjects = enumerate_subjects(args)
    if not subjects:
        print("[setup] no subjects found in cip26_metadata — nothing to compare", file=sys.stderr)
        return 2
    if args.limit:
        subjects = subjects[: args.limit]
    print(f"[setup] subjects={len(subjects)}")

    stats = {"agree": 0, "diverge": 0, "skip": 0, "skip_codes": {}}
    diverged: list[tuple[str, list[str]]] = []

    start = time.monotonic()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as ex:
        for subj, status, yc, cc, diffs in ex.map(lambda s: compare_one(s, args), subjects):
            stats[status] += 1
            if status == "skip":
                key = f"yaci={yc},cf={cc}"
                stats["skip_codes"][key] = stats["skip_codes"].get(key, 0) + 1
            elif status == "diverge":
                diverged.append((subj, diffs or []))
    elapsed = time.monotonic() - start

    total = stats["agree"] + stats["diverge"] + stats["skip"]
    print()
    print(f"total={total}  agree={stats['agree']}  diverged={stats['diverge']}  "
          f"skip={stats['skip']}  elapsed={elapsed:.1f}s")
    if stats["skip"]:
        print(f"  skip codes: {stats['skip_codes']}")
    if diverged:
        print()
        print(f"first {min(len(diverged), args.show_diff_limit)} of {len(diverged)} diverged subjects:")
        for subj, diffs in diverged[: args.show_diff_limit]:
            print(f"  DIVERGED  {subj}")
            for d in diffs[:5]:
                print(f"     {d}")

    return 0 if stats["diverge"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
