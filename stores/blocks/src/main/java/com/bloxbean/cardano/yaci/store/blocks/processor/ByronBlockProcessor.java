package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import com.bloxbean.cardano.yaci.store.events.ByronEbBlockEvent;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Component
@Slf4j
public class ByronBlockProcessor {
    private BlockStorage blockStorage;

    public ByronBlockProcessor(BlockStorage blockStorage) {
        this.blockStorage = blockStorage;
    }

    @EventListener
    @Transactional
    public void handleGenesisBlockEvent(GenesisBlockEvent genesisBlockEvent) {
        Block block = Block.builder()
                .era(genesisBlockEvent.getEra().getValue())
                .hash(genesisBlockEvent.getBlockHash())
                .slot(genesisBlockEvent.getSlot())
                .number(genesisBlockEvent.getBlock())
                .epochNumber(0)
                .epochSlot(-1)
                .totalOutput(BigInteger.valueOf(0))
                .blockTime(genesisBlockEvent.getBlockTime())
                .prevHash(null)
                .build();

        blockStorage.save(block);
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleByronMainBlockEvent(ByronMainBlockEvent event) {
        ByronMainBlock byronBlock = event.getByronMainBlock();

        long slot = event.getEventMetadata().getSlot();

        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .number(event.getEventMetadata().getBlock())
                .hash(byronBlock.getHeader().getBlockHash())
                .slot(slot)
                .epochNumber(event.getEventMetadata().getEpochNumber())
                .epochSlot((int) event.getEventMetadata().getEpochSlot())
                .totalOutput(BigInteger.ZERO)
                .prevHash(byronBlock.getHeader().getPrevBlock())
                .blockTime(event.getEventMetadata().getBlockTime())
                .slotLeader(event.getEventMetadata().getSlotLeader())
                .build();

        //Find total tx output
        BigInteger totalTxOutput = byronBlock.getBody().getTxPayload().stream()
                .flatMap(tx -> tx.getTransaction().getOutputs().stream())
                .map(output -> output.getAmount())
                .reduce(BigInteger::add)
                .orElse(BigInteger.ZERO);
        block.setTotalOutput(totalTxOutput);

        //TODO Find total fee ??
        //fee = totalInput - totalOutput

        blockStorage.save(block);
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleByronEbBlockEvent(ByronEbBlockEvent event) {
        ByronEbBlock byronEbBlock = event.getByronEbBlock();
        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .number(event.getEventMetadata().getBlock())
                .hash(byronEbBlock.getHeader().getBlockHash())
                .slot(event.getEventMetadata().getSlot())
                .epochNumber(event.getEventMetadata().getEpochNumber())
                .epochSlot((int) event.getEventMetadata().getEpochSlot())
                .prevHash(byronEbBlock.getHeader().getPrevBlock())
                .blockTime(event.getEventMetadata().getBlockTime())
                .slotLeader(event.getEventMetadata().getSlotLeader())
                .build();

        blockStorage.save(block);
    }
}
