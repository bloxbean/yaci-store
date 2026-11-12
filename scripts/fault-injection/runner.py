#!/usr/bin/env python3
"""Runs fault scenarios against one instance and appends a JSON line per scenario to results/<inst>.jsonl.

usage: runner.py <instance> <range|tip> [--only ID,ID] [--skip ID,ID] [--list] [--gap SECONDS]
A scenario injects a fault, holds it, heals it, then waits for the sync to progress by itself. Afterwards the blocks
touched since it began are checked. STOP file in results/ ends the run after the current scenario.
"""
import argparse, json, os, re, signal, subprocess, sys, threading, time
KIT = os.path.dirname(os.path.abspath(__file__))
R = os.environ.get("RT_HOME", os.path.expanduser("~/fault-injection-test"))
_jh = os.environ.get("RT_JAVA_HOME") or os.environ.get("JAVA_HOME")
JCMD = os.path.join(_jh, "bin", "jcmd") if _jh else "jcmd"
SLOT_T0 = 1655683200   # preprod only: unix time = slot + this

def instance_index(name):
    """Index of an instance in instances.conf ($RT_HOME/instances.conf wins), which fixes its ports."""
    conf = f"{R}/instances.conf" if os.path.exists(f"{R}/instances.conf") else f"{KIT}/instances.conf"
    for line in open(conf):
        f = line.split()
        if f and not f[0].startswith("#") and f[0] == name:
            return int(f[1])
    raise SystemExit(f"unknown instance {name} (see {conf})")
THRESH_HOLD = 210      # > block-receive-delay (120 s) + health-check interval (30 s) + backoff margin
MARKERS = {
 "health_restart_events": r"Published restart event",
 "restart_scheduled": r"Scheduling sync restart",
 "restart_completed": r"Sync restart completed",
 "restart_errors": r"Error during sync restart",
 "max_attempts": r"Reached max restart attempts",
 "debounced": r"Within debounce window",
 "restart_in_progress": r"Restart already in progress",
 "link_rejected": r"does not build on the last block written",
 "failed_store_stop": r"Sync is stopped after a failure",
 "parse_skips": r"Continue on parse error",
 "error_saving": r"Error saving :",
 "intersect_nf": r"Intersection not found",
 "no_blocks": r"NoBlocks :",
 "stop_npe": r"Error stopping blockRangeSync|NullPointerException",
 "range_starts": r"Start BlockRangeSync",
 "sync_starts": r"Start BlockSync",
 "range_to_sync": r"Start N2N sync from block",
 "conn_failed": r"Connection failed",
 "invalid_rollback": r"Rollback point doesn't seem to be valid",
 "rollback_after_stored": r"is after the last stored block",
 "publishing_rollback": r"Publishing rollback event",
 "unspent_stale": r"(?i)unspent.*(stale|gap)",
 "error_lines": r"\bERROR\b",
 "adapot_job_failed": r"Adapot job processing failed|Reward calculation failed|Error processing reward calc job",
 "adapot_mismatch": r"Expected (reserves|treasury):",
 "adapot_matched": r"Treasury and reserves value matched",
}

class ReachedTip(Exception):
    pass

class Ctx:
    def __init__(s, inst):
        s.inst = inst
        s.i = instance_index(inst)
        s.nc, s.dc = 14100 + s.i, 14110 + s.i
        s.pg = f"yaci-store-postgres-rt-{inst}"
        s.d = f"{R}/inst/{inst}"
        s.logf = f"{s.d}/logs/yaci-store.log"
        s.consolef = f"{s.d}/logs/console.out"   # Byteman's traceln lines go to stdout only
        s.job_epoch = None
        s.notes = []
        s.bg = []
        s.stop_bg = threading.Event()
    def sh(s, cmd, timeout=60):
        try:
            return subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout).stdout.strip()
        except subprocess.TimeoutExpired:
            return None
    def sql(s, q, timeout=120, db="yaci_store"):
        try:
            p = subprocess.run(["docker", "exec", "-i", s.pg, "psql", "-U", "yaci", "-d", db, "-AtF", " ", "-c", q],
                               capture_output=True, text=True, timeout=timeout)
            return p.stdout.strip() if p.returncode == 0 else None
        except subprocess.TimeoutExpired:
            return None
    def n(s, *a): return s.sh(f"python3 {KIT}/faultctl.py {s.nc} {' '.join(map(str, a))}")
    def db(s, *a): return s.sh(f"python3 {KIT}/faultctl.py {s.dc} {' '.join(map(str, a))}")
    def docker(s, *a): return s.sh("docker " + " ".join(a), timeout=120)
    def inst_cmd(s, *a): return s.sh(f"{KIT}/inst.sh {s.inst} " + " ".join(a), timeout=120)
    def trig(s, name): open(f"{s.d}/trig/{name}", "w").close()
    def untrig(s, name):
        try: os.remove(f"{s.d}/trig/{name}")
        except FileNotFoundError: pass
    def pid(s):
        try: return int(open(f"{s.d}/app.pid").read().strip())
        except Exception: return None
    def alive(s):
        p = s.pid()
        if not p: return False
        try:
            os.kill(p, 0)
            return not open(f"/proc/{p}/stat").read().rsplit(")", 1)[1].split()[0] == "Z"
        except OSError:
            return False
    def signal(s, sig):
        p = s.pid()
        if p: os.kill(p, sig)
    def cursor(s):
        r = s.sql("select block_number, slot from cursor_ order by slot desc limit 1", timeout=15)
        if not r: return None
        a = r.split()
        return int(a[0]), int(a[1])
    def lag(s, slot): return time.time() - (slot + SLOT_T0)
    def log_size(s):
        try: return os.path.getsize(s.logf)
        except OSError: return 0
    def log_since(s, off):
        try:
            with open(s.logf, errors="replace") as f:
                if off > os.path.getsize(s.logf):   # rotated
                    off = 0
                f.seek(off)
                return f.read()
        except OSError:
            return ""
    def wait_log(s, off, pattern, timeout):
        t = time.time()
        while time.time() - t < timeout:
            if re.search(pattern, s.log_since(off)): return True
            time.sleep(0.5)
        return False
    def console_size(s):
        try: return os.path.getsize(s.consolef)
        except OSError: return 0
    def wait_console(s, off, pattern, timeout):
        t = time.time()
        while time.time() - t < timeout:
            try:
                with open(s.consolef, errors="replace") as f:
                    f.seek(off if off <= os.path.getsize(s.consolef) else 0)
                    if re.search(pattern, f.read()): return True
            except OSError:
                pass
            time.sleep(1)
        return False
    def has_table(s, t):
        return s.sql(f"select count(*) from information_schema.tables where table_name='{t}'", timeout=30) == "1"
    def job(s, epoch):
        r = s.sql(f"select status, coalesce(left(error_message, 160), '') from adapot_jobs where type='REWARD_CALC' and epoch={epoch}", timeout=30)
        if not r: return None, None
        a = r.split(" ", 1)
        return a[0], (a[1] if len(a) > 1 else "")
    def repeat(s, fn, every, dur):
        def run():
            end = time.time() + dur
            while time.time() < end and not s.stop_bg.is_set():
                fn(); time.sleep(every)
        t = threading.Thread(target=run, daemon=True); t.start(); s.bg.append(t)
    def join_bg(s):
        s.stop_bg.set()
        for t in s.bg: t.join(timeout=30)
        s.bg = []; s.stop_bg = threading.Event()
    def lock_table(s, table, secs):
        """Hold an exclusive lock from a separate session, so writers to it block."""
        subprocess.Popen(["docker", "exec", "-i", s.pg, "psql", "-U", "yaci", "-d", "yaci_store", "-c",
                          f"begin; lock table {table} in access exclusive mode; select pg_sleep({secs}); commit;"],
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    def terminate_app_backends(s, timeout=20):
        return s.sql("select count(pg_terminate_backend(pid)) from pg_stat_activity where datname='yaci_store' and pid<>pg_backend_pid()", timeout=timeout)
    def heal_all(s):
        s.n("pass"); s.db("pass")
        for t in os.listdir(f"{s.d}/trig"): s.untrig(t)

class Sc:
    def __init__(s, id, modes, title, inject, heal=None, hold=0, deadline=720, expect_restart=None, cost="n"):
        s.id, s.modes, s.title, s.inject, s.heal, s.hold, s.deadline, s.expect_restart = id, modes, title, inject, heal, hold, deadline, expect_restart

H = THRESH_HOLD
def wait_forward_drained(c, timeout=300):
    t = time.time()
    while time.time() - t < timeout:
        st = json.loads(c.n("status") or "{}")
        if st.get("inject", 1) == 0: return True
        time.sleep(0.2)
    c.notes.append("injected chain-sync messages were not drained in time")
    return False

def _pg_cmd(*a):
    return lambda c: c.docker(*a, c.pg)

def inj_stall_then(after):
    def f(c):
        off = c.log_size(); c.n("stall", "2,3")
        if not c.wait_log(off, r"Stopping sync service", 420):
            c.notes.append("health check never triggered a restart while stalled")
        after(c)
    return f

def inj_stale_1138(c):
    off = c.log_size(); c.n("stall", "2,3")
    if c.wait_log(off, r"Scheduling sync restart", 420):
        c.n("pass"); c.notes.append("healed during the restart backoff")
    else:
        c.notes.append("no restart scheduled while stalled")

def inj_cold_start(setup, down=5):
    def f(c):
        c.signal(signal.SIGKILL); time.sleep(down); setup(c); c.inst_cmd("start")
    return f

def inj_rollback_then(k, after=None, kind="rollback", settle=True):
    def f(c):
        t = time.time()
        while time.time() - t < 1500 and json.loads(c.n("status") or "{}").get("ring", 0) <= k:
            time.sleep(10)
        r = c.n(kind, k); c.notes.append(f"{kind} {k}: {r}")
        wait_forward_drained(c)
        if after: after(c)
    return f

def inj_at_batch_done(action):
    """Downtime long enough for a range fetch on restart, then act on the range -> sync switch."""
    def f(c):
        c.signal(signal.SIGKILL); time.sleep(2300)   # > block-diff-to-start-sync-protocol (2000 slots)
        off = c.log_size(); c.inst_cmd("start")
        if c.wait_log(off, r"Batch Done >>>|Start N2N sync from block", 600):
            action(c)
        else:
            c.notes.append("range fetch did not finish in time")
    return f

def _denyconn(c):
    c.sql("alter database yaci_store allow_connections false", db="postgres"); c.terminate_app_backends()
def _allowconn(c): c.sql("alter database yaci_store allow_connections true", db="postgres")
def _readonly(c):
    c.sql("alter system set default_transaction_read_only=on", db="postgres"); c.sql("select pg_reload_conf()", db="postgres"); c.terminate_app_backends()
def _readwrite(c):
    c.sql("alter system reset default_transaction_read_only", db="postgres"); c.sql("select pg_reload_conf()", db="postgres"); c.terminate_app_backends()
def _kill_backend(c):
    p = c.sql("select pid from pg_stat_activity where datname='yaci_store' and pid<>pg_backend_pid() order by state='active' desc limit 1")
    if p: c.docker("exec", c.pg, "kill", "-9", p.split()[0])
def _machine_crash(c):
    c.signal(signal.SIGKILL); c.docker("kill", c.pg); time.sleep(15); c.docker("start", c.pg); time.sleep(10); c.inst_cmd("start")

CONWAY_SLOT = 68774400   # preprod: first slot of epoch 163, the first Conway epoch

def in_step(rule, action, conway=False, timeout=5400):
    """Arms a ledger-state Byteman rule, waits until the step runs (the next epoch boundary for the AdaPot job), then acts."""
    def f(c):
        if conway:
            t = time.time()
            while time.time() - t < 36000:
                cur = c.cursor()
                if cur and cur[1] >= CONWAY_SLOT: break
                time.sleep(30)
        c.job_epoch = None
        off = c.console_size(); c.trig(rule)
        if not c.wait_console(off, r"\[byteman\] " + re.escape(rule) + r"\b", timeout):
            c.untrig(rule); c.notes.append(f"{rule}: the step did not run within {timeout} s"); return
        r = c.sql("select epoch from adapot_jobs where type='REWARD_CALC' and status='STARTED' order by epoch desc limit 1") if rule != "account-pause-commit" else None
        c.job_epoch = int(r) if r else None
        c.notes.append(f"{rule} fired" + (f" in the job for epoch {c.job_epoch}" if c.job_epoch else ""))
        action(c)
    return f

def _kill_restart(down=10):
    return lambda c: (c.signal(signal.SIGKILL), time.sleep(down), c.inst_cmd("start"))
def _blocked_on_reward(after):
    """The reward write starts by deleting the epoch's rewards: lock the table so it blocks there, then act."""
    return lambda c: (c.lock_table("reward", 150), time.sleep(55), after(c))

BOTH = ("range", "tip"); TIP = ("tip",); RANGE = ("range",)
SCENARIOS = [
 # --- node connection
 Sc("N01", BOTH, "Node connection reset (RST); yaci reconnect", lambda c: c.n("rst")),
 Sc("N02", BOTH, "Node connection closed (FIN)", lambda c: c.n("fin")),
 Sc("N03", BOTH, "Node refuses connections for 210 s", lambda c: (c.n("rst"), c.n("refuse", "on")), lambda c: c.n("pass"), H),
 Sc("N04", BOTH, "Network partition 210 s: no FIN, nothing flows, healed with TCP intact", lambda c: c.n("freeze", "on"), lambda c: c.n("pass"), H),
 Sc("N05", BOTH, "Block-fetch stalls 210 s while keep-alive still answers", lambda c: c.n("stall", "3"), lambda c: c.n("pass"), H),
 Sc("N06", TIP, "Chain-sync stalls 210 s while keep-alive still answers", lambda c: c.n("stall", "2"), lambda c: c.n("pass"), H),
 Sc("N07", BOTH, "Partition, and new connections accept but never answer, 300 s (handshake/TipFinder hang)", lambda c: (c.n("freeze", "on")), lambda c: c.n("pass"), 300),
 Sc("N08", BOTH, "Flapping: RST every 20 s for 300 s", lambda c: c.repeat(lambda: c.n("rst"), 20, 300), lambda c: c.join_bg(), 300),
 Sc("N09", BOTH, "Long outage 20 min, past the restart attempt cap", lambda c: (c.n("rst"), c.n("refuse", "on")), lambda c: c.n("pass"), 1200, 900),
 Sc("N10", BOTH, "Node answers MsgNoBlocks to fetch requests for 210 s (#1312)", lambda c: (c.n("nofetch", "on"), c.n("rst")), lambda c: c.n("pass"), H),
 Sc("N11", BOTH, "Slow network: throttled to 30 KB/s for 300 s (must not look like a stall)", lambda c: c.n("throttle", 30), lambda c: c.n("pass"), 300),
 Sc("N12", TIP, "IntersectNotFound on reconnect, twice", lambda c: (c.n("intersect-nf", 2), c.n("rst"))),
 # --- block delivery
 Sc("B01", BOTH, "One block dropped by the peer: block-fetch batch has a hole", lambda c: c.n("drop-block", 1)),
 Sc("B02", BOTH, "One block delivered twice", lambda c: c.n("dup-block", 1)),
 Sc("B03", BOTH, "Two blocks delivered in swapped order", lambda c: c.n("swap-block")),
 Sc("B04", BOTH, "Undecodable block, continue-on-parse-error=true", lambda c: c.n("corrupt-block", 1)),
 Sc("B05", TIP, "RollForward hidden: client never hears of one block", lambda c: c.n("skip-forward", 1)),
 Sc("B06", TIP, "Same RollForward delivered twice", lambda c: c.n("dup-forward")),
 Sc("B07", RANGE, "Reconnect in the middle of a batch (re-delivery, #1319)", lambda c: (c.n("stall-after", 37), time.sleep(8), c.n("rst")), lambda c: c.n("pass")),
 Sc("B08", BOTH, "Undecodable block and saving its error row fails", lambda c: (c.trig("error-save-fail"), c.n("corrupt-block", 1)), lambda c: c.untrig("error-save-fail"), 60),
 # --- rollback
 Sc("RB01", TIP, "Rollback 1 block, replayed", inj_rollback_then(1)),
 Sc("RB02", TIP, "Rollback 3 blocks, replayed", inj_rollback_then(3)),
 Sc("RB03", TIP, "Rollback 30 blocks, replayed", inj_rollback_then(30)),
 Sc("RB04", TIP, "Rollback 3 three times in a row", lambda c: [inj_rollback_then(3)(c) or time.sleep(90) for _ in range(3)]),
 Sc("RB05", TIP, "Rollback 3, no replay: next block does not link", inj_rollback_then(3, kind="rollback-noreplay")),
 Sc("RB06", TIP, "Rollback 3, then connection reset as it finishes", inj_rollback_then(3, lambda c: c.n("rst"))),
 Sc("RB07", TIP, "Rollback 3, a rollback listener fails once", lambda c: (c.trig("rollback-listener-fail"), inj_rollback_then(3)(c))),
 Sc("RB08", TIP, "Rollback 3, app killed just after, restart", inj_rollback_then(3, lambda c: (c.signal(signal.SIGKILL), time.sleep(5), c.inst_cmd("start")))),
 Sc("RB09", TIP, "Rollback 3, all DB connections reset just after", inj_rollback_then(3, lambda c: c.db("rst"))),
 Sc("RB10", TIP, "Rollback 3 while address_utxo is locked 210 s (slow rollback passes the health threshold)", lambda c: (c.lock_table("address_utxo", 210), inj_rollback_then(3)(c)), None, 210),
 Sc("RB11", TIP, "Startup rollback: app killed, restart, killed again during the startup rollback, restart",
    lambda c: (lambda off: (c.signal(signal.SIGKILL), time.sleep(3), c.inst_cmd("start"), c.wait_log(off, r"Publishing (pre-)?rollback event", 240), c.signal(signal.SIGKILL), time.sleep(3), c.inst_cmd("start")))(c.log_size())),
 # --- database
 Sc("D01", BOTH, "Postgres restarted", _pg_cmd("restart", "-t", "1")),
 Sc("D02", BOTH, "Postgres down 210 s", _pg_cmd("stop", "-t", "1"), _pg_cmd("start"), H),
 Sc("D03", BOTH, "Postgres frozen 210 s (docker pause: sockets open, no answer)", _pg_cmd("pause"), _pg_cmd("unpause"), H),
 Sc("D04", BOTH, "DB network partition 210 s: JDBC sockets hang, no FIN", lambda c: c.db("freeze", "on"), lambda c: c.db("pass"), H),
 Sc("D05", BOTH, "All DB connections reset", lambda c: c.db("rst")),
 Sc("D06", BOTH, "cursor_ table locked 210 s: writer blocks past the health threshold", lambda c: c.lock_table("cursor_", 210), None, H),
 Sc("D07", BOTH, "block table locked 210 s", lambda c: c.lock_table("block", 210), None, H),
 Sc("D08", BOTH, "Database read-only 120 s (failover/disk-full behaviour)", _readonly, _readwrite, 120),
 Sc("D09", BOTH, "Database rejects connections 210 s", _denyconn, _allowconn, H),
 Sc("D10", BOTH, "Postgres backend killed -9 (crash recovery of the whole server)", _kill_backend),
 Sc("D11", BOTH, "New DB connections hang 210 s after existing ones reset", lambda c: (c.db("new-silent", "on"), c.db("rst")), lambda c: c.db("pass"), H),
 # --- JVM
 Sc("J01", BOTH, "JVM frozen 210 s (SIGSTOP)", lambda c: c.signal(signal.SIGSTOP), lambda c: c.signal(signal.SIGCONT), H),
 Sc("J02", BOTH, "kill -9, restart after 30 s", lambda c: (c.signal(signal.SIGKILL), time.sleep(30), c.inst_cmd("start"))),
 Sc("J03", BOTH, "SIGTERM, restart after 10 s", lambda c: (c.inst_cmd("stop"), time.sleep(10), c.inst_cmd("start"))),
 Sc("J04", BOTH, "A store listener fails once while processing a block", lambda c: c.trig("tx-listener-fail")),
 Sc("J05", BOTH, "OutOfMemoryError in onBlock's first lookup, once", lambda c: c.trig("oom-first-lookup")),
 Sc("J06", BOTH, "DB failure in onBlock's first lookup, once", lambda c: c.trig("db-fail-first-lookup")),
 Sc("J07", BOTH, "A listener hangs 300 s on one block (#1129 overlapping sessions)", lambda c: c.trig("listener-hang"), None, 320),
 Sc("J08", BOTH, "setCursor fails once", lambda c: c.trig("cursor-fail")),
 Sc("J09", BOTH, "App and Postgres killed together (host crash)", _machine_crash),
 # --- combinations aimed at the restart path
 Sc("C01", BOTH, "Stall, then Postgres down while the restart runs 180 s (#959)", inj_stall_then(lambda c: c.docker("stop", "-t", "1", c.pg)), lambda c: (c.docker("start", c.pg), c.n("pass")), 180),
 Sc("C02", BOTH, "Stall, then node refuses during the restart 180 s (#959)", inj_stall_then(lambda c: (c.n("rst"), c.n("refuse", "on"))), lambda c: c.n("pass"), 180),
 Sc("C03", BOTH, "Stall, then new node connections silent during the restart 240 s", inj_stall_then(lambda c: (c.n("new-silent", "on"), c.n("rst"))), lambda c: c.n("pass"), 240),
 Sc("C04", BOTH, "Stall, then StartService fails during the restart 180 s", inj_stall_then(lambda c: c.trig("init-fail")), lambda c: (c.untrig("init-fail"), c.n("pass")), 180),
 Sc("C05", BOTH, "Stall healed during the restart backoff: stale restart (#1138)", inj_stale_1138, lambda c: c.n("pass")),
 Sc("C06", BOTH, "Fresh JVM whose first connection never gets a block, 300 s (lastReceivedBlockTime=0)", inj_cold_start(lambda c: c.n("new-silent", "on")), lambda c: c.n("pass"), 300),
 Sc("C07", BOTH, "Fresh JVM while the node refuses, 120 s", inj_cold_start(lambda c: c.n("refuse", "on")), lambda c: (c.n("pass"), None if c.alive() else (c.notes.append("JVM exited on start, restarted by the harness"), c.inst_cmd("start"))), 120),
 Sc("C08", TIP, "Restart right at the tip (#1314 race), 10 times", lambda c: [(c.signal(signal.SIGKILL), time.sleep(2), c.inst_cmd("start"), time.sleep(70)) for _ in range(10)]),
 # --- range to sync transition: 38 min of downtime forces a range fetch on restart
 Sc("X01", TIP, "Range fetch to sync switch after 38 min down, no fault", inj_at_batch_done(lambda c: None), None, 0, 1500),
 Sc("X02", TIP, "Node reset right at the range to sync switch", inj_at_batch_done(lambda c: c.n("rst")), None, 0, 1500),
 Sc("X03", TIP, "kill -9 right at the range to sync switch", inj_at_batch_done(lambda c: (c.signal(signal.SIGKILL), time.sleep(3), c.inst_cmd("start"))), None, 0, 1500),
 # --- ledger-state (needs the ledger-state profile): faults inside the epoch-boundary AdaPot job and the account step
 Sc("L01", RANGE, "kill -9 during the reward calculation, restart after 10 s", in_step("adapot-pause-calc", _kill_restart())),
 Sc("L02", RANGE, "kill -9 in the middle of the reward write (after the AdaPot update)", in_step("adapot-pause-update", _blocked_on_reward(_kill_restart()))),
 Sc("L03", RANGE, "Postgres down 210 s during the reward calculation", in_step("adapot-pause-calc", _pg_cmd("stop", "-t", "1")), _pg_cmd("start"), H),
 Sc("L04", RANGE, "Postgres restarted during the stake snapshot", in_step("adapot-pause-snapshot", _pg_cmd("restart", "-t", "1"))),
 Sc("L05", RANGE, "All DB connections reset in the middle of the reward write", in_step("adapot-pause-update", _blocked_on_reward(lambda c: c.db("rst")))),
 Sc("L06", RANGE, "SIGTERM during the stake snapshot, restart after 10 s", in_step("adapot-pause-snapshot", lambda c: (c.inst_cmd("stop"), time.sleep(10), c.inst_cmd("start")))),
 Sc("L07", RANGE, "The reward calculation throws once", in_step("adapot-fail-calc", lambda c: None)),
 Sc("L08", RANGE, "The stake snapshot throws once", in_step("adapot-fail-snapshot", lambda c: None)),
 Sc("L09", RANGE, "Postgres frozen 210 s (docker pause) in the middle of the reward write", in_step("adapot-pause-update", _blocked_on_reward(_pg_cmd("pause"))), _pg_cmd("unpause"), H),
 Sc("L10", RANGE, "JVM frozen 210 s (SIGSTOP) during the reward calculation", in_step("adapot-pause-calc", lambda c: c.signal(signal.SIGSTOP)), lambda c: c.signal(signal.SIGCONT), H),
 Sc("L11", RANGE, "App and Postgres killed together in the middle of the reward write", in_step("adapot-pause-update", _blocked_on_reward(_machine_crash))),
 Sc("L12", RANGE, "kill -9 during the account balance step of a commit", in_step("account-pause-commit", _kill_restart())),
 Sc("L13", RANGE, "Postgres down 210 s during the account balance step of a commit", in_step("account-pause-commit", _pg_cmd("stop", "-t", "1")), _pg_cmd("start"), H),
 Sc("L14", RANGE, "Conway: kill -9 during the proposal status update", in_step("gov-pause-proposal", _kill_restart(), conway=True)),
 Sc("L15", RANGE, "Conway: the proposal status update throws once", in_step("gov-fail-proposal", lambda c: None, conway=True)),
 Sc("L16", RANGE, "Conway: all DB connections reset during the proposal status update", in_step("gov-pause-proposal", lambda c: c.db("rst"), conway=True)),
 Sc("L17", RANGE, "Conway: kill -9 during the reward calculation", in_step("adapot-pause-calc", _kill_restart(), conway=True)),
 Sc("L18", RANGE, "Conway: Postgres down 210 s during the reward calculation", in_step("adapot-pause-calc", _pg_cmd("stop", "-t", "1"), conway=True), _pg_cmd("start"), H),
 Sc("L19", RANGE, "Conway: kill -9 in the middle of the reward write", in_step("adapot-pause-update", _blocked_on_reward(_kill_restart()), conway=True)),
 Sc("L20", TIP, "At the tip: kill -9 during the reward calculation at the next epoch boundary", in_step("adapot-pause-calc", _kill_restart(), timeout=5 * 86400)),
]

def checks(c, frm, to):
    """Integrity of blocks frm..to. Anything non-empty is an anomaly."""
    q = {
     "gap_numbers": f"select n from generate_series({frm},{to}) n left join block b on b.number=n where b.hash is null limit 25",
     "broken_parent_link": f"select b.number from block b left join block p on p.hash=b.prev_hash where p.hash is null and b.number between {frm+1} and {to} limit 25",
     "two_blocks_same_number": f"select number from block where number between {frm} and {to} group by number having count(*)>1 limit 25",
     "tx_count_mismatch": f"with t as (select block,count(*) c from transaction where block between {frm} and {to} group by 1) select b.number,b.no_of_txs,coalesce(t.c,0) from block b left join t on t.block=b.number where b.number between {frm} and {to} and b.no_of_txs<>coalesce(t.c,0) limit 25",
     "duplicate_tx": f"select tx_hash from transaction where block between {frm} and {to} group by 1 having count(*)>1 limit 10",
     "dup_metadata": f"select tx_hash,label from transaction_metadata where block between {frm} and {to} group by 1,2 having count(*)>1 limit 10",
     "dup_assets": f"select tx_hash,unit from assets where block between {frm} and {to} group by 1,2 having count(*)>1 limit 10",
     "dup_scripts": f"select tx_hash,purpose,redeemer_index from transaction_scripts where block between {frm} and {to} group by 1,2,3 having count(*)>1 limit 10",
     "dup_mir": f"select tx_hash,cert_index,credential from mir where block between {frm} and {to} group by 1,2,3 having count(*)>1 limit 10",
     "cursor_hash_not_stored": "select c.block_number from cursor_ c left join block b on b.hash=c.block_hash where b.hash is null and c.slot=(select max(slot) from cursor_)",
    }
    out = {}
    for k, s in q.items():
        r = c.sql(s, timeout=300)
        if r is None: out[k] = "CHECK_FAILED"
        elif r: out[k] = r.replace("\n", "; ")[:400]
    return out

def run_scenario(c, sc, mode, resdir):
    c.notes = []
    res = {"id": sc.id, "inst": c.inst, "mode": mode, "title": sc.title, "start": time.strftime("%FT%TZ", time.gmtime())}
    def ensure_up():
        if not c.alive():
            c.notes.append("app was down before the scenario; started by the harness"); c.inst_cmd("start")
    ensure_up()
    t = time.time(); cur = None
    while time.time() - t < 600:
        cur = c.cursor()
        if cur: break
        time.sleep(5)
    if mode == "tip":
        # only meaningful once the instance is really following the tip
        t = time.time()
        while time.time() - t < 3600:
            cur = c.cursor()
            if cur and c.lag(cur[1]) < 240: break
            time.sleep(10)
        else:
            c.notes.append("instance was not at the tip after waiting 1 h")
    elif cur and c.lag(cur[1]) < 7200:
        print(f"{c.inst} {sc.id}: within 2 h of the tip, range scenarios cannot run any more", flush=True)
        raise ReachedTip()
    off = c.log_size()
    err0 = c.sql("select coalesce(max(id),0) from error") or "0"
    rb0 = c.sql("select coalesce(max(id),0) from rollback") or "0"
    res["cursor_before"] = cur[0] if cur else None
    res["lag_before_s"] = int(c.lag(cur[1])) if cur else None
    print(f"[{time.strftime('%T')}] {c.inst} {sc.id} {mode}: {sc.title} (cursor {cur})", flush=True)
    sc.inject(c)
    if sc.hold: time.sleep(sc.hold)
    alive_during = c.alive()
    if sc.heal: sc.heal(c)
    t_heal = time.time()
    base = None
    for _ in range(40):
        base = c.cursor()
        if base: break
        time.sleep(3)
    # progress in range mode: a few batches; at the tip: a new block and close to the chain
    need = 300 if mode == "range" else 1
    recovered = False; ttr = None; exited = False
    while time.time() - t_heal < sc.deadline:
        cc = c.cursor()
        if cc and base and cc[0] >= base[0] + need and (mode == "range" or c.lag(cc[1]) < 240):
            recovered, ttr = True, int(time.time() - t_heal); break
        if not c.alive() and not exited:
            exited = True; res["app_exited"] = True; break
        time.sleep(3)
    if not recovered and c.alive():
        d = f"{resdir}/{c.inst}-{mode}-{sc.id}"
        os.makedirs(d, exist_ok=True)
        c.sh(f"{JCMD} {c.pid()} Thread.print > {d}/threads.txt 2>&1", timeout=60)
        c.sh(f"curl -s -m 5 localhost:{18100+c.i}/actuator/health > {d}/health.json", timeout=10)
    res.update(recovered=recovered, ttr_s=ttr, alive_during_fault=alive_during)
    res["node_proxy"] = json.loads(c.n("status") or "{}")
    res["open_node_conns"] = res["node_proxy"].get("conns")
    text = c.log_since(off)
    res["markers"] = {k: n for k, p in MARKERS.items() if (n := len(re.findall(p, text)))}
    d = f"{resdir}/{c.inst}-{mode}-{sc.id}"; os.makedirs(d, exist_ok=True)
    with open(f"{d}/errors.log", "w") as f:
        f.writelines(l + "\n" for l in text.splitlines() if re.search(r"\bERROR\b|\bWARN\b|Exception|byteman|restart|Restart", l))
    if not recovered:
        # record it and put the instance back, so the remaining scenarios still run
        c.heal_all()
        if c.alive(): c.signal(signal.SIGKILL); time.sleep(3)
        c.inst_cmd("start")
        res["needed_manual_restart"] = True
        t2 = time.time()
        while time.time() - t2 < 300:
            cc = c.cursor()
            if cc and base and cc[0] > base[0]: break
            time.sleep(5)
    if c.job_epoch is not None:
        t = time.time(); st = err = None
        while time.time() - t < 1800:
            st, err = c.job(c.job_epoch)
            if st == "COMPLETED": break
            time.sleep(10)
        res["job"] = {"epoch": c.job_epoch, "status": st, "error": err, "completed": st == "COMPLETED",
                      "wait_s": int(time.time() - t)}
        c.job_epoch = None
    if c.has_table("adapot_jobs"):
        res["adapot_jobs"] = c.sql("select status || ':' || count(*) || ':' || max(epoch) from adapot_jobs group by status order by 1")
        res["adapot_jobs_not_done"] = c.sql("select epoch || ' ' || status || ' ' || coalesce(left(error_message, 80), '') from adapot_jobs where status <> 'COMPLETED' order by epoch limit 10")
    # let it settle so batches in flight are committed, then check the blocks touched
    time.sleep(35)
    after = c.cursor()
    if after and cur:
        frm = max(cur[0] - 600, 1)
        res["cursor_after"] = after[0]; res["checked"] = [frm, after[0]]
        res["anomalies"] = checks(c, frm, after[0])
    res["new_error_rows"] = c.sql(f"select id,block,error_code,left(reason,100) from error where id>{err0} order by id limit 10")
    res["new_rollback_rows"] = c.sql(f"select count(*) from rollback where id>{rb0}")
    res["notes"] = c.notes
    res["end"] = time.strftime("%FT%TZ", time.gmtime())
    with open(f"{resdir}/{c.inst}.jsonl", "a") as f:
        f.write(json.dumps(res) + "\n")
    print(f"[{time.strftime('%T')}] {c.inst} {sc.id} -> recovered={recovered} ttr={ttr} anomalies={res.get('anomalies')} markers={res['markers']}", flush=True)

# --- fresh start: an empty database that starts syncing from a recent block (store.cardano.sync-start-slot).
# The start block is taken from a reference instance (--ref). Each scenario wipes the instance's database, so run
# these last, after the range/tip scenarios and the comparison with the reference.
class Fresh:
    def __init__(s, id, title, back, arm, expect, deadline=1200):
        # back: blocks behind the reference's tip (more than ~40 on preprod means a range fetch, fewer chain sync)
        # expect: "start" the start block is stored; "next" chain sync stores the block after it; "skip" the start
        # block is skipped as a recorded parse error and the next block is stored
        s.id, s.title, s.back, s.arm, s.expect, s.deadline = id, title, back, arm, expect, deadline
        s.modes = ("fresh",)

FRESH = [
 Fresh("F01", "Fresh start (range fetch): the start block is dropped by the peer", 600, lambda c: c.n("drop-block", 1), "start"),
 Fresh("F02", "Fresh start (range fetch), no fault: the start block is stored", 600, lambda c: None, "start"),
 Fresh("F03", "Fresh start (chain sync): the first RollForward is hidden", 10, lambda c: c.n("skip-forward", 1), "next"),
 Fresh("F04", "Fresh start (range fetch): the start block is undecodable, continue-on-parse-error=true", 600, lambda c: c.n("corrupt-block", 1), "skip"),
]

def run_fresh(c, sc, ref, resdir):
    c.notes = []
    res = {"id": sc.id, "inst": c.inst, "mode": "fresh", "title": sc.title, "start": time.strftime("%FT%TZ", time.gmtime())}
    r = ref.sql(f"select number, slot, hash from block where number = (select max(number) from block) - {sc.back}")
    if not r: raise RuntimeError(f"no start block from the reference {ref.inst}")
    num, slot, h = r.split(); num, slot = int(num), int(slot)
    res["start_block"] = {"number": num, "slot": slot, "hash": h}
    if c.alive(): c.signal(signal.SIGKILL); time.sleep(3)
    c.heal_all()
    if c.sql("drop database if exists yaci_store with (force)", db="postgres") is None or \
       c.sql("create database yaci_store owner yaci", db="postgres") is None:
        raise RuntimeError("could not recreate the database")
    sc.arm(c)
    print(f"[{time.strftime('%T')}] {c.inst} {sc.id} fresh: {sc.title} (start block {num})", flush=True)
    off = c.log_size() or 0
    c.inst_cmd("start", f"--store.cardano.sync-start-slot={slot}", f"--store.cardano.sync-start-blockhash={h}")
    t0 = time.time(); recovered = False; ttr = None
    while time.time() - t0 < sc.deadline:
        cc = c.cursor()
        if cc and cc[0] > num + 1 and c.lag(cc[1]) < 240:
            recovered, ttr = True, int(time.time() - t0); break
        if not c.alive(): res["app_exited"] = True; break
        time.sleep(5)
    c.heal_all()
    time.sleep(35)
    after = c.cursor()
    res.update(recovered=recovered, ttr_s=ttr)
    text = c.log_since(off)
    res["markers"] = {k: n for k, p in MARKERS.items() if (n := len(re.findall(p, text)))}
    res["first_block_check"] = len(re.findall(r"must be the start block", text))
    stored_start = c.sql(f"select count(*) from block where hash = '{h}'")
    # an undecodable block may not even give its number, so any recorded parse error counts
    errs = c.sql("select count(*) from error where error_code = 'BLOCK_PARSE_ERROR'")
    an = {}
    if sc.expect == "start" and stored_start != "1": an["start_block_missing"] = stored_start
    if sc.expect == "skip" and (stored_start != "0" or errs in (None, "0")): an["start_block_skip_not_recorded"] = f"stored={stored_start} error_rows={errs}"
    first = num if sc.expect == "start" else num + 1
    if c.sql(f"select count(*) from block where number = {num + 1}") != "1": an["block_after_start_missing"] = str(num + 1)
    if after:
        res["cursor_after"] = after[0]; res["checked"] = [first, after[0]]
        an.update(checks(c, first, after[0]))
    res["anomalies"] = an
    res["error_rows"] = c.sql("select id,block,error_code,left(reason,100) from error order by id limit 10")
    res["notes"] = c.notes
    res["end"] = time.strftime("%FT%TZ", time.gmtime())
    with open(f"{resdir}/{c.inst}.jsonl", "a") as f:
        f.write(json.dumps(res) + "\n")
    print(f"[{time.strftime('%T')}] {c.inst} {sc.id} -> recovered={recovered} ttr={ttr} anomalies={an} markers={res['markers']}", flush=True)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("inst"); ap.add_argument("mode", choices=BOTH + ("fresh",))
    ap.add_argument("--ref", default="ctrl", help="fresh mode: reference instance that gives the start block")
    ap.add_argument("--only", default=""); ap.add_argument("--skip", default=""); ap.add_argument("--list", action="store_true")
    ap.add_argument("--gap", type=int, default=60)
    ap.add_argument("--catalog", action="store_true", help="print every scenario as a markdown table and exit")
    a = ap.parse_args()
    if a.catalog:
        groups = {"N": "Node connection", "B": "Block delivery", "RB": "Rollback", "D": "Database", "J": "JVM and listeners",
                  "C": "Restart path", "X": "Range fetch to chain-sync switch",
                  "L": "Ledger-state (AdaPot job, account balances, governance)",
                  "F": "Fresh start from a recent block (mode fresh, wipes the database; run last)"}
        print("| ID | Fault | Initial sync | At the tip | Fault held for |\n|---|---|---|---|---|")
        last = None
        for sc in SCENARIOS + FRESH:
            g = "RB" if sc.id.startswith("RB") else sc.id[0]
            if g != last:
                print(f"| | **{groups[g]}** | | | |"); last = g
            held = f"{sc.hold} s" if getattr(sc, "hold", 0) else "one shot"
            print(f"| {sc.id} | {sc.title} | {'yes' if 'range' in sc.modes else '-'} | {'yes' if 'tip' in sc.modes else '-'} | {held} |")
        return
    only = set(filter(None, a.only.split(","))); skip = set(filter(None, a.skip.split(",")))
    todo = [s for s in SCENARIOS + FRESH if a.mode in s.modes and (not only or s.id in only) and s.id not in skip]
    if a.list:
        for s in todo: print(s.id, s.title)
        return
    resdir = f"{R}/results"; os.makedirs(resdir, exist_ok=True)
    c = Ctx(a.inst)
    done = set()
    p = f"{resdir}/{a.inst}.jsonl"
    if os.path.exists(p):
        done = {(j["id"], j["mode"]) for j in map(json.loads, open(p))}
    for sc in todo:
        if os.path.exists(f"{resdir}/STOP"): print("STOP file, ending"); break
        if (sc.id, a.mode) in done: continue
        try:
            if a.mode == "fresh": run_fresh(c, sc, Ctx(a.ref), resdir)
            else: run_scenario(c, sc, a.mode, resdir)
        except ReachedTip:
            with open(f"{resdir}/{a.inst}.jsonl", "a") as f:
                f.write(json.dumps({"id": sc.id, "inst": a.inst, "mode": a.mode, "title": sc.title, "not_run": "reached the tip first"}) + "\n")
            continue
        except Exception as e:
            print(f"scenario {sc.id} failed in the harness: {e!r}", flush=True)
            with open(f"{resdir}/{a.inst}.jsonl", "a") as f:
                f.write(json.dumps({"id": sc.id, "inst": a.inst, "mode": a.mode, "title": sc.title, "harness_error": repr(e)}) + "\n")
            c.heal_all(); c.join_bg()
        time.sleep(a.gap)
    print(f"RUNNER_DONE {a.inst} {a.mode}", flush=True)

if __name__ == "__main__":
    main()
