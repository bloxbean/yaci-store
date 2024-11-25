package com.bloxbean.cardano.yaci.store.governancerules.rule;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.*;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ProtocolParamGroup;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.util.ProtocolParamUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Slf4j
public class GovActionRatifier {

    /**
     * Determines the ratification result for a governance action.
     *
     * @param govAction              The governance action for which ratification result is needed.
     * @param ccYesVote              The total votes of the Constitution Committee that voted 'Yes':
     *                                  the number of registered, unexpired, unresigned committee members that voted yes
     * @param ccNoVote               The total votes of the Constitution Committee that voted 'No':
     *                                  - the number of registered, unexpired, unresigned committee members that voted no, plus
     *                                  - the number of registered, unexpired, unresigned committee members that did not vote for this action
     * @param ccThreshold            The threshold of the Constitution Committee.
     * @param spoYesVoteStake        The total delegated stake from SPO that voted 'Yes'.
     * @param spoAbstainVoteStake    The total delegated stake from SPO that voted 'Abstain'.
     * @param spoTotalStake          The total delegated stake from SPO.
     * @param dRepYesVoteStake       The total stake of:
     *                               1. Registered dReps that voted 'Yes', plus
     *                               2. The AlwaysNoConfidence dRep, in case the action is NoConfidence.
     * @param dRepNoVoteStake        The total stake of:
     *                               1. Registered dReps that voted 'No', plus
     *                               2. Registered dReps that did not vote for this action, plus
     *                               3. The AlwaysNoConfidence dRep.
     * @param ccState                The current Constitution Committee state.
     * @param lastEnactedGovActionId The last enacted governance action ID of the same purpose.
     * @param currentEpochParam      The current epoch parameters.
     * @return The ratification result.
     */
    public static RatificationResult getRatificationResult(GovAction govAction, Integer ccYesVote, Integer ccNoVote, BigDecimal ccThreshold,
                                                           BigInteger spoYesVoteStake, BigInteger spoAbstainVoteStake, BigInteger spoTotalStake,
                                                           BigInteger dRepYesVoteStake, BigInteger dRepNoVoteStake,
                                                           ConstitutionCommitteeState ccState, GovActionId lastEnactedGovActionId,
                                                           EpochParam currentEpochParam) {
        final GovActionType govActionType = govAction.getType();
        if (govActionType == GovActionType.INFO_ACTION) {
            log.error("Info actions cannot be ratified or enacted, since they do not have any effect on the protocol.");
            return RatificationResult.REJECT;
        }

        final int currentEpoch = currentEpochParam.getEpoch();
        final int expiredEpoch = currentEpochParam.getEpoch() + currentEpochParam.getParams().getGovActionLifetime();

        DRepVotingState dRepVotingState;
        SPOVotingState spoVotingState;
        CommitteeVotingState committeeVotingState;
        boolean isAccepted = false;
        boolean isExpired = GovernanceActionUtil.isExpired(expiredEpoch, currentEpoch);
        boolean isNotDelayed = false;

        switch (govActionType) {
            case NO_CONFIDENCE:
                NoConfidence noConfidence = (NoConfidence) govAction;
                dRepVotingState = buildDRepVotingState(govAction, dRepYesVoteStake, dRepNoVoteStake, ccState, currentEpochParam);
                spoVotingState = buildSPOVotingState(govAction, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake, ccState, currentEpochParam);
                isAccepted = dRepVotingState.isAccepted() && spoVotingState.isAccepted();
                isNotDelayed = GovernanceActionUtil.verifyPrevGovAction(govActionType, noConfidence.getGovActionId(), lastEnactedGovActionId);

                break;
            case UPDATE_COMMITTEE:
                //TODO: check if committee term is valid
                UpdateCommittee updateCommittee = (UpdateCommittee) govAction;

                dRepVotingState = buildDRepVotingState(govAction, dRepYesVoteStake, dRepNoVoteStake, ccState, currentEpochParam);
                spoVotingState = buildSPOVotingState(govAction, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake, ccState, currentEpochParam);
                isAccepted = dRepVotingState.isAccepted() && spoVotingState.isAccepted();
                isNotDelayed = GovernanceActionUtil.verifyPrevGovAction(govActionType, updateCommittee.getGovActionId(), lastEnactedGovActionId);

                break;
            case HARD_FORK_INITIATION_ACTION:
                HardForkInitiationAction hardForkInitiationAction = (HardForkInitiationAction) govAction;
                dRepVotingState = buildDRepVotingState(govAction, dRepYesVoteStake, dRepNoVoteStake, ccState, currentEpochParam);
                spoVotingState = buildSPOVotingState(govAction, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake, ccState, currentEpochParam);
                committeeVotingState = buildCommitteeVotingState(govAction, ccYesVote, ccNoVote, ccThreshold);
                isAccepted = committeeVotingState.isAccepted() && dRepVotingState.isAccepted() && spoVotingState.isAccepted();
                isNotDelayed = GovernanceActionUtil.verifyPrevGovAction(govActionType, hardForkInitiationAction.getGovActionId(), lastEnactedGovActionId);

                break;
            case NEW_CONSTITUTION:
                NewConstitution newConstitution = (NewConstitution) govAction;
                dRepVotingState = buildDRepVotingState(govAction, dRepYesVoteStake, dRepNoVoteStake, ccState, currentEpochParam);
                committeeVotingState = buildCommitteeVotingState(govAction, ccYesVote, ccNoVote, ccThreshold);
                isAccepted = committeeVotingState.isAccepted() && dRepVotingState.isAccepted();
                isNotDelayed = GovernanceActionUtil.verifyPrevGovAction(govActionType, newConstitution.getGovActionId(), lastEnactedGovActionId);

                break;
            case TREASURY_WITHDRAWALS_ACTION:
                //TODO: check if withdrawal is possible
                spoVotingState = buildSPOVotingState(govAction, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake, ccState, currentEpochParam);
                committeeVotingState = buildCommitteeVotingState(govAction, ccYesVote, ccNoVote, ccThreshold);
                isAccepted = committeeVotingState.isAccepted() && spoVotingState.isAccepted();
                isNotDelayed = true;

                break;
            case PARAMETER_CHANGE_ACTION:
                ParameterChangeAction parameterChangeAction = (ParameterChangeAction) govAction;
                committeeVotingState = buildCommitteeVotingState(govAction, ccYesVote, ccNoVote, ccThreshold);

                List<ProtocolParamGroup> ppGroupChangeList = ProtocolParamUtil.getGroupsWithNonNullField(parameterChangeAction.getProtocolParamUpdate());
                dRepVotingState = buildDRepVotingState(govAction, dRepYesVoteStake, dRepNoVoteStake, ccState, currentEpochParam);
                isNotDelayed = GovernanceActionUtil.verifyPrevGovAction(govActionType, parameterChangeAction.getGovActionId(), lastEnactedGovActionId);

                if (ppGroupChangeList.contains(ProtocolParamGroup.SECURITY)) {
                    spoVotingState = buildSPOVotingState(govAction, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake, ccState, currentEpochParam);
                    if (ppGroupChangeList.size() == 1) {
                        isAccepted = committeeVotingState.isAccepted() && spoVotingState.isAccepted();
                    } else
                        isAccepted = committeeVotingState.isAccepted() && spoVotingState.isAccepted() && dRepVotingState.isAccepted();
                } else
                    isAccepted = committeeVotingState.isAccepted() && dRepVotingState.isAccepted();
                break;
            default:
                break;
        }

        if (isAccepted && isNotDelayed && !isExpired) {
            return RatificationResult.ACCEPT;
        } else if (!isAccepted && isExpired) {
            return RatificationResult.REJECT;
        }

        return RatificationResult.CONTINUE;
    }

    /**
     * Determines the ratification result for a No Confidence governance action.
     *
     * @param noConfidence             The No Confidence governance action for which ratification result is needed.
     * @param spoYesVoteStake          The total delegated stake from SPO that voted 'Yes'.
     * @param spoAbstainVoteStake      The total delegated stake from SPO that voted 'Abstain'.
     * @param spoTotalStake            The total delegated stake from SPO.
     * @param dRepYesVoteStake         The total stake of:
     *                                 1. Registered dReps that voted 'Yes', plus
     *                                 2. The AlwaysNoConfidence dRep, in case the action is NoConfidence.
     * @param dRepNoVoteStake          The total stake of:
     *                                 1. Registered dReps that voted 'No', plus
     *                                 2. Registered dReps that did not vote for this action, plus
     *                                 3. The AlwaysNoConfidence dRep.
     * @param lastEnactedGovActionId   The last enacted governance action ID of the same purpose.
     * @param currentEpochParam        The current epoch parameters.
     * @return The ratification result for the No Confidence action.
     */
    public static RatificationResult getRatificationResultForNoConfidenceAction(NoConfidence noConfidence, BigInteger spoYesVoteStake,
                                                                                BigInteger spoAbstainVoteStake, BigInteger spoTotalStake,
                                                                                BigInteger dRepYesVoteStake, BigInteger dRepNoVoteStake,
                                                                                GovActionId lastEnactedGovActionId,
                                                                                EpochParam currentEpochParam) {
        return getRatificationResult(noConfidence, null, null, null, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake,
                dRepYesVoteStake, dRepNoVoteStake,
                null, lastEnactedGovActionId, currentEpochParam);
    }

    /**
     * Determines the ratification result for an Update Committee governance action.
     *
     * @param updateCommittee          The Update Committee governance action for which ratification result is needed.
     * @param spoYesVoteStake          The total delegated stake from SPO that voted 'Yes'.
     * @param spoAbstainVoteStake      The total delegated stake from SPO that voted 'Abstain'.
     * @param spoTotalStake            The total delegated stake from SPO.
     * @param dRepYesVoteStake         The total stake of:
     *                                 1. Registered dReps that voted 'Yes', plus
     *                                 2. The AlwaysNoConfidence dRep, in case the action is NoConfidence.
     * @param dRepNoVoteStake          The total stake of:
     *                                 1. Registered dReps that voted 'No', plus
     *                                 2. Registered dReps that did not vote for this action, plus
     *                                 3. The AlwaysNoConfidence dRep.
     * @param ccState                  The current Constitution Committee state.
     * @param lastEnactedGovActionId   The last enacted governance action ID of the same purpose.
     * @param currentEpochParam        The current epoch parameters.
     * @return The ratification result for the Update Committee action.
     */
    public static RatificationResult getRatificationResultForUpdateCommitteeAction(UpdateCommittee updateCommittee, BigInteger spoYesVoteStake,
                                                                                   BigInteger spoAbstainVoteStake, BigInteger spoTotalStake,
                                                                                   BigInteger dRepYesVoteStake, BigInteger dRepNoVoteStake,
                                                                                   ConstitutionCommitteeState ccState, GovActionId lastEnactedGovActionId,
                                                                                   EpochParam currentEpochParam) {
        return getRatificationResult(updateCommittee, null, null, null, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake,
                dRepYesVoteStake, dRepNoVoteStake,
                ccState, lastEnactedGovActionId, currentEpochParam);
    }

    /**
     * Determines the ratification result for a Hard Fork Initiation governance action.
     *
     * @param hardForkInitiationAction The Hard Fork Initiation governance action for which ratification result is needed.
     * @param ccYesVote                The total votes of the Constitution Committee that voted 'Yes':
     *                                      the number of registered, unexpired, unresigned committee members that voted yes
     * @param ccNoVote               The total votes of the Constitution Committee that voted 'No':
     *                                  - the number of registered, unexpired, unresigned committee members that voted no, plus
     *                                  - the number of registered, unexpired, unresigned committee members that did not vote for this action
     * @param ccThreshold              The threshold of the Constitution Committee.
     * @param spoYesVoteStake          The total delegated stake from SPO that voted 'Yes'.
     * @param spoAbstainVoteStake      The total delegated stake from SPO that voted 'Abstain'.
     * @param spoTotalStake            The total delegated stake from SPO.
     * @param dRepYesVoteStake         The total stake of:
     *                                 1. Registered dReps that voted 'Yes', plus
     *                                 2. The AlwaysNoConfidence dRep, in case the action is NoConfidence.
     * @param dRepNoVoteStake          The total stake of:
     *                                 1. Registered dReps that voted 'No', plus
     *                                 2. Registered dReps that did not vote for this action, plus
     *                                 3. The AlwaysNoConfidence dRep.
     * @param lastEnactedGovActionId   The last enacted governance action ID of the same purpose.
     * @param currentEpochParam        The current epoch parameters.
     * @return The ratification result for the Hard Fork Initiation action.
     */
    public static RatificationResult getRatificationResultForHardForkInitiationAction(HardForkInitiationAction hardForkInitiationAction,
                                                                                      Integer ccYesVote, Integer ccNoVote, BigDecimal ccThreshold,
                                                                                      BigInteger spoYesVoteStake, BigInteger spoAbstainVoteStake, BigInteger spoTotalStake,
                                                                                      BigInteger dRepYesVoteStake, BigInteger dRepNoVoteStake,
                                                                                      GovActionId lastEnactedGovActionId,
                                                                                      EpochParam currentEpochParam) {
        return getRatificationResult(hardForkInitiationAction, ccYesVote, ccNoVote, ccThreshold, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake,
                dRepYesVoteStake, dRepNoVoteStake,
                null, lastEnactedGovActionId, currentEpochParam);
    }

    /**
     * Determines the ratification result for a New Constitution governance action.
     *
     * @param newConstitution          The New Constitution governance action for which ratification result is needed.
     * @param ccYesVote                The total votes of the Constitution Committee that voted 'Yes':
     *                                      the number of registered, unexpired, unresigned committee members that voted yes
     * @param ccNoVote                 The total votes of the Constitution Committee that voted 'No':
     *                                  - the number of registered, unexpired, unresigned committee members that voted no, plus
     *                                  - the number of registered, unexpired, unresigned committee members that did not vote for this action
     * @param ccThreshold              The threshold of the Constitution Committee.
     * @param dRepYesVoteStake         The total stake of registered dReps that voted 'Yes'.
     * @param dRepNoVoteStake          The total stake of:
     *                                 1. Registered dReps that voted 'No', plus
     *                                 2. Registered dReps that did not vote for this action, plus
     *                                 3. The AlwaysNoConfidence dRep.
     * @param lastEnactedGovActionId   The last enacted governance action ID of the same purpose.
     * @param currentEpochParam        The current epoch parameters.
     * @return The ratification result for the New Constitution action.
     */
    public static RatificationResult getRatificationResultForNewConstitutionAction(NewConstitution newConstitution,
                                                                                   Integer ccYesVote, Integer ccNoVote, BigDecimal ccThreshold,
                                                                                   BigInteger dRepYesVoteStake, BigInteger dRepNoVoteStake,
                                                                                   GovActionId lastEnactedGovActionId,
                                                                                   EpochParam currentEpochParam) {
        return getRatificationResult(newConstitution, ccYesVote, ccNoVote,  ccThreshold, null, null, null,
                dRepYesVoteStake, dRepNoVoteStake,
                null, lastEnactedGovActionId, currentEpochParam);
    }

    /**
     * Determines the ratification result for a Treasury Withdrawals governance action.
     *
     * @param treasuryWithdrawalsAction The Treasury Withdrawals governance action for which ratification result is needed.
     * @param ccYesVote                The total votes of the Constitution Committee that voted 'Yes':
     *                                      the number of registered, unexpired, unresigned committee members that voted yes
     * @param ccNoVote                 The total votes of the Constitution Committee that voted 'No':
     *                                  - the number of registered, unexpired, unresigned committee members that voted no, plus
     *                                  - the number of registered, unexpired, unresigned committee members that did not vote for this action
     * @param ccThreshold              The threshold of the Constitution Committee.
     * @param spoYesVoteStake          The total delegated stake from SPO that voted 'Yes'.
     * @param spoAbstainVoteStake      The total delegated stake from SPO that voted 'Abstain'.
     * @param spoTotalStake            The total delegated stake from SPO.
     * @param lastEnactedGovActionId   The last enacted governance action ID of the same purpose.
     * @param currentEpochParam        The current epoch parameters.
     * @return The ratification result for the Treasury Withdrawals action.
     */
    public static RatificationResult getRatificationResultForTreasuryWithdrawalsAction(TreasuryWithdrawalsAction treasuryWithdrawalsAction,
                                                                                       Integer ccYesVote, Integer ccNoVote, BigDecimal ccThreshold,
                                                                                       BigInteger spoYesVoteStake, BigInteger spoAbstainVoteStake, BigInteger spoTotalStake,
                                                                                       GovActionId lastEnactedGovActionId,
                                                                                       EpochParam currentEpochParam) {
        return getRatificationResult(treasuryWithdrawalsAction, ccYesVote, ccNoVote, ccThreshold, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake,
                null, null,
                null, lastEnactedGovActionId, currentEpochParam);
    }

    /**
     * Determines the ratification result for a Protocol Parameters Change governance action.
     *
     * @param parameterChangeAction    The Parameter Change governance action for which ratification result is needed.
     * @param ccYesVote                The total votes of the Constitution Committee that voted 'Yes':
     *                                      the number of registered, unexpired, unresigned committee members that voted yes
     * @param ccNoVote               The total votes of the Constitution Committee that voted 'No':
     *                                  - the number of registered, unexpired, unresigned committee members that voted no, plus
     *                                  - the number of registered, unexpired, unresigned committee members that did not vote for this action
     * @param ccThreshold              The threshold of the Constitution Committee.
     * @param spoYesVoteStake          The total delegated stake from SPO that voted 'Yes'.
     * @param spoAbstainVoteStake      The total delegated stake from SPO that voted 'Abstain'.
     * @param spoTotalStake            The total delegated stake from SPO.
     * @param dRepYesVoteStake         The total stake of:
     *                                 1. Registered dReps that voted 'Yes', plus
     *                                 2. The AlwaysNoConfidence dRep, in case the action is NoConfidence.
     * @param dRepNoVoteStake          The total stake of:
     *                                 1. Registered dReps that voted 'No', plus
     *                                 2. Registered dReps that did not vote for this action, plus
     *                                 3. The AlwaysNoConfidence dRep.
     * @param lastEnactedGovActionId   The last enacted governance action ID of the same purpose.
     * @param currentEpochParam        The current epoch parameters.
     * @return The ratification result for the Parameter Change action.
     */
    public static RatificationResult getRatificationResultForParameterChangeAction(ParameterChangeAction parameterChangeAction,
                                                                                   Integer ccYesVote, Integer ccNoVote, BigDecimal ccThreshold,
                                                                                   BigInteger spoYesVoteStake, BigInteger spoAbstainVoteStake, BigInteger spoTotalStake,
                                                                                   BigInteger dRepYesVoteStake, BigInteger dRepNoVoteStake,
                                                                                   GovActionId lastEnactedGovActionId,
                                                                                   EpochParam currentEpochParam) {
        return getRatificationResult(parameterChangeAction, ccYesVote, ccNoVote, ccThreshold, spoYesVoteStake, spoAbstainVoteStake, spoTotalStake,
                dRepYesVoteStake, dRepNoVoteStake,
                null, lastEnactedGovActionId, currentEpochParam);
    }

    private static DRepVotingState buildDRepVotingState(GovAction govAction, BigInteger dRepYesVoteStake, BigInteger dRepNoVoteStake,
                                                        ConstitutionCommitteeState ccState,
                                                        EpochParam currentEpochParam) {

        return DRepVotingState.builder()
                .govAction(govAction)
                .dRepVotingThresholds(currentEpochParam.getParams().getDrepVotingThresholds())
                .yesVoteStake(dRepYesVoteStake)
                .noVoteStake(dRepNoVoteStake)
                .ccState(ccState)
                .build();
    }

    private static SPOVotingState buildSPOVotingState(GovAction govAction, BigInteger spoYesVoteStake, BigInteger spoAbstainVoteStake, BigInteger spoTotalStake,
                                                      ConstitutionCommitteeState ccState, EpochParam currentEpochParam) {
        return SPOVotingState.builder()
                .govAction(govAction)
                .poolVotingThresholds(currentEpochParam.getParams().getPoolVotingThresholds())
                .yesVoteStake(spoYesVoteStake)
                .abstainVoteStake(spoAbstainVoteStake)
                .totalStake(spoTotalStake)
                .ccState(ccState)
                .build();
    }

    private static CommitteeVotingState buildCommitteeVotingState(GovAction govAction,
                                                                  Integer ccYesVote, Integer ccNoVote,
                                                                  BigDecimal threshold) {
        return CommitteeVotingState.builder()
                .govAction(govAction)
                .yesVote(ccYesVote)
                .noVote(ccNoVote)
                .threshold(threshold)
                .build();
    }

}
