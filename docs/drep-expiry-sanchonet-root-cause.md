# Sanchonet DRep `active_until` Root Cause

Date: 2026-05-26

## Summary

`cardano-db-sync.drep_distr.active_until` stores the ledger snapshot's raw `DRepState.drepExpiry`.
`yaci-store.drep_dist.active_until` was derived from yaci-store's own `expiry` value by subtracting
the current `gov_epoch_activity.dormant_epoch_count` and by applying an extra PV9 dormant-period
override. That derivation is not equivalent to the ledger.

The ledger updates DRep expiry statefully:

1. DRep registration/update/vote sets `drepExpiry` from the current epoch and the current dormant
   epoch counter.
2. A transaction that submits a governance proposal flushes pending dormant epochs into every active
   DRep's stored `drepExpiry`, then resets the dormant counter.
3. Later dormant epochs only increase the pending counter; they do not continuously change the stored
   `drepExpiry` until another proposal/update/vote event occurs.

The mismatch on Sanchonet is caused by yaci-store treating `active_until` as an epoch-level derived
value instead of replaying the ledger's stored `drepExpiry` transitions.

## Root Cause Diagrams

High-level data flow:

```mermaid
flowchart LR
    subgraph Ledger["cardano-ledger"]
        Reg["DRep registration"]
        Vote["DRep vote/update"]
        Dormant["Dormant epoch counter<br/>vsNumDormantEpochs"]
        Proposal["Proposal transaction"]
        RawExpiry["Stored DRepState.drepExpiry"]

        Reg --> RawExpiry
        Vote --> RawExpiry
        Dormant --> Proposal
        Proposal -->|"add pending dormant epochs<br/>then reset counter"| RawExpiry
    end

    RawExpiry --> Snapshot["Ledger snapshot"]
    Snapshot --> DbSync["cardano-db-sync<br/>drep_distr.active_until"]

    subgraph YaciPrePatch["yaci-store pre-patch"]
        EffectiveExpiry["calculateDRepExpiry()<br/>effective expiry"]
        GovActivity["gov_epoch_activity.dormant_epoch_count"]
        DerivedActiveUntil["drep_dist.active_until<br/>expiry - dormant count<br/>plus PV9 override"]
        EffectiveExpiry --> DerivedActiveUntil
        GovActivity --> DerivedActiveUntil
    end

    DbSync -. mismatch .- DerivedActiveUntil
```

Ledger state machine for raw `active_until`:

```mermaid
stateDiagram-v2
    [*] --> Unregistered
    Unregistered --> Registered: REG_DREP_CERT
    Registered --> Registered: UPDATE_DREP_CERT / set expiry = epoch + activity - counter
    Registered --> Registered: DRep vote / set expiry = epoch + activity - counter
    Registered --> Registered: dormant epoch / counter = counter + 1
    Registered --> Registered: proposal tx / expiry = expiry + counter; counter = 0
    Registered --> Expired: currentEpoch > expiry + counter
    Expired --> Expired: proposal tx does not revive expired DRep
    Registered --> Unregistered: UNREG_DREP_CERT
```

Sanchonet PV9 sample for DRep `967c86...`:

```mermaid
flowchart LR
    E493["493 REG_DREP_CERT<br/>PV9 drep_activity=20<br/>active_until=513"]
    D493_579["493..579 dormant<br/>pending counter=87"]
    E580["580 proposal tx<br/>flush +87<br/>active_until=600"]
    D581_591["581..591 dormant<br/>pending counter=11"]
    E592["592 proposal tx<br/>flush +11<br/>active_until=611"]
    E598["598 DRep vote<br/>active_until=598+20=618"]

    E493 --> D493_579 --> E580 --> D581_591 --> E592 --> E598

    Yaci582["yaci-store pre-patch at 582<br/>active_until=513"]
    DbSync582["db-sync at 582<br/>active_until=600"]
    E580 --> DbSync582
    E493 --> Yaci582
```

Sanchonet off-by-one sample for DRep `ff72...` at epoch 916:

```mermaid
flowchart LR
    Vote910["910 DRep vote<br/>active_until=930"]
    D912_914["912..914 dormant<br/>pending counter=3"]
    Proposal915["915 proposal tx<br/>ledger flushes +3"]
    Ledger916["916 db-sync<br/>active_until=933"]
    Yaci916["916 yaci-store pre-patch<br/>active_until=934"]
    Count915["Root error<br/>proposal epoch 915 counted<br/>as dormant for stored value"]

    Vote910 --> D912_914 --> Proposal915 --> Ledger916
    D912_914 --> Count915 --> Yaci916
```

## Why The Focused Test Was Slow

The command attempted was:

```bash
./gradlew :aggregates:governance-aggr:test --tests com.bloxbean.cardano.yaci.store.governanceaggr.processor.DRepExpiryUtilTest
```

It did not jump straight to the two unit tests. Gradle first configured the multi-module build and
ran dependent setup tasks, including jOOQ/code generation and Flyway migration for store modules.
The run had reached `:stores:governance:flywayMigrate` and emitted a schema-version warning before it
was interrupted. The setup cost dominated the runtime; the test logic itself is small.

## Inputs

- Report: `drep-mismatches/sanchonet/compare_all_20260521_173930`
- DB Sync info: `drep-mismatches/sanchonet/db-sync-db.txt`
  - `postgresql://postgres:***@10.4.10.135:5688/cexplorer`
- yaci-store info: `drep-mismatches/sanchonet/yaci-store-db.txt`
  - `jdbc:postgresql://10.4.10.112:54320/yaci_store?currentSchema=yaci_store`
- DB Sync schema: `drep-mismatches/dbsync/dbsync_schema.md`
  - `drep_distr.active_until`: "The epoch until which this drep is active."
- Comparator: `scripts/compare/compare_drep_active_until.py`
  - lines 51-58 select DB Sync `drep_distr.active_until` and yaci-store `drep_dist.active_until`.

## Debug Query Pack

The queries below are written for `psql`. Adjust `search_path`, host, port, and credentials to match
the target environment. For yaci-store Sanchonet, the report points at schema `yaci_store`.

Set useful variables first:

```sql
\set epoch_a 582
\set epoch_b 916
\set epoch_c 1000
\set epoch_d 1015
\set drep_967 '967c86ac16aea79dea7609c8b186f91c259a458a00e7d4ed3a7f9d3a'
\set drep_ff72 'ff72c7e189bc85b3ac3928fea9248ae1f64feb8bdbc9269b1019dd27'
\set drep_192 '1923810f1a50c04d70ed0aa8c6e548b41931ff07eaa27861a768c364'
\set drep_fa3a 'fa3a5db6a24b09748d2f83dd14161314e45cf38ff32bf4e8a874b322'
```

### DB Sync: read baseline `active_until`

```sql
SELECT
    dd.epoch_no,
    encode(dh.raw, 'hex') AS drep_hash,
    dh.has_script,
    dd.active_until
FROM drep_distr dd
JOIN drep_hash dh ON dh.id = dd.hash_id
WHERE dd.epoch_no IN (:epoch_a, :epoch_b, :epoch_c, :epoch_d)
  AND encode(dh.raw, 'hex') IN (:'drep_967', :'drep_ff72', :'drep_192', :'drep_fa3a')
  AND dd.active_until IS NOT NULL
ORDER BY dd.epoch_no, drep_hash;
```

Expected rows from the investigation included:

```text
582|967c86...|f|600
916|192381...|t|923
916|ff72...|f|933
1000|fa3a...|f|997
1000|ff72...|f|1013
1015|fa3a...|f|1016
1015|ff72...|f|1033
```

### yaci-store: read computed `active_until` and `expiry`

```sql
SET search_path TO yaci_store;

SELECT
    epoch,
    drep_hash,
    drep_type,
    active_until,
    expiry
FROM drep_dist
WHERE epoch IN (:epoch_a, :epoch_b, :epoch_c, :epoch_d)
  AND drep_hash IN (:'drep_967', :'drep_ff72', :'drep_192', :'drep_fa3a')
ORDER BY epoch, drep_hash, drep_type;
```

Expected rows from the investigation included:

```text
582|967c86...|ADDR_KEYHASH|513|601
916|192381...|SCRIPTHASH|924|924
916|ff72...|ADDR_KEYHASH|934|934
1000|fa3a...|ADDR_KEYHASH|994|1006
1000|ff72...|ADDR_KEYHASH|1007|1019
1015|fa3a...|ADDR_KEYHASH|994|1021
1015|ff72...|ADDR_KEYHASH|1007|1034
```

### yaci-store: inspect dormant counter history

Use this to see whether yaci-store's epoch-level dormant counter can explain a mismatch:

```sql
SET search_path TO yaci_store;

SELECT
    epoch,
    dormant,
    dormant_epoch_count
FROM gov_epoch_activity
WHERE epoch BETWEEN 492 AND 595
ORDER BY epoch;
```

For the epoch 916 off-by-one sample:

```sql
SET search_path TO yaci_store;

SELECT
    epoch,
    dormant,
    dormant_epoch_count
FROM gov_epoch_activity
WHERE epoch BETWEEN 908 AND 916
ORDER BY epoch;
```

The key signal is that `gov_epoch_activity` can show epoch 915 as dormant, but a proposal transaction
inside epoch 915 resets the ledger's pending dormant counter before DB Sync snapshots `active_until`.

### yaci-store: inspect DRep registration and protocol params

```sql
SET search_path TO yaci_store;

SELECT
    dr.drep_hash,
    dr.cred_type,
    dr.type,
    dr.epoch,
    dr.slot,
    dr.tx_index,
    dr.cert_index,
    ep.params ->> 'protocol_major_ver' AS protocol_major_ver,
    ep.params ->> 'drep_activity' AS drep_activity
FROM drep_registration dr
JOIN epoch_param ep ON ep.epoch = dr.epoch
WHERE dr.drep_hash IN (:'drep_967', :'drep_ff72', :'drep_192', :'drep_fa3a')
ORDER BY dr.drep_hash, dr.epoch, dr.slot, dr.tx_index, dr.cert_index;
```

Expected row for the first Sanchonet sample:

```text
967c86...|ADDR_KEYHASH|REG_DREP_CERT|493|42596698|0|0|9|20
```

### yaci-store: inspect proposal submissions and proposal params

Use this to find proposal transactions that should flush the pending dormant counter:

```sql
SET search_path TO yaci_store;

SELECT
    gap.epoch,
    gap.slot,
    gap.tx_index,
    gap.idx,
    gap.type,
    ep.params ->> 'gov_action_lifetime' AS gov_action_lifetime
FROM gov_action_proposal gap
JOIN epoch_param ep ON ep.epoch = gap.epoch
WHERE gap.epoch BETWEEN 492 AND 1015
ORDER BY gap.epoch, gap.slot, gap.tx_index, gap.idx;
```

Focused variants:

```sql
-- First mismatch window.
SELECT epoch, slot, tx_index, idx, type
FROM gov_action_proposal
WHERE epoch BETWEEN 575 AND 595
ORDER BY epoch, slot, tx_index, idx;

-- Epoch 916 off-by-one window.
SELECT epoch, slot, tx_index, idx, type
FROM gov_action_proposal
WHERE epoch BETWEEN 908 AND 916
ORDER BY epoch, slot, tx_index, idx;
```

Expected proposal rows included:

```text
580|50184338|0|0|HARD_FORK_INITIATION_ACTION
592|51202227|0|0|INFO_ACTION
915|79111020|0|0|UPDATE_COMMITTEE
```

### yaci-store: inspect DRep vote/update interaction events

This query lists the events that should set raw `active_until` for a DRep. It normalizes
`voting_procedure.voter_type` to the same DRep type names used by `drep_dist`.

```sql
SET search_path TO yaci_store;

WITH target_dreps(drep_hash, drep_type) AS (
    VALUES
        (:'drep_967', 'ADDR_KEYHASH'),
        (:'drep_ff72', 'ADDR_KEYHASH'),
        (:'drep_192', 'SCRIPTHASH'),
        (:'drep_fa3a', 'ADDR_KEYHASH')
),
interactions AS (
    SELECT
        'UPDATE_DREP_CERT' AS event_kind,
        dr.drep_hash,
        dr.cred_type AS drep_type,
        dr.epoch,
        dr.slot,
        dr.tx_index,
        dr.cert_index AS event_index
    FROM drep_registration dr
    JOIN target_dreps td
      ON td.drep_hash = dr.drep_hash
     AND td.drep_type = dr.cred_type
    WHERE dr.type = 'UPDATE_DREP_CERT'

    UNION ALL

    SELECT
        'DREP_VOTE' AS event_kind,
        vp.voter_hash AS drep_hash,
        CASE
            WHEN vp.voter_type = 'DREP_KEY_HASH' THEN 'ADDR_KEYHASH'
            WHEN vp.voter_type = 'DREP_SCRIPT_HASH' THEN 'SCRIPTHASH'
        END AS drep_type,
        vp.epoch,
        vp.slot,
        vp.tx_index,
        vp.idx AS event_index
    FROM voting_procedure vp
    JOIN target_dreps td
      ON td.drep_hash = vp.voter_hash
     AND td.drep_type = CASE
            WHEN vp.voter_type = 'DREP_KEY_HASH' THEN 'ADDR_KEYHASH'
            WHEN vp.voter_type = 'DREP_SCRIPT_HASH' THEN 'SCRIPTHASH'
        END
    WHERE vp.voter_type IN ('DREP_KEY_HASH', 'DREP_SCRIPT_HASH')
)
SELECT
    i.event_kind,
    i.drep_hash,
    i.drep_type,
    i.epoch,
    i.slot,
    i.tx_index,
    i.event_index,
    ep.params ->> 'drep_activity' AS drep_activity
FROM interactions i
JOIN epoch_param ep ON ep.epoch = i.epoch
WHERE i.epoch <= 1015
ORDER BY i.drep_hash, i.epoch, i.slot, i.tx_index, i.event_index;
```

Focused vote history for `ff72...`:

```sql
SET search_path TO yaci_store;

SELECT
    epoch,
    slot,
    tx_index,
    idx,
    voter_type,
    voter_hash,
    vote
FROM voting_procedure
WHERE voter_hash = :'drep_ff72'
  AND voter_type = 'DREP_KEY_HASH'
  AND epoch BETWEEN 900 AND 1015
ORDER BY epoch, slot, tx_index, idx;
```

### yaci-store: build an event timeline in ledger order

This query is useful when debugging one DRep because it puts registration, interaction, and proposal
events into one sorted stream. The result is not a full recursive replay, but it shows the exact event
order the replay algorithm must consume.

```sql
SET search_path TO yaci_store;

WITH target AS (
    SELECT :'drep_967'::text AS drep_hash, 'ADDR_KEYHASH'::text AS drep_type
),
events AS (
    SELECT
        'REGISTRATION' AS event_kind,
        1 AS event_priority,
        dr.epoch,
        dr.slot,
        dr.tx_index,
        dr.cert_index AS event_index,
        dr.drep_hash,
        dr.cred_type AS drep_type,
        ep.params ->> 'protocol_major_ver' AS protocol_major_ver,
        ep.params ->> 'drep_activity' AS drep_activity,
        NULL::text AS proposal_type
    FROM drep_registration dr
    JOIN target t ON t.drep_hash = dr.drep_hash AND t.drep_type = dr.cred_type
    JOIN epoch_param ep ON ep.epoch = dr.epoch
    WHERE dr.type = 'REG_DREP_CERT'

    UNION ALL

    SELECT
        'DREP_UPDATE' AS event_kind,
        2 AS event_priority,
        dr.epoch,
        dr.slot,
        dr.tx_index,
        dr.cert_index AS event_index,
        dr.drep_hash,
        dr.cred_type AS drep_type,
        NULL::text AS protocol_major_ver,
        ep.params ->> 'drep_activity' AS drep_activity,
        NULL::text AS proposal_type
    FROM drep_registration dr
    JOIN target t ON t.drep_hash = dr.drep_hash AND t.drep_type = dr.cred_type
    JOIN epoch_param ep ON ep.epoch = dr.epoch
    WHERE dr.type = 'UPDATE_DREP_CERT'

    UNION ALL

    SELECT
        'DREP_VOTE' AS event_kind,
        2 AS event_priority,
        vp.epoch,
        vp.slot,
        vp.tx_index,
        vp.idx AS event_index,
        vp.voter_hash AS drep_hash,
        CASE
            WHEN vp.voter_type = 'DREP_KEY_HASH' THEN 'ADDR_KEYHASH'
            WHEN vp.voter_type = 'DREP_SCRIPT_HASH' THEN 'SCRIPTHASH'
        END AS drep_type,
        NULL::text AS protocol_major_ver,
        ep.params ->> 'drep_activity' AS drep_activity,
        NULL::text AS proposal_type
    FROM voting_procedure vp
    JOIN target t
      ON t.drep_hash = vp.voter_hash
     AND t.drep_type = CASE
            WHEN vp.voter_type = 'DREP_KEY_HASH' THEN 'ADDR_KEYHASH'
            WHEN vp.voter_type = 'DREP_SCRIPT_HASH' THEN 'SCRIPTHASH'
        END
    JOIN epoch_param ep ON ep.epoch = vp.epoch
    WHERE vp.voter_type IN ('DREP_KEY_HASH', 'DREP_SCRIPT_HASH')

    UNION ALL

    SELECT
        'PROPOSAL' AS event_kind,
        0 AS event_priority,
        gap.epoch,
        gap.slot,
        gap.tx_index,
        gap.idx AS event_index,
        NULL::text AS drep_hash,
        NULL::text AS drep_type,
        NULL::text AS protocol_major_ver,
        NULL::text AS drep_activity,
        gap.type::text AS proposal_type
    FROM gov_action_proposal gap
)
SELECT
    event_kind,
    epoch,
    slot,
    tx_index,
    event_index,
    drep_hash,
    drep_type,
    protocol_major_ver,
    drep_activity,
    proposal_type
FROM events
WHERE epoch BETWEEN 492 AND 598
ORDER BY epoch, slot, tx_index, event_priority, event_index;
```

Change the `target` CTE to `:'drep_ff72'` and adjust `epoch BETWEEN 900 AND 916` to debug the
off-by-one sample.

### Local report inspection

These commands inspect the stored mismatch report without querying either database:

```bash
sed -n '6,18p' drep-mismatches/sanchonet/compare_all_20260521_173930/summary.log
sed -n '1,8p' drep-mismatches/sanchonet/compare_all_20260521_173930/mismatches/drep_active_until_epoch_582.csv
sed -n '1,8p' drep-mismatches/sanchonet/compare_all_20260521_173930/mismatches/drep_active_until_epoch_916.csv
```

Report summary:

```text
Command          : /usr/bin/python3 compare_all.py --start-epoch 492 --end-epoch 1058 --only drep_active_until --config config.json
Epoch scope      : 492 -> 1058
Total mismatches : 1547
Bad epochs       : 160/567
Comparator       : drep_active_until
```

Representative CSV mismatches:

```text
epoch 582, drep 967c86ac16aea79dea7609c8b186f91c259a458a00e7d4ed3a7f9d3a:
  dbsync_active_until=600, yaci_store_active_until=513

epoch 593, same drep:
  dbsync_active_until=611, yaci_store_active_until=612

epoch 916, drep ff72c7e189bc85b3ac3928fea9248ae1f64feb8bdbc9269b1019dd27:
  dbsync_active_until=933, yaci_store_active_until=934
```

## DB Evidence

Direct DB reads showed that DB Sync and yaci-store disagree on `active_until`, while yaci-store also
has a separate `expiry` column that often differs from `active_until`.

Sample DB Sync rows:

```text
epoch|drep_hash|has_script|active_until
582|967c86ac16aea79dea7609c8b186f91c259a458a00e7d4ed3a7f9d3a|f|600
916|1923810f1a50c04d70ed0aa8c6e548b41931ff07eaa27861a768c364|t|923
916|ff72c7e189bc85b3ac3928fea9248ae1f64feb8bdbc9269b1019dd27|f|933
1000|fa3a5db6a24b09748d2f83dd14161314e45cf38ff32bf4e8a874b322|f|997
1000|ff72c7e189bc85b3ac3928fea9248ae1f64feb8bdbc9269b1019dd27|f|1013
1015|fa3a5db6a24b09748d2f83dd14161314e45cf38ff32bf4e8a874b322|f|1016
1015|ff72c7e189bc85b3ac3928fea9248ae1f64feb8bdbc9269b1019dd27|f|1033
```

Sample yaci-store rows from `drep_dist`:

```text
epoch|drep_hash|drep_type|active_until|expiry
582|967c86ac16aea79dea7609c8b186f91c259a458a00e7d4ed3a7f9d3a|ADDR_KEYHASH|513|601
916|1923810f1a50c04d70ed0aa8c6e548b41931ff07eaa27861a768c364|SCRIPTHASH|924|924
916|ff72c7e189bc85b3ac3928fea9248ae1f64feb8bdbc9269b1019dd27|ADDR_KEYHASH|934|934
1000|fa3a5db6a24b09748d2f83dd14161314e45cf38ff32bf4e8a874b322|ADDR_KEYHASH|994|1006
1000|ff72c7e189bc85b3ac3928fea9248ae1f64feb8bdbc9269b1019dd27|ADDR_KEYHASH|1007|1019
1015|fa3a5db6a24b09748d2f83dd14161314e45cf38ff32bf4e8a874b322|ADDR_KEYHASH|994|1021
1015|ff72c7e189bc85b3ac3928fea9248ae1f64feb8bdbc9269b1019dd27|ADDR_KEYHASH|1007|1034
```

Governance activity around the first mismatch:

```text
gov_epoch_activity:
492|dormant=true|dormant_count=1
...
580|dormant=true|dormant_count=89
581|dormant=true|dormant_count=90
592|dormant=true|dormant_count=101
593|dormant=false|dormant_count=0

gov_action_proposal:
580|slot=50184338|tx_index=0|HARD_FORK_INITIATION_ACTION
592|slot=51202227|tx_index=0|INFO_ACTION
```

DRep `967c86...` registration:

```text
drep_registration:
967c86ac16aea79dea7609c8b186f91c259a458a00e7d4ed3a7f9d3a|ADDR_KEYHASH|REG_DREP_CERT|493|slot=42596698|tx_index=0|cert_index=0

epoch_param:
493|protocol_major=9|drep_activity=20
582|protocol_major=10|drep_activity=20
```

This explains the DB Sync values under ledger rules:

```text
registration at epoch 493 under PV9:
  active_until = 493 + 20 = 513

proposal at epoch 580:
  pending dormant epochs after registration = 493..579 = 87
  active_until = 513 + 87 = 600

proposal at epoch 592:
  pending dormant epochs after previous proposal = 581..591 = 11
  active_until = 600 + 11 = 611
```

For the epoch 916 off-by-one sample:

```text
gov_epoch_activity:
912|dormant=true|dormant_count=1
913|dormant=true|dormant_count=2
914|dormant=true|dormant_count=3
915|dormant=true|dormant_count=4
916|dormant=false|dormant_count=0

gov_action_proposal:
915|slot=79111020|tx_index=0|UPDATE_COMMITTEE

voting_procedure:
ff72... voted at epoch 910
```

The ledger stores `910 + 20 = 930` after the vote, then the proposal at epoch 915 flushes only the
three already pending dormant epochs 912, 913, and 914:

```text
active_until = 930 + 3 = 933
```

yaci-store returned 934, which is consistent with counting the proposal epoch 915 as dormant for the
stored value. The ledger does not do that; the proposal transaction resets the pending counter.

## Ledger Evidence

`cardano-ledger` is the algorithm reference.

Registration/update logic:

- `cardano-ledger/eras/conway/impl/src/Cardano/Ledger/Conway/Rules/GovCert.hs`
- Lines 222-226 create `DRepState { drepExpiry = computeDRepExpiryVersioned ... vsNumDormantEpochs }`.
- Lines 265-269 update an existing DRep with `drepExpiryL .~ computeDRepExpiry ... vsNumDormantEpochs`.
- Lines 286-303 define the versioned calculation:
  - Conway bootstrap/PV9: `currentEpoch + ppDRepActivity`
  - PV10 and later: `currentEpoch + ppDRepActivity - numDormantEpochs`

Proposal/vote state transitions:

- `cardano-ledger/eras/conway/impl/src/Cardano/Ledger/Conway/Rules/Certs.hs`
- Lines 260-266 call `updateDormantDRepExpiry currentEpoch` when a transaction has proposal procedures.
- Lines 279-285 update voting DReps with `computeDRepExpiry drepActivity currentEpoch numDormantEpochs`.
- Lines 313-326 add `numDormantEpochs` to each active DRep's stored expiry and reset
  `vsNumDormantEpochs` to zero; expired DReps are not revived.

Dormant counter:

- `cardano-ledger/eras/conway/impl/src/Cardano/Ledger/Conway/Rules/Epoch.hs`
- Lines 196-201 increment `vsNumDormantEpochs` only when there are no active governance proposals.

## DB Sync Evidence

`cardano-db-sync` stores the ledger snapshot value directly:

- `/home/sotatek/Projects/cardano-db-sync/cardano-db-sync/src/Cardano/DbSync/Era/Universal/Insert/GovAction.hs`
- Line 398: `DB.drepDistrActiveUntil = unEpochNo <$> isActiveEpochNo drep`
- Line 405: `DRepCredential cred -> drepExpiry <$> Map.lookup cred (psDRepState pSnapshot)`

Therefore DB Sync's `active_until` is the raw `DRepState.drepExpiry` from the ledger state, not a
post-processed effective expiry.

## yaci-store Evidence

Pre-patch yaci-store logic in `DRepExpiryService.java` derived `active_until` from `expiry`:

- `aggregates/governance-aggr/src/main/java/.../service/DRepExpiryService.java` at `HEAD`
- Line 145 reads `recentGovEpochActivityOpt.get().getDormantEpochCount()`.
- Line 162 sets `activeUntil = expiry - dormantEpochCount`.
- Lines 170-172 override PV9 registrations in a dormant range to `registrationEpoch + dRepActivity`.

That logic loses event ordering:

- It uses the current epoch-level dormant count, not the ledger's pending `vsNumDormantEpochs` at the
  exact transaction position.
- It does not replay proposal submissions as state transitions that flush `numDormantEpochs` into
  stored `drepExpiry` and reset the counter.
- It treats proposal epochs through `gov_epoch_activity` as if they can still be counted for the
  stored value, which creates the epoch 916 `+1` mismatch.
- It can keep `active_until` stable at the old PV9 registration value even after a later proposal has
  already flushed dormant epochs into the ledger state, which creates the epoch 582 `513` vs `600`
  mismatch.

`DRepExpiryUtil.calculateDRepExpiry` is still useful for yaci-store's effective `expiry` concept:

- `DRepExpiryUtil.java` lines 67-95 compute PV10 expiry as `lastActivityEpoch + activityWindow + dormantCount`.
- `DRepExpiryUtil.java` lines 103-141 add PV9 bonus handling.

But that is not the same value DB Sync writes into `drep_distr.active_until`.

## Algorithm Difference

| Step | cardano-ledger / cardano-db-sync | yaci-store pre-patch |
| --- | --- | --- |
| Registration PV9 | Store `epoch + dRepActivity` | May later override active_until back to registration expiry during dormant range |
| Registration PV10+ | Store `epoch + dRepActivity - currentDormantCounter` | Uses effective expiry based on total dormant epochs after activity |
| DRep vote/update | Store `epoch + dRepActivity - currentDormantCounter` at transaction time | Keeps only last interaction epoch; does not preserve exact event ordering against proposals |
| Proposal tx | Add pending dormant counter to stored expiry for active DReps, reset counter | No stateful replay; uses `gov_epoch_activity.dormant_epoch_count` at boundary |
| Dormant epochs after vote/proposal | Stored `drepExpiry` remains unchanged until another event | `active_until` changes when subtracting current dormant count from effective `expiry` |
| DB Sync storage | Raw ledger `DRepState.drepExpiry` | Derived yaci-store value |

## Minimal Fix

Do not compute `drep_dist.active_until` from `expiry - dormantEpochCount`.

Instead, compute `active_until` by replaying the same state transitions as the ledger up to the
evaluated epoch:

1. Load registration event with slot, epoch, tx index, cert index, `drep_activity`, protocol major.
2. Load every DRep update/vote event up to the evaluated epoch with slot, epoch, tx index, and event index.
3. Load every proposal submission up to the evaluated epoch with slot, epoch, tx index, and proposal index.
4. Sort events by epoch, slot, tx index, ledger event priority, and event index.
5. Maintain `activeUntil` and a pending `dormantCounter`.
6. On proposal: if the DRep is registered and still active after applying the pending counter, add
   `dormantCounter` to `activeUntil`; then reset the counter.
7. On registration:
   - PV9/bootstrap: `activeUntil = epoch + dRepActivity`
   - PV10+: `activeUntil = epoch + dRepActivity - dormantCounter`
8. On update/vote: `activeUntil = epoch + dRepActivity - dormantCounter`.
9. After processing an epoch, increment `dormantCounter` only if the epoch is dormant and has no
   proposal submission in that epoch.

Current worktree patch follows this approach:

- `DRepExpiryService.java`
  - line 85 loads all DRep interaction events through `findInteractionInfos`.
  - line 147 calls `DRepExpiryUtil.calculateDRepActiveUntil`.
  - line 294 defines `findInteractionInfos` for update certificates and voting procedures.
  - line 511 loads proposal submissions with `tx_index` and `idx`.
- `DRepExpiryUtil.java`
  - line 270 adds `calculateDRepActiveUntil`.
  - lines 341-357 implement proposal flush, registration, and interaction transitions.
  - lines 459-486 extend event records with transaction ordering fields while keeping old constructors.
- `DRepExpiryUtilTest.java`
  - lines 694-720 cover the Sanchonet PV9 proposal-flush sample: 513 -> 600 -> 611 -> 618.
  - lines 723-741 cover raw `active_until` staying stable through dormant epochs after a vote, then
    updating at a later proposal/vote.

## Verification Status

Completed:

- Read mismatch report and representative mismatch CSVs.
- Read DB connection/schema notes.
- Queried DB Sync and yaci-store rows for representative DReps/epochs.
- Queried yaci-store `gov_epoch_activity`, `drep_registration`, `voting_procedure`,
  `gov_action_proposal`, and `epoch_param` rows needed to reconstruct the mismatch.
- Matched the DB values to the `cardano-ledger` transition rules.
- Confirmed `cardano-db-sync` writes raw ledger `DRepState.drepExpiry`.
- Prepared a minimal yaci-store patch and focused regression tests.

Not completed:

- The focused Gradle test run was interrupted before completion while Gradle was still in build setup
  tasks. The patch should still be compiled and tested before merge.

Recommended verification command:

```bash
export JAVA_HOME=/home/sotatek/.sdkman/candidates/java/21.0.9-graal
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :aggregates:governance-aggr:test --tests com.bloxbean.cardano.yaci.store.governanceaggr.processor.DRepExpiryUtilTest
```

If Gradle setup continues to dominate runtime, first run:

```bash
export JAVA_HOME=/home/sotatek/.sdkman/candidates/java/21.0.9-graal
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :aggregates:governance-aggr:compileJava
```
