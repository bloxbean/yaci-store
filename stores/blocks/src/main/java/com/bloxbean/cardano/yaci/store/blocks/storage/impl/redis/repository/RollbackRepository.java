package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model.RollbackEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

@Repository(value = "rollbackRepositoryRedis")
public interface RollbackRepository extends RedisDocumentRepository<RollbackEntity, Long> {
}
