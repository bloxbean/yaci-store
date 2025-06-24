package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.Constitution;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.ConstitutionEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class ConstitutionMapperImpl extends ConstitutionMapper {

    @Override
    public ConstitutionEntity toConstitutionEntity(Constitution constitution) {
        if ( constitution == null ) {
            return null;
        }

        ConstitutionEntity constitutionEntity = new ConstitutionEntity();

        constitutionEntity.setActiveEpoch( constitution.getActiveEpoch() );
        constitutionEntity.setAnchorHash( constitution.getAnchorHash() );
        constitutionEntity.setAnchorUrl( constitution.getAnchorUrl() );
        constitutionEntity.setScript( constitution.getScript() );
        constitutionEntity.setSlot( constitution.getSlot() );

        return constitutionEntity;
    }

    @Override
    public Constitution toConstitution(ConstitutionEntity constitutionEntity) {
        if ( constitutionEntity == null ) {
            return null;
        }

        Constitution.ConstitutionBuilder<?, ?> constitution = Constitution.builder();

        constitution.activeEpoch( constitutionEntity.getActiveEpoch() );
        constitution.anchorHash( constitutionEntity.getAnchorHash() );
        constitution.anchorUrl( constitutionEntity.getAnchorUrl() );
        constitution.script( constitutionEntity.getScript() );
        constitution.slot( constitutionEntity.getSlot() );

        return constitution.build();
    }
}
