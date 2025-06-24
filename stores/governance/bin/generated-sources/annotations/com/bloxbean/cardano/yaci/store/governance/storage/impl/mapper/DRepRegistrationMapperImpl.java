package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:19+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class DRepRegistrationMapperImpl extends DRepRegistrationMapper {

    @Override
    public DRepRegistrationEntity toDRepRegistrationEntity(DRepRegistration drepRegistration) {
        if ( drepRegistration == null ) {
            return null;
        }

        DRepRegistrationEntity.DRepRegistrationEntityBuilder<?, ?> dRepRegistrationEntity = DRepRegistrationEntity.builder();

        dRepRegistrationEntity.blockNumber( drepRegistration.getBlockNumber() );
        dRepRegistrationEntity.blockTime( drepRegistration.getBlockTime() );
        dRepRegistrationEntity.anchorHash( drepRegistration.getAnchorHash() );
        dRepRegistrationEntity.anchorUrl( drepRegistration.getAnchorUrl() );
        dRepRegistrationEntity.certIndex( drepRegistration.getCertIndex() );
        dRepRegistrationEntity.credType( drepRegistration.getCredType() );
        dRepRegistrationEntity.deposit( drepRegistration.getDeposit() );
        dRepRegistrationEntity.drepHash( drepRegistration.getDrepHash() );
        dRepRegistrationEntity.drepId( drepRegistration.getDrepId() );
        dRepRegistrationEntity.epoch( drepRegistration.getEpoch() );
        dRepRegistrationEntity.slot( drepRegistration.getSlot() );
        dRepRegistrationEntity.txHash( drepRegistration.getTxHash() );
        dRepRegistrationEntity.txIndex( drepRegistration.getTxIndex() );
        dRepRegistrationEntity.type( drepRegistration.getType() );

        return dRepRegistrationEntity.build();
    }

    @Override
    public DRepRegistration toDRepRegistration(DRepRegistrationEntity drepRegistration) {
        if ( drepRegistration == null ) {
            return null;
        }

        DRepRegistration.DRepRegistrationBuilder<?, ?> dRepRegistration = DRepRegistration.builder();

        dRepRegistration.blockNumber( drepRegistration.getBlockNumber() );
        dRepRegistration.blockTime( drepRegistration.getBlockTime() );
        dRepRegistration.anchorHash( drepRegistration.getAnchorHash() );
        dRepRegistration.anchorUrl( drepRegistration.getAnchorUrl() );
        dRepRegistration.certIndex( drepRegistration.getCertIndex() );
        dRepRegistration.credType( drepRegistration.getCredType() );
        dRepRegistration.deposit( drepRegistration.getDeposit() );
        dRepRegistration.drepHash( drepRegistration.getDrepHash() );
        dRepRegistration.drepId( drepRegistration.getDrepId() );
        dRepRegistration.epoch( drepRegistration.getEpoch() );
        dRepRegistration.slot( drepRegistration.getSlot() );
        dRepRegistration.txHash( drepRegistration.getTxHash() );
        dRepRegistration.txIndex( drepRegistration.getTxIndex() );
        dRepRegistration.type( drepRegistration.getType() );

        return dRepRegistration.build();
    }
}
