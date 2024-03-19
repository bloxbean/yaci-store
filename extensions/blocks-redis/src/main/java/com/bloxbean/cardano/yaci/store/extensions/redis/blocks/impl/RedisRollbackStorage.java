package com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl;

import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.model.RedisRollbackEntity;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.repository.RedisRollbackRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RedisRollbackStorage implements RollbackStorage {

    private final RedisRollbackRepository redisRollbackRepository;

    @Override
    public void save(RollbackEvent rollbackEvent) {
        RedisRollbackEntity entity = RedisRollbackEntity.builder()
                .rollbackToSlot(rollbackEvent.getRollbackTo().getSlot())
                .rollbackToBlockHash(rollbackEvent.getRollbackTo().getHash())
                .currentSlot(rollbackEvent.getCurrentPoint().getSlot())
                .currentBlockHash(rollbackEvent.getCurrentPoint().getHash())
                .currentBlock(rollbackEvent.getCurrentBlock())
                .build();

        redisRollbackRepository.save(entity);
    }
}
