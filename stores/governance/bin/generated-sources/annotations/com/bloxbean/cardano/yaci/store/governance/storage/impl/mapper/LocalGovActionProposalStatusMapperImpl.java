package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalGovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalGovActionProposalStatusEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:19+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class LocalGovActionProposalStatusMapperImpl extends LocalGovActionProposalStatusMapper {

    @Override
    public LocalGovActionProposalStatusEntity toLocalGovActionProposalStatusEntity(LocalGovActionProposalStatus localGovActionProposalStatus) {
        if ( localGovActionProposalStatus == null ) {
            return null;
        }

        LocalGovActionProposalStatusEntity.LocalGovActionProposalStatusEntityBuilder localGovActionProposalStatusEntity = LocalGovActionProposalStatusEntity.builder();

        localGovActionProposalStatusEntity.epoch( localGovActionProposalStatus.getEpoch() );
        localGovActionProposalStatusEntity.govActionIndex( localGovActionProposalStatus.getGovActionIndex() );
        localGovActionProposalStatusEntity.govActionTxHash( localGovActionProposalStatus.getGovActionTxHash() );
        localGovActionProposalStatusEntity.slot( localGovActionProposalStatus.getSlot() );
        localGovActionProposalStatusEntity.status( localGovActionProposalStatus.getStatus() );

        return localGovActionProposalStatusEntity.build();
    }

    @Override
    public LocalGovActionProposalStatus toLocalGovActionProposalStatus(LocalGovActionProposalStatusEntity localGovActionProposalStatusEntity) {
        if ( localGovActionProposalStatusEntity == null ) {
            return null;
        }

        LocalGovActionProposalStatus.LocalGovActionProposalStatusBuilder localGovActionProposalStatus = LocalGovActionProposalStatus.builder();

        localGovActionProposalStatus.epoch( localGovActionProposalStatusEntity.getEpoch() );
        localGovActionProposalStatus.govActionIndex( localGovActionProposalStatusEntity.getGovActionIndex() );
        localGovActionProposalStatus.govActionTxHash( localGovActionProposalStatusEntity.getGovActionTxHash() );
        localGovActionProposalStatus.slot( localGovActionProposalStatusEntity.getSlot() );
        localGovActionProposalStatus.status( localGovActionProposalStatusEntity.getStatus() );

        return localGovActionProposalStatus.build();
    }
}
