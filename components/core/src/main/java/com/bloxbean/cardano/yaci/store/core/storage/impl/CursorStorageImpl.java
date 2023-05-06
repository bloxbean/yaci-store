package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.CursorEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class CursorStorageImpl implements CursorStorage {
    private final CursorRepository cursorRepository;

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

    @Override
    public Optional<Cursor> getCursorAtCurrentMinusOffset(long eventPublisherId, int offset) {
        List<CursorEntity> cursorEntities =
                cursorRepository.findTop50ByIdOrderBySlotDesc(eventPublisherId);

        if (cursorEntities == null || cursorEntities.size() == 0)
            return Optional.empty();

        CursorEntity cursorEntity = cursorEntities.get(cursorEntities.size() - 1);

        return Optional.of(cursorEntityToCursor(cursorEntity));
    }

    @Override
    public Optional<Cursor> getCurrentCursor(long eventPublisherId) {
        return cursorRepository.findTopByIdOrderBySlotDesc(eventPublisherId)
                .map(cursorEntity -> cursorEntityToCursor(cursorEntity));
    }

    @Override
    public Optional<Cursor> findByBlockHash(long eventPublisherId, String blockHash) {
        return cursorRepository.findByIdAndBlockHash(eventPublisherId, blockHash)
                .map(cursorEntity -> cursorEntityToCursor(cursorEntity));
    }

    @Override
    public int deleteBySlotGreaterThan(long eventPublisherId, long slot) {
        return cursorRepository.deleteByIdAndSlotGreaterThan(eventPublisherId, slot);
    }

    private static Cursor cursorEntityToCursor(CursorEntity cursorEntity) {
        return Cursor.builder()
                .slot(cursorEntity.getSlot())
                .blockHash(cursorEntity.getBlockHash())
                .block(cursorEntity.getBlock())
                .prevBlockHash(cursorEntity.getPrevBlockHash())
                .era(EraMapper.intToEra(cursorEntity.getEra()))
                .build();
    }
}
