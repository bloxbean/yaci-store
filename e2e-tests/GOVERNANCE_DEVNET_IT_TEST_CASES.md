# Governance Devnet Integration Test Cases

This file lists the governance DevKit integration tests currently present in
`e2e-tests/src/test/java/com/bloxbean/cardano/yaci/store/test/e2e/gov`.

The rule-focused tests submit real governance transactions to a DevKit devnet,
wait for yaci-store indexing/AdaPot processing, assert the database outcome, and
cross-check comparable facts against the cardano-node ledger snapshot through
`LedgerGovernanceStateReader`.

API status translation is intentionally separate from rule parity tests.

## Summary

| Test class | Active JUnit methods | Purpose |
|---|---:|---|
| `GovernanceProposalLifecycleIT` | 1 | Proposal lifecycle/indexer stability across epochs |
| `GovernanceProposalApiStatusIT` | 1 | Public API status mapping from stored proposal status rows |
| `GovernanceProposalOutcomeIT` | 2 | Post-bootstrap action outcome and ratification-gate parity |
| `GovernanceDRepVoteTallyIT` | 2 | DRep vote aggregation, replacement, and accepted-ratio parity |
| `GovernanceDRepLifecycleVoteTallyIT` | 1 | DRep unregister/re-registration and same-transaction cleanup effects on active proposal tallies |
| `GovernanceSPOVoteTallyIT` | 2 | SPO stake distribution and mixed voting-group parity |
| `GovernanceCommitteeVoteTallyIT` | 1 | Committee size and quorum parity |
| `GovernanceRuleEdgeIT` | 6 active, 1 disabled | Proposal-scoped/default-vote reshaping and voter eligibility edges |
| `GovernanceInterProposalIT` | 5 | Multi-proposal ordering, drop, delay, and effect-context behavior |
| `GovernanceEffectContextIT` | 2 active, 1 disabled | Post-audit enacted-state effect contexts |

## Assertion Boundaries

- Rule parity tests do not assert `ProposalApiService` or API DTO behavior.
- Rule parity tests assert DB/state-layer output through `ProposalStateClient`,
  `GovActionProposalStatusRepository`, and `GovernanceRuleAssertionHelper`.
- Rule parity tests compare DB status with node ledger state using
  `LedgerGovernanceStateReader`.
- API translation tests use `GovernanceApiAssertionHelper` and
  `ProposalApiService` over controlled stored rows.

## `GovernanceProposalLifecycleIT`

### `devnetProposals_shouldExpireRefundAndRemainStableAcrossEpochDrift`

Covers real proposal lifecycle/indexer behavior:

- `InfoAction` proposal is indexed as a real on-chain proposal.
- `InfoAction` remains `ACTIVE` until the last voting opportunity.
- `InfoAction` becomes `EXPIRED` at the lifecycle boundary.
- `InfoAction` deposit refund is recorded in the expected spendable epoch.
- A staggered `NoConfidence` proposal follows the same no-vote expiry path.
- Status rows do not duplicate as later AdaPot jobs and epoch evaluations arrive.

## `GovernanceProposalApiStatusIT`

### `getProposalById_shouldTranslateCurrentAndStaleStorageStatuses`

Uses synthetic proposal/status rows to isolate API status mapping:

- Current storage `ACTIVE` maps to API `LIVE`.
- Storage `EXPIRED` stays API `EXPIRED`.
- Stale storage `ACTIVE` maps to API `DROPPED`.
- Current storage `RATIFIED` maps to API `RATIFIED`.
- Stale storage `RATIFIED` maps to API `ENACTED`.

## `GovernanceProposalOutcomeIT`

### `postBootstrapActionTypes_shouldMatchLedgerOutcomes`

Covers active post-bootstrap action outcome rows:

- `InfoAction` with votes still expires.
- `InfoAction` without votes expires.
- `HardForkInitiation` without committee support expires.
- `HardForkInitiation` with DRep, SPO, and committee support ratifies.
- `NewConstitution` without committee support expires.
- `NewConstitution` with DRep and committee support ratifies.
- `TreasuryWithdrawals` without committee support expires.
- `TreasuryWithdrawals` with DRep and committee support ratifies.
- Security parameter change without SPO support expires.
- Network-only parameter change with DRep and committee support ratifies.
- Committee update rejected by DRep expires.
- Committee update with DRep and SPO support ratifies.
- No-confidence action rejected by DRep expires.

Deferred row:

- No-confidence action with DRep and SPO support should ratify, but this row is
  disabled/deferred until yaci-core `GovStateQuery` can decode the post
  no-confidence ledger state without `IndexOutOfBoundsException`.

### `postBootstrapRatificationGates_shouldBlockOtherwiseAcceptedProposals`

Covers post-bootstrap qualifier gates where votes would otherwise be sufficient:

- Treasury withdrawal above available treasury expires.
- Committee update with invalid member term expires.

## `GovernanceDRepVoteTallyIT`

### `dRepAbstainStake_shouldStayOutOfAcceptedRatioDenominator`

- DRep `AlwaysAbstain` stake is recorded as auto-abstain.
- Auto-abstain stake is excluded from the `yes / (yes + no)` accepted-ratio denominator.
- DRep YES/NO tallies match ledger stake snapshots.
- Committee quorum also passes.

### `latestDRepVote_shouldReplaceEarlierVote`

- The same DRep votes `YES`, then votes `NO` for the same proposal.
- The later vote replaces the earlier vote.
- Effective DRep YES stake becomes zero.
- Effective DRep NO stake equals that DRep's ledger stake.
- The proposal expires because the DRep accepted ratio is zero.

## `GovernanceDRepLifecycleVoteTallyIT`

### `dRepLifecycle_shouldClearInvalidatedVotesAndAcceptNewVote`

- A DRep with dominant stake votes `YES`, unregisters, re-registers the same
  credential, and restores its delegation without casting a new vote for the proposal.
- A smaller DRep votes `NO`.
- Re-registration plus a new delegation restores the DRep's voting power but
  does not restore the cleared `YES` vote, so the proposal expires.
- A control proposal verifies that a new `NO` vote cast after re-registration
  is counted while the earlier `YES` vote remains cleared.
- A same-transaction regression creates an info proposal followed by a new-constitution
  proposal, then submits votes for both proposals together with DRep unregistration and
  re-registration certificates. The constitution proposal has enough committee support
  and would ratify if the dominant DRep's invalidated `YES` vote were retained.
- The second vote has `voting_procedure.idx = 1` while the unregistration has
  `drep_registration.cert_index = 0`; both votes must still be cleared because
  ledger cleanup is transaction-wide and the two index spaces are unrelated.
- The DRep delegation is restored before the outcome snapshot so distribution
  eligibility cannot hide an incorrectly retained vote.
- The test asserts that the restored stake remains in the distribution, calculates
  the counterfactual accepted ratio, and verifies that the correctly cleared vote
  changes the outcome from `RATIFIED` to `EXPIRED`.
- The test asserts effective voting stats and DB-vs-ledger outcome, not deletion of raw historical vote rows.

## `GovernanceSPOVoteTallyIT`

### `spoVotes_shouldUseStakeFromAllKnownPools`

- SPO YES, NO, and non-voting pool buckets use real pool stake snapshots.
- SPO accepted ratio includes all known pools used by the fixture.
- Committee update ratifies when the SPO accepted ratio crosses the configured threshold.

### `mixedSecurityAndNetworkParameterChange_shouldRequireAllVotingGroups`

Covers mixed protocol parameter groups:

- Mixed security + network parameter change expires when DRep votes `NO`, even
  though known SPOs and committee members vote `YES`.
- Mixed security + network parameter change ratifies when DRep, SPO, and committee
  groups all pass.

## `GovernanceCommitteeVoteTallyIT`

### `committeeBelowMinimumSize_shouldBlockNewConstitutionRatification`

- A prior committee update reduces the active committee below `committeeMinSize`.
- A later `NewConstitution` proposal expires even with DRep YES support.
- The test asserts the ledger semantic that an undersized committee blocks
  ratification outside bootstrap.

## `GovernanceRuleEdgeIT`

### Active methods

#### `spoAlwaysAbstainDefault_shouldBeAppliedPerProposal`

- Proposals A and B are created in the same epoch and evaluated from the same
  SPO stake snapshot; B is a security protocol-parameter change.
- Pool P delegates its reward account to `AlwaysAbstain` and votes Yes only on
  A. Pool Q uses the ordinary default No and votes Yes only on B.
- B includes P in effective Abstain rather than do-not-vote/effective No, while
  Q supplies B's explicit Yes stake.
- DRep and committee votes deliberately pass their thresholds, leaving SPO
  classification as the only deciding rule for B.
- B is ratified when P's default is classified independently for B. Classifying
  P as No would put its stake in the denominator and fail the SPO threshold.

#### `predefinedDRepAlwaysNoConfidence_votesNoForOtherActions`

- Stake delegated to the predefined `AlwaysNoConfidence` DRep counts as effective
  DRep `NO` for non-no-confidence actions.
- A network-only parameter change expires despite normal DRep YES and committee support.

#### `predefinedDRepAlwaysAbstain_excludedFromDenominator`

- Stake delegated to predefined `AlwaysAbstain` is present as auto-abstain stake.
- Auto-abstain stake is excluded from the DRep accepted-ratio denominator.
- A `NewConstitution` proposal ratifies with the normal DRep YES bucket and committee support.

#### `spoAlwaysAbstainDefaultVote_reshapedByAction`

Covers action-specific SPO default-vote reshaping:

- For `HardForkInitiation`, non-voting SPO stake delegated through an
  `AlwaysAbstain` reward account is treated as effective SPO `NO`, so the proposal expires.
- For `UpdateCommittee`, the same non-voting SPO stake is treated as effective
  SPO `ABSTAIN`, so the proposal ratifies.

#### `drepInactive_excludedFromRatio`

- A DRep is made inactive by advancing through non-dormant governance epochs.
- Inactive DRep stake is excluded from the DRep accepted-ratio denominator.
- A `NewConstitution` proposal ratifies with only the active DRep YES bucket and committee support.

#### `committeeHotKeyResigned`

- A committee hot key casts a vote, then resigns before ratification.
- The resigned member is excluded from the committee ratio denominator.
- Remaining non-voting committee members still keep the committee ratio at zero.
- The `NewConstitution` proposal expires.

### Disabled/deferred method

#### `predefinedDRepAlwaysNoConfidence_votesYesOnlyForNoConfidence`

- Intended coverage: predefined `AlwaysNoConfidence` stake becomes effective YES
  for a `NoConfidence` action.
- Deferred because the proposal can ratify, but yaci-core `GovStateQuery` currently
  fails while decoding the post no-confidence ledger state.

## `GovernanceInterProposalIT`

### `higherPriorityDelayingAction_shouldHoldLowerPriorityProposal`

- A higher-priority `NewConstitution` proposal ratifies first.
- A lower-priority parameter change with passing votes remains `ACTIVE` and delayed
  at the same ratification boundary.

### `childProposal_shouldRatifyAfterReferencedParentIsEnacted`

- A root constitution proposal ratifies.
- A child constitution proposal referencing the parent's `prevGovActionId` ratifies
  after the parent is enacted.

### `ratifiedProposal_shouldDropCompetingSiblingInSamePurpose`

- Two root constitution proposals compete in the same purpose group.
- One sibling ratifies.
- The competing sibling is removed from ledger current proposals and is not ratified.

### `delayingHardFork_shouldDeferParameterChangeUntilNextBoundary`

- A delaying `HardForkInitiation` ratifies at the first boundary.
- A non-security parameter change with passing votes remains active and delayed at
  that boundary.
- The parameter change ratifies at the next boundary when `rsDelayed` resets.

### `laterTreasuryWithdrawal_shouldUseTreasuryAfterPriorWithdrawal`

- A first treasury withdrawal ratifies and changes the ledger treasury context.
- A later treasury withdrawal asks for more than the remaining treasury.
- The second withdrawal expires even with passing votes.

## `GovernanceEffectContextIT`

### `enactedCommitteeThreshold_shouldControlLaterNewConstitution`

- A committee-update proposal ratifies and raises the committee threshold from `1/3` to `2/3`.
- Indexed committee threshold and member state are asserted before the follow-up proposal.
- A later `NewConstitution` proposal has DRep support and one committee YES vote.
- The later proposal expires because one-of-three committee support no longer satisfies the enacted threshold.

### `ratifiedSibling_shouldPruneDescendantProposals`

- Two root `NewConstitution` siblings, one child of the second root, and one grandchild of that child are active at ratification time.
- Only the first root receives passing votes.
- The first root ratifies.
- The competing root, child, and grandchild are not ratified and are removed from ledger current proposals.
- Proposal refund rewards include deposits for the enacted root and the full pruned subtree.

### Disabled/deferred method

#### `noConfidence_shouldRemoveCommitteeUntilUpdateCommitteeRestoresIt`

- Intended coverage: no-confidence enactment removes the committee and a later update committee restores it.
- Deferred until yaci-core `GovStateQuery` can decode absent committee state after no-confidence.

## Deferred Follow-Up

- Runtime status: the split vote-tally classes and active
  `GovernanceEffectContextIT` rows have been manually re-run successfully
  against DevKit after the fixture split.
- Re-enable the positive no-confidence rows after the yaci-core `GovStateQuery`
  decoding issue is fixed.
- Add opt-in CI/runbook coverage for DevKit-backed E2E once the suite and runtime
  environment are stable enough (if needed)

## Useful Commands

Compile E2E test sources:

```bash
./gradlew :e2e-tests:compileTestJava --console=plain --quiet
```

Run one class:

```bash
./gradlew :e2e-tests:test -PrunE2ETests --tests "*GovernanceDRepVoteTallyIT" --console=plain --quiet
```

Run the split vote-tally classes:

```bash
./gradlew :e2e-tests:test -PrunE2ETests --tests "*GovernanceDRepVoteTallyIT" --tests "*GovernanceDRepLifecycleVoteTallyIT" --tests "*GovernanceSPOVoteTallyIT" --tests "*GovernanceCommitteeVoteTallyIT" --console=plain --quiet
```

Run the governance E2E classes:

```bash
./gradlew :e2e-tests:test -PrunE2ETests --tests "*Governance*IT" --console=plain --quiet
```
