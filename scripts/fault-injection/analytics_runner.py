#!/usr/bin/env python3
"""Fault scenarios for the analytics store. Appends one JSON line per scenario to results/<instance>-analytics.jsonl.

usage: analytics_runner.py <instance> [--ref <reference instance>] [--only ID,ID] [--skip ID,ID] [--list]
       analytics_runner.py <instance> --ref <ref> --verify-all      # every exported partition of every table vs the reference

Run it once the instance follows the tip and its analytics export has caught up (GET /api/v1/analytics/admin/status:
missingExportCount 0). Most scenarios start a real export (a repair of a range of days already exported), break
something while it runs, heal it, wait until those days are exported again, and then compare them with the same days
of a fault-free reference instance (same build, no proxies): row count and a content hash per partition, read through
each instance's query API. Without --ref, the days' own fingerprint taken just before the fault is the reference. The export state of each day must be COMPLETED with the same row count as the data.
"""
import argparse, json, os, re, signal, subprocess, sys, threading, time, urllib.error, urllib.parse, urllib.request
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import runner as rt  # noqa: E402  (Ctx, KIT, R, JCMD, THRESH_HOLD)

R, KIT, H = rt.R, rt.KIT, rt.THRESH_HOLD
BUSY_TABLE = os.environ.get("RT_ANALYTICS_TABLE", "address_utxo")   # a table with many rows per day on preprod/preview
DUCKDB_CLI = os.environ.get("RT_DUCKDB_CLI", "duckdb")   # a DuckDB CLI of the same version as the app's DuckDB
VOLATILE = re.compile(r"(create|update)_?datetime|^(created|updated)_at$|^_ducklake", re.I)

# ---------------------------------------------------------------- HTTP and state helpers

def api(c, method, path, params=None, body=None, timeout=120):
    url = f"http://127.0.0.1:{18100 + c.i}{path}"
    if params: url += "?" + urllib.parse.urlencode(params)
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method,
                                 headers={"Content-Type": "application/json"} if data else {})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode(errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode(errors="replace")
    except Exception as e:  # connection refused, timeout, ...
        return None, repr(e)

def sql(c, q, timeout=300):
    code, text = api(c, "POST", "/api/v1/analytics/query/sql", body={"sql": q, "maxRows": 100000}, timeout=timeout)
    if code != 200: return None, f"{code} {text[:300]}"
    try: return json.loads(text), None
    except ValueError: return None, text[:300]

def status(c):
    code, text = api(c, "GET", "/api/v1/analytics/admin/status", timeout=30)
    try: return json.loads(text) if code == 200 else None
    except ValueError: return None

def states(c, table, parts):
    vals = ",".join(f"'{p}'" for p in parts)
    r = c.sql(f"select partition_value, export_status, coalesce(row_count,-1) from analytics_export_state "
              f"where table_name='{table}' and partition_value in ({vals})", timeout=60) or ""
    out = {}
    for line in r.splitlines():
        p, st, n = line.split(" ")
        out[p] = (st, int(n))
    return out

def completed_days(c, table, n, skip):
    """n completed day partitions of table, skipping the newest `skip` (they are close to the finality window)."""
    r = c.sql(f"select partition_value from analytics_export_state where table_name='{table}' and export_status='COMPLETED' "
              f"and partition_value like 'date=%' order by partition_value desc offset {skip} limit {n}", timeout=60) or ""
    days = sorted(p.split("=", 1)[1] for p in r.splitlines())
    return days

def pick_days(c, ref, table, n, skip):
    """completed_days, plus a pre-fault fingerprint when there is no reference instance."""
    days = completed_days(c, table, n, skip)
    take_baseline(c, ref, table, "date", days)
    return days

def wait_completed(c, table, days, timeout):
    parts = [f"date={d}" for d in days]
    t = time.time(); st = {}
    while time.time() - t < timeout:
        st = states(c, table, parts)
        if len(st) == len(parts) and all(v[0] == "COMPLETED" for v in st.values()): return True, int(time.time() - t), st
        time.sleep(10)
    return False, None, st

def columns(c, table):
    code, text = api(c, "GET", f"/api/v1/analytics/query/schema/{table}", timeout=60)
    if code != 200: return None
    try: d = json.loads(text)
    except ValueError: return None
    cols = d.get("columns") or []
    names = [x.get("name") or x.get("columnName") for x in cols if isinstance(x, dict)]
    return [n for n in names if n and not VOLATILE.search(n)]

def fingerprint(c, table, key, values, cols):
    """{partition value: (rows, content hash)} read through the query API."""
    if not values: return {}, None
    hcols = ", ".join(f'"{x}"' for x in cols)
    lst = ", ".join(f"'{v}'" for v in values) if key == "date" else ", ".join(str(v) for v in values)
    cast = "CAST(date AS VARCHAR)" if key == "date" else "epoch"
    q = (f"SELECT {cast} AS p, count(*) AS n, CAST(sum(hash({hcols})) AS VARCHAR) AS h FROM {table} "
         f"WHERE {key} IN ({lst}) GROUP BY 1 ORDER BY 1")
    rows, err = sql(c, q, timeout=900)
    if rows is None: return None, err
    return {str(r["p"]): (int(r["n"]), r["h"]) for r in rows}, None

BASELINES = {}   # (inst, table, key, values) -> fingerprint taken before the fault, when there is no reference

def take_baseline(c, ref, table, key, values):
    """Without a reference instance, the partitions' own fingerprint before the fault is the reference."""
    if ref is not None or not values: return
    cols = columns(c, table)
    fp, _ = fingerprint(c, table, key, values, cols) if cols else (None, None)
    if fp is not None: BASELINES[(c.inst, table, key, tuple(map(str, values)))] = (cols, fp)

def compare(c, ref, table, key, values):
    """Differences between c and ref (or c's own pre-fault baseline) for these partitions, plus state rows of
    c that disagree with the data."""
    if ref is None:
        base = BASELINES.get((c.inst, table, key, tuple(map(str, values))))
        if not base: return {"check_failed": f"no reference and no baseline for {table}"}
        cols, b = base; eb = None
    else:
        cols = columns(ref, table)
        if not cols: return {"check_failed": f"no schema for {table} on {ref.inst}"}
        b, eb = fingerprint(ref, table, key, values, cols)
    a, ea = fingerprint(c, table, key, values, cols)
    if a is None or b is None: return {"check_failed": ea or eb}
    out = {}
    diff = [v for v in map(str, values) if a.get(v) != b.get(v)]
    if diff: out["data_differs_from_ref" if ref is not None else "data_differs_from_before"] = \
        {v: {"inst": a.get(v), "ref": b.get(v)} for v in diff[:10]}
    pre = "date=" if key == "date" else "epoch="
    st = states(c, table, [pre + str(v) for v in values])
    bad = {p: s for p, s in st.items() if s[0] != "COMPLETED" or s[1] != (a.get(p.split("=", 1)[1]) or (0, None))[0]}
    if bad: out["state_disagrees"] = dict(list(bad.items())[:10])
    missing = [v for v in map(str, values) if (pre + v) not in st]
    if missing: out["state_missing"] = missing[:10]
    return out

def jvm_crash_files(c):
    return [f for f in os.listdir(c.d) if f.startswith("hs_err")]

# ---------------------------------------------------------------- scenario building blocks

class A:
    def __init__(s, id, title, run, deadline=3600, needs_ref=True):
        s.id, s.title, s.run, s.deadline, s.needs_ref = id, title, run, deadline, needs_ref

def repair_bg(c, table, days, out):
    """POST the repair of days in a thread (the request blocks until the repair ends or fails)."""
    def go():
        out["repair"] = api(c, "POST", f"/api/v1/analytics/admin/repair/table/{table}/range",
                            {"startDate": days[0], "endDate": days[-1]}, timeout=7200)
    t = threading.Thread(target=go, daemon=True); t.start(); return t

def wait_export_running(c, table, days, timeout=300):
    """Until one of the days is IN_PROGRESS (a repair resets the state first, then exports day by day)."""
    t = time.time()
    while time.time() - t < timeout:
        st = states(c, table, [f"date={d}" for d in days])
        if any(v[0] == "IN_PROGRESS" for v in st.values()): return True
        time.sleep(0.5)
    return False

def fault_during_export(fault, heal=None, hold=0, ndays=6, offset=30, after_start=3):
    """Repair ndays of the busy table, inject the fault while a day exports, heal, let the app re-export them."""
    def f(c, ref, res):
        days = pick_days(c, ref, BUSY_TABLE, ndays, offset)
        res["days"] = days
        if len(days) < ndays: res["notes"].append("not enough completed days"); return days
        bg = {}; th = repair_bg(c, BUSY_TABLE, days, bg)
        res["export_running"] = wait_export_running(c, BUSY_TABLE, days)
        time.sleep(after_start)
        fault(c)
        if hold: time.sleep(hold)
        if heal: heal(c)
        th.join(timeout=60)
        res["repair_response"] = (bg.get("repair") or (None, "still running"))[0]
        return days
    return f

def _kill_restart(delay=5):
    return lambda c: (c.signal(signal.SIGKILL), time.sleep(delay), c.inst_cmd("start"))

def _pg(*a): return lambda c: c.docker(*a, c.pg)

def trigger_sync(c):
    return api(c, "POST", "/api/v1/analytics/admin/sync/trigger", timeout=3600)

def admin_edges(c, ref, res):
    """Admin API inputs that must be refused without touching the export state."""
    today = time.strftime("%Y-%m-%d", time.gmtime())
    future = time.strftime("%Y-%m-%d", time.gmtime(time.time() + 10 * 86400))
    before = c.sql("select count(*) from analytics_export_state")
    probes = {
        "export_today": ("POST", f"/api/v1/analytics/admin/export/table/block/date/{today}", None),
        "export_future": ("POST", f"/api/v1/analytics/admin/export/table/block/date/{future}", None),
        "export_epoch_negative": ("POST", "/api/v1/analytics/admin/export/table/epoch/epoch/-3", None),
        "epoch_range_overflow": ("POST", "/api/v1/analytics/admin/export/table/epoch/epoch-range", {"startEpoch": 0, "endEpoch": 2147483647}),
        "range_reversed": ("POST", "/api/v1/analytics/admin/export/table/block/range", {"startDate": "2024-02-01", "endDate": "2024-01-01"}),
        "bad_date": ("POST", "/api/v1/analytics/admin/export/table/block/date/2024-13-45", None),
        "unknown_table": ("POST", "/api/v1/analytics/admin/export/table/no_such_table/date/2024-01-01", None),
        "reset_unknown_key": ("DELETE", "/api/v1/analytics/admin/state/block/date=1999-01-01", None),
    }
    out = {}
    for k, (m, p, params) in probes.items():
        t = time.time(); code, text = api(c, m, p, params, timeout=60)
        out[k] = {"code": code, "s": round(time.time() - t, 1), "body": text[:160]}
    # a range reset with plain dates must reset exactly the days in the range, then the scheduler re-exports them
    days = pick_days(c, ref, "block", 2, 40)
    code, text = api(c, "DELETE", "/api/v1/analytics/admin/state/block/range", {"startDate": days[0], "endDate": days[-1]})
    out["range_reset_plain_dates"] = {"code": code, "body": text[:160],
                                      "left": len(states(c, "block", [f"date={d}" for d in days]))}
    res["probes"] = out
    after = c.sql("select count(*) from analytics_export_state")
    polluted = c.sql(f"select partition_value from analytics_export_state where partition_value like 'epoch=-%' "
                     f"or partition_value >= 'date={today}' and partition_value like 'date=%'")
    res["state_rows_before_after"] = [before, after]
    if polluted: res["anomalies"]["state_polluted"] = polluted.replace("\n", "; ")
    trigger_sync(c)
    res["checked"] = [("block", "date", days)]
    return None

def query_probes(c, ref, res):
    """Query API inputs that must be refused, and the JVM must survive them."""
    probes = {
        "cr_comment_bypass": "SELECT 1 AS x -- c\r, (SELECT count(*) FROM duckdb_settings()) AS n",
        "enable_logging": "SELECT * FROM enable_logging()",
        "peg_parser": "SELECT * FROM enable_peg_parser()",
        "lpad_1e9": "SELECT length(lpad('a', 1000000000, 'b')) AS n",
        "list_resize_3e8": "SELECT len(list_resize([1::BIGINT], 300000000)) AS n",
        "read_text": "SELECT * FROM read_text('/etc/passwd')",
    }
    out = {}
    for k, q in probes.items():
        code, text = api(c, "POST", "/api/v1/analytics/query/sql", body={"sql": q}, timeout=120)
        out[k] = {"code": code, "body": text[:160]}
        if code == 200: res["anomalies"][f"accepted_{k}"] = text[:160]
    api(c, "POST", "/api/v1/analytics/query/sql", body={"sql": "SELECT * FROM disable_logging()"})
    res["probes"] = out
    res["alive_after"] = c.alive()
    return None

def heavy_query_during_export(c, ref, res):
    """Spilling queries in the query layer while the writer exports: separate temp dirs must keep the JVM alive."""
    days = pick_days(c, ref, BUSY_TABLE, 6, 50); res["days"] = days
    bg = {}; th = repair_bg(c, BUSY_TABLE, days, bg)
    wait_export_running(c, BUSY_TABLE, days)
    q = (f"SELECT owner_addr, count(*) n, sum(lovelace_amount) s FROM {BUSY_TABLE} GROUP BY 1 ORDER BY 3 DESC LIMIT 10")
    out = []
    for _ in range(4):
        code, text = api(c, "POST", "/api/v1/analytics/query/sql", body={"sql": q}, timeout=600)
        out.append(code)
    th.join(timeout=3600)
    res["query_codes"] = out
    res["checked"] = [(BUSY_TABLE, "date", days)]
    return None

def adapot_lookup_fail(c, ref, res):
    """The AdaPot job lookup fails for 5 min while the newest AdaPot-gated epochs are exported again."""
    tables = ["reward", "epoch_stake", "adapot", "drep_dist"]
    picked = {}
    for t in tables:
        r = c.sql(f"select partition_value from analytics_export_state where table_name='{t}' and export_status='COMPLETED' "
                  f"order by length(partition_value) desc, partition_value desc limit 1")
        if r: picked[t] = int(r.split("=")[1])
    for t, e in picked.items(): take_baseline(c, ref, t, "epoch", [e])
    c.trig("analytics-adapot-lookup-fail")
    for t, e in picked.items():
        api(c, "DELETE", f"/api/v1/analytics/admin/state/{t}/epoch={e}")
    trigger_sync(c)
    time.sleep(300)
    during = {t: states(c, t, [f"epoch={e}"]) for t, e in picked.items()}
    res["state_during_failure"] = during
    for t, st in during.items():
        for p, (s, n) in st.items():
            if s == "COMPLETED": res["anomalies"][f"exported_while_lookup_fails_{t}"] = f"{p} {n} rows"
    c.untrig("analytics-adapot-lookup-fail")
    trigger_sync(c)
    res["checked"] = [(t, "epoch", [e]) for t, e in picked.items()]
    return None

def catalog_locked(during_restart):
    """Another process holds the duckdb-file catalog (as an operator inspecting it would) for 120 s."""
    def f(c, ref, res):
        cat = os.path.join(c.d, "data/analytics/ducklake.catalog.db")
        if not os.path.exists(cat): res["notes"].append("no duckdb-file catalog (postgresql catalog?)"); return None
        days = pick_days(c, ref, BUSY_TABLE, 3, 60); res["days"] = days
        # the CLI keeps the file open (and locked) until its stdin closes, 120 s later
        holder = subprocess.Popen(f"sleep 120 | {DUCKDB_CLI} -readonly '{cat}'", shell=True,
                                  stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        time.sleep(3)
        if during_restart:
            c.signal(signal.SIGKILL); time.sleep(3); c.inst_cmd("start")
        else:
            bg = {}; th = repair_bg(c, BUSY_TABLE, days, bg); th.join(timeout=150)
            res["repair_response"] = (bg.get("repair") or (None, ""))[0]
        holder.wait(timeout=200)
        res["holder_rc"] = holder.returncode
        time.sleep(30)
        res["alive_after_release"] = c.alive()
        if not c.alive(): res["notes"].append("app exited while the catalog was locked; started by the harness"); c.inst_cmd("start")
        trigger_sync(c)
        res["checked"] = [(BUSY_TABLE, "date", days)]
        return None
    return f

def concurrent_triggers(c, ref, res):
    """Five /sync/trigger calls and two repairs of the same days at once: no partition may end up duplicated."""
    days = pick_days(c, ref, BUSY_TABLE, 3, 70); res["days"] = days
    bg1, bg2 = {}, {}
    ths = [repair_bg(c, BUSY_TABLE, days, bg1), repair_bg(c, BUSY_TABLE, days, bg2)]
    for _ in range(5):
        ths.append(threading.Thread(target=trigger_sync, args=(c,), daemon=True)); ths[-1].start()
    for t in ths: t.join(timeout=3600)
    res["repair_codes"] = [(bg1.get("repair") or (None,))[0], (bg2.get("repair") or (None,))[0]]
    res["checked"] = [(BUSY_TABLE, "date", days)]
    return None

def restart_storm(c, ref, res):
    days = pick_days(c, ref, BUSY_TABLE, 10, 80); res["days"] = days
    bg = {}; repair_bg(c, BUSY_TABLE, days, bg)
    wait_export_running(c, BUSY_TABLE, days)
    for _ in range(5):
        time.sleep(45); c.signal(signal.SIGKILL); time.sleep(3); c.inst_cmd("start")
    return days

SCENARIOS = [
 A("A01", "kill -9 while a day is exported, restart", fault_during_export(_kill_restart())),
 A("A02", "kill -9 after the DuckLake commit, before the state is saved", fault_during_export(
     lambda c: (c.trig("analytics-pause-before-mark-completed"),
                c.wait_console(c.console_size(), r"analytics-pause-before-mark-completed \S+", 600),
                _kill_restart()(c)), after_start=0)),
 A("A03", "Saving the state fails once after the DuckLake commit", fault_during_export(
     lambda c: c.trig("analytics-mark-completed-fail"), after_start=0)),
 A("A04", "Postgres down 210 s during an export", fault_during_export(_pg("stop", "-t", "1"), _pg("start"), H)),
 A("A05", "Postgres frozen 210 s (docker pause) during an export", fault_during_export(_pg("pause"), _pg("unpause"), H)),
 A("A06", "All DB connections reset during an export", fault_during_export(lambda c: c.db("rst"))),
 A("A07", "DB network partition 210 s during an export (no FIN)", fault_during_export(lambda c: c.db("freeze", "on"), lambda c: c.db("pass"), H)),
 A("A08", "JVM frozen 210 s (SIGSTOP) during an export", fault_during_export(lambda c: c.signal(signal.SIGSTOP), lambda c: c.signal(signal.SIGCONT), H)),
 A("A09", f"Source table {BUSY_TABLE} locked 210 s during an export", fault_during_export(lambda c: c.lock_table(BUSY_TABLE, 210), None, H, after_start=0)),
 A("A10", "App and Postgres killed together during an export (host crash)", fault_during_export(rt._machine_crash)),
 A("A11", "Restart storm: kill -9 every 45 s, 5 times, during a 10-day repair", restart_storm),
 A("A12", "Five sync triggers and two repairs of the same days at once", concurrent_triggers),
 A("A13", "AdaPot job lookup fails for 5 min while AdaPot-gated epochs export", adapot_lookup_fail),
 A("A14", "Catalog file held by another process for 120 s during an export", catalog_locked(False)),
 A("A15", "Catalog file held by another process for 120 s while the app restarts", catalog_locked(True)),
 A("A16", "Spilling queries in the query layer during an export", heavy_query_during_export),
 A("A17", "Admin API: today, future, negative epoch, overflow, bad input, range reset", admin_edges),
 A("A18", "Query API: comment bypass, state-changing and allocating functions, file reads", query_probes, needs_ref=False),
]

# ---------------------------------------------------------------- run loop

def wait_up(c, timeout=900):
    t = time.time()
    while time.time() - t < timeout:
        if status(c): return True
        if not c.alive(): c.inst_cmd("start")
        time.sleep(10)
    return False

def run(c, ref, sc, resdir):
    res = {"id": sc.id, "inst": c.inst, "mode": "analytics", "title": sc.title,
           "start": time.strftime("%FT%TZ", time.gmtime()), "notes": [], "anomalies": {}}
    wait_up(c)
    crash0 = set(jvm_crash_files(c)); off = c.log_size()
    print(f"[{time.strftime('%T')}] {c.inst} {sc.id}: {sc.title}", flush=True)
    t0 = time.time()
    days = sc.run(c, ref, res)
    for t in os.listdir(f"{c.d}/trig"):
        if t.startswith("analytics-"): c.untrig(t)
    c.heal_all()
    res["app_up_after"] = wait_up(c)
    if days:
        trigger_sync(c)
        ok, ttr, st = wait_completed(c, BUSY_TABLE, days, sc.deadline)
        res.update(reexported=ok, ttr_s=ttr)
        if not ok: res["anomalies"]["not_reexported"] = {k: v for k, v in st.items() if v[0] != "COMPLETED"} or "state rows missing"
        res.setdefault("checked", [(BUSY_TABLE, "date", days)])
    for table, key, values in res.get("checked", []):
        if sc.needs_ref:   # compared with --ref, or with the pre-fault baseline when there is no reference
            if key == "epoch":
                t = time.time()
                while time.time() - t < sc.deadline:
                    st = states(c, table, [f"epoch={v}" for v in values])
                    if st and all(s[0] == "COMPLETED" for s in st.values()): break
                    trigger_sync(c); time.sleep(30)
            # The query layer refreshes its views every 5 minutes, so a partition exported a moment
            # ago can still read the old file list: retry a mismatch for 6 minutes before reporting it
            d = compare(c, ref, table, key, values)
            for _ in range(6):
                if not d or "check_failed" in d: break
                time.sleep(60); d = compare(c, ref, table, key, values)
            if d: res["anomalies"][f"{table}"] = d
    crashes = sorted(set(jvm_crash_files(c)) - crash0)
    if crashes: res["anomalies"]["jvm_crash"] = crashes
    text = c.log_since(off)
    res["log_counts"] = {k: n for k, p in {
        "errors": r"\bERROR\b", "export_failed": r"Export failed", "verification_failed": r"verification failed",
        "lock_conflict": r"Conflicting lock|Could not set lock", "sigsegv": r"SIGSEGV|SIGBUS"}.items()
        if (n := len(re.findall(p, text)))}
    d = f"{resdir}/{c.inst}-analytics-{sc.id}"; os.makedirs(d, exist_ok=True)
    with open(f"{d}/errors.log", "w") as f:
        f.writelines(l + "\n" for l in text.splitlines() if re.search(r"\bERROR\b|\bWARN\b|Exception|byteman", l))
    res["duration_s"] = int(time.time() - t0)
    res["ok"] = not res["anomalies"] and res["app_up_after"]
    return res

def verify_all(c, ref, out):
    """Every partition both instances have COMPLETED, every table: row count and content hash must match."""
    code, text = api(c, "GET", "/api/v1/analytics/admin/tables", timeout=60)
    tables = json.loads(text) if code == 200 else []
    names = [t if isinstance(t, str) else (t.get("tableName") or t.get("name")) for t in tables]
    result = {}
    for t in names:
        q = (f"select partition_value from analytics_export_state where table_name='{t}' and export_status='COMPLETED'")
        a = set((c.sql(q, timeout=120) or "").splitlines()); b = set((ref.sql(q, timeout=120) or "").splitlines())
        both = sorted(a & b)
        key = "epoch" if both and both[0].startswith("epoch=") else "date"
        vals = [p.split("=", 1)[1] for p in both]
        diffs = {}
        for i in range(0, len(vals), 200):
            chunk = vals[i:i + 200]
            d = compare(c, ref, t, key, chunk if key == "date" else [int(v) for v in chunk])
            for k, v in d.items():
                if isinstance(v, dict): diffs.setdefault(k, {}).update(v)
                else: diffs.setdefault(k, []).extend(v if isinstance(v, list) else [v])
        result[t] = {"partitions": len(vals), "only_inst": len(a - b), "only_ref": len(b - a), "diffs": diffs}
        print(f"{t}: {len(vals)} partitions, {'OK' if not diffs else 'DIFF ' + json.dumps(diffs)[:300]}", flush=True)
    with open(out, "w") as f: json.dump(result, f, indent=1)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("inst"); ap.add_argument("--ref")
    ap.add_argument("--only"); ap.add_argument("--skip"); ap.add_argument("--list", action="store_true")
    ap.add_argument("--verify-all", action="store_true")
    a = ap.parse_args()
    if a.list:
        for s in SCENARIOS: print(f"{s.id}  {s.title}")
        return
    c = rt.Ctx(a.inst); ref = rt.Ctx(a.ref) if a.ref else None
    resdir = f"{R}/results"; os.makedirs(resdir, exist_ok=True)
    if a.verify_all:
        verify_all(c, ref, f"{resdir}/{a.inst}-vs-{a.ref}-analytics.json"); return
    out = f"{resdir}/{a.inst}-analytics.jsonl"
    done = set()
    if os.path.exists(out):
        done = {json.loads(l)["id"] for l in open(out) if l.strip()}
    only = set(a.only.split(",")) if a.only else None; skip = set(a.skip.split(",")) if a.skip else set()
    for sc in SCENARIOS:
        if sc.id in done or (only and sc.id not in only) or sc.id in skip: continue
        if os.path.exists(f"{resdir}/STOP"): print("STOP file found"); break
        res = run(c, ref, sc, resdir)
        with open(out, "a") as f: f.write(json.dumps(res) + "\n")
        print(f"  -> ok={res['ok']} ttr={res.get('ttr_s')} anomalies={json.dumps(res['anomalies'])[:300]}", flush=True)
        time.sleep(60)
    print(f"ANALYTICS_RUNNER_DONE {a.inst}", flush=True)

if __name__ == "__main__":
    main()
