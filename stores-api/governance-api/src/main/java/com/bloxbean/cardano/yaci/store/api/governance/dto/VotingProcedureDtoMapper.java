package com.bloxbean.cardano.yaci.store.api.governance.dto;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
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
                .anchorHash(votingProcedure.getAnchorHash())
                .anchorHash(votingProcedure.getAnchorUrl())
                .govActionTxHash(votingProcedure.getGovActionTxHash())
                .govActionIndex(votingProcedure.getGovActionIndex())
                .index(votingProcedure.getIndex())
                .blockNumber(votingProcedure.getBlockNumber())
                .blockTime(votingProcedure.getBlockTime())
                .build();

        String dRepId;
        if (votingProcedure.getVoterType() == VoterType.DREP_KEY_HASH) {
            //Todo: use ccl instead when new version of ccl is released
            dRepId = Bech32.encode(HexUtil.decodeHexString(votingProcedure.getVoterHash()), "drep");
            votingProcedureDto.setDRepId(dRepId);
        } else if (votingProcedure.getVoterType() == VoterType.DREP_SCRIPT_HASH) {
            //Todo: check this case
        }

        return votingProcedureDto;
    }
}
