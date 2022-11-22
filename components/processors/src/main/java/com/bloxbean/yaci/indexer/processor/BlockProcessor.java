package com.bloxbean.yaci.indexer.processor;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.indexer.entity.BlockEntity;
import com.bloxbean.cardano.yaci.indexer.entity.Vrf;
import com.bloxbean.cardano.yaci.indexer.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.indexer.repository.BlockRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class BlockProcessor {

    private BlockRepository blockRepository;
    private AtomicInteger count;

    public BlockProcessor(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
        count = new AtomicInteger(0);
    }

    @EventListener
    @Order(1)
    public void handleBlockHeaderEvent(@NonNull BlockHeaderEvent blockHeaderEvent) {
        BlockHeader blockHeader = blockHeaderEvent.getBlockHeader();
        BlockEntity block = BlockEntity.builder()
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
                .build();
        blockRepository.save(block);

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
}
