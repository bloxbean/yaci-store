package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.ByronEbBlockEvent;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.store.blocks.BlocksStoreConfiguration.STORE_BLOCKS_ENABLED;

@Component
@EnableIf(STORE_BLOCKS_ENABLED)
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
                .epochNumber(genesisBlockEvent.getEpoch())
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

        long slot = event.getMetadata().getSlot();

        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .number(event.getMetadata().getBlock())
                .hash(byronBlock.getHeader().getBlockHash())
                .slot(slot)
                .epochNumber(event.getMetadata().getEpochNumber())
                .epochSlot((int) event.getMetadata().getEpochSlot())
                .totalOutput(BigInteger.ZERO)
                .prevHash(byronBlock.getHeader().getPrevBlock())
                .blockTime(event.getMetadata().getBlockTime())
                .slotLeader(event.getMetadata().getSlotLeader())
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
                .number(event.getMetadata().getBlock())
                .hash(byronEbBlock.getHeader().getBlockHash())
                .slot(event.getMetadata().getSlot())
                .epochNumber(event.getMetadata().getEpochNumber())
                .epochSlot((int) event.getMetadata().getEpochSlot())
                .prevHash(byronEbBlock.getHeader().getPrevBlock())
                .blockTime(event.getMetadata().getBlockTime())
                .slotLeader(event.getMetadata().getSlotLeader())
                .build();

        blockStorage.save(block);
    }
}
