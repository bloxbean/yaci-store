package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.Constitution;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.ConstitutionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ConstitutionMapper {
    public abstract ConstitutionEntity toConstitutionEntity(Constitution constitution);

    public abstract Constitution toConstitution(ConstitutionEntity constitutionEntity);
}
