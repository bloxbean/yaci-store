package com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model.RedisTxInputEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedisTxInputRepository extends RedisDocumentRepository<RedisTxInputEntity, String> {

    Integer deleteBySpentAtSlotGreaterThan(Long slot);

    List<RedisTxInputEntity> findByTxHashIsNotNullAndSpentAtBlockLessThan(Long block);

    List<RedisTxInputEntity> findBySpentAtBlockBetween(Long start, Long End);
}

