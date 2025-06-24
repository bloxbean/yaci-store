package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.DRep;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class DRepMapperImpl implements DRepMapper {

    @Override
    public DRepEntity toDRepEntity(DRep dRep) {
        if ( dRep == null ) {
            return null;
        }

        DRepEntity.DRepEntityBuilder<?, ?> dRepEntity = DRepEntity.builder();

        dRepEntity.blockNumber( dRep.getBlockNumber() );
        dRepEntity.blockTime( dRep.getBlockTime() );
        dRepEntity.blockHash( dRep.getBlockHash() );
        dRepEntity.certIndex( dRep.getCertIndex() );
        dRepEntity.certType( dRep.getCertType() );
        dRepEntity.deposit( dRep.getDeposit() );
        dRepEntity.drepHash( dRep.getDrepHash() );
        dRepEntity.drepId( dRep.getDrepId() );
        dRepEntity.epoch( dRep.getEpoch() );
        dRepEntity.registrationSlot( dRep.getRegistrationSlot() );
        dRepEntity.slot( dRep.getSlot() );
        dRepEntity.status( dRep.getStatus() );
        dRepEntity.txHash( dRep.getTxHash() );
        dRepEntity.txIndex( dRep.getTxIndex() );

        return dRepEntity.build();
    }

    @Override
    public DRep toDRep(DRepEntity dRepEntity) {
        if ( dRepEntity == null ) {
            return null;
        }

        DRep.DRepBuilder<?, ?> dRep = DRep.builder();

        dRep.blockNumber( dRepEntity.getBlockNumber() );
        dRep.blockTime( dRepEntity.getBlockTime() );
        dRep.blockHash( dRepEntity.getBlockHash() );
        dRep.certIndex( dRepEntity.getCertIndex() );
        dRep.certType( dRepEntity.getCertType() );
        dRep.deposit( dRepEntity.getDeposit() );
        dRep.drepHash( dRepEntity.getDrepHash() );
        dRep.drepId( dRepEntity.getDrepId() );
        dRep.epoch( dRepEntity.getEpoch() );
        dRep.registrationSlot( dRepEntity.getRegistrationSlot() );
        dRep.slot( dRepEntity.getSlot() );
        dRep.status( dRepEntity.getStatus() );
        dRep.txHash( dRepEntity.getTxHash() );
        dRep.txIndex( dRepEntity.getTxIndex() );

        return dRep.build();
    }
}
