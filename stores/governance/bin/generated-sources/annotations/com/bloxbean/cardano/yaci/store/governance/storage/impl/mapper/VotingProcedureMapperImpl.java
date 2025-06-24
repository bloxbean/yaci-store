package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class VotingProcedureMapperImpl extends VotingProcedureMapper {

    @Override
    public VotingProcedureEntity toVotingProcedureEntity(VotingProcedure votingProcedure) {
        if ( votingProcedure == null ) {
            return null;
        }

        VotingProcedureEntity.VotingProcedureEntityBuilder<?, ?> votingProcedureEntity = VotingProcedureEntity.builder();

        votingProcedureEntity.blockNumber( votingProcedure.getBlockNumber() );
        votingProcedureEntity.blockTime( votingProcedure.getBlockTime() );
        votingProcedureEntity.anchorHash( votingProcedure.getAnchorHash() );
        votingProcedureEntity.anchorUrl( votingProcedure.getAnchorUrl() );
        votingProcedureEntity.epoch( votingProcedure.getEpoch() );
        votingProcedureEntity.govActionIndex( votingProcedure.getGovActionIndex() );
        votingProcedureEntity.govActionTxHash( votingProcedure.getGovActionTxHash() );
        votingProcedureEntity.id( votingProcedure.getId() );
        votingProcedureEntity.index( votingProcedure.getIndex() );
        votingProcedureEntity.slot( votingProcedure.getSlot() );
        votingProcedureEntity.txHash( votingProcedure.getTxHash() );
        votingProcedureEntity.vote( votingProcedure.getVote() );
        votingProcedureEntity.voterHash( votingProcedure.getVoterHash() );
        votingProcedureEntity.voterType( votingProcedure.getVoterType() );

        return votingProcedureEntity.build();
    }

    @Override
    public VotingProcedure toVotingProcedure(VotingProcedureEntity votingProcedureEntity) {
        if ( votingProcedureEntity == null ) {
            return null;
        }

        VotingProcedure.VotingProcedureBuilder<?, ?> votingProcedure = VotingProcedure.builder();

        votingProcedure.blockNumber( votingProcedureEntity.getBlockNumber() );
        votingProcedure.blockTime( votingProcedureEntity.getBlockTime() );
        votingProcedure.anchorHash( votingProcedureEntity.getAnchorHash() );
        votingProcedure.anchorUrl( votingProcedureEntity.getAnchorUrl() );
        votingProcedure.epoch( votingProcedureEntity.getEpoch() );
        votingProcedure.govActionIndex( votingProcedureEntity.getGovActionIndex() );
        votingProcedure.govActionTxHash( votingProcedureEntity.getGovActionTxHash() );
        votingProcedure.id( votingProcedureEntity.getId() );
        votingProcedure.index( votingProcedureEntity.getIndex() );
        votingProcedure.slot( votingProcedureEntity.getSlot() );
        votingProcedure.txHash( votingProcedureEntity.getTxHash() );
        votingProcedure.vote( votingProcedureEntity.getVote() );
        votingProcedure.voterHash( votingProcedureEntity.getVoterHash() );
        votingProcedure.voterType( votingProcedureEntity.getVoterType() );

        return votingProcedure.build();
    }
}
