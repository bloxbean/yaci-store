package com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.model.RedisBlockEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RedisBlockRepository extends RedisDocumentRepository<RedisBlockEntity, String> {

    Optional<RedisBlockEntity> findTopByOrderByNumberDesc();

    Optional<RedisBlockEntity> findByNumber(Long number);

    List<RedisBlockEntity> findByEpochNumber(int epochNumber);

    List<RedisBlockEntity> getBlockEntitiesBySlotLeaderAndEpochNumber(String slotLeader, int epochNumber);

    Integer deleteBySlotGreaterThan(Long slot);
}

