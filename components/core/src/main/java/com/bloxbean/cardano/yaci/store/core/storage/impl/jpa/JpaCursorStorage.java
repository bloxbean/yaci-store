package com.bloxbean.cardano.yaci.store.core.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.storage.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.mapper.JpaEraMapper;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.model.JpaCursorEntity;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.repository.JpaCursorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JpaCursorStorage implements CursorStorage {

    private final JpaCursorRepository jpaCursorRepository;

    @Override
    public void saveCursor(Long eventPublisherId, Cursor cursor) {
        JpaCursorEntity jpaCursorEntity = JpaCursorEntity
                .builder()
                .id(eventPublisherId)
                .slot(cursor.getSlot())
                .blockHash(cursor.getBlockHash())
                .block(cursor.getBlock())
                .prevBlockHash(cursor.getPrevBlockHash())
                .era(cursor.getEra() != null? cursor.getEra().getValue(): null)
                .build();

        jpaCursorRepository.save(jpaCursorEntity);
    }

    @Override
    public Optional<Cursor> getCurrentCursor(Long eventPublisherId) {
        return jpaCursorRepository.findTopByIdOrderBySlotDesc(eventPublisherId)
                .map(JpaCursorStorage::cursorEntityToCursor);
    }

    @Override
    public Optional<Cursor> getPreviousCursor(Long eventPublisherId, Long slot) {
        return jpaCursorRepository.findTopByIdAndSlotBeforeOrderBySlotDesc(eventPublisherId, slot)
                .map(JpaCursorStorage::cursorEntityToCursor);
    }

    @Override
    public Optional<Cursor> findByBlockHash(Long eventPublisherId, String blockHash) {
        return jpaCursorRepository.findByIdAndBlockHash(eventPublisherId, blockHash)
                .map(JpaCursorStorage::cursorEntityToCursor);
    }

    @Transactional
    @Override
    public Integer deleteBySlotGreaterThan(Long eventPublisherId, Long slot) {
        return jpaCursorRepository.deleteByIdAndSlotGreaterThan(eventPublisherId, slot);
    }

    @Override
    public Integer deleteCursorBefore(Long eventPublisherId, Long blockNumber) {
        return jpaCursorRepository.deleteByIdAndBlockLessThan(eventPublisherId, blockNumber);
    }

    private static Cursor cursorEntityToCursor(JpaCursorEntity jpaCursorEntity) {
        return Cursor.builder()
                .slot(jpaCursorEntity.getSlot())
                .blockHash(jpaCursorEntity.getBlockHash())
                .block(jpaCursorEntity.getBlock())
                .prevBlockHash(jpaCursorEntity.getPrevBlockHash())
                .era(jpaCursorEntity.getEra() != null? JpaEraMapper.intToEra(jpaCursorEntity.getEra()): null)
                .build();
    }
}
