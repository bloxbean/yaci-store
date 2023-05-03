package com.bloxbean.cardano.yaci.store.core.service;

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
import com.bloxbean.cardano.yaci.store.core.configuration.EpochConfig;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.configuration.StoreConfig;
import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.events.*;
import com.bloxbean.cardano.yaci.store.events.domain.TxAuxData;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.events.domain.TxMintBurn;
import com.bloxbean.cardano.yaci.store.events.domain.TxScripts;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BlockFetchService implements BlockChainDataListener {
    private final ApplicationEventPublisher publisher;

    private MeterRegistry meterRegistry;

    @Autowired
    private BlockRangeSync blockRangeSync;

    @Autowired
    private BlockSync blockSync;

    @Autowired
    private CursorService cursorService;

    @Autowired
    private StoreConfig storeConfig;

    @Autowired
    private GenesisConfig genesisConfig;

    @Autowired
    private EpochConfig epochConfig;

    @Value("${store.cardano.protocol-magic}")
    private long protocolMagic;


    private boolean syncMode;


    public BlockFetchService(ApplicationEventPublisher applicationEventPublisher, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.publisher = applicationEventPublisher;
        Counter counter = this.meterRegistry.counter("blocks.processed");
    }

    @Transactional
    @Override
    public void onTransactions(Era era, BlockHeader blockHeader, List<Transaction> transactions) {
        final long slot = blockHeader.getHeaderBody().getSlot();
        final int epochNumber = epochConfig.epochFromSlot(protocolMagic, era, slot);

        EventMetadata eventMetadata = EventMetadata.builder()
                .era(era)
                .block(blockHeader.getHeaderBody().getBlockNumber())
                .epochNumber(epochNumber)
                .blockHash(blockHeader.getHeaderBody().getBlockHash())
                .slot(slot)
                .noOfTxs(transactions.size())
                .syncMode(syncMode)
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
                    eventMetadata.getBlock(), eventMetadata.getEra()));
        } catch (Exception e) {
            log.error("Error saving : " + eventMetadata, e);
            log.error("Stopping fetcher");
            log.error("Error at block no #" + blockHeader.getHeaderBody().getBlockNumber());
            stopSync();
            throw new RuntimeException(e);
        }
    }

    //Replace asset name contains \u0000 -- postgres can't convert this to text. so replace
    private List<Amount> sanitizeAmounts(List<Amount> amounts) {
        if (amounts == null) return Collections.EMPTY_LIST;
        //Fix -- some asset name contains \u0000 -- postgres can't convert this to text. so replace
        return amounts.stream().map(amount ->
                Amount.builder()
                        .unit(amount.getUnit() != null? amount.getUnit().replace(".", ""): null)
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
        try {
            long absoluteSlot = genesisConfig.absoluteSlot(Era.Byron,
                    byronBlock.getHeader().getConsensusData().getSlotId().getEpoch(),
                    byronBlock.getHeader().getConsensusData().getSlotId().getSlot());
            final int epochNumber = epochConfig.epochFromSlot(protocolMagic, Era.Byron, absoluteSlot);

            EventMetadata eventMetadata = EventMetadata.builder()
                    .era(Era.Byron)
                    .block(-1)
                    .blockHash(byronBlock.getHeader().getBlockHash())
                    .epochNumber(epochNumber)
                    .slot(absoluteSlot)
                    .syncMode(syncMode)
                    .build();

            ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(eventMetadata, byronBlock);
            publisher.publishEvent(byronMainBlockEvent);

            //Finally Set the cursor
            cursorService.setByronEraCursor(byronBlock.getHeader().getPrevBlock(), new Cursor(absoluteSlot, eventMetadata.getBlockHash(),
                    eventMetadata.getBlock(), eventMetadata.getEra()));
        } catch (Exception e) {
            log.error("Error saving : Slot >>" + byronBlock.getHeader().getConsensusData().getSlotId(), e);
            log.error("Error at block hash #" + byronBlock.getHeader().getBlockHash());
            log.error("Stopping fetcher");
            stopSync();
            throw new RuntimeException(e);
        }
    }

    @Transactional
    @Override
    public void onByronEbBlock(ByronEbBlock byronEbBlock) {
        try {
            long absoluteSlot = genesisConfig.absoluteSlot(Era.Byron,
                    byronEbBlock.getHeader().getConsensusData().getEpoch(), 0);
            final int epochNumber = epochConfig.epochFromSlot(protocolMagic, Era.Byron, absoluteSlot);

            EventMetadata eventMetadata = EventMetadata.builder()
                    .era(Era.Byron)
                    .block(-1)
                    .blockHash(byronEbBlock.getHeader().getBlockHash())
                    .epochNumber(epochNumber)
                    .slot(absoluteSlot)
                    .syncMode(syncMode)
                    .build();

            publisher.publishEvent(new ByronEbBlockEvent(eventMetadata, byronEbBlock));

            //Finally Set the cursor
            cursorService.setByronEraCursor(byronEbBlock.getHeader().getPrevBlock(), new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(),
                    eventMetadata.getBlock(), eventMetadata.getEra()));
        } catch (Exception e) {
            log.error("Error saving EbBlock : epoch >>" + byronEbBlock.getHeader().getConsensusData().getEpoch(), e);
            log.error("Error at block hash #" + byronEbBlock.getHeader().getBlockHash());
            log.error("Stopping fetcher");
            stopSync();
            throw new RuntimeException(e);
        }
    }

    @EventListener
    @Transactional
    public void handleGenesisBlockEvent(GenesisBlockEvent genesisBlockEvent) {
        log.info("Writing genesis block to cursor -->");
        cursorService.setCursor(new Cursor(genesisBlockEvent.getSlot(), genesisBlockEvent.getBlockHash(), 0L, null));
    }

    private void stopSync() {
        if (blockRangeSync != null)
            blockRangeSync.stop();
        if (blockSync != null)
            blockSync.stop();
    }

    @Transactional
    @Override
    public void onRollback(Point point) {
        Optional<Cursor> cursorOptional = cursorService.getCursor();
        Point currentPoint = cursorOptional
                .map(cursor -> new Point(cursor.getSlot(), cursor.getBlockHash()))
                .orElse(null);
        long currentBlockNum = cursorOptional.map(cursor -> cursor.getBlock())
                .orElse(-1L);

        cursorService.rollback(point.getSlot());

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

            if (storeConfig.getPrimaryInstance()) {
                //If primary instance, start sync
                //start sync
                cursorService.getCursor()
                        .ifPresent(cursor -> {
                            String blockHash = cursor.getBlockHash();
                            long slot = cursor.getSlot();

                            log.info("Start N2N sync from block >> " + cursor.getBlock());
                            //Start sync
                            startSync(new Point(slot, blockHash));
                        });
            } else {
                log.info("Blockfetch done");
            }
        }
    }

    public void startFetch(Point from, Point to) {
        blockRangeSync.restart(this);
        blockRangeSync.fetch(from, to);
        syncMode = false;
        cursorService.setSyncMode(syncMode);
    }

    public void startSync(Point from) {
        blockSync.startSync(from, this);
        syncMode = true;
        cursorService.setSyncMode(syncMode);
    }

    public void shutdown() {
        blockRangeSync.stop();
    }

    public void shutdownSync() {
        blockSync.stop();
    }
}
