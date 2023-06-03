package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.DelegationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.StakeRegistrationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class StakingMapper {
    public abstract StakeRegistrationEntity toStakeResistrationEntity(StakeRegistrationDetail stakeRegistrationDetail);
    public abstract StakeRegistrationDetail toStakeRegistrationDetail(StakeRegistrationEntity stakeRegistrationEntity);

    public abstract DelegationEntity toDelegationEntity(Delegation delegation);
    public abstract Delegation toDelegation(DelegationEntity delegationEntity);
}
