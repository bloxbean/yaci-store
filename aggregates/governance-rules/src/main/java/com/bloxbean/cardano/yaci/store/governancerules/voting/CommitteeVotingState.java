package com.bloxbean.cardano.yaci.store.governancerules.voting;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class CommitteeVotingState extends VotingState {
    private ConstitutionCommittee committee;
    // (hot key, vote)
    private Map<String, Vote> votes;

    @Override
    public boolean isAccepted() {
        UnitInterval threshold = committee.getThreshold();

        int yesVote = getTotalYesVotes();
        int noVote = getTotalNoVotes();

        if (threshold.safeRatio().equals(BigDecimal.ZERO)) {
            return true;
        }

        int totalVotes = yesVote + noVote;
        if (totalVotes == 0) {
            return false;
        }
        BigDecimal yesVoteRatio = BigDecimal.valueOf(yesVote).divide(BigDecimal.valueOf(totalVotes), 2, BigDecimal.ROUND_HALF_UP);

        return yesVoteRatio.compareTo(threshold.safeRatio()) >= 0;
    }

    public int getTotalYesVotes() {
        // calculate cc yes vote

        // Many CC Cold Credentials map to the same Hot Credential act as many votes.
        // If the hot credential is compromised at any point, the committee member must generate a new one and issue a new Authorization Certificate.
        // A new Authorization Certificate registered on-chain overrides the previous one, effectively invalidating any votes signed by the old hot credential.
        Map<String, List<String>> hotKeyColdKeysMap = committee.getMembers().stream()
                .collect(Collectors.groupingBy(CommitteeMember::getHotKey,
                        Collectors.mapping(CommitteeMember::getColdKey, Collectors.toList())));

        return (int) votes.entrySet().stream()
                .filter(e -> e.getValue() == Vote.YES && hotKeyColdKeysMap.containsKey(e.getKey()))
                .count();
    }

    public int getTotalNoVotes() {
        /*
            ccNoVote – The total number of committee members that voted 'No' plus the number of committee members that did not vote.
        */

        // map (hot key, cold key list)
        Map<String, List<String>> hotKeyColdKeysMap = committee.getMembers().stream()
                .collect(Collectors.groupingBy(CommitteeMember::getHotKey,
                        Collectors.mapping(CommitteeMember::getColdKey, Collectors.toList())));

        // map (cold key, hot key)
        Map<String, String> coldKeyHotKeyMap = committee.getMembers().stream()
                .collect(Collectors.toMap(CommitteeMember::getColdKey, CommitteeMember::getHotKey, (v1, v2) -> v1));

        int ccDoNotVote = (int)committee.getMembers().stream()
                .filter(committeeMember ->
                        coldKeyHotKeyMap.get(committeeMember.getColdKey()) == null ||
                                votes.keySet().stream().noneMatch(
                                        voterHash ->
                                                coldKeyHotKeyMap.get(committeeMember.getColdKey())
                                                        .equals(voterHash)))
                .count();

        return (int) votes.entrySet().stream()
                .filter(e -> e.getValue() == Vote.NO && hotKeyColdKeysMap.containsKey(e.getKey()))
                .count()
                + ccDoNotVote;
    }
}