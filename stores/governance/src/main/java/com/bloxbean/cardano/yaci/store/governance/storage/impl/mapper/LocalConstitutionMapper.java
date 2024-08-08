package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalConstitution;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalConstitutionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class LocalConstitutionMapper {
    public abstract LocalConstitution toLocalConstitution(LocalConstitutionEntity localConstitutionEntity);

    public abstract LocalConstitutionEntity toLocalConstitutionEntity(LocalConstitution localConstitution);
}
