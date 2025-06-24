package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalDRepDistr;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalDRepDistrEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class LocalDRepDistrMapperImpl extends LocalDRepDistrMapper {

    @Override
    public LocalDRepDistr toLocalDRepDist(LocalDRepDistrEntity localDRepDistEntity) {
        if ( localDRepDistEntity == null ) {
            return null;
        }

        LocalDRepDistr.LocalDRepDistrBuilder localDRepDistr = LocalDRepDistr.builder();

        localDRepDistr.amount( localDRepDistEntity.getAmount() );
        localDRepDistr.drepHash( localDRepDistEntity.getDrepHash() );
        localDRepDistr.drepType( localDRepDistEntity.getDrepType() );
        localDRepDistr.epoch( localDRepDistEntity.getEpoch() );
        localDRepDistr.slot( localDRepDistEntity.getSlot() );

        return localDRepDistr.build();
    }

    @Override
    public LocalDRepDistrEntity localDRepDistrEntity(LocalDRepDistr localDRepDist) {
        if ( localDRepDist == null ) {
            return null;
        }

        LocalDRepDistrEntity.LocalDRepDistrEntityBuilder localDRepDistrEntity = LocalDRepDistrEntity.builder();

        localDRepDistrEntity.amount( localDRepDist.getAmount() );
        localDRepDistrEntity.drepHash( localDRepDist.getDrepHash() );
        localDRepDistrEntity.drepType( localDRepDist.getDrepType() );
        localDRepDistrEntity.epoch( localDRepDist.getEpoch() );
        localDRepDistrEntity.slot( localDRepDist.getSlot() );

        return localDRepDistrEntity.build();
    }
}
