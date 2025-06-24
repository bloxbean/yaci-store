package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.EraEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class EraMapperImpl extends EraMapper {

    @Override
    public CardanoEra toEra(EraEntity eraEntity) {
        if ( eraEntity == null ) {
            return null;
        }

        CardanoEra.CardanoEraBuilder cardanoEra = CardanoEra.builder();

        cardanoEra.era( EraMapper.intToEra( eraEntity.getEra() ) );
        cardanoEra.block( eraEntity.getBlock() );
        cardanoEra.blockHash( eraEntity.getBlockHash() );
        cardanoEra.startSlot( eraEntity.getStartSlot() );

        return cardanoEra.build();
    }

    @Override
    public EraEntity toEraEntity(CardanoEra eraEntity) {
        if ( eraEntity == null ) {
            return null;
        }

        EraEntity.EraEntityBuilder eraEntity1 = EraEntity.builder();

        eraEntity1.era( EraMapper.eraToInt( eraEntity.getEra() ) );
        eraEntity1.block( eraEntity.getBlock() );
        eraEntity1.blockHash( eraEntity.getBlockHash() );
        eraEntity1.startSlot( eraEntity.getStartSlot() );

        return eraEntity1.build();
    }
}
