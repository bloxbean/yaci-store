#!/usr/bin/env python3
"""
Verify a running yaci-store assets-ext against the CF preprod token registry.

Both APIs claim CF V2 compatibility (yaci-store explicitly so), so for any
given subject they MUST return identical responses. Any divergence — at the
schema level OR in field values — is a contract bug worth reporting.

Workflow
--------
1. Maintain a local clone of the upstream registry source so we have a stable
   list of subjects to test against. (Used only for enumeration — the script
   does NOT compare API responses to the registry JSON itself; the registry
   schema differs from the V2 wire format and the two services are the
   contract surface.) On first run the repo is cloned; subsequent runs
   fast-forward via `git pull`.
2. For each subject:
   - If --check cf or --check both: GET CF V2, classify as 200 / 404 / error.
   - If --check yaci or --check both: GET yaci, classify same way.
   - If both checked AND both 200: byte-level diff of the responses.
3. Per-subject log line + final summary:
   - per-service presence (matched / missing / errors)
   - contract agreement (agree / diverged)

Why no V1 comparison
--------------------
CF V1 returns each property wrapped with the off-chain signatures from the
registry JSON (`{"value": "...", "signatures": [...], "sequenceNumber": N}`).
yaci-store does not persist signatures, so a faithful V1 comparison is not
possible. V2 is the canonical merged-values endpoint and is what consumers
actually use.

Reaching yaci-store via SSH
---------------------------
yaci-store is assumed to be running on the host reachable as `ssh rosetta-preprod`
on port 8080. By default this script spawns an SSH tunnel for the duration of
its run; pass --no-tunnel if you've set one up yourself (e.g. in a separate
terminal: `ssh -L 8080:localhost:8080 -N rosetta-preprod`).

Usage
-----
  python3 verify_assets_ext_cip26.py                       # both, auto-tunnel
  python3 verify_assets_ext_cip26.py --check cf            # CF only (no SSH tunnel)
  python3 verify_assets_ext_cip26.py --check yaci          # yaci only
  python3 verify_assets_ext_cip26.py --limit 25            # smoke test (first 25)
  python3 verify_assets_ext_cip26.py --subject <hex>       # debug one subject
  python3 verify_assets_ext_cip26.py --no-tunnel           # external tunnel
  python3 verify_assets_ext_cip26.py --no-pull             # skip git pull (offline)

Exit codes
----------
  0  all subjects match
  1  one or more subjects differ
  2  setup error (tunnel failed, GitHub unreachable, yaci-store down, ...)
"""

from __future__ import annotations

import argparse
import atexit
import json
import os
import pathlib
import re
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from typing import Any

# CIP-26 spec: subject = policyId (28 bytes / 56 hex) + optional assetName
# (0-32 bytes / 0-64 hex), total 56-120 lowercase hex chars. CF and yaci-store
# both reject anything else; we filter client-side so junk entries in the
# upstream registry don't pollute the run.
SUBJECT_REGEX = re.compile(r"^[0-9a-f]{56,120}$")

# Per-network config. The two registries differ in three ways:
#   - repo URL (testnet under input-output-hk; mainnet under cardano-foundation)
#   - registry subdir name ("registry/" vs "mappings/")
#   - CF V2 API host (preprod vs mainnet)
# The dedupe-by-inner-subject logic in list_subjects_from_clone() works for both —
# preprod has ~90% filename≠subject (per PR #87), mainnet 0%, but the algorithm is
# correct in both cases.
NETWORKS = {
    "preprod": {
        "repo_url": "https://github.com/input-output-hk/metadata-registry-testnet.git",
        "registry_subdir": "registry",
        "cache_subdir": "metadata-registry-testnet",
        "cf_base": "https://preprod.tokens.cardano.org",
    },
    "mainnet": {
        "repo_url": "https://github.com/cardano-foundation/cardano-token-registry.git",
        "registry_subdir": "mappings",
        "cache_subdir": "cardano-token-registry",
        "cf_base": "https://tokens.cardano.org",
    },
}
DEFAULT_NETWORK = "preprod"
DEFAULT_YACI_BASE = "http://localhost:8080"
DEFAULT_SSH_HOST = "rosetta-preprod"
DEFAULT_REMOTE_PORT = 8080
DEFAULT_LOCAL_PORT = 8080
HTTP_TIMEOUT_SECS = 15


# ----- HTTP helpers ----------------------------------------------------------

HTTP_RETRY_ATTEMPTS = 5
HTTP_RETRY_BACKOFFS = (1.0, 2.0, 4.0, 8.0, 16.0)  # seconds before attempts 2-6 (exponential)

# CF mainnet (tokens.cardano.org) is fronted by AWS ELB which rate-limits
# aggressively. Empirically: ~183 requests / 3 minutes triggers HTTP 429 with
# no Retry-After header. CF preprod is tolerant by comparison. Sleeping a small
# amount between CF GETs caps the request rate well under the limit.
# Set via --cf-throttle-ms (default 0 = no throttle, suitable for preprod).
CF_THROTTLE_SECS = 0.0


def _set_cf_throttle(seconds: float) -> None:
    global CF_THROTTLE_SECS
    CF_THROTTLE_SECS = max(0.0, seconds)


def http_get_json(url: str, headers: dict | None = None, throttle: float = 0.0) -> tuple[int, Any]:
    """
    Returns (status_code, body_or_None).

    A 404 returns (404, None) immediately (no terminal answer).
    For 5xx, 429 (rate limit), and network-level failures, retries up to
    HTTP_RETRY_ATTEMPTS times with exponential backoff. 429 honours the
    Retry-After header when present. The final failure raises (caller catches
    it as a fetch error). For non-retryable 4xx (other than 404 and 429), the
    body is parsed if JSON and returned alongside the status.

    `throttle`: seconds to sleep BEFORE the request (rate-limit prevention,
    not retry backoff). Apply only to the side that's rate-limited (CF).
    """
    if throttle > 0:
        time.sleep(throttle)
    req = urllib.request.Request(url, headers=headers or {})
    last_exc: Exception | None = None

    for attempt in range(HTTP_RETRY_ATTEMPTS):
        try:
            with urllib.request.urlopen(req, timeout=HTTP_TIMEOUT_SECS) as r:
                return r.status, json.load(r)
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return 404, None
            # Retry on 5xx and 429. For 429, prefer the Retry-After header if present.
            should_retry = (500 <= e.code < 600) or e.code == 429
            if should_retry and attempt < HTTP_RETRY_ATTEMPTS - 1:
                if e.code == 429:
                    retry_after = e.headers.get("Retry-After") if e.headers else None
                    try:
                        sleep_for = float(retry_after) if retry_after else HTTP_RETRY_BACKOFFS[attempt]
                    except (TypeError, ValueError):
                        sleep_for = HTTP_RETRY_BACKOFFS[attempt]
                    # Cap at 60s so a hostile Retry-After doesn't stall the run forever.
                    sleep_for = min(sleep_for, 60.0)
                else:
                    sleep_for = HTTP_RETRY_BACKOFFS[attempt]
                time.sleep(sleep_for)
                last_exc = e
                continue
            # Final non-2xx — return status + body so caller can decide.
            body_text = e.read().decode("utf-8", errors="replace") if e.fp else ""
            try:
                return e.code, json.loads(body_text)
            except Exception:
                return e.code, None
        except (urllib.error.URLError, TimeoutError, socket.timeout) as e:
            last_exc = e
            if attempt < HTTP_RETRY_ATTEMPTS - 1:
                time.sleep(HTTP_RETRY_BACKOFFS[attempt])
                continue
            raise

    # Unreachable in practice — the loop either returns or raises — but keep
    # mypy/static analyzers happy.
    raise last_exc if last_exc else RuntimeError("http_get_json: unreachable")


def ensure_registry_clone(cache_dir: pathlib.Path, repo_url: str, no_pull: bool) -> pathlib.Path:
    """
    Ensures cache_dir contains an up-to-date clone of the registry repo.
    Clones on first run, fast-forwards on subsequent runs (unless --no-pull).
    """
    cache_dir.parent.mkdir(parents=True, exist_ok=True)
    git_dir = cache_dir / ".git"

    if git_dir.is_dir():
        if no_pull:
            print(f"[setup] using cached registry clone at {cache_dir} (no pull)")
            return cache_dir
        print(f"[setup] updating registry clone at {cache_dir} (git pull)")
        subprocess.run(
            ["git", "-C", str(cache_dir), "pull", "--ff-only", "--quiet"],
            check=True,
        )
        return cache_dir

    print(f"[setup] cloning {repo_url} → {cache_dir}")
    subprocess.run(
        ["git", "clone", "--depth", "1", "--quiet", repo_url, str(cache_dir)],
        check=True,
    )
    return cache_dir


def list_subjects_from_clone(repo_dir: pathlib.Path, registry_subdir: str) -> list[str]:
    """
    Returns the deduplicated list of real subjects from the local registry clone.

    Filenames are NOT always a reliable subject source — preprod has ~90%
    filename≠subject mismatch (per PR #87), mainnet has 0%. We always read
    each JSON, extract the inner `subject` value (the actual on-chain
    identifier), and deduplicate; on mainnet that's a no-op, on preprod it
    collapses 5,526 files down to ~519 real subjects.

    Files that fail to parse, or have no `subject` field, are skipped with a
    warning.
    """
    registry_dir = repo_dir / registry_subdir
    if not registry_dir.is_dir():
        raise RuntimeError(f"{registry_subdir}/ directory not found in {repo_dir}")

    subjects: set[str] = set()
    skipped_unparseable = 0
    invalid_subjects: set[str] = set()
    for path in registry_dir.iterdir():
        if not (path.is_file() and path.suffix == ".json"):
            continue
        try:
            inner = json.loads(path.read_text()).get("subject")
        except Exception:
            skipped_unparseable += 1
            continue
        if not isinstance(inner, str) or not inner:
            skipped_unparseable += 1
            continue
        if not SUBJECT_REGEX.match(inner):
            # Junk entries in the upstream registry — too short, too long, or
            # non-hex. CF and yaci both reject these; comparing them produces
            # noise. Track and log but exclude.
            invalid_subjects.add(inner)
            continue
        subjects.add(inner)

    if skipped_unparseable:
        print(f"[warn] skipped {skipped_unparseable} registry file(s) with "
              f"missing/invalid `subject` field", file=sys.stderr)
    if invalid_subjects:
        print(f"[setup] filtered {len(invalid_subjects)} subject(s) that don't "
              f"match the CIP-26 spec (56-120 lowercase hex chars):",
              file=sys.stderr)
        for s in sorted(invalid_subjects):
            preview = s if len(s) <= 60 else s[:57] + "..."
            print(f"          {preview}", file=sys.stderr)
    return sorted(subjects)


# ----- SSH tunnel ------------------------------------------------------------

def port_is_open(host: str, port: int, timeout: float = 0.5) -> bool:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except OSError:
        return False


def open_ssh_tunnel(ssh_host: str, local_port: int, remote_port: int) -> subprocess.Popen:
    """Spawns `ssh -L local:localhost:remote -N <host>` and waits for it to be ready."""
    if port_is_open("127.0.0.1", local_port):
        raise RuntimeError(
            f"Port {local_port} is already in use locally. "
            f"Pass --no-tunnel if you already have a tunnel running, "
            f"or pass --local-port to choose a different local port."
        )

    cmd = [
        "ssh",
        "-o", "ExitOnForwardFailure=yes",
        "-o", "ServerAliveInterval=30",
        "-L", f"{local_port}:localhost:{remote_port}",
        "-N", ssh_host,
    ]
    print(f"[setup] opening SSH tunnel: {' '.join(cmd)}")
    proc = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    atexit.register(_cleanup_tunnel, proc)

    # Wait up to 10 s for the tunnel to come up.
    deadline = time.time() + 10
    while time.time() < deadline:
        if proc.poll() is not None:
            stderr = proc.stderr.read().decode("utf-8", errors="replace") if proc.stderr else ""
            raise RuntimeError(f"SSH exited unexpectedly: {stderr.strip()}")
        if port_is_open("127.0.0.1", local_port):
            return proc
        time.sleep(0.2)
    proc.terminate()
    raise RuntimeError(f"SSH tunnel to {ssh_host} did not come up within 10s")


def _cleanup_tunnel(proc: subprocess.Popen) -> None:
    if proc.poll() is None:
        proc.terminate()
        try:
            proc.wait(timeout=3)
        except subprocess.TimeoutExpired:
            proc.kill()


# ----- Pre-flight checks -----------------------------------------------------

def preflight(yaci_base: str, cf_base: str, check_cf: bool, check_yaci: bool) -> None:
    if check_yaci:
        # Accept any HTTP response from yaci-store — even 503 means "alive but
        # at least one health indicator is DOWN", which is fine for our
        # purposes because the API endpoints still serve in that state.
        health_url = f"{yaci_base}/actuator/health"
        print(f"[check] yaci-store    : GET {health_url}")
        try:
            status, body = http_get_json(health_url)
        except urllib.error.HTTPError as e:
            body_text = e.read().decode("utf-8", errors="replace") if e.fp else ""
            try:
                body = json.loads(body_text)
                health_status = body.get("status", "?")
            except Exception:
                health_status = "?"
            print(f"          HTTP {e.code}, health={health_status!r} — continuing "
                  f"(API endpoints are independent of aggregate health)")
        else:
            print(f"          HTTP {status}, "
                  f"status={(body or {}).get('status')!r}")

    if check_cf:
        print(f"[check] CF API        : GET {cf_base}/api/v2/subjects")
        # Use a known-bogus subject so we get a deterministic 404 fast — proves
        # the host responds at all without depending on a specific entry.
        status, _ = http_get_json(f"{cf_base}/api/v2/subjects/00")
        if status not in (200, 404):
            raise RuntimeError(f"CF API unexpected status: {status}")
        print("          reachable")


# ----- Diff logic ------------------------------------------------------------

DIFF_VALUE_MAX_CHARS = 80


def _short(v: Any) -> str:
    """repr() but truncated for readability — base64 logos otherwise blow up the output."""
    s = repr(v)
    if len(s) > DIFF_VALUE_MAX_CHARS:
        return s[: DIFF_VALUE_MAX_CHARS - 3] + "..."
    return s


def subset_diff(expected: Any, actual: Any, label_actual: str, path: str = "") -> list[str]:
    """
    Returns a list of human-readable diff strings.

    Asymmetric: every field in `expected` must appear in `actual` with the same
    value. Fields in `actual` but not in `expected` are tolerated (services
    may add e.g. `type`, `extensions.cip113`).

    `label_actual` is used in the diff message ("missing in <label>", etc.).
    """
    diffs: list[str] = []

    if isinstance(expected, dict) and isinstance(actual, dict):
        for key in sorted(expected):
            sub = f"{path}.{key}" if path else key
            if key not in actual:
                diffs.append(f"  {sub}: missing in {label_actual} "
                             f"(registry has {_short(expected[key])})")
            else:
                diffs.extend(subset_diff(expected[key], actual[key], label_actual, sub))
        return diffs

    if isinstance(expected, list) and isinstance(actual, list):
        if len(expected) != len(actual):
            diffs.append(f"  {path}: list length differs "
                         f"(registry={len(expected)}, {label_actual}={len(actual)})")
            return diffs
        for i, (e_item, a_item) in enumerate(zip(expected, actual)):
            diffs.extend(subset_diff(e_item, a_item, label_actual, f"{path}[{i}]"))
        return diffs

    if type(expected) != type(actual):
        diffs.append(f"  {path}: type differs (registry={type(expected).__name__} "
                     f"{_short(expected)}, {label_actual}={type(actual).__name__} "
                     f"{_short(actual)})")
        return diffs

    if expected != actual:
        diffs.append(f"  {path}: registry={_short(expected)} vs "
                     f"{label_actual}={_short(actual)}")
    return diffs


# ----- Per-subject comparison -----------------------------------------------

@dataclass
class ServiceResult:
    """
    Per-service presence tally — does the service have a 200 response for each
    subject the registry says exists?
    """
    matched: list[str] = field(default_factory=list)            # 200 OK
    missing: list[str] = field(default_factory=list)            # 404 (registry has it, service doesn't)
    fetch_errors: list[tuple[str, str]] = field(default_factory=list)


@dataclass
class ContractResult:
    """
    CF-vs-yaci contract agreement tally — when both services return 200 for
    the same subject, are the responses byte-equivalent? Any diff is a
    contract bug since both APIs claim CF V2 compatibility.
    """
    matched: list[str] = field(default_factory=list)
    diverged: list[tuple[str, list[str]]] = field(default_factory=list)


@dataclass
class Cip68Result:
    """
    CIP-68 sub-tally — derived from `standards.cip68` blocks in the
    show_cips_details responses. We only count subjects where AT LEAST ONE
    side reports CIP-68 data; the rest of the universe is CIP-26-only and
    out of scope here.

    `cf_only` / `yaci_only` flag tokens that one service knows as CIP-68 but
    the other doesn't — typically chain-sync gaps, not contract bugs.
    """
    agree: list[str] = field(default_factory=list)
    diverged: list[tuple[str, list[str]]] = field(default_factory=list)
    cf_only: list[str] = field(default_factory=list)
    yaci_only: list[str] = field(default_factory=list)


@dataclass
class Result:
    cf: ServiceResult = field(default_factory=ServiceResult)
    yaci: ServiceResult = field(default_factory=ServiceResult)
    contract: ContractResult = field(default_factory=ContractResult)
    cip68: Cip68Result = field(default_factory=Cip68Result)


def _fetch(url: str, label: str, subject: str, bucket: ServiceResult
           ) -> tuple[str | None, Any]:
    """
    Returns (status_token, body). status_token is one of
    "OK" | "MISSING" | None (None means a fetch error was already recorded).
    Applies CF_THROTTLE_SECS only to the CF side (yaci is local, no rate limit).
    """
    throttle = CF_THROTTLE_SECS if label == "cf" else 0.0
    try:
        status, body = http_get_json(url, throttle=throttle)
    except Exception as e:
        bucket.fetch_errors.append((subject, str(e)))
        return None, None
    if status not in (200, 404):
        bucket.fetch_errors.append((subject, f"HTTP {status}"))
        return None, None
    if status == 404:
        bucket.missing.append(subject)
        return "MISSING", None
    bucket.matched.append(subject)
    return "OK", body


def compare_subject(
    subject: str,
    yaci_base: str,
    cf_base: str,
    result: Result,
    check_cf: bool,
    check_yaci: bool,
) -> None:
    cf_token, cf_body = (None, None)
    yaci_token, yaci_body = (None, None)

    # show_cips_details=true exposes the per-CIP raw blocks under
    # subject.standards.{cip26,cip68}, which is what we need for the CIP-68
    # comparison. The merged metadata at the top level still reflects the
    # query priority and stays comparable byte-for-byte.
    if check_cf:
        cf_url = f"{cf_base}/api/v2/subjects/{subject}?show_cips_details=true"
        cf_token, cf_body = _fetch(cf_url, "cf", subject, result.cf)
    if check_yaci:
        yaci_url = f"{yaci_base}/api/v1/tokens/subject/{subject}?show_cips_details=true"
        yaci_token, yaci_body = _fetch(yaci_url, "yaci", subject, result.yaci)

    cf_label = (f"cf={cf_token}" if check_cf and cf_token else
                "cf=ERR" if check_cf else "cf=skipped")
    yaci_label = (f"yaci={yaci_token}" if check_yaci and yaci_token else
                  "yaci=ERR" if check_yaci else "yaci=skipped")

    contract_label = ""
    diffs: list[str] = []
    cip68_label = ""
    cip68_diffs: list[str] = []
    if check_cf and check_yaci and cf_token == "OK" and yaci_token == "OK":
        # Both APIs return 200 → they MUST agree byte-for-byte (both claim CF
        # V2 compat). Any diff is a contract bug.
        diffs = subset_diff(cf_body, yaci_body, "yaci")
        if diffs:
            result.contract.diverged.append((subject, diffs))
            contract_label = f"   DIVERGED({len(diffs)})"
        else:
            result.contract.matched.append(subject)
            contract_label = "   AGREE"

        # CIP-68 sub-comparison: pull standards.cip68 from each side and diff.
        # Only count subjects where at least one side reports CIP-68 data.
        cf_cip68 = _extract_cip68(cf_body)
        yaci_cip68 = _extract_cip68(yaci_body)
        cip68_label, cip68_diffs = _compare_cip68(subject, cf_cip68, yaci_cip68, result.cip68)

    print(f"  [{subject[:16]}…] {cf_label:<14} {yaci_label:<16}{contract_label:<22}{cip68_label}")
    for line in diffs:
        print(line)
    for line in cip68_diffs:
        print(f"   [cip68]{line}")


def _extract_cip68(body: Any) -> Any:
    """Pull subject.standards.cip68 out of a V2 show_cips_details response, or None."""
    if not isinstance(body, dict):
        return None
    subject = body.get("subject")
    if not isinstance(subject, dict):
        return None
    standards = subject.get("standards")
    if not isinstance(standards, dict):
        return None
    cip68 = standards.get("cip68")
    return cip68 if isinstance(cip68, dict) else None


def _compare_cip68(subject: str, cf_cip68: Any, yaci_cip68: Any,
                   bucket: Cip68Result) -> tuple[str, list[str]]:
    """
    Compares the CIP-68 standards block between CF and yaci.
    Returns (label, diff_lines). label is empty when neither side has CIP-68
    data; otherwise one of "  cip68=AGREE / DIVERGED(n) / cf-only / yaci-only".
    """
    has_cf = cf_cip68 is not None
    has_yaci = yaci_cip68 is not None
    if not has_cf and not has_yaci:
        return "", []
    if has_cf and not has_yaci:
        bucket.cf_only.append(subject)
        return "  cip68=cf-only", []
    if has_yaci and not has_cf:
        bucket.yaci_only.append(subject)
        return "  cip68=yaci-only", []
    # Both sides have CIP-68 — must agree byte-for-byte.
    diffs = subset_diff(cf_cip68, yaci_cip68, "yaci")
    if diffs:
        bucket.diverged.append((subject, diffs))
        return f"  cip68=DIVERGED({len(diffs)})", diffs
    bucket.agree.append(subject)
    return "  cip68=AGREE", []


# ----- Main ------------------------------------------------------------------

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--network", choices=tuple(NETWORKS.keys()), default=DEFAULT_NETWORK,
                        help=f"Which Cardano network to verify against (default: {DEFAULT_NETWORK}). "
                             f"Switches the CF V2 API host AND the registry repo.")
    parser.add_argument("--yaci-url", default=DEFAULT_YACI_BASE,
                        help=f"Base URL for yaci-store (default: {DEFAULT_YACI_BASE})")
    parser.add_argument("--ssh-host", default=DEFAULT_SSH_HOST,
                        help=f"SSH host for tunnel (default: {DEFAULT_SSH_HOST})")
    parser.add_argument("--remote-port", type=int, default=DEFAULT_REMOTE_PORT,
                        help=f"Remote port to forward (default: {DEFAULT_REMOTE_PORT})")
    parser.add_argument("--local-port", type=int, default=DEFAULT_LOCAL_PORT,
                        help=f"Local port for the tunnel (default: {DEFAULT_LOCAL_PORT})")
    parser.add_argument("--no-tunnel", action="store_true",
                        help="Don't open an SSH tunnel; assume yaci-url is reachable as-is")
    parser.add_argument("--limit", type=int, default=None,
                        help="Only check the first N subjects (smoke test)")
    parser.add_argument("--subject", default=None,
                        help="Compare just one specific subject (skips registry listing)")
    parser.add_argument("--repo-cache", default=None,
                        help="Local clone path for the registry "
                             "(default: <script_dir>/.cache/<repo-name-for-network>)")
    parser.add_argument("--no-pull", action="store_true",
                        help="Skip `git pull` on the registry clone (offline mode)")
    parser.add_argument("--check", choices=("cf", "yaci", "both"), default="both",
                        help="Which downstream(s) to verify against the registry. "
                             "'cf' skips yaci (and the SSH tunnel); 'yaci' skips CF.")
    parser.add_argument("--output-file", default=None,
                        help="Write the full diff report as JSON to this file")
    parser.add_argument("--cf-throttle-ms", type=int, default=0,
                        help="Sleep this many ms before each CF API call. Default 0 "
                             "(fine for preprod). For mainnet use 250-500 to stay under "
                             "the AWS ELB rate limit (HTTP 429 fires at ~60 req/min sustained).")
    args = parser.parse_args()
    check_cf = args.check in ("cf", "both")
    check_yaci = args.check in ("yaci", "both")
    netcfg = NETWORKS[args.network]
    cf_base = netcfg["cf_base"]
    repo_url = netcfg["repo_url"]
    registry_subdir = netcfg["registry_subdir"]
    if args.repo_cache is None:
        args.repo_cache = str(pathlib.Path(__file__).parent / ".cache" / netcfg["cache_subdir"])
    _set_cf_throttle(args.cf_throttle_ms / 1000.0)
    print(f"[setup] network={args.network}  cf_base={cf_base}  repo={repo_url}  "
          f"cf-throttle={args.cf_throttle_ms}ms")

    # SSH tunnel is only needed when yaci is in the check set and the user
    # is using the default localhost URL (not pointing at an already-reachable host).
    if check_yaci and not args.no_tunnel and args.yaci_url == DEFAULT_YACI_BASE:
        try:
            open_ssh_tunnel(args.ssh_host, args.local_port, args.remote_port)
        except Exception as e:
            print(f"[error] SSH tunnel setup failed: {e}", file=sys.stderr)
            return 2
        # Override yaci-url to point at the tunnel.
        args.yaci_url = f"http://127.0.0.1:{args.local_port}"

    try:
        preflight(args.yaci_url, cf_base, check_cf=check_cf, check_yaci=check_yaci)
    except Exception as e:
        print(f"[error] preflight failed: {e}", file=sys.stderr)
        return 2

    # Always set up the local registry clone — needed both for listing and for
    # the per-subject ground-truth lookup, even with --subject.
    try:
        repo_dir = ensure_registry_clone(pathlib.Path(args.repo_cache), repo_url, args.no_pull)
    except subprocess.CalledProcessError as e:
        print(f"[error] git command failed: {e}", file=sys.stderr)
        return 2
    except Exception as e:
        print(f"[error] registry clone setup failed: {e}", file=sys.stderr)
        return 2

    if args.subject:
        subjects = [args.subject]
        print(f"\n[run] comparing 1 subject")
    else:
        try:
            all_subjects = list_subjects_from_clone(repo_dir, registry_subdir)
        except Exception as e:
            print(f"[error] could not list subjects from local clone: {e}",
                  file=sys.stderr)
            return 2
        subjects = all_subjects[: args.limit] if args.limit else all_subjects
        print(f"\n[run] comparing {len(subjects)} subject(s) "
              f"(of {len(all_subjects)} total in registry)")

    print(f"     {'subject':<18} {'cf':<14} {'yaci':<16}contract")
    result = Result()
    for subject in subjects:
        compare_subject(subject, args.yaci_url, cf_base, result,
                        check_cf=check_cf, check_yaci=check_yaci)

    def fmt_svc(svc: ServiceResult) -> str:
        return (f"matched={len(svc.matched)} "
                f"missing={len(svc.missing)} "
                f"errors={len(svc.fetch_errors)}")

    print(f"\n{'-' * 60}")
    print(f"Subjects examined: {len(subjects)}")
    if check_cf:
        print(f"  CF {args.network:<12}: {fmt_svc(result.cf)}")
    if check_yaci:
        print(f"  yaci-store      : {fmt_svc(result.yaci)}")
    if check_cf and check_yaci:
        print(f"  CF vs yaci      : agree={len(result.contract.matched)} "
              f"diverged={len(result.contract.diverged)}")
        cip68_total = (len(result.cip68.agree) + len(result.cip68.diverged)
                       + len(result.cip68.cf_only) + len(result.cip68.yaci_only))
        print(f"  CIP-68 standards: total={cip68_total} "
              f"agree={len(result.cip68.agree)} "
              f"diverged={len(result.cip68.diverged)} "
              f"cf-only={len(result.cip68.cf_only)} "
              f"yaci-only={len(result.cip68.yaci_only)}")
    print(f"{'-' * 60}")

    if args.output_file:
        report = {
            "network": args.network,
            "yaci_url": args.yaci_url,
            "cf_url": cf_base,
            "registry_repo": repo_url,
            "cf": {
                "matched": result.cf.matched,
                "missing": result.cf.missing,
                "fetch_errors": [{"subject": s, "error": e}
                                 for s, e in result.cf.fetch_errors],
            },
            "yaci": {
                "matched": result.yaci.matched,
                "missing": result.yaci.missing,
                "fetch_errors": [{"subject": s, "error": e}
                                 for s, e in result.yaci.fetch_errors],
            },
            "contract": {
                "agree": result.contract.matched,
                "diverged": [{"subject": s, "diffs": d}
                             for s, d in result.contract.diverged],
            },
            "cip68": {
                "agree": result.cip68.agree,
                "diverged": [{"subject": s, "diffs": d}
                             for s, d in result.cip68.diverged],
                "cf_only": result.cip68.cf_only,
                "yaci_only": result.cip68.yaci_only,
            },
        }
        with open(args.output_file, "w") as f:
            json.dump(report, f, indent=2)
        print(f"Full report written to {args.output_file}")

    cf_bad = check_cf and bool(result.cf.missing or result.cf.fetch_errors)
    yaci_bad = check_yaci and bool(result.yaci.missing or result.yaci.fetch_errors)
    contract_bad = check_cf and check_yaci and bool(result.contract.diverged)
    cip68_bad = check_cf and check_yaci and bool(
        result.cip68.diverged or result.cip68.cf_only or result.cip68.yaci_only)
    return 1 if (cf_bad or yaci_bad or contract_bad or cip68_bad) else 0


if __name__ == "__main__":
    sys.exit(main())
