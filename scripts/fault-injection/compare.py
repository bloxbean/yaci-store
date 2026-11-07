#!/usr/bin/env python3
"""Compares every slot-keyed table of an instance with the unfaulted control, up to the lower of the two cursors.
Row counts and a content hash that ignores surrogate ids and timestamps. Differences are drilled down to blocks.

usage: compare.py <inst> [control=ctrl]
"""
import subprocess, sys
inst = sys.argv[1]; ctl = sys.argv[2] if len(sys.argv) > 2 else "ctrl"
IGNORE = {"id", "create_datetime", "update_datetime", "created_at", "updated_at"}
SKIP = {"cursor_", "rollback", "error", "flyway_schema_history", "store_state", "address_utxo_unspent"}
def sql(n, q, t=3600):
    p = subprocess.run(["docker", "exec", "-i", f"yaci-store-postgres-rt-{n}", "psql", "-U", "yaci", "-d", "yaci_store",
                        "-AtF", "\t", "-c", "set work_mem='512MB'; set statement_timeout=0;", "-c", q],
                       capture_output=True, text=True, timeout=t)
    if p.returncode: raise RuntimeError(p.stderr[:400])
    return [l for l in p.stdout.splitlines() if l != "SET"]
def cur(n): return int(sql(n, "select slot from cursor_ order by slot desc limit 1")[0])
S = min(cur(inst), cur(ctl)); print(f"comparing {inst} vs {ctl} at slot <= {S}")
cols = {}
for l in sql(inst, "select table_name,column_name from information_schema.columns where table_schema=current_schema() and data_type not in ('tsvector') order by table_name,ordinal_position"):
    t, c = l.split("\t"); cols.setdefault(t, []).append(c)
ctlcols = {}
for l in sql(ctl, "select table_name,column_name from information_schema.columns where table_schema=current_schema() order by table_name,ordinal_position"):
    t, c = l.split("\t"); ctlcols.setdefault(t, []).append(c)
bad = []
for t in sorted(cols):
    if t in SKIP or "slot" not in cols[t] or t not in ctlcols: continue
    if t.rsplit("_", 1)[-1].isdigit() or "_default" in t: continue   # partitions are covered by their parent
    use = [c for c in cols[t] if c not in IGNORE and c in ctlcols[t]]
    expr = ",".join(f'"{c}"::text' for c in use)
    q = (f"select count(*), coalesce(sum(('x'||substr(md5(concat_ws('|',{expr})),1,15))::bit(60)::bigint::numeric),0) "
         f'from "{t}" where slot <= {S}')
    a, b = sql(inst, q)[0], sql(ctl, q)[0]
    same = a == b
    print(f"{'ok  ' if same else 'DIFF'} {t:42} {a.split(chr(9))[0]:>12} {b.split(chr(9))[0]:>12}", flush=True)
    if not same: bad.append((t, use))
    # per-block drill down on differing tables that carry a slot
for t, use in bad:
    expr = ",".join(f'"{c}"::text' for c in use)
    q = (f"select slot, count(*), sum(('x'||substr(md5(concat_ws('|',{expr})),1,15))::bit(60)::bigint::numeric) "
         f'from "{t}" where slot <= {S} group by 1')
    A = dict((l.split("\t")[0], l.split("\t")[1:]) for l in sql(inst, q)); B = dict((l.split("\t")[0], l.split("\t")[1:]) for l in sql(ctl, q))
    diff = sorted({int(k) for k in A.keys() | B.keys() if A.get(k) != B.get(k)})
    print(f"-- {t}: {len(diff)} slots differ")
    for s in diff[:40]:
        print(f"   slot {s}: {inst}={A.get(str(s),['0'])[0]} rows, {ctl}={B.get(str(s),['0'])[0]} rows")
print(f"RESULT {inst}: {len(bad)} tables differ: {[t for t, _ in bad]}")
