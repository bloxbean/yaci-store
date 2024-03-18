package com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.model.JpaCursorEntity;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.model.JpaCursorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaCursorRepository extends JpaRepository<JpaCursorEntity, JpaCursorId> {
    Optional<JpaCursorEntity> findTopByIdOrderBySlotDesc(Long id);

    Optional<JpaCursorEntity> findTopByIdAndSlotBeforeOrderBySlotDesc(Long id, Long slot);

    Optional<JpaCursorEntity> findByIdAndBlockHash(Long id, String blockHash);

    int deleteByIdAndSlotGreaterThan(Long id, Long slot);

    //Required for history cleanup
    int deleteByIdAndBlockLessThan(Long id, Long block);
}
