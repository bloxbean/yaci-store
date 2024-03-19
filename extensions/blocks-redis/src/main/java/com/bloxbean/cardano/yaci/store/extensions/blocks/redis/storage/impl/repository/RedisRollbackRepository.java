package com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.model.RedisRollbackEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisRollbackRepository extends RedisDocumentRepository<RedisRollbackEntity, Long> {
}
