package com.bloxbean.cardano.yaci.store.adapot.job.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class AdaPotJobMapper {
    public abstract AdaPotJob toDomain(AdaPotJobEntity rewardCalcJobEntity);
    public abstract AdaPotJobEntity toEntity(AdaPotJob rewardCalcJob);
}
