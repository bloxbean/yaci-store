package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis;

import com.bloxbean.cardano.yaci.store.blocks.storage.api.RollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model.RollbackEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository.RollbackRepository;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RollbackStorageImpl implements RollbackStorage {

    private final RollbackRepository rollbackRepository;

    @Override
    public void save(RollbackEvent rollbackEvent) {
        RollbackEntity entity = RollbackEntity.builder()
                .rollbackToSlot(rollbackEvent.getRollbackTo().getSlot())
                .rollbackToBlockHash(rollbackEvent.getRollbackTo().getHash())
                .currentSlot(rollbackEvent.getCurrentPoint().getSlot())
                .currentBlockHash(rollbackEvent.getCurrentPoint().getHash())
                .currentBlock(rollbackEvent.getCurrentBlock())
                .build();

        rollbackRepository.save(entity);
    }
}
