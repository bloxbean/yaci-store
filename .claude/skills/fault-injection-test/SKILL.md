---
name: fault-injection-test
description: Run the yaci-store auto-recovery fault-injection test (scripts/fault-injection) on a Linux host. Breaks the node connection, single blocks, rollbacks, Postgres and the JVM while yaci-store syncs preprod, checks that it recovers on its own with no lost or duplicate data, and compares every table with a fault-free instance. Use when asked to rerun the fault-injection / recovery / resilience test, test a build or PRs against it, run single scenarios (e.g. C06, J07), or interpret its results.
---

# Fault-injection test

The kit and its full documentation are in `scripts/fault-injection/README.md`. Read it first. Earlier runs and their findings are in issue #1328.

## Before you start

- **Ask for the host.** Ask the user which host to use, and how to reach it, if they haven't said. Don't write host names, users or keys into the repo or this skill.
- **Use your own `RT_HOME`.** One preprod instance needs about 16 GB of RAM and about 150 GB of disk. Check `free -g` and `df -h` first.
- **Leave other workloads alone.** Never stop or remove containers, volumes or processes the kit did not create. Its own are named `yaci-store-postgres-rt-<name>`, `rt-<name>-pg`, and the processes started from `$RT_HOME`.
- **Use JDK 21 (`RT_JAVA_HOME`).** Newer JDKs break the Lombok build.
- **Get the Byteman agent.** Download it into `$RT_HOME/byteman.jar` (see the README).

## Steps

1. **Pick the builds.** Usually there are two: one with the change (candidate) and one without (baseline, `origin/main`), plus a fault-free reference (`ctrl`, normally on the candidate jar). Edit `$RT_HOME/instances.conf` (copy it from the kit) and add the per-build properties.
2. **Build:** `build.sh <jar-name> <ref> [PR ...]`. Building runs the host hard, so build at low priority (`nice`) if other things run there. Write down which commits went into each jar; `build.sh` appends this to `jars/BUILD_INFO`.
3. **Start each instance:** `inst.sh <name> init|pg|proxy|start`, and run `mux_smoke.py` once.
4. **Run the matrices in the background,** with the initial sync first, then the tip: `runner.py <name> range; tipwait.sh <name>`. Initial sync takes about 6 h on preprod, and the tip matrix about 12 h. Poll every 20–60 minutes with `summary.py`; there is nothing to watch in between.
5. **Compare the data after the matrices end:**
   - `compare.py <name> ctrl`;
   - `sql/verify-unspent.sql` and `sql/unspent-extra-rows.sql`, if the unspent table is on.

   Look into every difference down to the rows. The scenario checks only cover block-level integrity. The table comparison finds the rest: in the last run it found a pool registration rewritten after a replay (#1335).
6. **Report:**
   - recovered / stuck / gap / duplicates per build and mode;
   - every new finding, with the root cause in code;
   - which differences are expected.

   Use plain wording and post it where the user asks (umbrella issue, PR comments).

## Pitfalls seen in earlier runs

- **`inst.sh start` takes the newest `<jar>-*.jar`.** Adding a new jar while a matrix runs mixes builds within one run. Keep new jars outside `jars/` until the matrix ends. Then re-run the scenarios you need with `runner.py <name> <mode> --only ID`, after moving the old result line out of `results/<name>.jsonl` (the runner skips IDs already there).
- **Old proxies from an earlier run can keep their ports.** `inst.sh proxy` now refuses to start if a port is in use. Find the old process by port (`ss -ltnp`) and stop it by PID: it may have been started with a relative path, so `pkill -f <full path>` misses it.
- **A Byteman rule that never fires looks like a pass.** For J04–J08, B08, C04 and RB07, check `console.out` for `[byteman] <rule>`. Rules name internal classes, so a rename silently disables them.
- **At the tip the scenarios share one database.** A gap or duplicate is reported again by every later scenario. Count it only where it first appears.
- **B04 leaves a gap on purpose** (`continue-on-parse-error=true`): skipping the block is the configured behaviour.
- **Timings vary with the host:** compare two builds only when they ran side by side.
- **Some markers are harmless:**
  - "Sync is stopped after a failure" is the safe path (#1313);
  - "Rollback point doesn't seem to be valid … slot=0" is the reconnect rollback to genesis.
