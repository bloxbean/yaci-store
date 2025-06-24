package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalConstitution;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalConstitutionEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:19+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class LocalConstitutionMapperImpl extends LocalConstitutionMapper {

    @Override
    public LocalConstitution toLocalConstitution(LocalConstitutionEntity localConstitutionEntity) {
        if ( localConstitutionEntity == null ) {
            return null;
        }

        LocalConstitution.LocalConstitutionBuilder localConstitution = LocalConstitution.builder();

        localConstitution.anchorHash( localConstitutionEntity.getAnchorHash() );
        localConstitution.anchorUrl( localConstitutionEntity.getAnchorUrl() );
        localConstitution.epoch( localConstitutionEntity.getEpoch() );
        localConstitution.script( localConstitutionEntity.getScript() );
        localConstitution.slot( localConstitutionEntity.getSlot() );

        return localConstitution.build();
    }

    @Override
    public LocalConstitutionEntity toLocalConstitutionEntity(LocalConstitution localConstitution) {
        if ( localConstitution == null ) {
            return null;
        }

        LocalConstitutionEntity.LocalConstitutionEntityBuilder<?, ?> localConstitutionEntity = LocalConstitutionEntity.builder();

        localConstitutionEntity.anchorHash( localConstitution.getAnchorHash() );
        localConstitutionEntity.anchorUrl( localConstitution.getAnchorUrl() );
        localConstitutionEntity.epoch( localConstitution.getEpoch() );
        localConstitutionEntity.script( localConstitution.getScript() );
        localConstitutionEntity.slot( localConstitution.getSlot() );

        return localConstitutionEntity.build();
    }
}
