#!/usr/bin/env python3
"""One line per scenario from results/<inst>.jsonl. usage: summary.py [inst ...]"""
import json, os, sys
R = os.path.join(os.environ.get("RT_HOME", os.path.expanduser("~/fault-injection-test")), "results")
KEEP = ("health_restart_events","restart_completed","restart_errors","max_attempts","debounced","restart_in_progress","link_rejected","failed_store_stop","parse_skips","intersect_nf","no_blocks","stop_npe","range_starts","sync_starts","range_to_sync","invalid_rollback","rollback_after_stored")
for n in sys.argv[1:] or sorted(f[:-6] for f in os.listdir(R) if f.endswith(".jsonl")):
    rows = [json.loads(l) for l in open(f"{R}/{n}.jsonl")]
    print(f"== {n}: {len(rows)} scenarios")
    for j in rows:
        a = j.get("anomalies") or {}; m = j.get("markers") or {}
        st = "HARNESS" if j.get("harness_error") else "NOTRUN" if j.get("not_run") else "ok" if j.get("recovered") else "STUCK"
        print(f"{j['id']:5} {j['mode']:5} {st:7} ttr={j.get('ttr_s')} conns={j.get('open_node_conns')}"
              + (" MANUAL" if j.get("needed_manual_restart") else "") + (" EXITED" if j.get("app_exited") else "")
              + (f" ANOM={ {k: str(v)[:60] for k, v in a.items()} }" if a else "")
              + f" { {k: m[k] for k in KEEP if k in m} }" + (f" ERRROWS={j['new_error_rows'][:90]!r}" if j.get("new_error_rows") else "")
              + (f" NOTES={j['notes']}" if j.get("notes") else "") + (f" {j['harness_error'][:100]}" if j.get("harness_error") else ""))
