package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockCbor;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockCborStorage;
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
    private final BlockStorage blockStorage;
    private final BlockCborStorage blockCborStorage;
    private final BlocksStoreProperties blocksStoreProperties;

    public ByronBlockProcessor(BlockStorage blockStorage,
                               BlockCborStorage blockCborStorage,
                               BlocksStoreProperties blocksStoreProperties) {
        this.blockStorage = blockStorage;
        this.blockCborStorage = blockCborStorage;
        this.blocksStoreProperties = blocksStoreProperties;
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
        String blockHash = byronBlock.getHeader().getBlockHash();

        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .number(event.getMetadata().getBlock())
                .hash(blockHash)
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

        if (blocksStoreProperties.isSaveCbor()) {
            saveByronBlockCbor(event, blockHash, slot);
        }
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleByronEbBlockEvent(ByronEbBlockEvent event) {
        ByronEbBlock byronEbBlock = event.getByronEbBlock();
        String blockHash = byronEbBlock.getHeader().getBlockHash();
        long slot = event.getMetadata().getSlot();

        Block block = Block.builder()
                .era(Era.Byron.getValue())
                .number(event.getMetadata().getBlock())
                .hash(blockHash)
                .slot(slot)
                .epochNumber(event.getMetadata().getEpochNumber())
                .epochSlot((int) event.getMetadata().getEpochSlot())
                .prevHash(byronEbBlock.getHeader().getPrevBlock())
                .blockTime(event.getMetadata().getBlockTime())
                .slotLeader(event.getMetadata().getSlotLeader())
                .build();

        blockStorage.save(block);

        if (blocksStoreProperties.isSaveCbor()) {
            saveByronEbBlockCbor(event, blockHash, slot);
        }
    }

    /**
     * Save Byron main block CBOR data when CBOR storage is enabled.
     * CBOR data is retrieved from ByronMainBlock.cbor field.
     */
    private void saveByronBlockCbor(ByronMainBlockEvent event, String blockHash, long slot) {
        ByronMainBlock byronBlock = event.getByronMainBlock();

        String cborHex = byronBlock.getCbor();
        if (cborHex == null || cborHex.isEmpty()) {
            log.debug("No CBOR data available for Byron block {} (YaciConfig.INSTANCE.setReturnBlockCbor(true) may not be set)", blockHash);
            return;
        }

        byte[] cborData = HexUtil.decodeHexString(cborHex);

        BlockCbor blockCbor = BlockCbor.builder()
                .blockHash(blockHash)
                .cborData(cborData)
                .cborSize(cborData.length)
                .slot(slot)
                .build();

        blockCborStorage.save(blockCbor);
    }

    /**
     * Save Byron epoch boundary block CBOR data when CBOR storage is enabled.
     * CBOR data is retrieved from ByronEbBlock.cbor field.
     */
    private void saveByronEbBlockCbor(ByronEbBlockEvent event, String blockHash, long slot) {
        ByronEbBlock byronEbBlock = event.getByronEbBlock();

        String cborHex = byronEbBlock.getCbor();
        if (cborHex == null || cborHex.isEmpty()) {
            log.debug("No CBOR data available for Byron EB block {} (YaciConfig.INSTANCE.setReturnBlockCbor(true) may not be set)", blockHash);
            return;
        }

        byte[] cborData = HexUtil.decodeHexString(cborHex);

        BlockCbor blockCbor = BlockCbor.builder()
                .blockHash(blockHash)
                .cborData(cborData)
                .cborSize(cborData.length)
                .slot(slot)
                .build();

        blockCborStorage.save(blockCbor);
    }
}
