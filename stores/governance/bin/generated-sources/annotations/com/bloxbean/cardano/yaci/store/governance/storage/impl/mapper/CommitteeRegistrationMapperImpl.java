package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeRegistrationEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CommitteeRegistrationMapperImpl extends CommitteeRegistrationMapper {

    @Override
    public CommitteeRegistrationEntity toCommitteeRegistrationEntity(CommitteeRegistration committeeRegistration) {
        if ( committeeRegistration == null ) {
            return null;
        }

        CommitteeRegistrationEntity.CommitteeRegistrationEntityBuilder<?, ?> committeeRegistrationEntity = CommitteeRegistrationEntity.builder();

        committeeRegistrationEntity.blockNumber( committeeRegistration.getBlockNumber() );
        committeeRegistrationEntity.blockTime( committeeRegistration.getBlockTime() );
        committeeRegistrationEntity.certIndex( committeeRegistration.getCertIndex() );
        committeeRegistrationEntity.coldKey( committeeRegistration.getColdKey() );
        committeeRegistrationEntity.credType( committeeRegistration.getCredType() );
        committeeRegistrationEntity.epoch( committeeRegistration.getEpoch() );
        committeeRegistrationEntity.hotKey( committeeRegistration.getHotKey() );
        committeeRegistrationEntity.slot( committeeRegistration.getSlot() );
        committeeRegistrationEntity.txHash( committeeRegistration.getTxHash() );
        committeeRegistrationEntity.txIndex( committeeRegistration.getTxIndex() );

        return committeeRegistrationEntity.build();
    }

    @Override
    public CommitteeRegistration toCommitteeRegistration(CommitteeRegistrationEntity committeeRegistrationEntity) {
        if ( committeeRegistrationEntity == null ) {
            return null;
        }

        CommitteeRegistration.CommitteeRegistrationBuilder<?, ?> committeeRegistration = CommitteeRegistration.builder();

        committeeRegistration.blockNumber( committeeRegistrationEntity.getBlockNumber() );
        committeeRegistration.blockTime( committeeRegistrationEntity.getBlockTime() );
        committeeRegistration.certIndex( committeeRegistrationEntity.getCertIndex() );
        committeeRegistration.coldKey( committeeRegistrationEntity.getColdKey() );
        committeeRegistration.credType( committeeRegistrationEntity.getCredType() );
        committeeRegistration.epoch( committeeRegistrationEntity.getEpoch() );
        committeeRegistration.hotKey( committeeRegistrationEntity.getHotKey() );
        committeeRegistration.slot( committeeRegistrationEntity.getSlot() );
        committeeRegistration.txHash( committeeRegistrationEntity.getTxHash() );
        committeeRegistration.txIndex( committeeRegistrationEntity.getTxIndex() );

        return committeeRegistration.build();
    }
}
