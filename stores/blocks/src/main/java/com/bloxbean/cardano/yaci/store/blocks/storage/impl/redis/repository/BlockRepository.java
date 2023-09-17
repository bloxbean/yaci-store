package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model.BlockEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository(value = "blockRepositoryRedis")
public interface BlockRepository extends RedisDocumentRepository<BlockEntity, String> {

    Optional<BlockEntity> findTopByOrderByNumberDesc();

    Optional<BlockEntity> findByHash(String hash);

    Optional<BlockEntity> findByNumber(Long number);

    List<BlockEntity> findByEpochNumber(int epochNumber);

    List<BlockEntity> getBlockEntitiesBySlotLeaderAndEpochNumber(String slotLeader, int epochNumber);

    int deleteBySlotGreaterThan(Long slot);
}

