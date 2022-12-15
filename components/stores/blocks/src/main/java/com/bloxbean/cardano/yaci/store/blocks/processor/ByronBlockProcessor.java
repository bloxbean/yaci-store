package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.persistence.BlockPersistence;
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
    private BlockPersistence blockPersistence;
    private AtomicInteger count;

    public ByronBlockProcessor(BlockPersistence blockPersistence) {
        this.blockPersistence = blockPersistence;
        this.count = new AtomicInteger(0);
    }

    @EventListener
    @Transactional
    public void handleGenesisBlockEvent(GenesisBlockEvent genesisBlockEvent) {
        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .blockHash(genesisBlockEvent.getBlockHash())
                .slot(genesisBlockEvent.getSlot())
                .block(0L)
                .prevHash(null)
                .build();

        blockPersistence.save(block);
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleByronMainBlockEvent(ByronMainBlockEvent event) {
        ByronMainBlock byronBlock = event.getByronMainBlock();
        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .blockHash(byronBlock.getHeader().getBlockHash())
                .slot(byronBlock.getHeader().getConsensusData().getSlotId().getSlot())
                .prevHash(byronBlock.getHeader().getPrevBlock())
                .build();

        blockPersistence.findByBlockHash(byronBlock.getHeader().getPrevBlock()).ifPresent(preBlock -> {
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

        blockPersistence.save(block);
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleByronEbBlockEvent(ByronEbBlockEvent event) {
        ByronEbBlock byronEbBlock = event.getByronEbBlock();
        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .blockHash(byronEbBlock.getHeader().getBlockHash())
                .slot(0L)
               // .epoch(byronEbBlock.getHeader().getConsensusData().getEpoch())
                .prevHash(byronEbBlock.getHeader().getPrevBlock())
                .build();

        blockPersistence.findByBlockHash(byronEbBlock.getHeader().getPrevBlock()).ifPresent(preBlock -> {
            block.setBlock(preBlock.getBlock() + 1);
        });

        blockPersistence.save(block);
    }
}
