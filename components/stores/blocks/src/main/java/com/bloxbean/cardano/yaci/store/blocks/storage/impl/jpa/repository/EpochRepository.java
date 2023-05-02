package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.EpochEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EpochRepository extends JpaRepository<EpochEntity, String> {

    Optional<EpochEntity> findTopByOrderByNumberDesc();

    Optional<Epoch> findByNumber(int number);
}
