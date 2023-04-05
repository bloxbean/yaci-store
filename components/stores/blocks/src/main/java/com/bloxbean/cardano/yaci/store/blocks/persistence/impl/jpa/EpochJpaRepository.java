package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model.EpochEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EpochJpaRepository extends JpaRepository<EpochEntity, String> {

    Optional<EpochEntity> findTopByOrderByNumberDesc();

    Optional<Epoch> findByNumber(int number);
}
