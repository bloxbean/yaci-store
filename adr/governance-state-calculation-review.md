# Governance State Calculation - Code Review & Audit Report

**Date:** 2026-02-26
**Status:** Review
**Modules:** `aggregates/governance-aggr`, `aggregates/governance-rules`

## Context

This is a detailed code review and audit of the Yaci Store governance state calculation implementation (`aggregates/governance-aggr` and `aggregates/governance-rules` modules), verified against the Cardano node Haskell implementation (cardano-ledger Conway era) and CIP-1694. The goal is to identify any gaps, deviations, or hidden issues that could produce incorrect governance results.

**Scope:** DRep distribution, DRep expiry, proposal status evaluation, vote tallying, proposal refunds, and epoch boundary processing. Epoch stake and reward calculations are assumed correct (except where proposal refunds interact with epoch_stake).

---

## Executive Summary

The Yaci Store governance implementation is **well-structured and largely correct**. The core ratification logic, vote tallying formulas, threshold selection per action type, action priority ordering, delaying action semantics, previous action chaining, and proposal drop logic all align with the Cardano ledger specification. However, there are **several issues ranging from critical to minor** that need attention.

### Risk Classification
- **CRITICAL**: Will produce wrong results on mainnet under certain conditions
- **HIGH**: Likely to produce wrong results in edge cases
- **MEDIUM**: Deviation from spec that may not currently manifest but is incorrect
- **LOW**: Minor issue or code quality concern
- **INFO**: Observation, not necessarily a bug

---

## FINDINGS

---

### FINDING 1: DRep "do-not-vote" Stake Double-Counts AlwaysNoConfidence for Non-NoConfidence Actions [HIGH]

**Files:**
- `aggregates/governance-rules/src/main/java/.../voting/VoteTallyCalculator.java:47`

**Issue:**
In `computeDRepTallies()`, the NO stake is calculated as:
```java
BigInteger noStake = no.add(notVoted).add(noConfidence); // line 47
```

This adds `noConfidence` (AlwaysNoConfidence DRep stake) to the NO stake unconditionally. But for `NO_CONFIDENCE` actions, line 37-38 already added `noConfidence` to `yes`:
```java
if (type == GovActionType.NO_CONFIDENCE) {
    yes = yes.add(noConfidence);  // line 37-38
}
```

So for NO_CONFIDENCE actions, `noConfidence` stake is counted in BOTH `yes` AND `noStake`. The resulting ratio `yes/(yes+noStake)` has `noConfidence` in both numerator and denominator, which **dilutes the actual yes ratio**.

**Ledger spec:**
```haskell
DRepAlwaysNoConfidence ->
  case govAction of
    NoConfidence _ -> (yes + stake, tot + stake)  -- YES + in denominator
    _              -> (yes, tot + stake)           -- only in denominator
```

The Haskell `tot` accumulates the total of yes + no + notVoted + noConfidence. For NoConfidence actions, `yes` includes noConfidence, and `tot` also includes it once. The ratio is `yes/tot`.

**Yaci Store gives:** `(yesVotes + noConfidence) / (yesVotes + noConfidence + noVotes + notVoted + noConfidence)` -- noConfidence counted TWICE in denominator.

**Haskell gives:** `(yesVotes + noConfidence) / (yesVotes + noConfidence + noVotes + notVoted)` -- noConfidence counted ONCE.

**This is a bug.** The NO stake calculation should exclude `noConfidence` for `NO_CONFIDENCE` actions:

```java
BigInteger noStake;
if (type == GovActionType.NO_CONFIDENCE) {
    noStake = no.add(notVoted); // noConfidence already in yes, don't add to no
} else {
    noStake = no.add(notVoted).add(noConfidence);
}
```

**Impact:** For NO_CONFIDENCE actions, the yes ratio is slightly lower than it should be. This could prevent a valid NoConfidence motion from reaching its threshold.

---

### FINDING 2: DRep Voting Returns PASS When Total Yes+No Is Zero [MEDIUM]

**Files:**
- `aggregates/governance-rules/src/main/java/.../voting/drep/DRepVotingEvaluator.java:36-37`

**Issue:**
```java
if (totalYesAndNo.equals(BigInteger.ZERO)) {
    return VotingStatus.PASS_THRESHOLD;  // auto-pass when no stake
}
```

When `totalYes + totalNo == 0` (meaning either no DReps exist, all abstained, or all are inactive), the DRep vote auto-passes.

**Ledger spec:**
```haskell
dRepAccepted env st gas
  | r == minBound = True  -- threshold is zero: auto-accept
  | otherwise = ...       -- must meet threshold
```

The auto-pass in the Haskell code is based on the **threshold being zero** (`r == minBound`), not on the total stake being zero. When total stake is zero but threshold is non-zero, the Haskell `dRepAcceptedRatio` returns `0 % 1` when `tot == 0`. So `0 >= threshold` would be False for any non-zero threshold.

**This means Yaci Store auto-passes DRep vote when it should fail** (when there's zero participating stake but a non-zero threshold). In practice this scenario is unlikely on mainnet because there will always be AlwaysNoConfidence DRep stake in the denominator. The risk is on small test networks.

**Recommendation:** Return `NOT_PASS_THRESHOLD` when `totalYesAndNo == 0` (conservative approach matching the ledger).

---

### FINDING 3: SPO Voting Returns PASS When All Stake Is Abstain [MEDIUM]

**Files:**
- `aggregates/governance-rules/src/main/java/.../voting/spo/SPOVotingEvaluator.java:33-34`

**Issue:**
```java
if (totalStake.equals(BigInteger.ZERO) || totalAbstain.equals(totalStake)) {
    return VotingStatus.PASS_THRESHOLD;  // auto-pass
}
```

**Ledger spec:**
```haskell
spoAccepted env st gas
  | SJust r <- votingStakePoolThreshold rs action =
      r == minBound || ...  -- only auto-pass when threshold is zero
  | otherwise = False
```

When `totalAbstain == totalStake`, the Haskell code does NOT auto-pass. The ratio would be `0 / (total - total)` = `0/0`, which evaluates as 0 in Haskell's `Rational` type. So `0 >= threshold` is False for non-zero thresholds.

**Recommendation:** When `totalStake == totalAbstain`, check if the threshold is zero (minBound). Only auto-pass if threshold is zero. Otherwise return `NOT_PASS_THRESHOLD`.

---

### FINDING 4: Committee Voting - Do-Not-Vote as NO May Double-Count [LOW]

**Files:**
- `aggregates/governance-rules/src/main/java/.../voting/committee/CommitteeVotingEvaluator.java:34`
- `aggregates/governance-rules/src/main/java/.../voting/VoteTallyCalculator.java:126-131`

**Issue:**
In `CommitteeVotingEvaluator.java:34`:
```java
int noVotes = committeeVoteTallies.getNoCount() + committeeVoteTallies.getDoNotVoteCount();
```

The `doNotVoteCount` counts members where `hotKey == null || !votesByHotKey.containsKey(hotKey)`.

**Ledger spec:**
Members WITHOUT a hot key registered, or members who RESIGNED, are treated as **abstaining** (excluded from both numerator and denominator). They don't appear in the counting loop because the iteration only processes `CommitteeHotCredential` entries from `CommitteeState`.

**In Yaci Store:** Members without a hot key have `hotKey == null`, counted as `doNotVoteCount`, added to NO. **This is incorrect for members without registered hot keys** - they should be treated as abstaining (excluded), not as voting NO.

**Impact:** Depends on how `members` list is constructed upstream. If `committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch()` only returns members with valid hot key registrations, this is not an issue.

**Recommendation:** Ensure `members` passed to `computeCommitteeTallies` only includes members with valid hot key registrations who are not resigned and not expired.

---

### FINDING 5: Committee Size Check Missing Post-Bootstrap [HIGH]

**Files:**
- `aggregates/governance-rules/src/main/java/.../voting/committee/CommitteeVotingEvaluator.java`

**Issue:**
The `CommitteeVotingEvaluator` does not check `committeeMinSize`. The Haskell spec requires:

```haskell
threshold =
  case committeeThreshold <$> committee of
    SJust t
      | hardforkConwayBootstrapPhase pv || activeCommitteeSize >= minSize ->
          VotingThreshold t
    _ -> NoVotingThreshold  -- committee REJECTS
```

Post-bootstrap: if active committee members < `committeeMinSize`, committee threshold becomes `NoVotingThreshold`, meaning `committeeAccepted` returns **False** for all proposals requiring committee approval.

**Yaci Store** has no such check. If the committee shrinks below `committeeMinSize`, proposals could be incorrectly ratified.

**Recommendation:** Add `committeeMinSize` check in `CommitteeVotingEvaluator.evaluate()`. If `activeMembers < committeeMinSize && !isBootstrapPhase`, return `NOT_PASS_THRESHOLD`.

---

### FINDING 6: Committee Voting Threshold Check for NoConfidence/UpdateCommittee [INFO]

**Files:**
- `NoConfidenceRatificationEvaluator.java`, `UpdateCommitteeRatificationEvaluator.java`

**Issue:**
The Haskell spec sets committee voting threshold to `NoVotingAllowed` (auto-pass) for NoConfidence and UpdateCommittee. In Yaci Store, neither evaluator calls `CommitteeVotingEvaluator`, which has the same effect.

**Status:** CORRECT. No action needed.

---

### FINDING 7: Info Action Lifecycle Check Off-By-One [MEDIUM]

**Files:**
- `aggregates/governance-rules/src/main/java/.../ratification/impl/InfoActionRatificationEvaluator.java:21`

**Issue:**
```java
if (context.getGovernanceContext().getCurrentEpoch() - context.getMaxAllowedVotingEpoch() >= 1) {
    return RatificationResult.REJECT;
}
```

Other evaluators use `isOutOfLifecycle()` which checks `> 1`, giving proposals a "last ratification opportunity" at `== 1`. InfoAction uses `>= 1`, expiring one epoch earlier than other proposal types.

The ledger applies the same general expiry check (`gasExpiresAfter < reCurrentEpoch`) to ALL proposals. InfoAction should expire at the same boundary.

**Recommendation:** Change to:
```java
if (context.isOutOfLifecycle()) {
    return RatificationResult.REJECT;
}
return RatificationResult.CONTINUE;
```

The practical impact is minor since InfoActions have no protocol effect, but affects dormant epoch determination and deposit refund timing.

---

### FINDING 8: Proposal Expiry Boundary Check - "isLastRatificationOpportunity" Semantics [INFO]

**Files:**
- `aggregates/governance-rules/src/main/java/.../domain/RatificationContext.java:23-28`

**Analysis:** `isOutOfLifecycle()` uses `> 1` and `isLastRatificationOpportunity()` uses `== 1`. At epoch `maxAllowedVotingEpoch + 1`, the proposal gets one last chance to ratify, matching the Haskell behavior where ratification is attempted BEFORE expiry is checked.

**Status:** CORRECT.

---

### FINDING 9: ParameterChange - DRep Threshold Returns ZERO When Only Security Params Changed [HIGH]

**Files:**
- `aggregates/governance-rules/src/main/java/.../voting/drep/DRepVotingEvaluator.java:66-74`
- `aggregates/governance-rules/src/main/java/.../util/ProtocolParamUtil.java:39-42`

**Issue:**
```java
return groups.stream()
    .filter(group -> group != ProtocolParamGroup.SECURITY)  // exclude SECURITY
    .map(group -> getThresholdForGroup(group, context))
    .max(BigDecimal::compareTo)
    .orElse(BigDecimal.ZERO);  // returns ZERO if only SECURITY group
```

The DRep threshold for ParameterChange is computed by filtering out the SECURITY group and taking the max of remaining groups. This is correct ONLY IF every security-relevant parameter also belongs to a regular group (NETWORK, ECONOMIC, TECHNICAL, or GOVERNANCE).

**Problem:** `minFeeRefScriptCostPerByte` is listed ONLY in the SECURITY group check (`ProtocolParamUtil.java:39-42`). It does NOT appear in any regular group (ECONOMIC, NETWORK, TECHNICAL, GOVERNANCE). In the Haskell ledger, it has `PPGroups EconomicGroup SecurityGroup`.

If a ParameterChange modifies only `minFeeRefScriptCostPerByte`, the groups would be `[SECURITY]`. After filtering: empty. DRep threshold = 0 (auto-pass). **The correct threshold should be `dvtPPEconomicGroup`.**

**Recommendation:** Add `minFeeRefScriptCostPerByte` to the ECONOMIC group check in `ProtocolParamUtil`. Verify all SECURITY params also have a regular group classification matching the Haskell `PPGroups` tags.

---

### FINDING 10: ParameterChange - SPO Voting Required Check Overly Broad [LOW]

**Files:**
- `aggregates/governance-rules/src/main/java/.../voting/spo/SPOVotingEvaluator.java:47-51`

**Issue:**
`isSPOVotingRequired()` returns `true` for ALL `PARAMETER_CHANGE_ACTION`, but SPOs only vote when security-relevant params are changed. The actual gate is in `ParameterChangeRatificationEvaluator` (line 49).

**Status:** Not a functional bug due to the gate in the ratification evaluator. The `isSPOVotingRequired()` method is misleading but harmless.

---

### FINDING 11: DRep Expiry - V9 Bonus Logic Discrepancy [MEDIUM]

**Files:**
- `aggregates/governance-aggr/src/main/java/.../util/DRepExpiryUtil.java:103-151, 174-199`

**Issue:**
The Haskell ledger for bootstrap phase (v9) uses: `expiry = currentEpoch + drepActivity` (no dormant epoch deduction). Yaci Store implements a complex V9 bonus calculation with dormant period detection, gap calculations, and last-dormant-period lookups.

This is because Yaci Store computes expiry retroactively at epoch boundaries (from historical data), whereas the Haskell ledger updates expiry in real-time as DReps act. The V9 bonus is a workaround for the retrospective calculation approach.

The TODO on line 145 (`//TODO: "<" or "<="`) indicates active uncertainty about boundary conditions.

**Recommendation:** Add extensive test cases comparing Yaci Store DRep expiry calculations against known mainnet data for bootstrap-era DReps.

---

### FINDING 12: DRep Distribution - Stale Deregistration Check [LOW]

**Files:**
- `aggregates/governance-aggr/src/main/java/.../service/DRepDistService.java:481-489`

**Issue:**
The DRep distribution query filters delegations with deregistered stake addresses using:
```sql
AND sd.type = 'STAKE_DEREGISTRATION'
```

The Conway era introduces `UNREG_CERT` (CIP-94) which also deregisters stake addresses but is not checked.

**Recommendation:** Add `OR sd.type = 'UNREG_CERT'` to the join condition.

---

### FINDING 13: DRep Distribution - Abstain Stake Exclusion [INFO]

**Files:**
- `aggregates/governance-aggr/src/main/java/.../service/VotingDataCollector.java:116-139`

`abstainVoteStake` and `autoAbstainStake` are correctly subtracted before computing `doNotVoteStake`, ensuring abstainers don't appear in the final vote tally.

**Status:** Correct.

---

### FINDING 14: SPO Stake Epoch Offset [INFO]

**Files:**
- `aggregates/governance-aggr/src/main/java/.../service/SPOVotingDataCollector.java:45, 135`

SPO stake queried for `epoch + 2` is correct - Cardano's stake snapshot for epoch N is used for block production in epoch N+2.

**Status:** Correct.

---

### FINDING 15: Proposal Refund Includes All Removed Proposals [INFO]

**Files:**
- `aggregates/governance-aggr/src/main/java/.../processor/ProposalRefundProcessor.java:86-90`

Expired, ratified, and sibling/descendant proposals are all included in `droppedProposals` for refund processing.

**Status:** Correct.

---

### FINDING 16: Delaying Action Scope [INFO]

**Files:**
- `aggregates/governance-rules/src/main/java/.../api/GovernanceEvaluationService.java:73-90`

The `isDelayedByDelayingAction` flag is local to the evaluation loop, starting `false` each epoch. Matches the Haskell `rsDelayed` behavior.

**Status:** Correct.

---

### FINDING 17: `isCommitteeNormal()` Check Blocks NoConfidence/UpdateCommittee Recovery [CRITICAL]

**Files:**
- All ratification evaluators: `NoConfidenceRatificationEvaluator.java:38`, `UpdateCommitteeRatificationEvaluator.java:41`, `HardForkRatificationEvaluator.java:47`, `ParameterChangeRatificationEvaluator.java:39`, `TreasuryWithdrawalRatificationEvaluator.java:40`, `NewConstitutionRatificationEvaluator.java:36`

**Issue:**
Every evaluator includes:
```java
final boolean isNotDelayed = context.isNotDelayed() && context.isCommitteeNormal();
```

This conflates two separate concerns: `rsDelayed` (whether a delaying action was ratified in this pass) and committee state. In the Haskell ledger:

```haskell
ratifyTransition:
  prevActionAsExpected && validCommitteeTerm
  && not rsDelayed          -- ONLY checks delay flag
  && withdrawalCanWithdraw
  && acceptedByEveryone     -- committee state checked PER-ACTION via voting thresholds
```

The committee state in Haskell flows through `votingCommitteeThreshold`:
- **NoConfidence/UpdateCommittee**: `NoVotingAllowed` -> auto-pass (committee does NOT block these)
- **Other actions when committee is SNothing**: `NoVotingThreshold` -> `committeeAccepted = False` (committee DOES block these)

In Yaci Store, `isCommitteeNormal()` returns `false` when state = `NO_CONFIDENCE`. This makes `isNotDelayed = false` for ALL evaluators, including NoConfidence and UpdateCommittee. This creates a **governance deadlock**:

1. NoConfidence motion passes -> committee state = NO_CONFIDENCE
2. UpdateCommittee proposal to install new committee arrives
3. `isNotDelayed = context.isNotDelayed() && context.isCommitteeNormal()` -> `true && false = false`
4. UpdateCommittee cannot be ratified
5. No other committee action can pass either
6. **Governance is deadlocked**

**Additional context:** The `CommitteeVotingEvaluator` does NOT check `committee.getState()` - it only checks `committee == null`. Since the `ConstitutionCommittee` object still exists in NO_CONFIDENCE state (with `state = NO_CONFIDENCE`), the evaluator would still try to evaluate committee votes. The `isCommitteeNormal()` check serves as a proxy for the Haskell `ensCommittee = SNothing` behavior for actions requiring committee approval (HardFork, ParameterChange, NewConstitution, Treasury).

**Root cause:** Two concepts conflated into one check:
1. "Committee dissolved, so committee-requiring actions should be blocked" (correct for HardFork, ParameterChange, etc.)
2. "NoConfidence/UpdateCommittee should still be ratifiable to recover governance" (INCORRECTLY blocked)

**Recommended fix:**
1. Remove `context.isCommitteeNormal()` from `isNotDelayed` in `NoConfidenceRatificationEvaluator` and `UpdateCommitteeRatificationEvaluator` (these must work when committee is dissolved)
2. For other evaluators (HardFork, ParameterChange, NewConstitution, Treasury): move the committee state check INTO `CommitteeVotingEvaluator` - when `committee.getState() == NO_CONFIDENCE`, return `NOT_PASS_THRESHOLD` instead of evaluating votes. This correctly models `ensCommittee = SNothing`. Then remove `isCommitteeNormal()` from `isNotDelayed`.

**Impact:** If a NoConfidence motion ever passes on mainnet, governance would be permanently deadlocked in Yaci Store's evaluation. This hasn't manifested yet because no NoConfidence motion has passed.

---

### FINDING 18: Missing Bootstrap Phase DRep Threshold Override [HIGH]

**Files:**
- `aggregates/governance-rules/src/main/java/.../voting/drep/DRepVotingEvaluator.java`
- `aggregates/governance-rules/src/main/java/.../ratification/impl/ParameterChangeRatificationEvaluator.java:43-44`

**Issue:**
During bootstrap phase (protocol version 9), the Haskell code sets ALL DRep thresholds to zero:
```haskell
votingDRepThresholdInternal pp isElectedCommittee action =
  let thresholds = if hardforkConwayBootstrapPhase (pp ^. ppProtocolVersionL)
                   then def  -- all thresholds = 0
                   else pp ^. ppDRepVotingThresholdsL
```

In Yaci Store, bootstrap handling is done per-evaluator:
- **ParameterChange**: Bootstrap -> committee only (skips DRep/SPO). Correct for DRep, but **misses SPO vote for security param changes** (the ledger still requires SPO vote during bootstrap).
- **HardFork**: Bootstrap -> committee + SPO (skips DRep). Correct.
- **NoConfidence/UpdateCommittee/NewConstitution/TreasuryWithdrawals**: No bootstrap-specific handling. DRep votes are empty during bootstrap (from `VotingDataCollector:57`), so `DRepVotingEvaluator` returns `INSUFFICIENT_DATA`. Since `INSUFFICIENT_DATA != PASS_THRESHOLD`, `isAccepted = false`. **These actions CANNOT pass during bootstrap.**

Per the ledger, DRep thresholds are zero during bootstrap (auto-pass). Actions that require DRep votes should still be ratifiable with only the other required voter bodies.

**Impact:** During bootstrap, NoConfidence, UpdateCommittee, NewConstitution, and TreasuryWithdrawals may fail because DRep voting returns `INSUFFICIENT_DATA` instead of `PASS_THRESHOLD`. However, during bootstrap on mainnet, some of these action types may be restricted by the GOV rule (not all actions allowed during bootstrap), limiting the practical impact.

**Recommendation:** Either:
1. Make `DRepVotingEvaluator` bootstrap-aware (return `PASS_THRESHOLD` during bootstrap), OR
2. Each ratification evaluator should treat `INSUFFICIENT_DATA` as pass during bootstrap for DRep votes.

---

### FINDING 19: Proposal Expiry - `maxAllowedVotingEpoch` Source Parameters [INFO]

**Files:**
- Need to trace how `maxAllowedVotingEpoch` is set in `ProposalContext`

The `maxAllowedVotingEpoch` should equal `proposedInEpoch + govActionLifetime`. This needs to use the `govActionLifetime` from the protocol params at the time of proposal submission, not the current params.

**Status:** Needs upstream verification. If `govActionLifetime` is from current epoch params rather than the proposal's creation epoch, it could be wrong if `govActionLifetime` changed between epochs.

---

## SUMMARY TABLE

| # | Finding | Severity | Status |
|---|---------|----------|--------|
| 1 | DRep NoConfidence double-counts noConfidence in denominator | HIGH | Bug - fix VoteTallyCalculator |
| 2 | DRep auto-pass when total stake is zero | MEDIUM | Bug - should fail |
| 3 | SPO auto-pass when all abstain | MEDIUM | Bug - should fail |
| 4 | Committee do-not-vote may include unregistered members | LOW | Verify upstream filtering |
| 5 | Missing committeeMinSize check post-bootstrap | HIGH | Bug - add check |
| 6 | NoConfidence/UpdateCommittee skip committee vote | INFO | Correct by design |
| 7 | InfoAction expires one epoch early | MEDIUM | Bug - use isOutOfLifecycle() |
| 8 | Proposal expiry boundary semantics | INFO | Correct |
| 9 | ParameterChange DRep threshold for security-only params | HIGH | Check param group classification |
| 10 | SPO isSPOVotingRequired() overly broad | LOW | Not functional bug |
| 11 | V9 bonus complexity vs ledger simplicity | MEDIUM | Needs extensive testing |
| 12 | Missing UNREG_CERT in deregistration check | LOW | Bug - add cert type |
| 13 | Abstain stake exclusion | INFO | Correct |
| 14 | SPO epoch+2 offset | INFO | Correct |
| 15 | Proposal refund includes ratified proposals | INFO | Correct |
| 16 | Delaying action scope | INFO | Correct |
| 17 | isCommitteeNormal() blocks NoConfidence/UpdateCommittee recovery | CRITICAL | Bug - governance deadlock risk |
| 18 | Bootstrap phase DRep threshold override incomplete | HIGH | Bug - DRep should auto-pass |
| 19 | maxAllowedVotingEpoch source params | INFO | Needs verification |

---

## RECOMMENDED FIXES (Priority Order)

### P0 - Critical
1. **Finding 17**: Fix `isCommitteeNormal()` handling:
   - Remove `context.isCommitteeNormal()` from `isNotDelayed` in `NoConfidenceRatificationEvaluator` and `UpdateCommitteeRatificationEvaluator` (these must work when committee is dissolved)
   - For remaining evaluators (HardFork, ParameterChange, NewConstitution, Treasury): move committee state check into `CommitteeVotingEvaluator` - return `NOT_PASS_THRESHOLD` when `committee.getState() == NO_CONFIDENCE`, then remove `isCommitteeNormal()` from `isNotDelayed`

### P1 - High
2. **Finding 1**: Fix DRep NO_CONFIDENCE double-counting in `VoteTallyCalculator.computeDRepTallies()`
3. **Finding 5**: Add `committeeMinSize` check in `CommitteeVotingEvaluator`
4. **Finding 9**: Add `minFeeRefScriptCostPerByte` to ECONOMIC group in `ProtocolParamUtil`
5. **Finding 18**: Ensure DRep auto-pass during bootstrap for all applicable action types

### P2 - Medium
6. **Finding 2**: Change DRep zero-stake behavior to NOT_PASS_THRESHOLD
7. **Finding 3**: Change SPO all-abstain behavior to check threshold before auto-passing
8. **Finding 7**: Fix InfoAction expiry to use `isOutOfLifecycle()`
9. **Finding 11**: Add comprehensive V9 bonus test coverage with mainnet comparison data

### P3 - Low
10. **Finding 4**: Verify committee member filtering upstream
11. **Finding 10**: Consider making `isSPOVotingRequired` param-group-aware
12. **Finding 12**: Add `UNREG_CERT` to DRep dist deregistration check

---

## VERIFICATION PLAN

### Unit Tests
- Add test cases for each finding above
- For Finding 1: Test NO_CONFIDENCE action with varying noConfidence stake amounts
- For Finding 5: Test committee below minSize post-bootstrap
- For Finding 17: Test UpdateCommittee ratification when committee is in NO_CONFIDENCE state
- For Finding 18: Test all action types during bootstrap phase

### Integration Tests
- Compare Yaci Store governance results against cardano-node local state queries for known mainnet epochs
- Verify DRep expiry calculations against db-sync for bootstrap-era DReps
- Test governance recovery scenario: NoConfidence enacted -> UpdateCommittee should still ratify

### Mainnet Validation
- Compare proposal status snapshots (ACTIVE/RATIFIED/EXPIRED) against cardano-node for recent epochs
- Verify DRep distribution amounts against cardano-node `drepStakeDistribution` query
- Cross-reference committee voting results with known mainnet votes

---

## FILES REVIEWED

### governance-rules module
- `GovernanceEvaluationService.java` - Evaluation orchestration
- `VoteTallyCalculator.java` - Vote tallying
- `DRepVotingEvaluator.java` - DRep threshold evaluation
- `SPOVotingEvaluator.java` - SPO threshold evaluation
- `CommitteeVotingEvaluator.java` - Committee threshold evaluation
- `NoConfidenceRatificationEvaluator.java`
- `UpdateCommitteeRatificationEvaluator.java`
- `NewConstitutionRatificationEvaluator.java`
- `HardForkRatificationEvaluator.java`
- `ParameterChangeRatificationEvaluator.java`
- `TreasuryWithdrawalRatificationEvaluator.java`
- `InfoActionRatificationEvaluator.java`
- `GovernanceActionUtil.java` - Priority, delaying, previous action
- `ProtocolParamUtil.java` - Parameter group classification
- `ProposalDropService.java` - Sibling/descendant drop logic
- `RatificationContext.java` - Lifecycle checks
- `GovernanceContext.java` - State container
- `ProposalUtils.java` (governance-rules) - Purpose matching

### governance-aggr module
- `ProposalStateProcessor.java` - Epoch boundary orchestration
- `ProposalStatusMapper.java` - Status mapping
- `ProposalRefundProcessor.java` - Deposit refunds
- `GovEpochActivityProcessor.java` - Dormant epoch tracking
- `DRepDistService.java` - DRep distribution SQL
- `DRepExpiryUtil.java` - DRep expiry calculation
- `VotingDataCollector.java` - Vote data aggregation
- `SPOVotingDataCollector.java` - SPO stake aggregation
- `ProposalUtils.java` (governance-aggr) - Sibling/descendant finding
- `CommitteeStateProcessor.java` - Committee state transitions
- `CommitteeStateService.java` - Committee state persistence
