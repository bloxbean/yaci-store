package com.bloxbean.cardano.yaci.store.core.storage.impl.redis.repository;

import com.bloxbean.cardano.yaci.store.core.storage.impl.redis.model.RedisCursorEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RedisCursorRepository extends RedisDocumentRepository<RedisCursorEntity, String> {

    Optional<RedisCursorEntity> findTopByEventPublisherIdOrderBySlotDesc(Long id);

    Optional<RedisCursorEntity> findTopByEventPublisherIdAndSlotBeforeOrderBySlotDesc(Long id, Long slot);

    Optional<RedisCursorEntity> findByEventPublisherIdAndBlockHash(Long id, String blockHash);

    Integer deleteByEventPublisherIdAndSlotGreaterThan(Long id, Long slot);

    //Required for history cleanup
    Integer deleteByEventPublisherIdAndBlockLessThan(Long id, Long block);
}
