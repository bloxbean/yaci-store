package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.CursorEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class CursorStorageImpl implements CursorStorage {
    private final CursorRepository cursorRepository;

    @Transactional
    @Override
    public void saveCursor(long eventPublisherId, Cursor cursor) {
        CursorEntity cursorEntity = CursorEntity
                .builder()
                .id(eventPublisherId)
                .slot(cursor.getSlot())
                .blockHash(cursor.getBlockHash())
                .block(cursor.getBlock())
                .prevBlockHash(cursor.getPrevBlockHash())
                .era(cursor.getEra() != null? cursor.getEra().getValue(): null)
                .build();

        cursorRepository.save(cursorEntity);
    }

    @Transactional
    @Override
    public Optional<Cursor> getCurrentCursor(long eventPublisherId) {
        return cursorRepository.findTopByIdOrderBySlotDesc(eventPublisherId)
                .map(cursorEntity -> cursorEntityToCursor(cursorEntity));
    }

    @Transactional
    @Override
    public Optional<Cursor> getPreviousCursor(long eventPublisherId, long slot) {
        return cursorRepository.findTopByIdAndSlotBeforeOrderBySlotDesc(eventPublisherId, slot)
                .map(cursorEntity -> cursorEntityToCursor(cursorEntity));
    }

    @Transactional
    @Override
    public Optional<Cursor> findByBlockHash(long eventPublisherId, String blockHash) {
        return cursorRepository.findByIdAndBlockHash(eventPublisherId, blockHash)
                .map(cursorEntity -> cursorEntityToCursor(cursorEntity));
    }

    @Transactional
    @Override
    public int deleteBySlotGreaterThan(long eventPublisherId, long slot) {
        return cursorRepository.deleteByIdAndSlotGreaterThan(eventPublisherId, slot);
    }

    @Transactional
    @Override
    public int deleteCursorBefore(long eventPublisherId, long blockNumber) {
        return cursorRepository.deleteByIdAndBlockLessThan(eventPublisherId, blockNumber);
    }

    private static Cursor cursorEntityToCursor(CursorEntity cursorEntity) {
        return Cursor.builder()
                .slot(cursorEntity.getSlot())
                .blockHash(cursorEntity.getBlockHash())
                .block(cursorEntity.getBlock())
                .prevBlockHash(cursorEntity.getPrevBlockHash())
                .era(cursorEntity.getEra() != null? EraMapper.intToEra(cursorEntity.getEra()): null)
                .build();
    }
}
