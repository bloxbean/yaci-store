# governance-rules

Purpose-built library for evaluating Cardano Conway (CIP-1694) governance proposals. It determines per-epoch proposal outcomes (RATIFIED/ACTIVE/EXPIRED) and which proposals must be dropped at the epoch boundary, mirroring cardano-ledger semantics at a high level.

## What & Why
- What: A lightweight, reusable rules engine that:
  - Sorts and evaluates active proposals using action priority and delay semantics.
  - Computes DRep/SPO/Committee tallies against protocol thresholds to ratify or continue/reject proposals.
  - Propagates proposal drops across purpose-specific dependency chains (siblings/descendants) at epoch boundaries.
- Why: Provide a clean, library-style API so other services (e.g., yaci-store governance-aggr) can plug in snapshot data and obtain deterministic results consistent with the ledger’s RATIFY rule.

## Quickstart
```java
// 1) Build GovernanceEvaluationInput from your snapshot data
GovernanceEvaluationInput input = GovernanceEvaluationInput.builder()
    .currentProposals(List.of(
        ProposalContext.builder()
            .govAction(govAction)                    // com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction
            .votingData(votingData)                  // VotingData with DRep/SPO/Committee votes
            .govActionId(govActionId)
            .proposalSlot(slot)
            .maxAllowedVotingEpoch(proposalEpoch + govActionLifetime)
            .build()
    ))
    .currentEpoch(currentEpoch)
    .protocolParams(protocolParams)                  // Includes DRep/Pool thresholds
    .committee(committee)                            // Members, threshold, state
    .isBootstrapPhase(isBootstrap)
    .treasury(treasury)
    .lastEnactedGovActionIds(lastEnactedByPurpose)   // Map<ProposalType, GovActionId>
    .build();

// 2) Evaluate
GovernanceEvaluationService service = new GovernanceEvaluationService();
GovernanceEvaluationResult result = service.evaluateGovernanceState(input);

// 3) Consume output
result.getProposalResults();     // List<ProposalEvaluationResult> (ACCEPT/CONTINUE/REJECT)
result.getProposalsToDropNext(); // Proposals to drop at the epoch boundary
result.isActionRatificationDelayed(); // True if a delaying action was ACCEPT in this epoch
```

## Concepts and I/O
- Input
  - `currentProposals`: List of `ProposalContext` (GovAction, VotingData, GovActionId, slot, maxAllowedVotingEpoch).
  - Governance state: `currentEpoch`, `ProtocolParams` (thresholds), `ConstitutionCommittee` (members/threshold/state), `isBootstrapPhase`, `treasury`, `lastEnactedGovActionIds`.
- Output
  - `proposalResults`: ACCEPT (RATIFIED), CONTINUE (ACTIVE), REJECT (EXPIRED) per proposal.
  - `proposalsToDropNext`: proposals that must be dropped at the epoch boundary (cascade via siblings/descendants by purpose).
  - `isActionRatificationDelayed`: true if any delaying action (NoConfidence/UpdateCommittee/NewConstitution/HardFork) was ACCEPT at the epoch boundary.

## Core API (at-a-glance)
- `GovernanceEvaluationService`
  - `evaluateGovernanceState(GovernanceEvaluationInput)` → `GovernanceEvaluationResult`
  - Sorts proposals by action priority, applies ratification rules per action, and computes drop set.
- `VoteTallyCalculator`
  - Helpers to compute DRep/SPO/Committee tallies consistently with ledger rules; reused by evaluators and reporting.
- `VotingData`
  - Encapsulates DRep/SPO/Committee votes; different actions consume different subsets.
- `RatificationEvaluatorFactory`
  - Dispatches to action-specific evaluators (HardFork/ParameterChange/UpdateCommittee/NewConstitution/NoConfidence/TreasuryWithdrawals/InfoAction).
- `ProposalDropService`
  - Computes proposals to drop at the epoch boundary (expired descendants, ratified siblings and their descendants).

## Integration with yaci-store (governance-aggr)
- governance-aggr builds `AggregatedGovernanceData` (proposals + votes + committee + params + treasury) and maps it to `GovernanceEvaluationInput`.
- It calls `GovernanceEvaluationService` at epoch boundary to compute proposal status and uses `ProposalDropService` to pre-filter active proposals before evaluation.
- See:
  - `.../governanceaggr/processor/ProposalStateProcessor.java`
  - `.../governanceaggr/storage/impl/mapper/GovernanceEvaluationInputMapper.java`
  - `.../governanceaggr/service/ProposalCollectionService.java`

## Guarantees & Compatibility
- Ledger-aligned behaviors (high level):
  - Action priority ordering; delaying actions semantics.
  - DRep/SPO/Committee tallies and thresholds, including bootstrap-phase behavior.
  - `prevActionAsExpected`, `validCommitteeTerm`, and treasury withdrawal bounds.
- Limitations / notes:
  - Lifecycle boundaries depend on `maxAllowedVotingEpoch` supplied by caller; ensure it matches network params.
  - Provide consistent snapshot inputs (thresholds, stake, votes) for deterministic outcomes.

## Further Reading
- Architecture and design (mapping to ledger and action-specific rules): planned docs
  - ARCHITECTURE.md — rationale, layering, and data flow
  - LEDGER-MAPPING.md — references to cardano-ledger (Ratify/actionPriority/delayingAction/tally)
  - RULES.md — action-by-action acceptance criteria and examples
