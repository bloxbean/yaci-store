package com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRegistrationEnity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRetirementEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class PoolMapper {
    public abstract PoolRegistrationEnity toPoolRegistrationEntity(PoolRegistration poolRegistrationDetail);
    public abstract PoolRegistration toPoolRegistration(PoolRegistrationEnity poolRegistrationEnity);

    public abstract PoolRetirementEntityJpa toPoolRetirementEntity(PoolRetirement poolRetirement);
    public abstract PoolRetirement toPoolRetirement(PoolRetirementEntityJpa poolRetirementEntity);
}
