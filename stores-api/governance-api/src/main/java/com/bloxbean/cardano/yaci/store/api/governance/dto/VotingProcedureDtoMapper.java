package com.bloxbean.cardano.yaci.store.api.governance.dto;

import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
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
        if (votingProcedure.getVoterHash() != null) {
            if (votingProcedure.getVoterType() == VoterType.DREP_KEY_HASH) {
                dRepId = GovId.drepFromKeyHash(HexUtil.decodeHexString(votingProcedure.getVoterHash()));
                votingProcedureDto.setDRepId(dRepId);
            } else if (votingProcedure.getVoterType() == VoterType.DREP_SCRIPT_HASH) {
                dRepId = GovId.drepFromScriptHash(HexUtil.decodeHexString(votingProcedure.getVoterHash()));
                votingProcedureDto.setDRepId(dRepId);
            }
        } else
            log.warn("Voter hash is null for voting procedure with id: {}", votingProcedure.getId());

        return votingProcedureDto;
    }
}
