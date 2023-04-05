package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.store.blocks.configuration.BlockConfig;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.persistence.BlockPersistence;
import com.bloxbean.cardano.yaci.store.blocks.util.BlockUtil;
import com.bloxbean.cardano.yaci.store.events.ByronEbBlockEvent;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class ByronBlockProcessor {
    private BlockPersistence blockPersistence;

    @Autowired
    private BlockConfig blockConfig;

    @Value("${store.cardano.protocol-magic}")
    private long protocolMagic;

    public ByronBlockProcessor(BlockPersistence blockPersistence) {
        this.blockPersistence = blockPersistence;
    }

    @EventListener
    @Transactional
    public void handleGenesisBlockEvent(GenesisBlockEvent genesisBlockEvent) {
        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .hash(genesisBlockEvent.getBlockHash())
                .slot(genesisBlockEvent.getSlot())
                .number(0L)
                .totalOutput(BigInteger.valueOf(0))
                .blockTime(genesisBlockEvent.getBlockTime())
                .prevHash(null)
                .build();

        blockPersistence.save(block);
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleByronMainBlockEvent(ByronMainBlockEvent event) {
        ByronMainBlock byronBlock = event.getByronMainBlock();

        long slot = event.getEventMetadata().getSlot();

        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .hash(byronBlock.getHeader().getBlockHash())
                .slot(slot)
                .totalOutput(BigInteger.valueOf(0))
                .prevHash(byronBlock.getHeader().getPrevBlock())
                .build();

        blockPersistence.findByBlockHash(byronBlock.getHeader().getPrevBlock()).ifPresent(preBlock -> {
            long blockNumber = preBlock.getNumber() + 1;
            long time = blockConfig.getStartTime(protocolMagic);
            long lastByronBlock = blockConfig.getLastByronBlock(protocolMagic);
            long byronProcessingTime = blockConfig.getByronProcessTime();
            long shellyProcessingTime = blockConfig.getShellyProcessTime();

            block.setNumber(blockNumber);
            block.setBlockTime(BlockUtil.calculateBlockTime(blockNumber, slot, time,
                    lastByronBlock, byronProcessingTime, shellyProcessingTime));
        });

        blockPersistence.save(block);
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleByronEbBlockEvent(ByronEbBlockEvent event) {
        ByronEbBlock byronEbBlock = event.getByronEbBlock();
        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .hash(byronEbBlock.getHeader().getBlockHash())
                .slot(event.getEventMetadata().getSlot())
               // .epoch(byronEbBlock.getHeader().getConsensusData().getEpoch())
                .prevHash(byronEbBlock.getHeader().getPrevBlock())
                .build();

        blockPersistence.findByBlockHash(byronEbBlock.getHeader().getPrevBlock()).ifPresent(preBlock -> {
            block.setNumber(preBlock.getNumber() + 1);
        });

        blockPersistence.save(block);
    }
}
