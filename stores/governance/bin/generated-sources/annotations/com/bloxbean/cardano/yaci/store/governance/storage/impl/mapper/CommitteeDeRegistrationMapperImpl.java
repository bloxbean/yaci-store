package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeDeRegistrationEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CommitteeDeRegistrationMapperImpl extends CommitteeDeRegistrationMapper {

    @Override
    public CommitteeDeRegistrationEntity toCommitteeDeRegistrationEntity(CommitteeDeRegistration committeeDeRegistration) {
        if ( committeeDeRegistration == null ) {
            return null;
        }

        CommitteeDeRegistrationEntity.CommitteeDeRegistrationEntityBuilder<?, ?> committeeDeRegistrationEntity = CommitteeDeRegistrationEntity.builder();

        committeeDeRegistrationEntity.blockNumber( committeeDeRegistration.getBlockNumber() );
        committeeDeRegistrationEntity.blockTime( committeeDeRegistration.getBlockTime() );
        committeeDeRegistrationEntity.anchorHash( committeeDeRegistration.getAnchorHash() );
        committeeDeRegistrationEntity.anchorUrl( committeeDeRegistration.getAnchorUrl() );
        committeeDeRegistrationEntity.certIndex( committeeDeRegistration.getCertIndex() );
        committeeDeRegistrationEntity.coldKey( committeeDeRegistration.getColdKey() );
        committeeDeRegistrationEntity.credType( committeeDeRegistration.getCredType() );
        committeeDeRegistrationEntity.epoch( committeeDeRegistration.getEpoch() );
        committeeDeRegistrationEntity.slot( committeeDeRegistration.getSlot() );
        committeeDeRegistrationEntity.txHash( committeeDeRegistration.getTxHash() );
        committeeDeRegistrationEntity.txIndex( committeeDeRegistration.getTxIndex() );

        return committeeDeRegistrationEntity.build();
    }

    @Override
    public CommitteeDeRegistration toCommitteeDeRegistration(CommitteeDeRegistrationEntity committeeDeRegistrationEntity) {
        if ( committeeDeRegistrationEntity == null ) {
            return null;
        }

        CommitteeDeRegistration.CommitteeDeRegistrationBuilder<?, ?> committeeDeRegistration = CommitteeDeRegistration.builder();

        committeeDeRegistration.blockNumber( committeeDeRegistrationEntity.getBlockNumber() );
        committeeDeRegistration.blockTime( committeeDeRegistrationEntity.getBlockTime() );
        committeeDeRegistration.anchorHash( committeeDeRegistrationEntity.getAnchorHash() );
        committeeDeRegistration.anchorUrl( committeeDeRegistrationEntity.getAnchorUrl() );
        committeeDeRegistration.certIndex( committeeDeRegistrationEntity.getCertIndex() );
        committeeDeRegistration.coldKey( committeeDeRegistrationEntity.getColdKey() );
        committeeDeRegistration.credType( committeeDeRegistrationEntity.getCredType() );
        committeeDeRegistration.epoch( committeeDeRegistrationEntity.getEpoch() );
        committeeDeRegistration.slot( committeeDeRegistrationEntity.getSlot() );
        committeeDeRegistration.txHash( committeeDeRegistrationEntity.getTxHash() );
        committeeDeRegistration.txIndex( committeeDeRegistrationEntity.getTxIndex() );

        return committeeDeRegistration.build();
    }
}
