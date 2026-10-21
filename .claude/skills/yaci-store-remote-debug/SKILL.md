---
name: yaci-store-remote-debug
description: Debug and verify a yaci-store instance running on a remote VM over SSH — sync liveness, stalled adapot/reward jobs, disk exhaustion, log triage, and correctness cross-checks against Koios for both ada-pot values and governance proposal/voting status. Use when a user reports a remote yaci-store that has stopped syncing, is stuck on an epoch, has fallen behind tip, or whose ledger-state, ada-pot or proposal/vote numbers need verifying against an independent source.
---

# yaci-store remote instance debugging

Read-only triage of a yaci-store deployment on a remote host, plus a
correctness cross-check of ada-pot values against an independent source.

Built for yaci-store core developers debugging benchmark, staging and
long-running mainnet instances. Every command is read-only unless the
"Actions that change state" section says otherwise.

## Inputs

Ask only for what you cannot infer from the conversation or memory:

1. **Host alias** (required) — an entry in `~/.ssh/config` with the key already
   loaded in the agent. Password prompts cannot be answered; a non-interactive
   key is mandatory.
2. **Run-as user** (optional) — the account owning the deployment. Omit it if
   the login user can already reach docker and the files.
3. **Instance directory** (optional) — the compose/config directory. Auto-derived
   from container mounts when omitted.
4. **Container name** (optional, but required on hosts running more than one
   instance — benchmark hosts usually do).

## Helper scripts

In `scripts/`, next to this file. They exist because the ssh → sudo → docker →
psql chain needs four levels of quoting; getting that wrong wastes more time
than the debugging itself.

### `remote.sh` — run any command on the host

```sh
scripts/remote.sh --host <HOST> [--as <USER>] --probe
scripts/remote.sh --host <HOST> [--as <USER>] -- 'any shell command'
```

The command is base64-encoded in transit, so quotes, `$`, pipes and globs all
survive verbatim — write it exactly as you would type it locally. Run `--probe`
first: it reports the access mode, docker-group membership, and **both UTC and
local time** (see the timezone trap below). The detected mode is cached per
host+user; `--probe` re-detects and overwrites that cache, so use it whenever
access starts behaving unexpectedly (for example after a sudoers change).

It auto-detects how to become the target user, in order: `sudo -u USER` →
`sudo su - USER`. The second matters — a sudoers rule of `(root) NOPASSWD: ALL`
permits root but **not** other runas targets, so `sudo -u USER` fails while the
su hop through root works. It also strips terminal escapes that `sudo`'s
`use_pty` leaks into the first line of output.

### `db.sh` — run SQL against the instance's postgres

```sh
scripts/db.sh --host <HOST> [--as <USER>] --info
scripts/db.sh --host <HOST> --container <NAME> [--csv] -- 'SELECT ...'
```

Discovers the container, DB user, database and schema from the running host —
credentials come from the container's own environment, never from this file.
It sets `search_path` on every query. If the host runs several postgres
containers it lists them and stops rather than guessing.

### `koios_adapot_check.py` — verify ada-pot values

```sh
scripts/db.sh --host <HOST> --container <NAME> --csv -- \
  "select epoch,treasury,reserves,fees,deposits_stake,circulation
     from adapot where epoch between <A> and <B> order by epoch;" \
| grep -v '^SET$' | scripts/koios_adapot_check.py --network mainnet
```

Exits 0 on full agreement, 1 on any mismatch. See "Verifying correctness".

## Triage workflow

Work in this order. It is not arbitrary — disk exhaustion cascades into every
other symptom, and diagnosing upward from the application wastes time.

### 1. Disk first

```sh
scripts/remote.sh --host <HOST> -- 'df -h /; echo; sudo -n du -xh --max-depth=1 / 2>/dev/null | sort -rh | head -12'
```

Run this **without** `--as`: the deployment account usually has no sudo rights,
and `du` needs them (see the `du` gotcha). The login user is the one with sudo.

A full or nearly-full volume is the single most common cause of a stalled
instance. It kills postgres, which fails in-flight reward calculations, which
strands jobs and halts sync. If usage is above ~90%, treat it as the root cause
until proven otherwise and go to "Reclaiming disk".

### 2. Container state

```sh
scripts/remote.sh --host <HOST> -- 'docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Image}}" | head -20'
scripts/remote.sh --host <HOST> -- 'docker inspect -f "Running={{.State.Running}} OOMKilled={{.State.OOMKilled}} Restarts={{.RestartCount}} StartedAt={{.State.StartedAt}}" <CONTAINER>'
```

`OOMKilled=true` points at heap/`shm_size`, not disk. Note `StartedAt`: if the
app started *before* its postgres last restarted, the app may hold a dead
connection pool and need restarting even though it looks healthy.

### 3. Is the application actually alive?

```sh
scripts/remote.sh --host <HOST> -- 'date -u; docker logs --tail 5 <CONTAINER> 2>&1 | tail -5'
```

Compare the last log timestamp against **UTC**, not the host's local clock.

### 4. Sync position versus chain tip

Use `block_time`, which the block table already stores. It is network-agnostic
and needs no constants:

```sh
scripts/db.sh --host <HOST> --container <PG> -- \
  "select max(number) tip,
          to_timestamp(max(block_time)) at time zone 'UTC' as tip_time,
          extract(epoch from (now() at time zone 'UTC')
                  - to_timestamp(max(block_time)) at time zone 'UTC')::int as lag_seconds
     from block;"
```

`lag_seconds` under ~60 is at tip (mainnet produces a block roughly every 20s).
Minutes means falling behind; hours or days means stalled — find when it stopped
from the cursor below.

As an independent cross-check on **mainnet only**, slot can be converted
directly using the Shelley anchor — slot `4492800` at Unix `1596059091`:

```sql
to_timestamp(1596059091+(max(slot)-4492800)) at time zone 'UTC'
```

Both were verified to agree to the second on a live mainnet instance. Do **not**
reuse those constants on another network, and do not derive them from
`systemStart` alone: `systemStart` in `shelley-genesis.json` is the *Byron* chain
start (mainnet: 2017-09-23T21:44:51Z), and Byron slots are longer than Shelley's,
so `systemStart + slot` is wrong across the era boundary. Genesis files live at
`components/common/src/main/resources/store/networks/<network>/` in the repo, and
at whatever the instance's `store.cardano.{byron,shelley}-genesis-file` properties
point to on the host. Prefer `block_time` and avoid the problem entirely.

Then check the cursor, which shows when writes actually stopped:

```sh
scripts/db.sh --host <HOST> --container <PG> -- \
  "select block_number, slot, update_datetime from cursor_ order by slot desc limit 3;"
```

### 5. Ada-pot / reward job queue

```sh
scripts/db.sh --host <HOST> --container <PG> -- \
  "select status, count(*), min(epoch), max(epoch) from adapot_jobs group by status order by 3;"
scripts/db.sh --host <HOST> --container <PG> -- \
  "select epoch, status, error_message from adapot_jobs
    where coalesce(error_message,'') <> '' order by epoch;"
```

Healthy: a long `COMPLETED` run, at most one `STARTED`, the rest `NOT_STARTED`,
and **no** error messages. A `STARTED` row carrying an `error_message` is a dead
job, not a running one — see the failure catalog.

### 6. Errors in logs — check both sources

```sh
scripts/remote.sh --host <HOST> -- 'docker logs <CONTAINER> 2>&1 | grep -iE "error|exception|no space|fatal" | tail -40'
scripts/remote.sh --host <HOST> --as <USER> -- 'ls -la <INSTANCE_DIR>/logs/'
```

`docker logs` is rotated by the json-file driver (often only a few MB) and will
have discarded the window you care about. The rotated file logs under the
instance's `logs/` directory retain days more. Read those with `zcat` and
absolute paths.

### 7. Compare against a sibling instance

If the host runs another instance, check whether it stalled too. Two instances
with different feature sets failing at the same slot means the cause is
environmental, not a bug in whichever extension is suspected. This single check
prevents misattributing a disk outage to a store or extension.

## Known failure patterns

**Disk exhaustion cascade** — postgres logs `could not write lock file
"postmaster.pid": No space left on device` or `database system was interrupted`;
the app logs `UnknownHostException` for the postgres host (its container is
gone) or Hikari pool timeouts. Sync and reward calc stop within the same minute.
Fix: reclaim disk, then restart the app container.

**Stalled adapot job** — `adapot_jobs` shows one row `STARTED` with
`error_message` set, everything after it `NOT_STARTED`, and the analytics log
repeating `Skipping analytics export — AdaPot job in progress (epoch N)` every
minute for days. Cause: on retry exhaustion the processor records the error but
never changes `status`; `AdaPotJobStatus` has only `NOT_STARTED`, `STARTED` and
`COMPLETED` — there is no `FAILED`, so a dead job is indistinguishable from a
running one, and the analytics scheduler skips on `STARTED` without any
staleness or error check.
**Fix: restart the container — not SQL.** `AdaPotJobManager.resetStartedJobs()`
resets `STARTED → NOT_STARTED` at startup and re-queues. The re-run is safe:
`EpochRewardCalculationService` deletes existing leader/member rewards for the
epoch first, explicitly for the re-run case. Only fall back to a manual
`UPDATE` if a restart demonstrably fails to clear it.

**Sync dead while the app looks healthy** — schedulers still log every minute,
but the log contains no block-fetch or chain-sync entries and the cursor is
frozen. The sync pipeline died with the database and does not self-heal. Fix:
restart the container. Confirm with:
```sh
scripts/remote.sh --host <HOST> --as <USER> -- 'grep -oE "c\.b\.c\.y\.[a-zA-Z.]+ +:" <INSTANCE_DIR>/logs/<LOG> | sort | uniq -c | sort -rn | head'
```
Only scheduler loggers = dead pipeline.

**`relation "X" does not exist`** — almost always a missing `search_path`, not
data loss. `db.sh` sets it automatically; a bare `docker exec ... psql` does
not. Confirm the data is fine before alarming anyone:
```sh
scripts/db.sh --host <HOST> --container <PG> -- \
  "select table_schema, count(*) from information_schema.tables group by 1 order by 2 desc;"
```

**`role "postgres" does not exist`** — the deployment uses its own DB user. Use
`db.sh --info`; never assume `postgres`.

## Verifying correctness against Koios

Liveness is not correctness. An instance can sit at tip with wrong numbers.
Koios is an independent implementation, so agreement is real evidence.

```sh
scripts/db.sh --host <HOST> --container <PG> --csv -- \
  "select epoch,treasury,reserves,fees,deposits_stake,circulation
     from adapot where epoch between <A> and <B> order by epoch;" \
| grep -v '^SET$' | scripts/koios_adapot_check.py --network mainnet
```

Compared exactly: `treasury`, `reserves`, `fees`, `deposits_stake`.

**`circulation` needs a shift, and this is not a bug.** yaci reports the value
as at the *start* of epoch N (equivalently, the end of N−1), so
`yaci.circulation[N] == koios.supply[N−1]`. Koios's own `circulation` field is a
different quantity again. The script applies the shift; verified across
consecutive mainnet epochs, where `yaci.circulation[N] == 45e15 −
koios.reserves[N−1]` holds exactly. Comparing the two `circulation` fields
directly produces a false alarm — do not report it as a divergence.

Note the scope: this validates the **pot** calculation. Reward *distribution* is
a separate stage and can fail while the pots stay correct. To check that, use
Koios `/account_rewards` or `/pool_history` against `reward` for the epoch.

The script fetches all epochs in one request and skips any Koios has not
published yet.

Ada-pot values are finalised at the epoch boundary and have compared clean for
the current epoch in practice. Vote tallies do **not** behave that way — see
"Verifying governance and proposal status" before comparing those.

## Verifying governance and proposal status

Proposal status lives in `gov_action_proposal_status`, one row per proposal per
epoch, with a `voting_stats` JSONB column holding the DRep / SPO / committee
tallies.

```sh
scripts/db.sh --host <HOST> --container <PG> -- \
  "select gov_action_tx_hash, gov_action_index, type, status
     from gov_action_proposal_status
    where epoch = (select max(epoch) from gov_action_proposal_status) order by 1;"
```

Koios's `/proposal_list` is the cross-check: a proposal with no
`ratified_epoch`, `enacted_epoch`, `expired_epoch` or `dropped_epoch` is live,
and should correspond to an `ACTIVE` row. `/proposal_voting_summary?_proposal_id=`
gives the vote tallies for a deeper comparison.

### Two divergences against Koios that are NOT bugs

Both were investigated to the lovelace on a live mainnet instance. Recognise
them before reporting anything.

**1. yaci's `voting_stats` is an epoch-boundary snapshot; Koios is live.**

`ProposalStateProcessor` runs off `StakeSnapshotTakenEvent` — once, at the
boundary — and `ProposalStateService` collects votes with
`collectVotingDataBatch(proposals, epoch - 1)`. **Votes cast during the current
epoch are deliberately excluded.** Koios reflects them immediately.

So any DRep who votes during epoch N shows as YES in Koios while yaci still has
their stake under "did not vote" (which rolls into `drep_total_no_stake`). The
result is equal and opposite deltas in total-yes and total-no. Confirm rather
than guess:

```sh
# votes cast in the current epoch — each one explains part of the delta
scripts/db.sh --host <HOST> --container <PG> -- \
  "select vp.epoch, vp.vote, vp.voter_hash, dd.amount as drep_power
     from voting_procedure vp
     left join drep_dist dd
       on dd.drep_hash = vp.voter_hash and dd.epoch = vp.epoch
    where vp.gov_action_tx_hash = '<TX_HASH>'
      and vp.epoch = (select max(epoch) from gov_action_proposal_status)
    order by dd.amount desc nulls last;"
```

Sum those `drep_power` values: they should account for the yaci/Koios gap
exactly. A verified example matched a single voter's power to the lovelace. The
difference reconciles on its own at the next epoch boundary.

**Consequence for any comparison: only compare epochs that have closed.**
Mid-epoch drift against live Koios is expected, not a defect.

**2. Committee denominator differs — yaci excludes members with no authorized
hot key.**

A Conway committee member votes with a *hot* credential. A member who is seated
but has never authorized one cannot vote, and the ledger excludes them from the
acceptance ratio entirely rather than counting them as "no". yaci implements
this in `ProposalStateService`:

```java
// Ratification in epoch N uses the committee effective in epoch N,
// but only hot-key registrations that existed before entering epoch N.
committeeMemberStorage.getActiveCommitteeMembersDetailsForRatificationByEpoch(epoch);
```

which resolves to `COMMITTEE_REGISTRATION.EPOCH.lt(epoch)` — strictly before
epoch N. Koios's `committee_yes_pct` appears to divide by all seated members. So
a freshly seated member with no hot key makes yaci report `yes/6` where Koios
reports `yes/7`. **yaci is the stricter and ledger-correct side here.**

Note `VotingStatsService` also excludes abstentions from the denominator
(`ccYes + ccNo + ccDoNotVote`), which matches CIP-1694.

Before reporting a committee mismatch, list who can actually vote:

```sh
scripts/db.sh --host <HOST> --container <PG> -- \
  "select m.hash, m.start_epoch, m.expired_epoch,
          coalesce(max(r.epoch)::text,'NONE') as hotkey_reg_epoch,
          case when max(r.epoch) is null then 'CANNOT VOTE (no hot key)'
               when max(r.epoch) < <EPOCH> then 'can vote'
               else 'excluded (registered in current epoch)' end as ratification_status
     from committee_member m
     left join committee_registration r on r.cold_key = m.hash
    where m.epoch = <EPOCH> and m.expired_epoch > <EPOCH>
    group by 1,2,3 order by 4 desc;"
```

If yaci's denominator equals the "can vote" count, the numbers are right and the
gap is Koios counting seated-but-unauthorized members.

Separately, `committee_member` is epoch-versioned, so always filter
`epoch = <EPOCH> and expired_epoch > <EPOCH>`; an unfiltered `count(*)` returns
the full history, not the current committee.

### If you still need a tiebreaker

dbsync settles disagreements about ledger semantics. The repo has
`scripts/compare-dbsync-parquet/models/gov_action_proposal_status.py`, and the
`verify-dbsync` skill drives that comparison.

## Reclaiming disk

Establish what is actually orphaned before proposing any deletion.

```sh
scripts/remote.sh --host <HOST> -- 'sudo -n du -xh --max-depth=2 <DIR> 2>/dev/null | sort -rh | head -20'
scripts/remote.sh --host <HOST> -- 'for c in $(docker ps -aq); do n=$(docker inspect -f "{{.Name}}" $c); docker inspect -f "{{range .Mounts}}{{.Source}}{{\"\n\"}}{{end}}" $c | sed "s|^|$n |"; done'
scripts/remote.sh --host <HOST> -- 'docker system df'
```

Cross-reference every large directory against that mount list. For anything
that looks orphaned, prove it before recommending removal:

```sh
scripts/remote.sh --host <HOST> -- 'lsof +D <DIR> 2>/dev/null | head; ls <DIR>/postmaster.pid 2>/dev/null; cat <DIR>/PG_VERSION 2>/dev/null; stat -c "%y %n" <DIR>'
scripts/remote.sh --host <HOST> -- 'grep -rl "<DIR>" /etc/systemd/ /etc/fstab /etc/ 2>/dev/null | head'
```

Orphaned means all of: no container mount, no open files, no `postmaster.pid`,
no systemd/fstab/`/etc` reference, and an old mtime. A stale major-version
`PG_VERSION` that does not match any running container is strong corroboration.

Usually reclaimable: docker build cache and dangling images (`docker system df`
shows the reclaimable split), journald (`journalctl --vacuum-size=`), and node
databases that are no longer needed when the instance points at a remote relay
(check `store.cardano.host` in the config first).

## Reading configuration

Always redact when displaying config:

```sh
scripts/remote.sh --host <HOST> --as <USER> -- \
  'grep -vE "^\s*#|^\s*$" <INSTANCE_DIR>/config/application.properties | sed -E "s/(password|secret|key|token)=.*/\1=***/I"'
```

Worth noting: `store.cardano.host` (local node or remote relay),
`SPRING_PROFILES_ACTIVE` in the env file (which stores are on), and
`store.admin.auto-recovery-enabled` — check it before any restart, because if
enabled it triggers a recovery pass over the whole database at startup.

## Actions that change state

Everything above is read-only. Each of these needs explicit user confirmation,
every time — never infer approval from a general "go debug this":

- restarting or stopping a container
- `rm` of any path, however orphaned it looks
- `UPDATE`/`DELETE` SQL
- `docker system prune`
- `journalctl --vacuum-*`

Present the evidence and the reclaim/impact estimate, then ask. For deletions,
say plainly that it is irreversible.

## Gotchas

- **Timezone.** Container logs are UTC; `date`, `uptime` and file mtimes follow
  the host's local zone. Mixing them makes a live instance look dead. Always
  `date -u`, and use `--probe`, which prints both.
- **`cd` before `sudo` fails.** `cd <dir> && sudo cmd` runs `cd` as the login
  user and hits permission denied. Use absolute paths inside the elevated
  command; `remote.sh --as` handles this.
- **`docker logs` is lossy.** json-file rotation is small. Rotated file logs in
  the instance's `logs/` directory hold far more history.
- **`du` must run under sudo.** Database directories are root-owned, so an
  unprivileged `du` silently skips them — and `2>/dev/null` hides the
  permission-denied lines that would have told you. Observed undercount on a
  real host: 138G reported against 2.5T actual. Whenever `du` and `df`
  disagree, distrust `du` first and re-run it with `sudo -n`.
- **postgres 18 layout.** The image mounts `/var/lib/postgresql` with data under
  `<mount>/18/docker`, not `/var/lib/postgresql/data`.
- **Docker group.** Membership means docker commands need no sudo, but reading
  the instance's files still does.
- **One instance is not the whole host.** Check for siblings before attributing
  a failure to a store or extension.
