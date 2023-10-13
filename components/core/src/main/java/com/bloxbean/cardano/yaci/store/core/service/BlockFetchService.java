package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.core.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.util.SlotLeaderUtil;
import com.bloxbean.cardano.yaci.store.events.*;
import com.bloxbean.cardano.yaci.store.events.domain.TxAuxData;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.events.domain.TxMintBurn;
import com.bloxbean.cardano.yaci.store.events.domain.TxScripts;
import com.bloxbean.cardano.yaci.store.events.internal.BatchBlocksProcessedEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchBlock;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchByronBlock;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.common.util.ListUtil.partition;
import static com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig.DEFAULT_SECURITY_PARAM;

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
    private EraService eraService;

    @Autowired
    private StoreProperties storeProperties;

    @Autowired
    private GenesisConfig genesisConfig;

    @Value("${store.cardano.protocol-magic}")
    private long protocolMagic;

    @Value("${store.block.processing.threads:15}")
    private int blockProcessingThreads;

    @Value("${store.event.processing.threads:30}")
    private int eventProcessingThreads;

    @Value("${store.block.parallel-processing-enabled:false}")
    private boolean enableParallelProcessing;

    private boolean syncMode;

    private AtomicBoolean isError = new AtomicBoolean(false);

    private List<BatchBlock> batchBlockList = new ArrayList<>();
    private List<BatchByronBlock> byronBatchBlockList = new ArrayList<>();

    private ExecutorService executor;
    private ExecutorService eventExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${store.block.processing.batch-size:100}")
    private int BATCH_SIZE;

    @Value("${store.block.processing.partition-size:15}")
    private int partitionSize;

    @Value("${store.use-virtual-thread-for-batch-processing:false}")
    private boolean useVirtualThreadForBatchProcessing = false;

    @Value("${store.use-virtual-thread-for-event-processing:false}")
    private boolean useVirtualThreadForEventProcessing = false;

    private Thread keepAliveThread;

    public BlockFetchService(ApplicationEventPublisher applicationEventPublisher, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.publisher = applicationEventPublisher;
    }

    @PostConstruct
    public void init() {
        if (!enableParallelProcessing)
            return;

        if (useVirtualThreadForBatchProcessing) {
            executor = Executors.newVirtualThreadPerTaskExecutor();
            log.info("Block Batch processing will be done using virtual threads");
        } else {
            executor = Executors.newFixedThreadPool(blockProcessingThreads);
            log.info("Block Batch processing will be done using fixed thread pool of size {}", blockProcessingThreads);
        }

        if (useVirtualThreadForEventProcessing) {
            eventExecutor = Executors.newVirtualThreadPerTaskExecutor();
            log.info("Block Event processing will be done using virtual threads");
        } else {
            eventExecutor = Executors.newFixedThreadPool(eventProcessingThreads);
            log.info("Block Event processing will be done using fixed thread pool of size {}", eventProcessingThreads);
        }
    }

    @Transactional
    @Timed(value = "store.block.process", description = "Block processing time")
    @Counted(value = "store.block.process.count", description = "Block processing count")
    @Override
    public void onBlock(Era era, Block block, List<Transaction> transactions) {
        checkError();

        //Check if there is any pending ByronBlocks to be processed
        if (byronBatchBlockList.size() > 0) {
            processByronMainBlocksInParallel();
            byronBatchBlockList.clear();
        }

        BlockHeader blockHeader = block.getHeader();
        final long slot = blockHeader.getHeaderBody().getSlot();
        boolean byronToShelleyEraChange = eraService.checkIfNewEra(era, blockHeader); //Currently it only looks for Byron to Shelley transition
        final int epochNumber = eraService.getEpochNo(era, slot);
        final int epochSlot = eraService.getShelleyEpochSlot(slot);
        final long blockTime = eraService.blockTime(era, slot);
        final String slotLeader = SlotLeaderUtil.getShelleySlotLeader(blockHeader.getHeaderBody().getIssuerVkey());

        //paralleMode is true when not fully synced and parallel processing is enabled and not the first block of the era
        boolean parallelMode = !syncMode && enableParallelProcessing && !byronToShelleyEraChange;

        EventMetadata eventMetadata = EventMetadata.builder()
                .mainnet(storeProperties.isMainnet())
                .era(era)
                .block(blockHeader.getHeaderBody().getBlockNumber())
                .epochNumber(epochNumber)
                .slotLeader(slotLeader)
                .blockHash(blockHeader.getHeaderBody().getBlockHash())
                .blockTime(blockTime)
                .prevBlockHash(blockHeader.getHeaderBody().getPrevHash())
                .slot(slot)
                .epochSlot(epochSlot)
                .noOfTxs(transactions.size())
                .syncMode(syncMode)
                .parallelMode(parallelMode)
                .build();

        if (!parallelMode) { //For Sync mode, we need to process block by block
            processBlock(block, transactions, blockHeader, eventMetadata);
            publisher.publishEvent(new CommitEvent(List.of(new BatchBlock(eventMetadata, block, transactions))));

            cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(), eventMetadata.getBlock(),
                    eventMetadata.getPrevBlockHash(), eventMetadata.getEra()));
        } else {
            handleBlockBatchInParallel(era, block, transactions, eventMetadata);
        }
    }

    private void handleBlockBatchInParallel(Era era, Block block, List<Transaction> transactions, EventMetadata eventMetadata) {
        batchBlockList.add(new BatchBlock(eventMetadata, block, transactions));
        if (batchBlockList.size() != BATCH_SIZE)
            return;

        List<List<BatchBlock>> partitions = partition(batchBlockList, partitionSize);
        List<CompletableFuture> futures = new ArrayList<>();
        for (List<BatchBlock> partition : partitions) {
            var future = CompletableFuture.supplyAsync(() -> {
                for (BatchBlock blockCache : partition) {
                    processBlock(blockCache.getBlock(), blockCache.getTransactions(), blockCache.getBlock().getHeader(), blockCache.getEventMetadata());
                }

                return true;
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        //Publish BatchProcessedEvent. This may be useful for some schenarios where we need to do some processing before CommitEvent
        publisher.publishEvent(new BatchBlocksProcessedEvent(batchBlockList));
        publisher.publishEvent(new CommitEvent(batchBlockList));

        //Finally Set the cursor
        cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(), eventMetadata.getBlock(),
                eventMetadata.getPrevBlockHash(), eventMetadata.getEra()));

        log.info("Block No: " + eventMetadata.getBlock() + "  , Era: " + era);
        batchBlockList.clear();
    }

    private void processBlock(Block block, List<Transaction> transactions, BlockHeader blockHeader, EventMetadata eventMetadata) {
        try {
            var eraEventCf = publishEventAsync(eventMetadata.getEra());
            var blockEventCf = publishEventAsync(new BlockEvent(eventMetadata, block));
            var blockHeaderEventCf = publishEventAsync(new BlockHeaderEvent(eventMetadata, blockHeader));
            var txnEventCf = publishEventAsync(new TransactionEvent(eventMetadata, transactions));

            //Script Event
            var txScriptEvent = CompletableFuture.supplyAsync(() -> {
                List<TxScripts> txScriptsList = getTxScripts(transactions);
                publisher.publishEvent(new ScriptEvent(eventMetadata, txScriptsList));
                return true;
            }, eventExecutor);

            //AuxData event
            var txAuxDataEvent = CompletableFuture.supplyAsync(() -> {
                List<TxAuxData> txAuxDataList = transactions.stream()
                        .filter(transaction -> transaction.getAuxData() != null)
                        .map(transaction -> TxAuxData.builder()
                                .txHash(transaction.getTxHash())
                                .auxData(transaction.getAuxData())
                                .build()
                        ).collect(Collectors.toList());
                publisher.publishEvent(new AuxDataEvent(eventMetadata, txAuxDataList));
                return true;
            }, eventExecutor);

            //Certificate event
            var txCertificateEvent = CompletableFuture.supplyAsync(() -> {
                List<TxCertificates> txCertificatesList = transactions.stream().map(transaction -> TxCertificates.builder()
                        .txHash(transaction.getTxHash())
                        .certificates(transaction.getBody().getCertificates())
                        .build()
                ).collect(Collectors.toList());
                publisher.publishEvent(new CertificateEvent(eventMetadata, txCertificatesList));
                return true;
            }, eventExecutor);

            //Mints
            var txMintBurnEvent = CompletableFuture.supplyAsync(() -> {
                List<TxMintBurn> txMintBurnEvents = transactions.stream().filter(transaction ->
                                transaction.getBody().getMint() != null && transaction.getBody().getMint().size() > 0)
                        .map(transaction -> new TxMintBurn(transaction.getTxHash(), sanitizeAmounts(transaction.getBody().getMint())))
                        .collect(Collectors.toList());
                publisher.publishEvent(new MintBurnEvent(eventMetadata, txMintBurnEvents));
                return true;
            }, eventExecutor);

            CompletableFuture.allOf(eraEventCf, blockEventCf, blockHeaderEventCf, txnEventCf, txScriptEvent, txAuxDataEvent,
                    txCertificateEvent, txMintBurnEvent).join();
        } catch (Exception e) {
            log.error("Error saving : " + eventMetadata, e);
            log.error("Stopping fetcher");
            log.error("Error at block no #" + blockHeader.getHeaderBody().getBlockNumber());
            stopSyncOnError();
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<Boolean> publishEventAsync(Object event) {
        return CompletableFuture.supplyAsync(() -> {
            publisher.publishEvent(event);
            return true;
        }, eventExecutor);
    }

    //Replace asset name contains \u0000 -- postgres can't convert this to text. so replace
    private List<Amount> sanitizeAmounts(List<Amount> amounts) {
        if (amounts == null) return Collections.EMPTY_LIST;
        //Fix -- some asset name contains \u0000 -- postgres can't convert this to text. so replace
        return amounts.stream().map(amount ->
                Amount.builder()
                        .unit(amount.getUnit() != null ? amount.getUnit().replace(".", "") : null)
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
        checkError();
        try {
            long epochSlot = byronBlock.getHeader().getConsensusData().getSlotId().getSlot();
            final long absoluteSlot = genesisConfig.absoluteSlot(Era.Byron,
                    byronBlock.getHeader().getConsensusData().getSlotId().getEpoch(),
                    epochSlot);
            final long epochNumber = byronBlock.getHeader().getConsensusData().getSlotId().getEpoch();
            final long blockTime = eraService.blockTime(Era.Byron, absoluteSlot);

            long blockNumber = byronBlock.getHeader().getConsensusData().getDifficulty().longValue();
            final String slotLeader = SlotLeaderUtil
                    .getByronSlotLeader(byronBlock.getHeader().getConsensusData().getPubKey());

            //paralleMode is true when not fully synced and parallel processing is enabled
            boolean parallelMode = !syncMode && enableParallelProcessing;

            EventMetadata eventMetadata = EventMetadata.builder()
                    .mainnet(storeProperties.isMainnet())
                    .era(Era.Byron)
                    .block(blockNumber)
                    .blockHash(byronBlock.getHeader().getBlockHash())
                    .blockTime(blockTime)
                    .prevBlockHash(byronBlock.getHeader().getPrevBlock())
                    .epochNumber((int) epochNumber)
                    .slotLeader(slotLeader)
                    .slot(absoluteSlot)
                    .epochSlot(epochSlot)
                    .syncMode(syncMode)
                    .parallelMode(parallelMode)
                    .build();

            if (!parallelMode) { //For Sync mode, we need to process block by block
                ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(eventMetadata, byronBlock);

                publisher.publishEvent(byronMainBlockEvent);
                publisher.publishEvent(new CommitEvent<>(List.of(new BatchByronBlock(eventMetadata, byronBlock))));

                //Finally Set the cursor
                cursorService.setCursor(new Cursor(absoluteSlot, eventMetadata.getBlockHash(),
                        eventMetadata.getBlock(), eventMetadata.getPrevBlockHash(), eventMetadata.getEra()));
            } else {
                byronBatchBlockList.add(new BatchByronBlock(eventMetadata, byronBlock));
                if (byronBatchBlockList.size() != BATCH_SIZE)
                    return;

                processByronMainBlocksInParallel();

                log.info("Block No: " + eventMetadata.getBlock() + "  , Era: " + Era.Byron);
            }

        } catch (Exception e) {
            log.error("Error saving : Slot >>" + byronBlock.getHeader().getConsensusData().getSlotId(), e);
            log.error("Error at block hash #" + byronBlock.getHeader().getBlockHash());
            log.error("Stopping fetcher");
            byronBatchBlockList.clear();
            stopSyncOnError();
            throw new RuntimeException(e);
        }
    }

    private void processByronMainBlocksInParallel() {
        List<List<BatchByronBlock>> partitions = partition(byronBatchBlockList, partitionSize);

        List<CompletableFuture> futures = new ArrayList<>();
        for (List<BatchByronBlock> partition : partitions) {
            var future = CompletableFuture.supplyAsync(() -> {
                for (BatchByronBlock blockCache : partition) {
                    ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(blockCache.getEventMetadata(), blockCache.getBlock());
                    publisher.publishEvent(byronMainBlockEvent);
                }
                return true;
            });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        publisher.publishEvent(new CommitEvent(byronBatchBlockList));

        BatchByronBlock lastBlockCache = byronBatchBlockList.getLast();
        cursorService.setCursor(new Cursor(lastBlockCache.getEventMetadata().getSlot(), lastBlockCache.getEventMetadata().getBlockHash(),
                lastBlockCache.getEventMetadata().getBlock(), lastBlockCache.getEventMetadata().getPrevBlockHash(), lastBlockCache.getEventMetadata().getEra()));

        byronBatchBlockList.clear();
    }

    @Transactional
    @Override
    public void onByronEbBlock(ByronEbBlock byronEbBlock) {
        checkError();
        try {
            final long absoluteSlot = genesisConfig.absoluteSlot(Era.Byron,
                    byronEbBlock.getHeader().getConsensusData().getEpoch(), 0);
            final long epochNumber = byronEbBlock.getHeader().getConsensusData().getEpoch();
            final long blockTime = eraService.blockTime(Era.Byron, absoluteSlot);

            long blockNumber = byronEbBlock.getHeader().getConsensusData().getDifficulty().longValue();

            EventMetadata eventMetadata = EventMetadata.builder()
                    .mainnet(storeProperties.isMainnet())
                    .era(Era.Byron)
                    .block(blockNumber)
                    .blockHash(byronEbBlock.getHeader().getBlockHash())
                    .blockTime(blockTime)
                    .prevBlockHash(byronEbBlock.getHeader().getPrevBlock())
                    .epochNumber((int) epochNumber)
                    .slot(absoluteSlot)
                    .epochSlot(0)
                    .syncMode(syncMode)
                    .build();

            publisher.publishEvent(new ByronEbBlockEvent(eventMetadata, byronEbBlock));

            //Finally Set the cursor
            cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(),
                    eventMetadata.getBlock(), eventMetadata.getPrevBlockHash(), eventMetadata.getEra()));
        } catch (Exception e) {
            log.error("Error saving EbBlock : epoch >>" + byronEbBlock.getHeader().getConsensusData().getEpoch(), e);
            log.error("Error at block hash #" + byronEbBlock.getHeader().getBlockHash());
            log.error("Stopping fetcher");
            stopSyncOnError();
            throw new RuntimeException(e);
        }
    }

    @EventListener
    @Transactional
    public void handleGenesisBlockEvent(GenesisBlockEvent genesisBlockEvent) {
        checkError();
        log.info("Writing genesis block to cursor -->");
        cursorService.setCursor(new Cursor(genesisBlockEvent.getSlot(), genesisBlockEvent.getBlockHash(), 0L, null, genesisBlockEvent.getEra()));
        if (genesisBlockEvent.getEra().getValue() > Era.Byron.getValue()) { //If Genesis block is not byron era. Possible for preview and local devnet
            eraService.saveEra(genesisBlockEvent.getEra(), genesisBlockEvent.getSlot(), genesisBlockEvent.getBlockHash(), genesisBlockEvent.getBlock());
        }
    }

    private void stopSyncOnError() {
        setError();
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

        log.info("Current cursor point >> " + currentPoint);
        if ((point.getSlot() == 0 && point.getHash() == null)
                || (currentPoint.getSlot() - point.getSlot()) > DEFAULT_SECURITY_PARAM) {
            log.error("Rollback point doesn't seem to be valid. Ignoring rollback event to slot. " + point);
            return;
        }

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

            if (storeProperties.isPrimaryInstance()) {
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

    public synchronized void startFetch(Point from, Point to) {
        stopKeepAliveThread();
        blockRangeSync.restart(this);
        blockRangeSync.fetch(from, to);
        syncMode = false;
        cursorService.setSyncMode(syncMode);

        startKeepAliveThread();
    }

    public synchronized void startSync(Point from) {
        stopKeepAliveThread();
        blockSync.startSync(from, this);
        syncMode = true;
        cursorService.setSyncMode(syncMode);
    }

    public synchronized void shutdown() {
        blockRangeSync.stop();
    }

    public synchronized void shutdownSync() {
        blockSync.stop();
    }

    private void setError() {
        isError.set(true);
    }

    private void checkError() {
        if (isError.get())
            throw new IllegalStateException("Fetcher has already been stopped due to error.");
    }

    private synchronized void stopKeepAliveThread() {
        try {
            if (keepAliveThread != null && keepAliveThread.isAlive())
                keepAliveThread.interrupt();
        } catch (Exception e) {
            log.error("Error stopping keep alive thread", e);
        }
    }

    private synchronized void startKeepAliveThread() {
        stopKeepAliveThread();
        keepAliveThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                    int randomNo = getRandomNumber(0, 60000);
                    blockRangeSync.sendKeepAliveMessage(randomNo);
                } catch (InterruptedException e) {
                    log.info("Keep alive thread interrupted");
                    break;
                }
            }
        });
        keepAliveThread.start();
    }

    private int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}
