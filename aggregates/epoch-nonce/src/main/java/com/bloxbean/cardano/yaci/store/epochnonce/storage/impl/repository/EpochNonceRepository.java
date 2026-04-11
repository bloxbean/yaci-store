package com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.model.EpochNonceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpochNonceRepository extends JpaRepository<EpochNonceEntity, Integer> {
    Optional<EpochNonceEntity> findByEpoch(int epoch);

    int deleteBySlotGreaterThan(Long slot);
}
