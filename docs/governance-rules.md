# Governance Rules

## Overview

This module contains only rules / logic to derive governance ratification results and provide java apis for the caller,
it is a stateless module and only implements rule logic. Like given a gov action, voting results, decide the result.

## RatificationResult

Basically, when evaluating whether a governance action can be ratified or not, there are three possible outcomes.
These outcomes are represented through the RatificationResult enum.

The `RatificationResult` enumeration represents the possible outcomes of a ratification process. The possible values are:

- `ACCEPT`: The governance action is accepted.
- `REJECT`: The governance action is rejected.
- `CONTINUE`: the governance action is kept for further voting.

You can obtain this ratification result through the APIs of the `GovActionRatifier` class.

## GovActionRatifier API

The `GovActionRatifier` class provides several public methods that can be used to determine the ratification result for different types of governance actions. Here are the available methods:

### getRatificationResult

This method determines the ratification result for a governance action.

Parameters:
- `govAction`: The governance action for which ratification result is needed.
- `ccYesVote`: The total votes of the Constitution Committee that voted 'Yes': the number of registered, unexpired, unresigned committee members that voted yes.
- `ccNoVote`: The total votes of the Constitution Committee that voted 'No': the number of registered, unexpired, unresigned committee members that voted no, plus the number of registered, unexpired, unresigned committee members that did not vote for this action.
- `ccQuorum`: The quorum of the Constitution Committee.
- `spoYesVoteStake`: The total delegated stake from SPO that voted 'Yes'.
- `spoAbstainVoteStake`: The total delegated stake from SPO that voted 'Abstain'.
- `spoTotalStake`: The total delegated stake from SPO.
- `dRepYesVoteStake`: The total stake of registered dReps that voted 'Yes', plus the AlwaysNoConfidence dRep, in case the action is NoConfidence.
- `dRepNoVoteStake`: The total stake of registered dReps that voted 'No', plus registered dReps that did not vote for this action, plus the AlwaysNoConfidence dRep.
- `ccState`: The current Constitution Committee state.
- `lastEnactedGovActionId`: The last enacted governance action ID of the same purpose.
- `currentEpochParam`: The current epoch parameters.

Returns:
- The ratification result.

Note: For `HardForkInitiation` all SPOs that didn't vote are considered as `No` votes. 
Whereas, for all other `GovAction`s, SPOs that didn't vote are considered as `Abstain` votes.

### getRatificationResultForNoConfidenceAction

This method determines the ratification result for a No Confidence governance action.

Parameters:
- `noConfidence`: The No Confidence governance action for which ratification result is needed.
- `spoYesVoteStake`: The total delegated stake from SPO that voted 'Yes'.
- `spoAbstainVoteStake`: The total delegated stake from SPO that voted 'Abstain'.
- `spoTotalStake`: The total delegated stake from SPO.
- `dRepYesVoteStake`: The total stake of registered dReps that voted 'Yes', plus the AlwaysNoConfidence dRep, in case the action is NoConfidence.
- `dRepNoVoteStake`: The total stake of registered dReps that voted 'No', plus registered dReps that did not vote for this action, plus the AlwaysNoConfidence dRep.
- `lastEnactedGovActionId`: The last enacted governance action ID of the same purpose.
- `currentEpochParam`: The current epoch parameters.

Returns:
- The ratification result for the No Confidence action.

### getRatificationResultForUpdateCommitteeAction

This method determines the ratification result for an Update Committee governance action.

Parameters:
- `updateCommittee`: The Update Committee governance action for which ratification result is needed.
- `spoYesVoteStake`: The total delegated stake from SPO that voted 'Yes'
- `spoAbstainVoteStake`: The total delegated stake from SPO that voted 'Abstain'.
- `spoTotalStake`: The total delegated stake from SPO.
- `dRepYesVoteStake`: The total stake of registered dReps that voted 'Yes', plus the AlwaysNoConfidence dRep, in case the action is NoConfidence.
- `dRepNoVoteStake`: The total stake of registered dReps that voted 'No', plus registered dReps that did not vote for this action, plus the AlwaysNoConfidence dRep.
- `ccState`: The current Constitution Committee state.
- `lastEnactedGovActionId`: The last enacted governance action ID of the same purpose.
- `currentEpochParam`: The current epoch parameters.

Returns:
- The ratification result for the Update Committee action.

### getRatificationResultForInfoAction

This method determines the ratification result for an Info governance action.

Parameters:
- `infoAction`: The Info governance action for which ratification result is needed.
- `ccYesVote`: The total votes of the Constitution Committee that voted 'Yes': the number of registered, unexpired, unresigned committee members that voted yes.
- `ccNoVote`: The total votes of the Constitution Committee that voted 'No': the number of registered, unexpired, unresigned committee members that voted no, plus the number of registered, unexpired, unresigned committee members that did not vote for this action.
- `ccQuorum`: The quorum of the Constitution Committee.
- `spoYesVoteStake`: The total delegated stake from SPO that voted 'Yes'.
- `spoAbstainVoteStake`: The total delegated stake from SPO that voted 'Abstain'.
- `spoTotalStake`: The total delegated stake from SPO.
- `dRepYesVoteStake`: The total stake of registered dReps that voted 'Yes', plus the AlwaysNoConfidence dRep, in case the action is NoConfidence.
- `dRepNoVoteStake`: The total stake of registered dReps that voted 'No', plus registered dReps that did not vote for this action, plus the AlwaysNoConfidence dRep.
- `lastEnactedGovActionId`: The last enacted governance action ID of the same purpose.
- `currentEpochParam`: The current epoch parameters.

Returns:
- The ratification result for the Info action.

### getRatificationResultForHardForkInitiationAction

This method determines the ratification result for a Hard Fork Initiation governance action.

Parameters:
- `hardForkInitiationAction`: The Hard Fork Initiation governance action for which ratification result is needed.
- `ccYesVote`: The total votes of the Constitution Committee that voted 'Yes': the number of registered, unexpired, unresigned committee members that voted yes.
- `ccNoVote`: The total votes of the Constitution Committee that voted 'No': the number of registered, unexpired, unresigned committee members that voted no, plus the number of registered, unexpired, unresigned committee members that did not vote for this action.
- `ccQuorum`: The quorum of the Constitution Committee.
- `spoYesVoteStake`: The total delegated stake from SPO that voted 'Yes'.
- `spoAbstainVoteStake`: The total delegated stake from SPO that voted 'Abstain'.
- `spoTotalStake`: The total delegated stake from SPO.
- `dRepYesVoteStake`: The total stake of registered dReps that voted 'Yes', plus the AlwaysNoConfidence dRep, in case the action is NoConfidence.
- `dRepNoVoteStake`: The total stake of registered dReps that voted 'No', plus registered dReps that did not vote for this action, plus the AlwaysNoConfidence dRep.
- `lastEnactedGovActionId`: The last enacted governance action ID of the same purpose.
- `currentEpochParam`: The current epoch parameters.

Returns:
- The ratification result for the Hard Fork Initiation action.

### getRatificationResultForNewConstitutionAction

This method determines the ratification result for a New Constitution governance action.

Parameters:
- `newConstitution`: The New Constitution governance action for which ratification result is needed.
- `ccYesVote`: The total votes of the Constitution Committee that voted 'Yes': the number of registered, unexpired, unresigned committee members that voted yes.
- `ccNoVote`: The total votes of the Constitution Committee that voted 'No': the number of registered, unexpired, unresigned committee members that voted no, plus the number of registered, unexpired, unresigned committee members that did not vote for this action.
- `ccQuorum`: The quorum of the Constitution Committee.
- `dRepYesVoteStake`: The total stake of registered dReps that voted 'Yes', plus the AlwaysNoConfidence dRep, in case the action is NoConfidence.
- `dRepNoVoteStake`: The total stake of registered dReps that voted 'No', plus registered dReps that did not vote for this action, plus the AlwaysNoConfidence dRep.
- `lastEnactedGovActionId`: The last enacted governance action ID of the same purpose.
- `currentEpochParam`: The current epoch parameters.

Returns:
- The ratification result for the New Constitution action.

### getRatificationResultForTreasuryWithdrawalsAction

This method determines the ratification result for a Treasury Withdrawals governance action.

Parameters:
- `treasuryWithdrawalsAction`: The Treasury Withdrawals governance action for which ratification result is needed.
- `ccYesVote`: The total votes of the Constitution Committee that voted 'Yes': the number of registered, unexpired, unresigned committee members that voted yes.
- `ccNoVote`: The total votes of the Constitution Committee that voted 'No': the number of registered, unexpired, unresigned committee members that voted no, plus the number of registered, unexpired, unresigned committee members that did not vote for this action.
- `ccQuorum`: The quorum of the Constitution Committee.
- `spoYesVoteStake`: The total delegated stake from SPO that voted 'Yes'.
- `spoAbstainVoteStake`: The total delegated stake from SPO that voted 'Abstain'.
- `spoTotalStake`: The total delegated stake from SPO.
- `lastEnactedGovActionId`: The last enacted governance action ID of the same purpose.
- `currentEpochParam`: The current epoch parameters.

Returns:
- The ratification result for the Treasury Withdrawals action.

### getRatificationResultForParameterChangeAction

This method determines the ratification result for a Protocol Parameters Change governance action.

Parameters:
- `parameterChangeAction`: The Parameter Change governance action for which ratification result is needed.
- `ccYesVote`: The total votes of the Constitution Committee that voted 'Yes': the number of registered, unexpired, unresigned committee members that voted yes.
- `ccNoVote`: The total votes of the Constitution Committee that voted 'No': the number of registered, unexpired, unresigned committee members that voted no, plus the number of registered, unexpired, unresigned committee members that did not vote for this action.
- `ccQuorum`: The quorum of the Constitution Committee.
- `spoYesVoteStake`: The total delegated stake from SPO that voted 'Yes'.
- `spoAbstainVoteStake`: The total delegated stake from SPO that voted 'Abstain'.
- `spoTotalStake`: The total delegated stake from SPO.
- `dRepYesVoteStake`: The total stake of registered dReps that voted 'Yes', plus the AlwaysNoConfidence dRep, in case the action is NoConfidence.
- `dRepNoVoteStake`: The total stake of registered dReps that voted 'No', plus registered dReps that did not vote for this action, plus the AlwaysNoConfidence dRep.
- `lastEnactedGovActionId`: The last enacted governance action ID of the same purpose.
- `currentEpochParam`: The current epoch parameters.

Returns:
- The ratification result for the Parameter Change action.