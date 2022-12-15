package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.bloxbean.cardano.yaci.store.blocks.persistence.BlockPersistence;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.sun.istack.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class BlockProcessor {

    private BlockPersistence blockPersistence;
    private AtomicInteger count;

    public BlockProcessor(BlockPersistence blockPersistence) {
        this.blockPersistence = blockPersistence;
        count = new AtomicInteger(0);
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleBlockHeaderEvent(@NonNull BlockHeaderEvent blockHeaderEvent) {
        BlockHeader blockHeader = blockHeaderEvent.getBlockHeader();
        Block block = Block.builder()
                .blockHash(blockHeader.getHeaderBody().getBlockHash())
                .block(blockHeader.getHeaderBody().getBlockNumber())
                .slot(blockHeader.getHeaderBody().getSlot())
                .era(blockHeaderEvent.getMetadata().getEra().getValue())
                .prevHash(blockHeader.getHeaderBody().getPrevHash())
                .issuerVkey(blockHeader.getHeaderBody().getIssuerVkey())
                .vrfVkey(blockHeader.getHeaderBody().getVrfVkey())
                .nonceVrf(Vrf.from(blockHeader.getHeaderBody().getNonceVrf()))
                .leaderVrf(Vrf.from(blockHeader.getHeaderBody().getLeaderVrf()))
                .vrfResult(Vrf.from(blockHeader.getHeaderBody().getVrfResult()))
                .blockBodySize(blockHeader.getHeaderBody().getBlockBodySize())
                .blockBodyHash(blockHeader.getHeaderBody().getBlockBodyHash())
                .protocolVersion(blockHeader.getHeaderBody().getProtocolVersion().get_1()
                        + "." + blockHeader.getHeaderBody().getProtocolVersion().get_2())
                .noOfTxs(blockHeaderEvent.getMetadata().getNoOfTxs())
                .build();
        blockPersistence.save(block);

        count.incrementAndGet();
        double val = count.get() % 5000;

        if (!blockHeaderEvent.getMetadata().isSyncMode()) {
            if (val == 0) {
                log.info("# of blocks written: " + count.get());
                log.info("Block No: " + blockHeader.getHeaderBody().getBlockNumber() + "  , Era: " + blockHeaderEvent.getMetadata().getEra());
            }

        } else {
            log.info("# of blocks written: " + count.get());
            log.info("Block No: " + blockHeader.getHeaderBody().getBlockNumber());
        }
    }

    @EventListener
    @Order(1)
    @Transactional
    //TODO -- add test
    public void handleRollbackEvent(@NotNull RollbackEvent rollbackEvent) {
        int count = blockPersistence.deleteAllBeforeSlot(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} block records", count);
    }
}
