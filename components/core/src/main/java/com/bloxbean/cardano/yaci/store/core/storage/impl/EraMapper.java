package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.EraEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;

@Mapper(componentModel = "spring")
public abstract class EraMapper {

    @Mapping(source = "era", target = "era", qualifiedByName = "intToEra")
    public abstract CardanoEra toEra(EraEntity eraEntity);

    @Mapping(source = "era", target = "era", qualifiedByName = "eraToInt")
    public abstract EraEntity toEraEntity(CardanoEra eraEntity);

    @Named("eraToInt")
    public static int eraToInt(Era era) {
        return era.getValue();
    }

    @Named("intToEra")
    public static Era intToEra(int era) {
        return Arrays.stream(Era.values())
                .filter(p -> p.getValue() == era)
                .findFirst()
                .orElse(null);
    }
}
