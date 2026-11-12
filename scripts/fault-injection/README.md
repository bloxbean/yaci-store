# Auto-recovery fault-injection test

This kit checks one question: **when something breaks, does yaci-store recover on its own, without losing or duplicating data?**

It syncs yaci-store on **preprod** while it breaks things on purpose: the node connection, single blocks, rollbacks, the database, the JVM, and restarts at bad moments. After each fault it checks:

- that sync starts moving again with no help, and how long that takes;
- that no block is missing, no number has two blocks, and every block points to the one before it.

At the end, a script compares every table with a fault-free reference instance.

The first run and the re-test, with all findings and fixes, are in [#1328](https://github.com/bloxbean/yaci-store/issues/1328).

## How it works

```
                      ┌───────────────┐
 preprod relay ◀──────│  node proxy   │◀── yaci-store (JVM + Byteman rules) ──▶ DB proxy ──▶ Postgres (docker)
                      │ (fault-proxy) │                   ▲                          ▲
                      └───────▲───────┘                   │ kill / SIGSTOP           │ docker stop / pause / kill,
                              │ faultctl.py               │ trigger files            │ locks, read-only, …
                              └───────────── runner.py ───┴──────────────────────────┘
```

- **`fault-proxy.py`** sits between the store and the node (`--mux` mode) or between the store and Postgres (plain TCP mode). It is controlled over a small TCP port with `faultctl.py`.
  - In mux mode it understands the Ouroboros mini-protocol framing. So it can:
    - drop, repeat, swap or corrupt **one block** in a block-fetch batch;
    - hide or repeat a chain-sync RollForward;
    - answer `IntersectNotFound` or `NoBlocks`;
    - forge a rollback;
    - stall one mini-protocol while keep-alive still answers.
  - In both modes it can reset, close, refuse, freeze, slow down or throttle connections.
- **`rules.btm`** has Byteman rules loaded into the JVM (`-javaagent`). A rule fires when a file with its name exists in the instance's `trig/` directory, so the runner arms it with `touch`. The rules throw at chosen points:
  - a store listener;
  - the first lookup in `onBlock` (a database error, or an OutOfMemoryError);
  - `setCursor`;
  - saving an error row;
  - `StartService` init;
  - a rollback listener;
  - a listener that hangs for 300 s.
- **`runner.py`** runs the scenarios one after another against one instance. For each one it:
  1. applies the fault;
  2. holds it;
  3. removes it;
  4. waits for the sync to move by itself;
  5. checks the blocks touched since the scenario began.

  It appends one JSON line per scenario to `$RT_HOME/results/<instance>.jsonl`, and keeps the scenario's log lines, error lines and a thread dump (when stuck) in `$RT_HOME/results/<instance>-<mode>-<ID>/`. It skips scenarios already in that file, so an interrupted run resumes where it stopped. Creating `$RT_HOME/results/STOP` ends the run after the current scenario.
- **`inst.sh`** manages one instance (config, Postgres container, proxies, JVM). Instances are listed in `instances.conf`.

### When a scenario passes

After the fault is removed, the sync must move forward within the scenario's deadline: 12 minutes by default, 15 for N09 (the 20-minute outage) and 25 for X01–X03:
- **initial sync:** 300 blocks past the cursor;
- **at the tip:** a new block, with the cursor less than 240 s behind the chain.

`ttr_s` is the time from the fault ending to that point. If the sync does not recover, the runner restarts the app (`needed_manual_restart`) so the next scenario can run, and marks this one as stuck.

The runner then checks the block range the scenario touched:
- `gap_numbers` (missing block);
- `two_blocks_same_number`;
- `broken_parent_link`;
- `tx_count_mismatch`;
- duplicate rows in the UUID-keyed tables.

Anything found is stored under `anomalies`. Log counters (restarts, link-check rejections, parse skips, NPEs, …) are stored under `markers`.

At the tip the scenarios run back to back on one database, so a gap found by one scenario is seen again by the next ones. Count each anomaly only where it first appears.

## Requirements

- A Linux host with Docker, Python 3.10+, git, and JDK 21. Plan for about 16 GB of RAM per instance (6 GB heap plus Postgres) and about 150 GB of disk per preprod instance.
- The Byteman agent jar (tested with 4.0.25):
  ```
  curl -fLo $RT_HOME/byteman.jar https://repo1.maven.org/maven2/org/jboss/byteman/byteman/4.0.25/byteman-4.0.25.jar
  ```
- Network access to a preprod relay.

### Settings (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `RT_HOME` | `~/fault-injection-test` | Jars, instance data and logs, results |
| `RT_JAVA_HOME` | `$JAVA_HOME` | JDK 21 used to build and run |
| `RT_RELAY` | `preprod-node.play.dev.cardano.org:3001` | Upstream preprod node |
| `RT_BYTEMAN_JAR` | `$RT_HOME/byteman.jar` | Byteman agent |
| `RT_BASE_PROPERTIES` | `config/application.properties` of this repo | Base config copied into each instance |
| `RT_LEDGER_PROPERTIES` | `application-ledger-state.properties` next to the base config | Copied into instances with the `ledger-state` profile |

The test config (`inst.sh init`) uses:
- `store.continue-on-parse-error=true`;
- auto-recovery on, with `store.admin.health-check-interval=30` and `store.block-receive-delay-seconds=120`;
- parallel processing on;
- adapot and governance-aggr off, unless the instance has the `ledger-state` profile.

For a ledger-state run, give the instance the profiles `ledger-state,rt`, in that order. The later profile wins, and `rt` sets the log file the runner reads.

Per-instance properties go in the last column of `instances.conf`. For example, `store.utxo.unspent-table-enabled=true`.

## Running it

```bash
export RT_HOME=~/fault-injection-test RT_JAVA_HOME=~/.sdkman/candidates/java/21.0.3-tem
K=scripts/fault-injection          # this directory

# 1. Build the jars named in instances.conf (or edit instances.conf)
$K/build.sh baseline  origin/main
$K/build.sh candidate origin/main 1313 1271      # main + PRs

# 2. Start each instance: config, Postgres, proxies, app
for n in cand base ctrl; do $K/inst.sh $n init; $K/inst.sh $n pg; $K/inst.sh $n proxy; $K/inst.sh $n start; done
python3 $K/mux_smoke.py $K/fault-proxy.py        # parser self-test of the proxy

# 3. Initial-sync matrix (runs while the instance syncs from genesis, about 6 h on preprod),
#    then the tip matrix once the instance follows the tip (about 12 h)
nohup sh -c "python3 $K/runner.py cand range; $K/tipwait.sh cand" > $RT_HOME/logs/runner-cand.log 2>&1 &
nohup sh -c "python3 $K/runner.py base range; $K/tipwait.sh base" > $RT_HOME/logs/runner-base.log 2>&1 &
# ctrl gets no faults: it is the reference for the final comparison

# 4. Results
python3 $K/summary.py cand base                  # one line per scenario
python3 $K/compare.py cand ctrl                  # every table vs the reference, drilled down to slots
docker exec -i yaci-store-postgres-rt-cand psql -U yaci -d yaci_store < $K/sql/verify-unspent.sql   # if the unspent table is on
```

More options:
- **Run some scenarios only:** `runner.py cand tip --only C06,J07`. To run one again, first remove its line from `results/<instance>.jsonl`.
- **List the scenarios of a mode:** `runner.py cand tip --list`. Print the catalog below: `runner.py x tip --catalog`.
- **Keep two builds comparable:** run them side by side on the same host, starting from the same point. Timings depend on the stretch of chain and on what else runs on the host.

### Fault proxy commands

`python3 faultctl.py <control port> <command>`. The node proxy control port is 14100+i, and the DB proxy is 14110+i.

| Command | Effect |
|---|---|
| `pass` | Remove every fault |
| `status` | Counters, and which faults are on |
| `rst` / `fin` | Reset / close every open connection |
| `refuse on` | Refuse new connections |
| `freeze on` | Stop forwarding in both directions, keep sockets open (network partition) |
| `new-silent on` / `new-rst on` | New connections are accepted and never answered / are reset |
| `latency MS` / `throttle KBPS` | Slow the connection down |
| `stall 2,3` | Hold chain-sync (2) and/or block-fetch (3) frames while keep-alive still answers |
| `stall-after N` | Stall block-fetch after N more blocks |
| `nofetch on` | Answer every block-fetch request with `NoBlocks` |
| `intersect-nf N` | Answer the next N FindIntersect with `IntersectNotFound` |
| `drop-block N` / `dup-block N` / `swap-block` / `corrupt-block N` | Change one block of the next block-fetch batch |
| `skip-forward N` / `dup-forward` | Hide / repeat a chain-sync RollForward |
| `rollback K` / `rollback-noreplay K` | Forge a rollback of K blocks, then replay them (or not) |

The block, forward and rollback commands are mux mode only.

## Scenarios

| ID | Fault | Initial sync | At the tip | Fault held for |
|---|---|---|---|---|
| | **Node connection** | | | |
| N01 | Node connection reset (RST); yaci reconnect | yes | yes | one shot |
| N02 | Node connection closed (FIN) | yes | yes | one shot |
| N03 | Node refuses connections for 210 s | yes | yes | 210 s |
| N04 | Network partition 210 s: no FIN, nothing flows, healed with TCP intact | yes | yes | 210 s |
| N05 | Block-fetch stalls 210 s while keep-alive still answers | yes | yes | 210 s |
| N06 | Chain-sync stalls 210 s while keep-alive still answers | - | yes | 210 s |
| N07 | Partition, and new connections accept but never answer, 300 s (handshake/TipFinder hang) | yes | yes | 300 s |
| N08 | Flapping: RST every 20 s for 300 s | yes | yes | 300 s |
| N09 | Long outage 20 min, past the restart attempt cap | yes | yes | 1200 s |
| N10 | Node answers MsgNoBlocks to fetch requests for 210 s (#1312) | yes | yes | 210 s |
| N11 | Slow network: throttled to 30 KB/s for 300 s (must not look like a stall) | yes | yes | 300 s |
| N12 | IntersectNotFound on reconnect, twice | - | yes | one shot |
| | **Block delivery** | | | |
| B01 | One block dropped by the peer: block-fetch batch has a hole | yes | yes | one shot |
| B02 | One block delivered twice | yes | yes | one shot |
| B03 | Two blocks delivered in swapped order | yes | yes | one shot |
| B04 | Undecodable block, continue-on-parse-error=true | yes | yes | one shot |
| B05 | RollForward hidden: client never hears of one block | - | yes | one shot |
| B06 | Same RollForward delivered twice | - | yes | one shot |
| B07 | Reconnect in the middle of a batch (re-delivery, #1319) | yes | - | one shot |
| B08 | Undecodable block and saving its error row fails | yes | yes | 60 s |
| | **Rollback** | | | |
| RB01 | Rollback 1 block, replayed | - | yes | one shot |
| RB02 | Rollback 3 blocks, replayed | - | yes | one shot |
| RB03 | Rollback 30 blocks, replayed | - | yes | one shot |
| RB04 | Rollback 3 three times in a row | - | yes | one shot |
| RB05 | Rollback 3, no replay: next block does not link | - | yes | one shot |
| RB06 | Rollback 3, then connection reset as it finishes | - | yes | one shot |
| RB07 | Rollback 3, a rollback listener fails once | - | yes | one shot |
| RB08 | Rollback 3, app killed just after, restart | - | yes | one shot |
| RB09 | Rollback 3, all DB connections reset just after | - | yes | one shot |
| RB10 | Rollback 3 while address_utxo is locked 210 s (slow rollback passes the health threshold) | - | yes | 210 s |
| RB11 | Startup rollback: app killed, restart, killed again during the startup rollback, restart | - | yes | one shot |
| | **Database** | | | |
| D01 | Postgres restarted | yes | yes | one shot |
| D02 | Postgres down 210 s | yes | yes | 210 s |
| D03 | Postgres frozen 210 s (docker pause: sockets open, no answer) | yes | yes | 210 s |
| D04 | DB network partition 210 s: JDBC sockets hang, no FIN | yes | yes | 210 s |
| D05 | All DB connections reset | yes | yes | one shot |
| D06 | cursor_ table locked 210 s: writer blocks past the health threshold | yes | yes | 210 s |
| D07 | block table locked 210 s | yes | yes | 210 s |
| D08 | Database read-only 120 s (failover/disk-full behaviour) | yes | yes | 120 s |
| D09 | Database rejects connections 210 s | yes | yes | 210 s |
| D10 | Postgres backend killed -9 (crash recovery of the whole server) | yes | yes | one shot |
| D11 | New DB connections hang 210 s after existing ones reset | yes | yes | 210 s |
| | **JVM and listeners** | | | |
| J01 | JVM frozen 210 s (SIGSTOP) | yes | yes | 210 s |
| J02 | kill -9, restart after 30 s | yes | yes | one shot |
| J03 | SIGTERM, restart after 10 s | yes | yes | one shot |
| J04 | A store listener fails once while processing a block | yes | yes | one shot |
| J05 | OutOfMemoryError in onBlock's first lookup, once | yes | yes | one shot |
| J06 | DB failure in onBlock's first lookup, once | yes | yes | one shot |
| J07 | A listener hangs 300 s on one block (#1129 overlapping sessions) | yes | yes | 320 s |
| J08 | setCursor fails once | yes | yes | one shot |
| J09 | App and Postgres killed together (host crash) | yes | yes | one shot |
| | **Restart path** | | | |
| C01 | Stall, then Postgres down while the restart runs 180 s (#959) | yes | yes | 180 s |
| C02 | Stall, then node refuses during the restart 180 s (#959) | yes | yes | 180 s |
| C03 | Stall, then new node connections silent during the restart 240 s | yes | yes | 240 s |
| C04 | Stall, then StartService fails during the restart 180 s | yes | yes | 180 s |
| C05 | Stall healed during the restart backoff: stale restart (#1138) | yes | yes | one shot |
| C06 | Fresh JVM whose first connection never gets a block, 300 s (lastReceivedBlockTime=0) | yes | yes | 300 s |
| C07 | Fresh JVM while the node refuses, 120 s | yes | yes | 120 s |
| C08 | Restart right at the tip (#1314 race), 10 times | - | yes | one shot |
| | **Range fetch to chain-sync switch** | | | |
| X01 | Range fetch to sync switch after 38 min down, no fault | - | yes | one shot |
| X02 | Node reset right at the range to sync switch | - | yes | one shot |
| X03 | kill -9 right at the range to sync switch | - | yes | one shot |

"Fault held for" is how long the fault stays on before the runner removes it. "One shot" faults happen once (a reset, a dropped block, a kill).

### Ledger-state scenarios (L01–L20)
These need the `ledger-state` profile. They aim at work that runs outside the block-by-block sync:
- the AdaPot job at each epoch boundary: reward calculation, reward write, stake snapshot;
- the account balance step of each commit;
- the governance proposal status update (Conway only).

How they work:
- **Placing the fault:** a Byteman rule holds the step for 45 s, or makes it throw once. The runner waits for the rule's marker in `console.out` and injects the fault while the step is held. For the reward write, it also locks `reward`, so the job is stopped after the AdaPot row is updated.
- **Passing:** besides the normal checks, the AdaPot job for that epoch must reach `COMPLETED` within 30 min. Every scenario also records the `adapot_jobs` state.
- **Timing:** L01–L13 run on the next epoch boundary, so start them early in the initial sync. L14–L19 first wait for the first Conway slot. L20 waits at the tip for the next real epoch boundary, up to 5 days, so run it on its own (`--only L20`) after the tip matrix.

`compare.py` also compares epoch-keyed tables (`epoch_stake`, `drep_dist`, …), up to the last epoch whose job completed in both stores.

### Fresh-start scenarios (F01–F04)
These check the first block of a sync that starts on an empty database. They run in their own mode, `fresh`. **Each one drops and recreates the instance's database**, so run them last, after the range and tip matrices and `compare.py`:

```bash
python3 $K/runner.py cand fresh --ref ctrl
```

How they work:
- **Starting:** the start block is the reference instance's (`--ref`) tip minus 600 blocks, so the store does a range fetch. For F03 it is 10 blocks back, so the store uses chain sync. The runner kills the app, recreates the `yaci_store` database, arms the fault, and starts the app with `store.cardano.sync-start-slot` and `store.cardano.sync-start-blockhash`.
- **Passing:**
  - the store reaches the tip;
  - the normal block checks find nothing, from the start block (or, for chain sync, the block after it) to the cursor;
  - the start block is stored (F01, F02). For F04 it is skipped and a `BLOCK_PARSE_ERROR` row records the skip.
- **What they catch:** F01 drops the start block itself. A store that accepts any first block on a fresh start would sync on without it and never notice.

## Keeping it working

- **Byteman rules name internal classes and methods** (`TransactionProcessor.handleTransactionEvent`, `EraService.checkIfNewEra`, `CursorServiceImpl.setCursor`, `InitService.init`, …). If one is renamed, its rule stops firing without an error. Check that the `[byteman] <rule>` line appears in `console.out` when the scenario runs.
- **The runner reads log lines** to time some faults and to count markers (`MARKERS` in `runner.py`). Update the patterns when those log messages change.
- **Preprod only:** the runner uses preprod's slot-to-time offset (`SLOT_T0`), and `protocol-magic=1`.
- **Proxy changes:** run `mux_smoke.py` after changing the proxy's CBOR parsing.
