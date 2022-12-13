package com.bloxbean.cardano.yaci.store.service;

import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.domain.Cursor;
import com.bloxbean.cardano.yaci.store.events.*;
import com.bloxbean.cardano.yaci.store.events.domain.TxAuxData;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.events.domain.TxMintBurn;
import com.bloxbean.cardano.yaci.store.events.domain.TxScripts;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
                .noOfTxs(transactions.size())
                .isSyncMode(syncMode)
                .build();

        try {
            publisher.publishEvent(era);
            publisher.publishEvent(new BlockHeaderEvent(eventMetadata, blockHeader));
            publisher.publishEvent(new TransactionEvent(eventMetadata, transactions));

            //Addtional events
            //TxScript Event
            List<TxScripts> txScriptsList = getTxScripts(transactions);
            publisher.publishEvent(new ScriptEvent(eventMetadata, txScriptsList));

            //AuxData event
            List<TxAuxData> txAuxDataList = transactions.stream()
                    .filter(transaction -> transaction.getAuxData() != null)
                    .map(transaction -> TxAuxData.builder()
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

            //Mints
            List<TxMintBurn> txMintBurnEvents = transactions.stream().filter(transaction ->
                            transaction.getBody().getMint() != null && transaction.getBody().getMint().size() > 0)
                    .map(transaction -> new TxMintBurn(transaction.getTxHash(), sanitizeAmounts(transaction.getBody().getMint())))
                    .collect(Collectors.toList());
            publisher.publishEvent(new MintBurnEvent(eventMetadata, txMintBurnEvents));

            //Finally Set the cursor
            cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(),
                    eventMetadata.getBlock()));
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

    //Replace asset name contains \u0000 -- postgres can't convert this to text. so replace
    private List<Amount> sanitizeAmounts(List<Amount> amounts) {
        if (amounts == null) return Collections.EMPTY_LIST;
        //Fix -- some asset name contains \u0000 -- postgres can't convert this to text. so replace
        return amounts.stream().map(amount ->
                Amount.builder()
                        .unit(amount.getUnit())
                        .policyId(amount.getPolicyId())
                        .assetName(amount.getAssetName().replace('\u0000', ' '))
                        .quantity(amount.getQuantity())
                        .build()).collect(Collectors.toList());
    }

    private List<TxScripts> getTxScripts(List<Transaction> transactions) {
        List<TxScripts> txScriptsList = transactions.stream().map(transaction -> TxScripts.builder()
                .txHash(transaction.getTxHash())
                .plutusV1Scripts(transaction.getWitnesses().getPlutusV1Scripts())
                .plutusV2Scripts(transaction.getWitnesses().getPlutusV2Scripts())
                .nativeScripts(transaction.getWitnesses().getNativeScripts())
                .datums(transaction.getWitnesses().getDatums())
                .redeemers(transaction.getWitnesses().getRedeemers())
                .build()
        ).collect(Collectors.toList());
        return txScriptsList;
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

        //Finally Set the cursor
        cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(),
                eventMetadata.getBlock()));
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
        Optional<Cursor> cursorOptional = cursorService.getCursor();
        Point currentPoint = cursorOptional
                .map(cursor -> new Point(cursor.getSlot(), cursor.getBlockHash()))
                .orElse(null);
        long currentBlockNum = cursorOptional.map(cursor -> cursor.getBlock())
                .orElse(null);

        //Reset cursor
        cursorService.setCursor(Cursor.builder()
                .slot(point.getSlot())
                .blockHash(point.getHash())
                .block(null)
                .build());

        //Publish rollback event
        RollbackEvent rollbackEvent = RollbackEvent
                .builder()
                .rollbackTo(point)
                .currentPoint(currentPoint)
                .currentBlock(currentBlockNum)
                .build();
        log.info("Publishing rollback event : " + rollbackEvent);
        publisher.publishEvent(rollbackEvent);
    }

    @Override
    public void batchDone() {

        if (!syncMode) {
            log.info("Batch Done >>>");
            //start sync
            cursorService.getCursor()
                    .ifPresent(cursor -> {
                        String blockHash = cursor.getBlockHash();
                        long slot = cursor.getSlot();

                        log.info("Start N2N sync from block >> " + cursor.getBlock());
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
