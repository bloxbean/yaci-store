package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.DepositEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.DepositId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositRepository extends JpaRepository<DepositEntity, DepositId> {

    int deleteBySlotGreaterThan(Long slot);
}
