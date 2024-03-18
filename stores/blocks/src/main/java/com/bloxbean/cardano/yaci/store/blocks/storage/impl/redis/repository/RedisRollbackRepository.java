package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model.RedisRollbackEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisRollbackRepository extends RedisDocumentRepository<RedisRollbackEntity, Long> {
}
