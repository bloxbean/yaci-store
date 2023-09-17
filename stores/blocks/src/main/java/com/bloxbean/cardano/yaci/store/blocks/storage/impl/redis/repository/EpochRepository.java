package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model.EpochEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository(value = "epochRepositoryRedis")
public interface EpochRepository extends RedisDocumentRepository<EpochEntity, Long> {

    Optional<EpochEntity> findTopByOrderByNumberDesc();
}
