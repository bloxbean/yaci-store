# DRep `active_until` Off-by-One Fix

Date: 2026-05-28

## Problem

After deploying the initial `calculateDRepActiveUntil` replay algorithm (commit `10f57718`),
comparison against `cardano-db-sync` on Sanchonet still showed **1489 mismatches** across 376
epochs. The pattern was a consistent **off-by-one**: yaci-store was -1 compared to db-sync for
most DReps (e.g., db-sync=700, yaci=699).

## Root Cause

Two bugs in `DRepExpiryUtil.calculateDRepActiveUntil`:

### 1. Dormant counter increment timing

In `cardano-ledger`, `updateNumDormantEpochs` runs at the **epoch boundary transition** (entering
epoch N), BEFORE any transactions in epoch N execute. When a proposal tx or DRep registration
happens in epoch N, it sees the dormant counter that already includes epoch N's boundary increment.

The yaci-store code incremented the dormant counter at the **END** of the epoch loop iteration
(after processing events). Events in epoch N saw the counter from epoch N-1, missing one increment.

### 2. Incorrect `proposalEpochs` guard

The condition `!proposalEpochs.contains(epoch)` prevented the counter from incrementing for epochs
where a proposal was submitted. The ledger's epoch boundary check only considers whether proposals
are **active** (i.e., `currentEpoch <= gasExpiresAfter`), not whether a new proposal is submitted
in that epoch. A proposal submitted in epoch N doesn't exist at the boundary N-1→N.

## Concrete Example

DRep `af0daeebaa7fc8e72cd4ed1d3a1606939c888cae0cdf0fd2c88035d8`:
- Registered at epoch 670 (PV10, dRepActivity=20): `drepExpiry = 670 + 20 - 0 = 690`
- Epochs 674-683 are dormant (no active proposals)
- Proposal submitted at epoch 683

**Ledger behavior:**
- Boundary 682→683: counter increments to 10 (epochs 674-683 all dormant)
- Proposal tx in 683: flush → `690 + 10 = 700`
- DB-sync `active_until = 700`

**Old yaci-store behavior:**
- Counter increments at END of each epoch, so at epoch 683 events see counter=9 (only 674-682)
- `proposalEpochs.contains(683)=true` prevents increment for epoch 683
- Proposal flush: `690 + 9 = 699`
- yaci-store `active_until = 699` ← off by 1

## Fix

```java
// BEFORE (buggy):
for (int epoch = eraFirstEpoch; epoch <= evaluatedEpoch; epoch++) {
    // process events first...
    while (...) { ... }
    // increment AFTER events — wrong timing
    if (!activeProposalEpochs.contains(epoch) && !proposalEpochs.contains(epoch)) {
        dormantCounter++;
    }
}

// AFTER (fixed):
for (int epoch = eraFirstEpoch; epoch <= evaluatedEpoch; epoch++) {
    // increment BEFORE events — matches ledger epoch boundary
    if (epoch > eraFirstEpoch && !activeProposalEpochs.contains(epoch)) {
        dormantCounter++;
    }
    // then process events...
    while (...) { ... }
}
```

Key changes:
1. Move dormant increment to **before** event processing
2. Guard with `epoch > eraFirstEpoch` (VState initializes with counter=0; first boundary increment
   is at eraFirstEpoch→eraFirstEpoch+1)
3. Remove `proposalEpochs` guard — only `activeProposalEpochs` matters

## `activeProposalEpochs` Semantics

The `activeProposalEpochs` set (from `GOV_ACTION_PROPOSAL_STATUS` with status `ACTIVE`) must
correctly represent which epochs are non-dormant from the ledger's perspective.

Important timing detail: a proposal RATIFIED at epoch N (in `GOV_ACTION_PROPOSAL_STATUS`) means
it was removed from `newProposals` at boundary N-1→N. However, the proposal was still in
`newProposals` at boundary N-2→N-1 (making epoch N-1 non-dormant). The status table captures
the post-removal state.

For a proposal submitted in epoch P:
- At boundary P→P+1: the proposal is in `newProposals` (not yet ratified) → epoch P+1 is
  non-dormant. `activeProposalEpochs` should include P+1 (or the epoch after submission).
- If ratified at epoch R (status shows RATIFIED at R): the proposal was removed at boundary
  R-1→R. Epoch R-1 is the last epoch where the proposal makes things non-dormant via the
  status table (shown as ACTIVE at R-1).

## Ledger Evidence

```haskell
-- Epoch.hs: called at epoch boundary entering eNo
updateNumDormantEpochs currentEpoch ps vState =
  if null $ OMap.filter ((currentEpoch <=) . gasExpiresAfter) $ ps ^. pPropsL
    then vState & vsNumDormantEpochsL %~ succ
    else vState

-- Epoch.hs:341: called with newProposals (after ratification/expiry removal)
certState2 = mkConwayCertState
    (updateNumDormantEpochs eNo newProposals vState ...)
```

## Verification

```bash
./gradlew :aggregates:governance-aggr:test --tests \
  com.bloxbean.cardano.yaci.store.governanceaggr.processor.DRepExpiryUtilTest
```

After deploy, re-run comparison:
```bash
python3 compare_all.py --start-epoch 492 --end-epoch 1058 --only drep_active_until --config config.json
```
