package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.WithdrawalEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.WithdrawalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRepository extends JpaRepository<WithdrawalEntity, WithdrawalId> {

    int deleteBySlotGreaterThan(Long slot);
}
