package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.StakeRegistrationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.StakeRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StakeRegistrationRepository
        extends JpaRepository<StakeRegistrationEntity, StakeRegistrationId> {

    int deleteBySlotGreaterThan(Long slot);
}
