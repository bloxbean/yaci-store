package com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.model.RedisTxInputEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisTxInputRepository extends RedisDocumentRepository<RedisTxInputEntity, String> {

    Integer deleteBySpentAtSlotGreaterThan(Long slot);
}

