package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommittee;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalCommitteeEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:19+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class LocalCommitteeMapperImpl extends LocalCommitteeMapper {

    @Override
    public LocalCommittee toLocalCommittee(LocalCommitteeEntity localCommitteeEntity) {
        if ( localCommitteeEntity == null ) {
            return null;
        }

        LocalCommittee.LocalCommitteeBuilder localCommittee = LocalCommittee.builder();

        localCommittee.epoch( localCommitteeEntity.getEpoch() );
        localCommittee.slot( localCommitteeEntity.getSlot() );
        localCommittee.threshold( localCommitteeEntity.getThreshold() );

        return localCommittee.build();
    }

    @Override
    public LocalCommitteeEntity toLocalCommitteeEntity(LocalCommittee localCommittee) {
        if ( localCommittee == null ) {
            return null;
        }

        LocalCommitteeEntity.LocalCommitteeEntityBuilder<?, ?> localCommitteeEntity = LocalCommitteeEntity.builder();

        localCommitteeEntity.epoch( localCommittee.getEpoch() );
        localCommitteeEntity.slot( localCommittee.getSlot() );
        localCommitteeEntity.threshold( localCommittee.getThreshold() );

        return localCommitteeEntity.build();
    }
}
