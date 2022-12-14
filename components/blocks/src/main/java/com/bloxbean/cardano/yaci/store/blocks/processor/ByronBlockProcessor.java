package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.store.blocks.model.BlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.repository.BlockRepository;
import com.bloxbean.cardano.yaci.store.events.ByronEbBlockEvent;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class ByronBlockProcessor {
    private BlockRepository blockRepository;
    private AtomicInteger count;

    public ByronBlockProcessor(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
        this.count = new AtomicInteger(0);
    }

    @EventListener
    @Transactional
    public void handleGenesisBlockEvent(GenesisBlockEvent genesisBlockEvent) {
        BlockEntity block = BlockEntity.builder()
                .era(Era.Byron.getValue())
                .blockHash(genesisBlockEvent.getBlockHash())
                .slot(genesisBlockEvent.getSlot())
                .block(0L)
                .prevHash(null)
                .build();

        blockRepository.save(block);
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleByronMainBlockEvent(ByronMainBlockEvent event) {
        ByronMainBlock byronBlock = event.getByronMainBlock();
        BlockEntity block = BlockEntity.builder()
                .era(Era.Byron.getValue())
                .blockHash(byronBlock.getHeader().getBlockHash())
                .slot(byronBlock.getHeader().getConsensusData().getSlotId().getSlot())
                .prevHash(byronBlock.getHeader().getPrevBlock())
                .build();

        blockRepository.findByBlockHash(byronBlock.getHeader().getPrevBlock()).ifPresent(preBlock -> {
            block.setBlock(preBlock.getBlock() + 1);
        });

        count.incrementAndGet();
        double val = count.get() % 5000;

        if (!event.getEventMetadata().isSyncMode()) {
            if (val == 0) {
                log.info("# of blocks written: " + count.get());
                log.info("Block No: " + block.getBlock() + "  , Era: " + block.getEra());
            }

        } else {
            log.info("# of blocks written: " + count.get());
            log.info("Block No: " + block.getBlock() + "  , Era: " + block.getEra());
        }

        blockRepository.save(block);
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleByronEbBlockEvent(ByronEbBlockEvent event) {
        ByronEbBlock byronEbBlock = event.getByronEbBlock();
        BlockEntity block = BlockEntity.builder()
                .era(Era.Byron.getValue())
                .blockHash(byronEbBlock.getHeader().getBlockHash())
                .slot(0L)
                .epoch(byronEbBlock.getHeader().getConsensusData().getEpoch())
                .prevHash(byronEbBlock.getHeader().getPrevBlock())
                .build();

        blockRepository.findByBlockHash(byronEbBlock.getHeader().getPrevBlock()).ifPresent(preBlock -> {
            block.setBlock(preBlock.getBlock() + 1);
        });

        blockRepository.save(block);
    }
}
