package com.bloxbean.cardano.yaci.store.governancerules.voting;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class VoteTallyCalculator {

    private VoteTallyCalculator() {}

    public static VoteTallies.DRepTallies computeDRepTallies(VotingData.DRepVotes drep, GovActionType type) {
        if (drep == null) {
            return VoteTallies.DRepTallies.builder()
                    .totalYesStake(BigInteger.ZERO)
                    .totalNoStake(BigInteger.ZERO)
                    .build();
        }

        BigInteger yes = nz(drep.getYesVoteStake());
        BigInteger no = nz(drep.getNoVoteStake());
        BigInteger noConfidence = nz(drep.getNoConfidenceStake());
        BigInteger notVoted = nz(drep.getDoNotVoteStake());

        /*
            Total DRep yes stake – The total stake of:
            1. Registered dReps that voted 'Yes', plus
            2. The AlwaysNoConfidence dRep, in case the action is NoConfidence.
         */

        if (type == GovActionType.NO_CONFIDENCE) {
            yes = yes.add(noConfidence);
        }

        /*
            DRep No Stake – The total stake of:
            1. Registered dReps that voted 'No', plus
            2. Registered dReps that did not vote for this action, plus
            3. The AlwaysNoConfidence dRep.
        */
        BigInteger noStake = no.add(notVoted).add(noConfidence);

        return VoteTallies.DRepTallies.builder()
                .totalYesStake(yes)
                .totalNoStake(noStake)
                .build();
    }

    public static VoteTallies.SPOTallies computeSPOTallies(VotingData.SPOVotes spo, GovActionType type, boolean isInBootstrapPhase) {
        if (spo == null) {
            return VoteTallies.SPOTallies.builder()
                    .totalYesStake(BigInteger.ZERO)
                    .totalAbstainStake(BigInteger.ZERO)
                    .totalNoStake(BigInteger.ZERO)
                    .build();
        }

        BigInteger yesVote = nz(spo.getYesVoteStake());
        BigInteger abstainVote = nz(spo.getAbstainVoteStake());
        BigInteger total = nz(spo.getTotalStake());
        BigInteger delegateAutoAbstainDRep = nz(spo.getDelegateToAutoAbstainDRepStake());
        BigInteger delegateToNoConfidenceDRep = nz(spo.getDelegateToNoConfidenceDRepStake());
        BigInteger doNotVote = nz(spo.getDoNotVoteStake());

        BigInteger totalYesStake = yesVote;
        if (type == GovActionType.NO_CONFIDENCE) {
            totalYesStake = totalYesStake.add(delegateToNoConfidenceDRep);
        }

        BigInteger totalAbstainStake = abstainVote.add(delegateAutoAbstainDRep);

        // In bootstrap phase, all do not vote stake is considered as abstain stake except for HardForkInitiationAction
        if (isInBootstrapPhase && type != GovActionType.HARD_FORK_INITIATION_ACTION) {
            totalAbstainStake = totalAbstainStake.add(doNotVote);
        }

        BigInteger totalNoStake = total.subtract(totalYesStake).subtract(totalAbstainStake);
        if (totalNoStake.signum() < 0) totalNoStake = BigInteger.ZERO;

        return VoteTallies.SPOTallies.builder()
                .totalYesStake(totalYesStake)
                .totalAbstainStake(totalAbstainStake)
                .totalNoStake(totalNoStake)
                .build();
    }

    public static VoteTallies.CommitteeTallies computeCommitteeTallies(Map<String, Vote> votesByHotKey, List<CommitteeMember> members) {
        int yes, no, abstain, didNotVote;

        // calculate cc yes vote

        // Many CC Cold Credentials map to the same Hot Credential act as many votes.
        Map<String, List<String>> hotKeyColdKeysMap = members.stream()
                .collect(Collectors.groupingBy(
                        CommitteeMember::getHotKey,
                        Collectors.mapping(CommitteeMember::getColdKey, Collectors.toList())
                ));
        yes = votesByHotKey.entrySet().stream()
                .filter(e -> e.getValue() == Vote.YES && hotKeyColdKeysMap.containsKey(e.getKey()))
                .mapToInt(e -> hotKeyColdKeysMap.get(e.getKey()).size())
                .sum();

        no = votesByHotKey.entrySet().stream()
                .filter(e -> e.getValue() == Vote.NO && hotKeyColdKeysMap.containsKey(e.getKey()))
                .mapToInt(e -> hotKeyColdKeysMap.get(e.getKey()).size())
                .sum();

        abstain = votesByHotKey.entrySet().stream()
                .filter(e -> e.getValue() == Vote.ABSTAIN && hotKeyColdKeysMap.containsKey(e.getKey()))
                .mapToInt(e -> hotKeyColdKeysMap.get(e.getKey()).size())
                .sum();

        Map<String, String> coldKeyHotKeyMap = members.stream()
                .collect(Collectors.toMap(
                        CommitteeMember::getColdKey,
                        CommitteeMember::getHotKey,
                        (v1, v2) -> v1
                ));

        didNotVote = (int) members.stream()
                .filter(member -> {
                    String hotKey = coldKeyHotKeyMap.get(member.getColdKey());
                    return hotKey == null || !votesByHotKey.containsKey(hotKey);
                })
                .count();

        return VoteTallies.CommitteeTallies.builder()
                .yesCount(yes)
                .noCount(no)
                .abstainCount(abstain)
                .doNotVoteCount(didNotVote)
                .build();
    }

    private static BigInteger nz(BigInteger v) {
        return Objects.requireNonNullElse(v, BigInteger.ZERO);
    }
}
