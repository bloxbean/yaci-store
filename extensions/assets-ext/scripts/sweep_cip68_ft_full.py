#!/usr/bin/env python3
"""
Full CIP-68 fungible-token regression sweep: enumerate every distinct CIP-68
reference NFT whose history contains a label=333 row in yaci-store's
`cip68_metadata` table, derive the user-token (FT) subject for each, and
byte-compare each subject's `standards.cip68` block between yaci-store and a
CF V2 API source.

DB-enumerated (fast and complete) — requires SSH+Postgres access to the host
running yaci-store. For a setup that needs no DB access, use
`verify_against_cf_registry.py` instead (slower, registry-enumerated; misses
CIP-68 tokens that don't have a CF GitHub registry entry).

Targets V2 endpoints on both sides. CF V1 is intentionally not exercised.

Usage examples
--------------
  # Default: yaci on :8081, CF mirror on :8082 (no rate limit), 16 workers
  python sweep_cip68_ft_full.py

  # Against public CF mainnet (rate-limited — throttle, drop workers to 1)
  python sweep_cip68_ft_full.py --cf-url https://tokens.cardano.org \\
                                 --cf-throttle-ms 1500 --workers 1

  # Smoke test on first 100 subjects
  python sweep_cip68_ft_full.py --limit 100

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


REF_NFT_PREFIX = "000643b0"
FT_PREFIX = "0014df10"


def enumerate_ft_subjects(args) -> list[tuple[str, str]]:
    """Return distinct (policy_id, ref_nft_asset_name) pairs that have at least
    one label=333 row in cip68_metadata."""
    sql = (
        "SELECT policy_id, asset_name FROM ("
        "  SELECT DISTINCT policy_id, asset_name FROM cip68_metadata"
        f" WHERE asset_name LIKE '{REF_NFT_PREFIX}%' AND label = 333"
        ") s;"
    )
    cmd = [
        "ssh", "-o", "ConnectTimeout=5", args.db_host,
        f'PGPASSWORD={args.db_password} psql -h {args.db_pg_host} -U {args.db_user} '
        f"-d {args.db_name} -At -F'|' -c \"{sql}\"",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        print(f"[setup] DB enumeration failed: {result.stderr}", file=sys.stderr)
        sys.exit(2)
    rows = []
    for line in result.stdout.splitlines():
        line = line.strip()
        if not line:
            continue
        pol, asset = line.split("|", 1)
        rows.append((pol, asset))
    return rows


def http_get(url: str, timeout: int = 15) -> tuple[int, str]:
    try:
        with urllib.request.urlopen(url, timeout=timeout) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception:
        return 0, ""


def extract_cip68(body: str) -> dict | None:
    try:
        return json.loads(body).get("subject", {}).get("standards", {}).get("cip68")
    except Exception:
        return None


def compare_one(pol: str, ref_asset: str, args) -> tuple[str, str, int, int, list[str] | None]:
    if args.cf_throttle_ms > 0:
        time.sleep(args.cf_throttle_ms / 1000.0)

    # Reference-NFT asset name → user-facing FT asset name (label-prefix swap)
    ft_asset = FT_PREFIX + ref_asset[len(REF_NFT_PREFIX):]
    subject = pol + ft_asset

    yaci_url = f"{args.yaci_url}/api/v1/tokens/subject/{subject}?show_cips_details=true"
    cf_url = f"{args.cf_url}/api/v2/subjects/{subject}?show_cips_details=true"

    yc, yb = http_get(yaci_url)
    cc, cb = http_get(cf_url)

    if yc != 200 or cc != 200:
        return subject, "skip", yc, cc, None

    y_cip = extract_cip68(yb)
    c_cip = extract_cip68(cb)

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
    parser.add_argument("--db-pg-host", default="localhost")
    parser.add_argument("--db-name", default="yaci_store")
    parser.add_argument("--db-user", default="postgres")
    parser.add_argument("--db-password", default="postgres")
    parser.add_argument("--show-diff-limit", type=int, default=20,
                        help="how many diverged subjects to print in detail (default: %(default)s)")
    args = parser.parse_args()

    print(f"[setup] yaci={args.yaci_url}  cf={args.cf_url}  workers={args.workers}")
    rows = enumerate_ft_subjects(args)
    if not rows:
        print("[setup] no FT reference NFTs found — nothing to compare", file=sys.stderr)
        return 2
    if args.limit:
        rows = rows[: args.limit]
    print(f"[setup] subjects={len(rows)}")

    stats = {"agree": 0, "diverge": 0, "skip": 0, "skip_codes": {}}
    diverged: list[tuple[str, list[str]]] = []

    start = time.monotonic()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as ex:
        for subj, status, yc, cc, diffs in ex.map(lambda r: compare_one(r[0], r[1], args), rows):
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
