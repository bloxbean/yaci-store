package com.bloxbean.cardano.yaci.store.api.governance.dto;

import com.bloxbean.cardano.client.governance.DRepId;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import org.springframework.stereotype.Component;

@Component
public class VotingProcedureDtoMapper {
    public VotingProcedureDto toVotingProcedureDto(VotingProcedure votingProcedure) {

        VotingProcedureDto votingProcedureDto = VotingProcedureDto.builder()
                .id(votingProcedure.getId())
                .txHash(votingProcedure.getTxHash())
                .epoch(votingProcedure.getEpoch())
                .slot(votingProcedure.getSlot())
                .vote(votingProcedure.getVote())
                .voterType(votingProcedure.getVoterType())
                .voterHash(votingProcedure.getVoterHash())
                .anchorUrl(votingProcedure.getAnchorUrl())
                .anchorHash(votingProcedure.getAnchorHash())
                .govActionTxHash(votingProcedure.getGovActionTxHash())
                .govActionIndex(votingProcedure.getGovActionIndex())
                .index(votingProcedure.getIndex())
                .blockNumber(votingProcedure.getBlockNumber())
                .blockTime(votingProcedure.getBlockTime())
                .build();

        String dRepId;
        if (votingProcedure.getVoterType() == VoterType.DREP_KEY_HASH) {
            dRepId = DRepId.fromKeyHash(votingProcedure.getVoterHash());
            votingProcedureDto.setDRepId(dRepId);
        } else if (votingProcedure.getVoterType() == VoterType.DREP_SCRIPT_HASH) {
            dRepId = DRepId.fromScriptHash(votingProcedure.getVoterHash());
            votingProcedureDto.setDRepId(dRepId);
        }

        return votingProcedureDto;
    }
}
