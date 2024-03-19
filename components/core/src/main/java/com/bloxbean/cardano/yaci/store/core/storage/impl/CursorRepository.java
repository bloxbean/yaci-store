package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.store.core.storage.impl.model.CursorEntity;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.CursorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CursorRepository extends JpaRepository<CursorEntity, CursorId> {
    Optional<CursorEntity> findTopByIdOrderBySlotDesc(Long id);

    Optional<CursorEntity> findTopByIdAndSlotBeforeOrderBySlotDesc(Long id, Long slot);

    Optional<CursorEntity> findByIdAndBlockHash(Long id, String blockHash);

    int deleteByIdAndSlotGreaterThan(Long id, Long slot);

    //Required for history cleanup
    int deleteByIdAndBlockLessThan(Long id, Long block);
}
