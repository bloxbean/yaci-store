package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalHardForkInitiation;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalHardForkInitiationEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:19+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class LocalHardForkInitiationMapperImpl extends LocalHardForkInitiationMapper {

    @Override
    public LocalHardForkInitiationEntity toLocalHardForkInitiationEntity(LocalHardForkInitiation localHardForkInitiation) {
        if ( localHardForkInitiation == null ) {
            return null;
        }

        LocalHardForkInitiationEntity.LocalHardForkInitiationEntityBuilder localHardForkInitiationEntity = LocalHardForkInitiationEntity.builder();

        localHardForkInitiationEntity.epoch( localHardForkInitiation.getEpoch() );
        localHardForkInitiationEntity.govActionIndex( localHardForkInitiation.getGovActionIndex() );
        localHardForkInitiationEntity.govActionTxHash( localHardForkInitiation.getGovActionTxHash() );
        localHardForkInitiationEntity.majorVersion( localHardForkInitiation.getMajorVersion() );
        localHardForkInitiationEntity.minorVersion( localHardForkInitiation.getMinorVersion() );
        localHardForkInitiationEntity.slot( localHardForkInitiation.getSlot() );

        return localHardForkInitiationEntity.build();
    }

    @Override
    public LocalHardForkInitiation toLocalHardForkInitiation(LocalHardForkInitiationEntity localHardForkInitiationEntity) {
        if ( localHardForkInitiationEntity == null ) {
            return null;
        }

        LocalHardForkInitiation.LocalHardForkInitiationBuilder localHardForkInitiation = LocalHardForkInitiation.builder();

        if ( localHardForkInitiationEntity.getEpoch() != null ) {
            localHardForkInitiation.epoch( localHardForkInitiationEntity.getEpoch() );
        }
        localHardForkInitiation.govActionIndex( localHardForkInitiationEntity.getGovActionIndex() );
        localHardForkInitiation.govActionTxHash( localHardForkInitiationEntity.getGovActionTxHash() );
        localHardForkInitiation.majorVersion( localHardForkInitiationEntity.getMajorVersion() );
        localHardForkInitiation.minorVersion( localHardForkInitiationEntity.getMinorVersion() );
        if ( localHardForkInitiationEntity.getSlot() != null ) {
            localHardForkInitiation.slot( localHardForkInitiationEntity.getSlot() );
        }

        return localHardForkInitiation.build();
    }
}
