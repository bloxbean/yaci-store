package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:28+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class GovActionProposalStatusMapperImpl implements GovActionProposalStatusMapper {

    @Override
    public GovActionProposalStatusEntity toGovActionProposalStatusEntity(GovActionProposalStatus govActionProposalStatus) {
        if ( govActionProposalStatus == null ) {
            return null;
        }

        GovActionProposalStatusEntity.GovActionProposalStatusEntityBuilder govActionProposalStatusEntity = GovActionProposalStatusEntity.builder();

        govActionProposalStatusEntity.epoch( govActionProposalStatus.getEpoch() );
        govActionProposalStatusEntity.govActionIndex( govActionProposalStatus.getGovActionIndex() );
        govActionProposalStatusEntity.govActionTxHash( govActionProposalStatus.getGovActionTxHash() );
        govActionProposalStatusEntity.status( govActionProposalStatus.getStatus() );
        govActionProposalStatusEntity.type( govActionProposalStatus.getType() );
        govActionProposalStatusEntity.votingStats( govActionProposalStatus.getVotingStats() );

        return govActionProposalStatusEntity.build();
    }

    @Override
    public GovActionProposalStatus toGovActionProposalStatus(GovActionProposalStatusEntity govActionProposalStatusEntity) {
        if ( govActionProposalStatusEntity == null ) {
            return null;
        }

        GovActionProposalStatus.GovActionProposalStatusBuilder govActionProposalStatus = GovActionProposalStatus.builder();

        govActionProposalStatus.epoch( govActionProposalStatusEntity.getEpoch() );
        govActionProposalStatus.govActionIndex( govActionProposalStatusEntity.getGovActionIndex() );
        govActionProposalStatus.govActionTxHash( govActionProposalStatusEntity.getGovActionTxHash() );
        govActionProposalStatus.status( govActionProposalStatusEntity.getStatus() );
        govActionProposalStatus.type( govActionProposalStatusEntity.getType() );
        govActionProposalStatus.votingStats( govActionProposalStatusEntity.getVotingStats() );

        return govActionProposalStatus.build();
    }
}
