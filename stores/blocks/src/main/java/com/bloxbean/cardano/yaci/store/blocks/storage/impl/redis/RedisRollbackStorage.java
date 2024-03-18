package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis;

import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model.RedisRollbackEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository.RedisRollbackRepository;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
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
