package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.persistence.RollbackPersistence;
import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model.RollbackEntity;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RollbackJpaPersistence implements RollbackPersistence {
    private final RollbackJpaRepository rollbackJpaRepository;

    @Override
    public void save(RollbackEvent rollbackEvent) {
        RollbackEntity entity = RollbackEntity.builder()
                .rollbackToSlot(rollbackEvent.getRollbackTo().getSlot())
                .rollbackToBlockHash(rollbackEvent.getRollbackTo().getHash())
                .currentSlot(rollbackEvent.getCurrentPoint().getSlot())
                .currentBlockHash(rollbackEvent.getCurrentPoint().getHash())
                .currentBlock(rollbackEvent.getCurrentBlock())
                .build();

        rollbackJpaRepository.save(entity);
    }
}
