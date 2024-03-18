package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.JpaRollbackEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.JpaRollbackRepository;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JpaRollbackStorage implements RollbackStorage {
    private final JpaRollbackRepository jpaRollbackRepository;

    @Override
    public void save(RollbackEvent rollbackEvent) {
        JpaRollbackEntity entity = JpaRollbackEntity.builder()
                .rollbackToSlot(rollbackEvent.getRollbackTo().getSlot())
                .rollbackToBlockHash(rollbackEvent.getRollbackTo().getHash())
                .currentSlot(rollbackEvent.getCurrentPoint().getSlot())
                .currentBlockHash(rollbackEvent.getCurrentPoint().getHash())
                .currentBlock(rollbackEvent.getCurrentBlock())
                .build();

        jpaRollbackRepository.save(entity);
    }
}
