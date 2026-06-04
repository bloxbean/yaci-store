#!/usr/bin/env python3
"""Shared core for Blockfrost compatibility comparison.

Not run directly — imported by compare_<module>.py scripts.

Environment variables required (network-locked Blockfrost keys):
  BF_PROJECT_ID_MAINNET
  BF_PROJECT_ID_PREPROD
  BF_PROJECT_ID_PREVIEW
"""
import csv, datetime, json, os, sys, time, urllib.request, urllib.error

NETWORKS = {
    "mainnet": dict(local="http://localhost:8101",
                    bf="https://cardano-mainnet.blockfrost.io/api/v0",
                    pid_env="BF_PROJECT_ID_MAINNET"),
    "preprod": dict(local="http://localhost:8301",
                    bf="https://cardano-preprod.blockfrost.io/api/v0",
                    pid_env="BF_PROJECT_ID_PREPROD"),
    "preview": dict(local="http://localhost:8201",
                    bf="https://cardano-preview.blockfrost.io/api/v0",
                    pid_env="BF_PROJECT_ID_PREVIEW"),
}
LOCAL_PREFIX = "/api/v1/blockfrost"
LIST_QUERY   = "count=100&page=1&order=asc"

# Volatile: always stripped before diffing (change between the two calls)
IGNORE_VOLATILE = {"confirmations", "next_block"}

# Confirmed expected divergences — add here with a comment after verifying
IGNORE_DIVERGENT: dict = {
    # "field": "reason",
}

# Known-active sample identifiers per network.
# block/slot/epoch/tx_hash/address/asset/policy_id are auto-seeded from local chain.
PARAMS: dict = {
    "preprod": dict(
        stake_address="stake_test1uqgvl08c9kfzduvfd7r9n2eqh4n9j6nkxcxhkzuq5gv37psg34zyp",
        pool_id="pool1lenzcfx02maescnpv8mk6gc6c59t0dramqucdgcvr4revw89xpz",
        script_hash="", datum_hash="", label="674",
    ),
    "preview": dict(stake_address="", pool_id="pool1ynfnjspgckgxjf2zeye8s33jz3e3ndk9pcwp0qzaupzvvd8ukwt", script_hash="", datum_hash="", label="674"),
    "mainnet": dict(
        epoch="633",
        pool_id="pool1pu5jlj4q9w9jlxeu370a3c9myx47md5j5m2str0naunn2qnikdy",
        stake_address="", script_hash="", datum_hash="", label="674",
    ),
}


# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------

def http_get(url, project_id=None, timeout=30):
    req = urllib.request.Request(url)
    if project_id:
        req.add_header("project_id", project_id)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode() or "null")
    except urllib.error.HTTPError as e:
        body = e.read().decode(errors="replace")
        try:
            body = json.loads(body)
        except Exception:
            pass
        return e.code, body
    except Exception as e:
        return None, str(e)


# ---------------------------------------------------------------------------
# Seeding: discover block/slot/epoch/tx/address/asset from local chain
# ---------------------------------------------------------------------------

def seed_params(net, cfg, depth=20):
    p = dict(PARAMS.get(net, {}))
    st, latest = http_get(cfg["local"] + LOCAL_PREFIX + "/blocks/latest")
    if st != 200 or not isinstance(latest, dict):
        print(f"  ! cannot read local /blocks/latest: {latest}", file=sys.stderr)
        return p
    target = max(1, int(latest["height"]) - depth)
    st, blk = http_get(cfg["local"] + LOCAL_PREFIX + f"/blocks/{target}")
    if st == 200 and isinstance(blk, dict):
        p["block"] = str(blk["height"])
        p["slot"]  = str(blk["slot"])
        p.setdefault("epoch", str(int(blk["epoch"]) - 1))   # completed epoch

    # walk back to find a block with txs
    for h in range(int(p.get("block", target)), max(1, target - 500), -1):
        st, txs = http_get(cfg["local"] + LOCAL_PREFIX + f"/blocks/{h}/txs?{LIST_QUERY}")
        if st == 200 and isinstance(txs, list) and txs:
            p["tx_hash"] = txs[0]
            p["block"]   = str(h)
            st, blk = http_get(cfg["local"] + LOCAL_PREFIX + f"/blocks/{h}")
            if st == 200:
                p["slot"]  = str(blk["slot"])
                p.setdefault("epoch", str(int(blk["epoch"]) - 1))
            break

    # derive address + asset from tx utxos
    if p.get("tx_hash"):
        st, u = http_get(cfg["local"] + LOCAL_PREFIX + f"/txs/{p['tx_hash']}/utxos")
        if st == 200 and isinstance(u, dict):
            for io in (u.get("outputs") or []):
                p.setdefault("address", io.get("address"))
                for amt in io.get("amount", []):
                    if amt.get("unit") and amt["unit"] != "lovelace":
                        p.setdefault("asset", amt["unit"])
                        p.setdefault("policy_id", amt["unit"][:56])
                        break
                if p.get("asset"):
                    break
    return p


# ---------------------------------------------------------------------------
# Diff helpers
# ---------------------------------------------------------------------------

def strip(obj, keys):
    if isinstance(obj, dict):
        return {k: strip(v, keys) for k, v in obj.items() if k not in keys}
    if isinstance(obj, list):
        return [strip(v, keys) for v in obj]
    return obj


def diff(a, b, path=""):
    out = []
    if isinstance(a, dict) and isinstance(b, dict):
        for k in sorted(set(a) | set(b)):
            if k not in a:
                out.append(f"{path}.{k}: missing in Yaci")
            elif k not in b:
                out.append(f"{path}.{k}: missing in Blockfrost")
            else:
                out += diff(a[k], b[k], f"{path}.{k}")
    elif isinstance(a, list) and isinstance(b, list):
        if len(a) != len(b):
            out.append(f"{path}: length BF={len(b)} Yaci={len(a)}")
        for i in range(min(len(a), len(b))):
            out += diff(a[i], b[i], f"{path}[{i}]")
    else:
        if a != b:
            out.append(f"{path}: BF={b!r}  Yaci={a!r}")
    return out


# ---------------------------------------------------------------------------
# Core runner — called by each per-module script
# ---------------------------------------------------------------------------

def run_module(net, module, endpoints, strict=False, depth=20, throttle=0.2,
               csv_out=None, orders=("asc", "desc"), pages=(1, 2), count=100):
    """
    endpoints: list of (path_template, is_list, [required_param_keys])
    csv_out:   optional file path; if given, appends one row per comparison to a CSV.
    orders:    order values to exercise on list endpoints (e.g. ("asc","desc")).
    pages:     page numbers to exercise on list endpoints (e.g. (1, 2)).
    count:     page size for list endpoints.
    Non-list endpoints are compared once (order/page do not apply).
    List endpoints are compared once per (order, page) combination.
    Returns totals dict {ok, diff, skip, err}.
    """
    cfg = NETWORKS[net]
    pid = os.environ.get(cfg["pid_env"])
    if not pid:
        sys.exit(f"Set ${cfg['pid_env']} to your Blockfrost {net} project_id")

    ignore = IGNORE_VOLATILE | (set() if strict else set(IGNORE_DIVERGENT))
    params = seed_params(net, cfg, depth)

    print(f"\n{'='*60}")
    print(f"  {module.upper()}  |  {net}")
    print(f"  block={params.get('block')}  slot={params.get('slot')}  epoch={params.get('epoch')}")
    tx  = str(params.get('tx_hash', ''))
    adr = str(params.get('address', ''))
    print(f"  tx={tx[:24]}…  addr={adr[:24]}…")
    print(f"  list variants: orders={list(orders)}  pages={list(pages)}  count={count}")
    print(f"{'='*60}")

    totals = {"ok": 0, "diff": 0, "skip": 0, "err": 0}
    run_dt = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    csv_rows = []

    CSV_FIELDS = ["date", "network", "module", "endpoint",
                  "result", "yaci_status", "bf_status",
                  "yaci_ms", "bf_ms", "slow", "diff_count", "diff_fields"]

    def compare_call(label, path, q):
        """Run one Yaci-vs-BF comparison for a fully-resolved path + query string."""
        t0 = time.monotonic()
        ls, lb = http_get(cfg["local"] + LOCAL_PREFIX + path + q)
        yaci_ms = int((time.monotonic() - t0) * 1000)
        time.sleep(throttle)
        t0 = time.monotonic()
        bs, bb = http_get(cfg["bf"] + path + q, project_id=pid)
        bf_ms = int((time.monotonic() - t0) * 1000)
        time.sleep(throttle)

        slow = bool(yaci_ms > 500 or yaci_ms > bf_ms * 3)

        if ls != bs:
            print(f"  STATUS  {label}  BF={bs}  Yaci={ls}")
            if ls != 200:
                print(f"    Yaci body: {json.dumps(lb, indent=2)[:500]}")
            if bs != 200:
                print(f"    BF   body: {json.dumps(bb, indent=2)[:500]}")
            totals["err"] += 1
            csv_rows.append(dict(date=run_dt, network=net, module=module,
                                 endpoint=label, result="STATUS_MISMATCH",
                                 yaci_status=ls, bf_status=bs,
                                 yaci_ms=yaci_ms, bf_ms=bf_ms, slow=slow,
                                 diff_count="", diff_fields=""))
            return

        if ls != 200:
            print(f"  ({ls})  {label}  — both non-200, skipping diff")
            totals["skip"] += 1
            csv_rows.append(dict(date=run_dt, network=net, module=module,
                                 endpoint=label, result="SKIP_NON200",
                                 yaci_status=ls, bf_status=bs,
                                 yaci_ms=yaci_ms, bf_ms=bf_ms, slow=slow,
                                 diff_count="", diff_fields=""))
            return

        d = diff(strip(lb, ignore), strip(bb, ignore))
        slow_flag = " :warning: SLOW" if slow else ""
        timing = f"  [Yaci={yaci_ms}ms  BF={bf_ms}ms{slow_flag}]"
        if d:
            print(f"  DIFF  {label}  ({len(d)} field(s)){timing}")
            for line in d[:15]:
                print(f"        {line}")
            if len(d) > 15:
                print(f"        … +{len(d)-15} more")
            print(f"    --- BF   sample: {json.dumps(bb if not isinstance(bb, list) else bb[:2], indent=2)[:800]}")
            print(f"    --- Yaci sample: {json.dumps(lb if not isinstance(lb, list) else lb[:2], indent=2)[:800]}")
            totals["diff"] += 1
            csv_rows.append(dict(date=run_dt, network=net, module=module,
                                 endpoint=label, result="DIFF",
                                 yaci_status=ls, bf_status=bs,
                                 yaci_ms=yaci_ms, bf_ms=bf_ms, slow=slow,
                                 diff_count=len(d),
                                 diff_fields=" | ".join(d[:5])))
        else:
            print(f"  OK    {label}{timing}")
            totals["ok"] += 1
            csv_rows.append(dict(date=run_dt, network=net, module=module,
                                 endpoint=label, result="OK",
                                 yaci_status=ls, bf_status=bs,
                                 yaci_ms=yaci_ms, bf_ms=bf_ms, slow=slow,
                                 diff_count=0, diff_fields=""))

    for entry in endpoints:
        path_tpl, is_list = entry[0], entry[1]
        req_keys = entry[2] if len(entry) > 2 else []
        missing = [k for k in req_keys if not params.get(k)]
        if missing:
            print(f"  SKIP  {path_tpl}  (no param: {missing})")
            totals["skip"] += 1
            csv_rows.append(dict(date=run_dt, network=net, module=module,
                                 endpoint=path_tpl, result="SKIP",
                                 yaci_status="", bf_status="",
                                 yaci_ms="", bf_ms="", slow="",
                                 diff_count="", diff_fields=f"missing params: {missing}"))
            continue

        path = path_tpl.format(**params)

        if not is_list:
            compare_call(path_tpl, path, "")
            continue

        # List endpoint: exercise every (order, page) combination.
        for order in orders:
            for page in pages:
                q     = f"?count={count}&page={page}&order={order}"
                label = f"{path_tpl}?order={order}&page={page}"
                compare_call(label, path, q)

    print(f"\n  [{module}/{net}] ok={totals['ok']}  diff={totals['diff']}  "
          f"skip={totals['skip']}  err={totals['err']}")

    # When no --csv path is given, write to the default report directory
    # alongside this module: scripts/compare-blockfrost/report/<module>_<net>.csv
    if csv_out is None:
        csv_out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                               "report", f"{module}_{net}.csv")
    os.makedirs(os.path.dirname(csv_out), exist_ok=True)
    write_header = not os.path.exists(csv_out)
    with open(csv_out, "a", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=CSV_FIELDS)
        if write_header:
            w.writeheader()
        w.writerows(csv_rows)
    print(f"  CSV appended → {csv_out}  ({len(csv_rows)} rows)")

    return totals
