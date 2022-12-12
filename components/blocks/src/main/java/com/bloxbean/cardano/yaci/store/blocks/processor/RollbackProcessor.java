package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.store.blocks.model.RollbackEntity;
import com.bloxbean.cardano.yaci.store.blocks.repository.BlockRepository;
import com.bloxbean.cardano.yaci.store.blocks.repository.RollbackRepository;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@Transactional
public class RollbackProcessor {
    private RollbackRepository rollbackRepository;
    private BlockRepository blockRepository;

    public RollbackProcessor(RollbackRepository rollbackRepository, BlockRepository blockRepository) {
        this.rollbackRepository = rollbackRepository;
        this.blockRepository = blockRepository;
    }

    @EventListener
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
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
