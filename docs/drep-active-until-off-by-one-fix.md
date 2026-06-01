# DRep `active_until` Root Cause and Fix

Date: 2026-06-01

## Scope

This document explains the remaining Sanchonet `drep_dist.active_until` mismatch found after
commit `10f577184eb1de7bf7eb7630781f3c52afdea819`.

Baseline:

- `cardano-ledger` is the reference implementation.
- `cardano-db-sync` matches ledger behavior and is used as the comparison baseline.
- yaci-store schema `yaci_store_1` contains the result from commit `10f57718`.
- Report: `drep-mismatches/sanchonet/compare_all_20260528_173543`.

## Summary

There are two root causes:

1. `10f57718` replayed the dormant counter at the end of each epoch loop. Ledger increments the
   dormant counter at the epoch boundary before transactions in that epoch are applied. This caused
   yaci-store to flush one fewer dormant epoch for proposal transactions and to subtract one fewer
   dormant epoch for DRep registration/update/vote transactions.
2. The non-dormant proposal epoch set was derived only from `gov_action_proposal_status.status =
   'ACTIVE'`. In yaci-store, a `RATIFIED` status row is also evidence that the proposal was present
   in the current ratification state for that epoch. Excluding `RATIFIED` marks those epochs dormant
   even though ledger did not.

The minimal fix is:

- Replay dormant increment before processing events in an epoch.
- Use proposal status epochs with `ACTIVE` or `RATIFIED` as non-dormant epochs for `active_until`.
- Do not use `gov_epoch_activity` for `active_until`; it is still used by the separate `expiry`
  calculation path.

## Ledger Evidence

### Dormant Counter Is Updated At Epoch Boundary

File: `cardano-ledger/eras/conway/impl/src/Cardano/Ledger/Conway/Rules/Epoch.hs`

```haskell
-- | When there have been zero governance proposals to vote on in the previous epoch
-- increase the dormant-epoch counter by one.
updateNumDormantEpochs :: EpochNo -> Proposals era -> VState era -> VState era
updateNumDormantEpochs currentEpoch ps vState =
  if null $ OMap.filter ((currentEpoch <=) . gasExpiresAfter) $ ps ^. pPropsL
    then vState & vsNumDormantEpochsL %~ succ
    else vState
```

The EPOCH transition applies ratification/enactment results first, then runs
`updateNumDormantEpochs eNo newProposals vState`:

```haskell
(newProposals, enactedActions, removedDueToEnactment, expiredActions) =
  proposalsApplyEnactment rsEnacted rsExpired (govState0 ^. proposalsGovStateL)

certState2 =
  mkConwayCertState
    ( updateNumDormantEpochs eNo newProposals vState
        & vsCommitteeStateL %~ updateCommitteeState (govState1 ^. cgsCommitteeL)
    )
```

This is the boundary state entering `eNo`. Any transaction processed in epoch `eNo` sees the
counter after that boundary update.

### Proposal Transactions Flush Pending Dormant Epochs

File: `cardano-ledger/eras/conway/impl/src/Cardano/Ledger/Conway/Rules/Certs.hs`

```haskell
updateDormantDRepExpiries tx currentEpoch =
  let hasProposals = not . OSet.null $ tx ^. bodyTxL . proposalProceduresTxBodyL
   in if hasProposals
        then certVStateL %~ updateDormantDRepExpiry currentEpoch
        else id
```

```haskell
updateDormantDRepExpiry currentEpoch vState =
  if numDormantEpochs == EpochNo 0
    then vState
    else
      vState
        & vsNumDormantEpochsL .~ EpochNo 0
        & vsDRepsL %~ Map.map updateExpiry
  where
    numDormantEpochs = vState ^. vsNumDormantEpochsL
    updateExpiry =
      drepExpiryL
        %~ \currentExpiry ->
          let actualExpiry = binOpEpochNo (+) numDormantEpochs currentExpiry
           in if actualExpiry < currentEpoch
                then currentExpiry
                else actualExpiry
```

So a proposal transaction in epoch `N` flushes the counter that already includes the boundary
increment for epoch `N`.

### Registration, Update, And Vote Use The Current Dormant Counter

DRep registration/update:

File: `cardano-ledger/eras/conway/impl/src/Cardano/Ledger/Conway/Rules/GovCert.hs`

```haskell
computeDRepExpiryVersioned pp currentEpoch numDormantEpochs
  | hardforkConwayBootstrapPhase (pp ^. ppProtocolVersionL) =
      addEpochInterval currentEpoch (pp ^. ppDRepActivityL)
  | otherwise =
      computeDRepExpiry (pp ^. ppDRepActivityL) currentEpoch numDormantEpochs

computeDRepExpiry ppDRepActivity currentEpoch =
  binOpEpochNo
    (-)
    (addEpochInterval currentEpoch ppDRepActivity)
```

DRep vote:

File: `cardano-ledger/eras/conway/impl/src/Cardano/Ledger/Conway/Rules/Certs.hs`

```haskell
let numDormantEpochs = certState ^. certVStateL . vsNumDormantEpochsL
...
Map.adjust
  (drepExpiryL .~ computeDRepExpiry drepActivity currentEpoch numDormantEpochs)
```

For protocol version 10 and later, the stored raw expiry for a DRep interaction is:

```text
current_epoch + drep_activity - current_num_dormant_epochs
```

For protocol version 9 bootstrap registration, ledger intentionally does not subtract dormant
epochs during registration.

## yaci-store Governance Aggregate Status Semantics

The `gov_action_proposal_status` rows used by `DRepExpiryService` are produced by
`governance-aggr`, not by the local-state-query store.

File: `yaci-store/aggregates/adapot/src/main/java/.../AdaPotJobProcessor.java`

```java
stakeSnapshotService.takeStakeSnapshot(epoch - 1);
publisher.publishEvent(new StakeSnapshotTakenEvent(epoch - 1, job.getSlot()));
```

File: `yaci-store/aggregates/governance-aggr/src/main/java/.../ProposalStateProcessor.java`

```java
int epoch = stakeSnapshotTakenEvent.getEpoch();
int currentEpoch = epoch + 1;

takeDRepDistrSnapshot(currentEpoch);
Optional<List<GovActionProposalStatus>> statusListOpt =
        evaluateProposalStatuses(currentEpoch);
```

So a status row with `epoch = X` is yaci-store's governance aggregate evaluation result for epoch
`X`, produced after the stake snapshot event for `X - 1`.

The proposal set evaluated for epoch `X` is built by carrying forward active proposals from epoch
`X - 1` and dropping proposals that were already `RATIFIED` or `EXPIRED` in epoch `X - 1`:

File: `yaci-store/aggregates/governance-aggr/src/main/java/.../ProposalCollectionService.java`

```java
List<GovActionProposal> expiredProposalsInPrevSnapshot =
        proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.EXPIRED, epoch - 1);
List<GovActionProposal> ratifiedProposalsInPrevSnapshot =
        proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch - 1);

List<GovActionProposal> activeProposalsInPrevEpoch = getActiveProposalsInEpoch(epoch - 1);
...
return activeProposalsInPrevEpoch
        .stream()
        .filter(govActionProposal -> !scheduledToDropProposals.contains(...))
        .toList();
```

That remaining proposal set is evaluated for epoch `X`:

File: `yaci-store/aggregates/governance-aggr/src/main/java/.../ProposalStateProcessor.java`

```java
GovernanceEvaluationResult result =
        governanceEvaluationService.evaluateGovernanceState(input);
```

The result is then mapped to status rows:

File: `yaci-store/aggregates/governance-aggr/src/main/java/.../ProposalStatusMapper.java`

```java
GovActionStatus status = switch (proposalResult.getStatus()) {
    case ACCEPT -> GovActionStatus.RATIFIED;
    case REJECT -> GovActionStatus.EXPIRED;
    case CONTINUE -> GovActionStatus.ACTIVE;
};
```

Therefore, in `governance-aggr`, a `RATIFIED` row at epoch `X` means the proposal survived the
boundary into epoch `X`, was included in the proposal set evaluated for epoch `X`, and the
evaluation accepted it. It does not mean the proposal was already removed before epoch `X`.

This matches the ledger timing. At the boundary into epoch `X`, ledger first removes proposals
that were already enacted/expired from the previous pulser, forming `newProposals`, then uses
`newProposals` for dormant detection:

File: `cardano-ledger/eras/conway/impl/src/Cardano/Ledger/Conway/Rules/Epoch.hs`

```haskell
(newProposals, enactedActions, removedDueToEnactment, expiredActions) =
  proposalsApplyEnactment rsEnacted rsExpired (govState0 ^. proposalsGovStateL)

certState2 =
  mkConwayCertState
    ( updateNumDormantEpochs eNo newProposals vState
        & vsCommitteeStateL %~ updateCommitteeState (govState1 ^. cgsCommitteeL)
    )
```

Only after that does ledger start the fresh pulser for epoch `X` from the current proposal set:

File: `cardano-ledger/eras/conway/impl/src/Cardano/Ledger/Conway/Governance.hs`

```haskell
dpCurrentEpoch = epochNo
dpProposals = proposalsActions props
```

So, for yaci-store `active_until`, `ACTIVE` alone is not a complete proxy for ledger
non-dormancy. A `RATIFIED` status row at epoch `X` is also evidence that epoch `X` had a proposal
present for governance evaluation, so epoch `X` must be treated as non-dormant. This does not mean
the ratified proposal remains voteable after evaluation; it means the proposal was present at the
epoch `X` evaluation point and will be removed by the next boundary's drop/enactment handling.

## Database Evidence

### Report Pattern

Report `compare_all_20260528_173543`:

```text
total mismatches: 1489
bad epochs:       376 / 567
delta counts:
  yaci - dbsync = -1: 1440 rows
  yaci - dbsync = +1:   49 rows
```

Replay against `yaci_store_1` showed:

```text
10f57718 algorithm, ACTIVE only        -> matches yaci: 1489/1489, matches db-sync: 0/1489
boundary-before-events, ACTIVE only    -> matches db-sync: 1089/1489
boundary-before-events, ACTIVE+RATIFIED -> matches db-sync: 1489/1489
```

This proves the deployed `10f57718` behavior is fully reproduced by the old replay, and both fix
parts are required to match db-sync.

### Example 1: yaci-store Was 1 Too Low

DRep:

```text
af0daeebaa7fc8e72cd4ed1d3a1606939c888cae0cdf0fd2c88035d8
```

Relevant yaci-store rows:

```sql
select drep_hash, cred_type, type, epoch, slot, tx_index, cert_index
from yaci_store_1.drep_registration
where drep_hash = 'af0daeebaa7fc8e72cd4ed1d3a1606939c888cae0cdf0fd2c88035d8'
order by epoch, slot;

select epoch, dormant, dormant_epoch_count
from yaci_store_1.gov_epoch_activity
where epoch between 670 and 684
order by epoch;

select epoch, slot, tx_hash, idx, type
from yaci_store_1.gov_action_proposal
where epoch between 680 and 684
order by epoch, slot, tx_index, idx;
```

Observed data:

```text
registration: epoch 670, protocol_major_ver=10, drep_activity=20
base active_until: 670 + 20 = 690
dormant sequence before proposal flush: epochs 674..683 = 10
proposal tx: epoch 683
db-sync active_until at report epoch 684: 700
yaci-store active_until at report epoch 684: 699
```

Ledger-compatible replay:

```text
boundary into 683 increments dormant counter to 10
proposal tx in 683 flushes: 690 + 10 = 700
```

The old yaci-store replay processed the proposal before the boundary increment for epoch 683, so
it flushed only 9 and stored 699.

### Example 2: yaci-store Was 1 Too High

DRep:

```text
fa3a5db6a24b09748d2f83dd14161314e45cf38ff32bf4e8a874b322
```

Relevant rows:

```sql
select drep_hash, cred_type, type, epoch, slot, tx_index, cert_index
from yaci_store_1.drep_registration
where drep_hash = 'fa3a5db6a24b09748d2f83dd14161314e45cf38ff32bf4e8a874b322'
order by epoch, slot;
```

Observed data:

```text
REG_DREP_CERT:    epoch 682, slot 58962644
UPDATE_DREP_CERT: epoch 682, slot 58971358
db-sync active_until at report epoch 683: 693
yaci-store active_until at report epoch 683: 694
```

Ledger-compatible replay:

```text
boundary into 682 already includes 9 dormant epochs
update in epoch 682 computes: 682 + 20 - 9 = 693
```

The old yaci-store replay had only 8 dormant epochs visible to the update event and computed 694.

### Example 3: ACTIVE-only Misses RATIFIED Epochs

DRep:

```text
ff72c7e189bc85b3ac3928fea9248ae1f64feb8bdbc9269b1019dd27
```

Status rows around the mismatch:

```sql
select epoch, status, type, gov_action_tx_hash, gov_action_index
from yaci_store_1.gov_action_proposal_status
where epoch between 900 and 925
order by epoch, status, type, gov_action_tx_hash;
```

Observed rows include:

```text
epoch 911: ACTIVE and RATIFIED rows
epoch 912: RATIFIED only
epoch 916..922: ACTIVE rows
epoch 923: RATIFIED only
```

Replay result for report epoch 943:

```text
ACTIVE only:          944
ACTIVE + RATIFIED:    943
db-sync:              943
```

So `RATIFIED` epochs must be included in the non-dormant epoch set used for `active_until`.

## Code Fix

### DRepExpiryService

File: `aggregates/governance-aggr/src/main/java/.../DRepExpiryService.java`

The status query now returns proposal-status epochs that are non-dormant from the replay's
perspective:

```java
return dsl.selectDistinct(GOV_ACTION_PROPOSAL_STATUS.EPOCH)
        .from(GOV_ACTION_PROPOSAL_STATUS)
        .where(GOV_ACTION_PROPOSAL_STATUS.STATUS.in(
                        GovActionStatus.ACTIVE.name(),
                        GovActionStatus.RATIFIED.name())
                .and(GOV_ACTION_PROPOSAL_STATUS.EPOCH.ge(fromEpoch))
                .and(GOV_ACTION_PROPOSAL_STATUS.EPOCH.le(toEpoch)))
        .fetchSet(GOV_ACTION_PROPOSAL_STATUS.EPOCH);
```

### DRepExpiryUtil

File: `aggregates/governance-aggr/src/main/java/.../DRepExpiryUtil.java`

The replay increments the dormant counter before processing events in that epoch:

```java
for (int epoch = eraFirstEpoch; epoch <= evaluatedEpoch; epoch++) {
    if (epoch > eraFirstEpoch && !nonDormantProposalEpochs.contains(epoch)) {
        dormantCounter++;
    }

    while (eventIndex < events.size() && events.get(eventIndex).epoch() == epoch) {
        ...
    }
}
```

This matches the ledger ordering: epoch boundary first, transactions/events after that.

## Verification

Focused tests:

```bash
./gradlew :aggregates:governance-aggr:test \
  --tests com.bloxbean.cardano.yaci.store.governanceaggr.service.DRepExpiryServiceTest \
  --tests com.bloxbean.cardano.yaci.store.governanceaggr.processor.DRepExpiryUtilTest
```

Expected coverage:

- `shouldFixPv10OffByOneWhenProposalFlushesInDormantEpoch` covers the `af0d...` `-1` pattern.
- `shouldNotCountProposalSubmissionEpochAsDormantForActiveUntil` covers the Sanchonet epoch 912
  `RATIFIED` non-dormant pattern.
- `activeUntilTreatsRatifiedProposalStatusEpochAsNonDormant` covers the service-level SQL query
  that includes `RATIFIED` status rows.

After deployment, rerun:

```bash
python3 compare_all.py --start-epoch 492 --end-epoch 1058 \
  --only drep_active_until --config config.json
```
