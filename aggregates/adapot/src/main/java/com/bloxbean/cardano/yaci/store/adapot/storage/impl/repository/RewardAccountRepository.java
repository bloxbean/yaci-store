package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardAccountEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardAccountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RewardAccountRepository extends JpaRepository<RewardAccountEntity, RewardAccountId> {

    Optional<RewardAccountEntity> findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(String address, Long slot);

    int deleteBySlotGreaterThan(Long slot);
}
