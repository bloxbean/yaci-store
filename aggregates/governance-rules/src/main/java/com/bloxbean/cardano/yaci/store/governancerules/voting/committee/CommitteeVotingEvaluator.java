package com.bloxbean.cardano.yaci.store.governancerules.voting.committee;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommitteeVotingEvaluator implements VotingEvaluator<VotingData> {
    
    @Override
    public VotingResult evaluate(VotingData votingData, VotingEvaluationContext context) {
        var committee = context.getCommittee();
        var votes = votingData.getCommitteeVotes();
        
        if (committee == null || votes == null) {
            return VotingResult.INSUFFICIENT_DATA;
        }
        
        var threshold = committee.getThreshold().safeRatio();
        if (threshold.equals(BigDecimal.ZERO)) {
            return VotingResult.PASSED_THRESHOLD;
        }
        
        int yesVotes = calculateYesVotes(committee.getMembers(), votes.getVotes());
        int noVotes = calculateNoVotes(committee.getMembers(), votes.getVotes());

        int totalVotes = yesVotes + noVotes;
        
        if (totalVotes == 0) {
            return VotingResult.NOT_PASSED_THRESHOLD;
        }
        
        BigDecimal yesRatio = BigDecimal.valueOf(yesVotes)
            .divide(BigDecimal.valueOf(totalVotes), 2, BigDecimal.ROUND_HALF_UP);
            
        return yesRatio.compareTo(threshold) >= 0 ? 
            VotingResult.PASSED_THRESHOLD : VotingResult.NOT_PASSED_THRESHOLD;
    }
    
    private int calculateYesVotes(List<CommitteeMember> members, Map<String, Vote> votes) {
        // calculate cc yes vote

        // Many CC Cold Credentials map to the same Hot Credential act as many votes.
        // If the hot credential is compromised at any point, the committee member must generate a new one and issue a new Authorization Certificate.
        // A new Authorization Certificate registered on-chain overrides the previous one, effectively invalidating any votes signed by the old hot credential.

        Map<String, List<String>> hotKeyColdKeysMap = members.stream()
            .collect(Collectors.groupingBy(
                CommitteeMember::getHotKey,
                Collectors.mapping(CommitteeMember::getColdKey, Collectors.toList())
            ));
            
        return (int) votes.entrySet().stream()
            .filter(e -> e.getValue() == Vote.YES && hotKeyColdKeysMap.containsKey(e.getKey()))
            .count();
    }
    
    private int calculateNoVotes(List<CommitteeMember> members, Map<String, Vote> votes) {
        /*
            ccNoVote – The total number of committee members that voted 'No' plus the number of committee members that did not vote.
        */
        Map<String, List<String>> hotKeyColdKeysMap = members.stream()
            .collect(Collectors.groupingBy(
                CommitteeMember::getHotKey,
                Collectors.mapping(CommitteeMember::getColdKey, Collectors.toList())
            ));
            
        Map<String, String> coldKeyHotKeyMap = members.stream()
            .collect(Collectors.toMap(
                CommitteeMember::getColdKey, 
                CommitteeMember::getHotKey, 
                (v1, v2) -> v1
            ));
        
        int noVotes = (int) votes.entrySet().stream()
            .filter(e -> e.getValue() == Vote.NO && hotKeyColdKeysMap.containsKey(e.getKey()))
            .count();
            
        int didNotVote = (int) members.stream()
            .filter(member -> {
                String hotKey = coldKeyHotKeyMap.get(member.getColdKey());
                return hotKey == null || !votes.containsKey(hotKey);
            })
            .count();
            
        return noVotes + didNotVote;
    }
}