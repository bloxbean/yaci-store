package com.bloxbean.cardano.yaci.indexer.service;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.indexer.blocks.repository.BlockRepository;
import com.bloxbean.cardano.yaci.indexer.events.*;
import com.bloxbean.cardano.yaci.indexer.events.model.TxAuxData;
import com.bloxbean.cardano.yaci.indexer.events.model.TxCertificates;
import com.bloxbean.cardano.yaci.indexer.events.model.TxScripts;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BlockFetchService implements BlockChainDataListener {
    private final ApplicationEventPublisher publisher;

    private MeterRegistry meterRegistry;

    private AtomicInteger count;

    @Autowired
    private BlockRangeSync blockRangeSync;

    @Autowired
    private BlockSync blockSync;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private CursorService cursorService;

    private boolean syncMode;


    public BlockFetchService(ApplicationEventPublisher applicationEventPublisher, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.publisher = applicationEventPublisher;
        Counter counter = this.meterRegistry.counter("blocks.processed");
        count = new AtomicInteger(0);
    }

    @Transactional
    @Override
    public void onTransactions(Era era, BlockHeader blockHeader, List<Transaction> transactions) {
        EventMetadata eventMetadata = EventMetadata.builder()
                .era(era)
                .block(blockHeader.getHeaderBody().getBlockNumber())
                .blockHash(blockHeader.getHeaderBody().getBlockHash())
                .slot(blockHeader.getHeaderBody().getSlot())
                .isSyncMode(syncMode)
                .build();
        cursorService.setEra(era.getValue());
        cursorService.setSlot(eventMetadata.getSlot());
        cursorService.setBlockNumber(eventMetadata.getBlock());
        cursorService.setBlockHash(eventMetadata.getBlockHash());

        try {
            publisher.publishEvent(era);
            publisher.publishEvent(new BlockHeaderEvent(eventMetadata, blockHeader));
            publisher.publishEvent(new TransactionEvent(eventMetadata, transactions));

            //Addtional events
            //TxScript Event
            List<TxScripts> txScriptsList = transactions.stream().map(transaction -> TxScripts.builder()
                    .txHash(transaction.getTxHash())
                    .plutusV1Scripts(transaction.getWitnesses().getPlutusV1Scripts())
                    .plutusV2Scripts(transaction.getWitnesses().getPlutusV2Scripts())
                    .nativeScripts(transaction.getWitnesses().getNativeScripts())
                    .datums(transaction.getWitnesses().getDatums())
                    .redeemers(transaction.getWitnesses().getRedeemers())
                    .build()
            ).collect(Collectors.toList());
            publisher.publishEvent(new ScriptEvent(eventMetadata, txScriptsList));

            //AuxData event
            List<TxAuxData> txAuxDataList = transactions.stream().map(transaction -> TxAuxData.builder()
                    .txHash(transaction.getTxHash())
                    .auxData(transaction.getAuxData())
                    .build()
            ).collect(Collectors.toList());
            publisher.publishEvent(new AuxDataEvent(eventMetadata, txAuxDataList));

            //Certificate event
            List<TxCertificates> txCertificatesList = transactions.stream().map(transaction -> TxCertificates.builder()
                    .txHash(transaction.getTxHash())
                    .certificates(transaction.getBody().getCertificates())
                    .build()
            ).collect(Collectors.toList());
            publisher.publishEvent(new CertificateEvent(eventMetadata, txCertificatesList));

        } catch (Exception e) {
            log.error("Error saving", e);
            log.error("Stopping fetcher");
            log.error("Error at block no #" + blockHeader.getHeaderBody().getBlockNumber());
            if (blockRangeSync != null)
                blockRangeSync.stop();
            if (blockSync != null)
                blockSync.stop();
            throw e;
        }
    }

    @Transactional
    @Override
    public void onByronBlock(ByronMainBlock byronBlock) {
        EventMetadata eventMetadata = EventMetadata.builder()
                .era(Era.Byron)
                .block(-1)
                .blockHash(byronBlock.getHeader().getBlockHash())
                .slot(byronBlock.getHeader().getConsensusData().getSlotId().getSlot())
                .isSyncMode(syncMode)
                .build();

        ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(eventMetadata, byronBlock);
        publisher.publishEvent(byronMainBlockEvent);
    }

    @Transactional
    @Override
    public void onByronEbBlock(ByronEbBlock byronEbBlock) {
        EventMetadata eventMetadata = EventMetadata.builder()
                .era(Era.Byron)
                .block(-1)
                .blockHash(byronEbBlock.getHeader().getBlockHash())
                .slot(0)
                .isSyncMode(syncMode)
                .build();

        publisher.publishEvent(new ByronEbBlockEvent(eventMetadata, byronEbBlock));
    }

    @Override
    public void onRollback(Point point) {

    }

    @Override
    public void batchDone() {

        if (!syncMode) {
            log.info("Batch Done >>>");
            //start sync
            blockRepository.findTopByOrderByBlockDesc()
                    .ifPresent(blockEntity -> {
                        String blockHash = blockEntity.getBlockHash();
                        long slot = blockEntity.getSlot();

                        log.info("Start N2N sync from block >> " + blockEntity.getBlock());
                        //Start sync
                        startSync(new Point(slot, blockHash));
                    });
        }
    }

    public void startFetch(Point from, Point to) {
        blockRangeSync.restart(this);
        blockRangeSync.fetch(from, to);
        syncMode = false;
    }

    public void startSync(Point from) {
        blockSync.startSync(from, this);
        syncMode = true;
    }

    public void shutdown() {
        blockRangeSync.stop();
    }

    public void shutdownSync() {
        blockSync.stop();
    }
}
