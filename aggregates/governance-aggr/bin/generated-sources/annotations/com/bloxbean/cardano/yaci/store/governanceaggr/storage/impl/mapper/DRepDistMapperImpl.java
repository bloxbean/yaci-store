package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDist;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepDistEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:28+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class DRepDistMapperImpl implements DRepDistMapper {

    @Override
    public DRepDist toDRepDist(DRepDistEntity dRepDistEntity) {
        if ( dRepDistEntity == null ) {
            return null;
        }

        DRepDist.DRepDistBuilder dRepDist = DRepDist.builder();

        dRepDist.activeUntil( dRepDistEntity.getActiveUntil() );
        dRepDist.amount( dRepDistEntity.getAmount() );
        dRepDist.drepHash( dRepDistEntity.getDrepHash() );
        dRepDist.drepId( dRepDistEntity.getDrepId() );
        dRepDist.drepType( dRepDistEntity.getDrepType() );
        dRepDist.epoch( dRepDistEntity.getEpoch() );
        dRepDist.expiry( dRepDistEntity.getExpiry() );

        return dRepDist.build();
    }

    @Override
    public DRepDistEntity toDRepDistEntity(DRepDist dRepDist) {
        if ( dRepDist == null ) {
            return null;
        }

        DRepDistEntity dRepDistEntity = new DRepDistEntity();

        dRepDistEntity.setActiveUntil( dRepDist.getActiveUntil() );
        dRepDistEntity.setAmount( dRepDist.getAmount() );
        dRepDistEntity.setDrepHash( dRepDist.getDrepHash() );
        dRepDistEntity.setDrepId( dRepDist.getDrepId() );
        dRepDistEntity.setDrepType( dRepDist.getDrepType() );
        dRepDistEntity.setEpoch( dRepDist.getEpoch() );
        dRepDistEntity.setExpiry( dRepDist.getExpiry() );

        return dRepDistEntity;
    }
}
