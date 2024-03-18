package com.bloxbean.cardano.yaci.store.core.storage.impl.redis;

import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.storage.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.mapper.RedisEraMapper;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.model.RedisCursorEntity;
import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.repository.RedisCursorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RedisCursorStorage implements CursorStorage {

    private final RedisCursorRepository redisCursorRepository;

    @Override
    public void saveCursor(Long eventPublisherId, Cursor cursor) {
        RedisCursorEntity redisCursorEntity = new RedisCursorEntity();
        redisCursorEntity.setCursorId(eventPublisherId + "-" + cursor.getBlockHash());
        redisCursorEntity.setEventPublisherId(eventPublisherId);
        redisCursorEntity.setBlockHash(cursor.getBlockHash());
        redisCursorEntity.setSlot(cursor.getSlot());
        redisCursorEntity.setBlock(cursor.getBlock());
        redisCursorEntity.setPrevBlockHash(cursor.getPrevBlockHash());
        redisCursorEntity.setEra(cursor.getEra() != null ? cursor.getEra().getValue() : null);
        redisCursorRepository.save(redisCursorEntity);
    }

    @Override
    public Optional<Cursor> getCurrentCursor(Long eventPublisherId) {
        return redisCursorRepository.findTopByEventPublisherIdOrderBySlotDesc(eventPublisherId)
                .map(RedisCursorStorage::cursorEntityToCursor);
    }

    @Override
    public Optional<Cursor> getPreviousCursor(Long eventPublisherId, Long slot) {
        return redisCursorRepository.findTopByEventPublisherIdAndSlotBeforeOrderBySlotDesc(eventPublisherId, slot)
                .map(RedisCursorStorage::cursorEntityToCursor);
    }

    @Override
    public Optional<Cursor> findByBlockHash(Long eventPublisherId, String blockHash) {
        return redisCursorRepository.findByEventPublisherIdAndBlockHash(eventPublisherId, blockHash)
                .map(RedisCursorStorage::cursorEntityToCursor);
    }

    @Override
    public Integer deleteBySlotGreaterThan(Long eventPublisherId, Long slot) {
        return redisCursorRepository.deleteByEventPublisherIdAndSlotGreaterThan(eventPublisherId, slot);
    }

    @Override
    public Integer deleteCursorBefore(Long eventPublisherId, Long blockNumber) {
        return redisCursorRepository.deleteByEventPublisherIdAndBlockLessThan(eventPublisherId, blockNumber);
    }

    private static Cursor cursorEntityToCursor(RedisCursorEntity redisCursorEntity) {
        return Cursor.builder()
                .slot(redisCursorEntity.getSlot())
                .blockHash(redisCursorEntity.getBlockHash())
                .block(redisCursorEntity.getBlock())
                .prevBlockHash(redisCursorEntity.getPrevBlockHash())
                .era(redisCursorEntity.getEra() != null ? RedisEraMapper.intToEra(redisCursorEntity.getEra()) : null)
                .build();
    }
}
