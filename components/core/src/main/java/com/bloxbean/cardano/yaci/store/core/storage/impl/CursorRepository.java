package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.store.core.storage.impl.model.CursorEntity;

import java.util.Optional;

public interface CursorRepository {
    void save(CursorEntity cursorEntity);

    Optional<CursorEntity> findTopByIdOrderBySlotDesc(Long id);

    Optional<CursorEntity> findTopByIdAndSlotBeforeOrderBySlotDesc(Long id, Long slot);

    Optional<CursorEntity> findByIdAndBlockHash(Long id, String blockHash);

    long deleteByIdAndSlotGreaterThan(Long id, Long slot);

    //Required for history cleanup
    long deleteByIdAndBlockLessThan(Long id, Long block);

}
