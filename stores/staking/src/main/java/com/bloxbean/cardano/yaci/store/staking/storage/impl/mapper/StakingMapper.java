package com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.DelegationEntityJpa;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.StakeRegistrationEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class StakingMapper {
    public abstract StakeRegistrationEntityJpa toStakeResistrationEntity(StakeRegistrationDetail stakeRegistrationDetail);
    public abstract StakeRegistrationDetail toStakeRegistrationDetail(StakeRegistrationEntityJpa stakeRegistrationEntity);

    public abstract DelegationEntityJpa toDelegationEntity(Delegation delegation);
    public abstract Delegation toDelegation(DelegationEntityJpa delegationEntity);
}
