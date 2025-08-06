package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.exception.BlockParseRuntimeException;
import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.util.ErrorCode;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.util.SlotLeaderUtil;
import com.bloxbean.cardano.yaci.store.events.*;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig.DEFAULT_SECURITY_PARAM;

@Component
@ReadOnly(false)
@RequiredArgsConstructor
@Slf4j
public class BlockFetchService implements BlockChainDataListener {
    private final ApplicationEventPublisher publisher;
    private final MetricsService metricsService;
    private final BlockRangeSync blockRangeSync;
    private final BlockSync blockSync;
    private final CursorService cursorService;
    private final EraService eraService;
    private final StoreProperties storeProperties;
    private final GenesisConfig genesisConfig;
    private final ShelleyBlockEventPublisher postShelleyBlockEventPublisher;
    private final ByronBlockEventPublisher byronBlockEventPublisher;

    private boolean syncMode;
    private AtomicBoolean isError = new AtomicBoolean(false);
    private AtomicBoolean scheduledToStop = new AtomicBoolean(false);
    private Thread keepAliveThread;

    //Required to publish EpochChangeEvent
    private Integer previousEpoch;
    private Era previousEra;

    @Getter
    private long lastReceivedBlockTime;

    @Transactional
    @Override
    public void onBlock(Era era, Block block, List<Transaction> transactions) {
        if (scheduledToStop.get()) {
            log.debug("Stopping BlockFetchService as scheduled");
            return;
        }

        checkError();
        lastReceivedBlockTime = System.currentTimeMillis();
        byronBlockEventPublisher.processBlocksInParallel();

        final BlockHeader blockHeader = block.getHeader();
        final long slot = blockHeader.getHeaderBody().getSlot();
        final boolean newEra = eraService.checkIfNewEra(era, blockHeader);
        final int epochNumber = eraService.getEpochNo(era, slot);
        final int epochSlot = eraService.getShelleyEpochSlot(slot);
        final long blockTime = eraService.blockTime(era, slot);
        final String slotLeader = SlotLeaderUtil.getShelleySlotLeader(blockHeader.getHeaderBody().getIssuerVkey());
        final boolean newEpoch = detectIfNewEpoch(epochNumber, slot);

        //paralleMode is true when not fully synced and parallel processing is enabled
        boolean parallelMode = !syncMode && storeProperties.isEnableParallelProcessing();

        EventMetadata eventMetadata = EventMetadata.builder()
                .mainnet(storeProperties.isMainnet())
                .protocolMagic(storeProperties.getProtocolMagic())
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


        try {
            if (newEra || newEpoch) { //Also add epoch change
                log.info("Publish EpochChangeEvent >>>");
                //Incase of era change. Process all pending blocks and then process era change block first
                if (parallelMode) {
                    postShelleyBlockEventPublisher.processBlocksInParallel();
                }

                //Publish epoch change event first and then process first block of the epoch
                publishEpochChangeEvent(eventMetadata);

                //Now process first block
                log.info("Processing first block of the epoch {}, block {}", eventMetadata.getEpochNumber(), eventMetadata.getBlock());
                postShelleyBlockEventPublisher.publishBlockEvents(eventMetadata, block, transactions);
            } else {
                if (parallelMode) {
                    postShelleyBlockEventPublisher.publishBlockEventsInParallel(eventMetadata, block, transactions);
                } else {
                    postShelleyBlockEventPublisher.publishBlockEvents(eventMetadata, block, transactions);
                }
            }

            previousEpoch = eventMetadata.getEpochNumber();
            previousEra = eventMetadata.getEra();

        } catch (Exception e) {
            log.error("Error saving : " + eventMetadata, e);
            log.error("Stopping fetcher");
            log.error("Error at block no #" + blockHeader.getHeaderBody().getBlockNumber());
            stopSyncOnError();
            throw new RuntimeException(e);
        }

        //Update metrics
        try {
            metricsService.updateMetrics(eventMetadata);
            metricsService.updateLastReceivedBlockTime(lastReceivedBlockTime);
        } catch (Exception e) {
            log.warn("Error updating metrics for block: " + block.getHeader().getHeaderBody().getBlockNumber(), e);
        }
    }

    @Transactional
    @Override
    public void onByronBlock(ByronMainBlock byronBlock) {
        checkError();
        lastReceivedBlockTime = System.currentTimeMillis();
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
            final boolean newEpoch = detectIfNewEpochByronEra((int)epochNumber);

            //paralleMode is true when not fully synced and parallel processing is enabled
            boolean parallelMode = !syncMode && storeProperties.isEnableParallelProcessing();

            EventMetadata eventMetadata = EventMetadata.builder()
                    .mainnet(storeProperties.isMainnet())
                    .protocolMagic(storeProperties.getProtocolMagic())
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

            if (newEpoch) {
                if (parallelMode) {
                    byronBlockEventPublisher.processBlocksInParallel();
                }

                log.info("Publish EpochChangeEvent >>>");
                //Publish epoch change event
                publishEpochChangeEvent(eventMetadata);

                log.info("Processing first block of the epoch {}, block {}", eventMetadata.getEpochNumber(), eventMetadata.getBlock());
                //Process single block (First block of epoch)
                byronBlockEventPublisher.publishBlockEvents(eventMetadata, byronBlock, Collections.emptyList());

            } else {
                if (parallelMode) {
                    byronBlockEventPublisher.publishBlockEventsInParallel(eventMetadata, byronBlock, Collections.emptyList());
                } else {
                    byronBlockEventPublisher.publishBlockEvents(eventMetadata, byronBlock, Collections.emptyList());
                }
            }

            previousEpoch = eventMetadata.getEpochNumber();
            previousEra = eventMetadata.getEra();

        } catch (Exception e) {
            log.error("Error saving : Slot >>" + byronBlock.getHeader().getConsensusData().getSlotId(), e);
            log.error("Error at block hash #" + byronBlock.getHeader().getBlockHash());
            log.error("Stopping fetcher");
            stopSyncOnError();
            throw new RuntimeException(e);
        }
    }

    @Transactional
    @Override
    public void onByronEbBlock(ByronEbBlock byronEbBlock) {
        checkError();
        lastReceivedBlockTime = System.currentTimeMillis();
        try {
            final long absoluteSlot = genesisConfig.absoluteSlot(Era.Byron,
                    byronEbBlock.getHeader().getConsensusData().getEpoch(), 0);
            final long epochNumber = byronEbBlock.getHeader().getConsensusData().getEpoch();
            final long blockTime = eraService.blockTime(Era.Byron, absoluteSlot);

            long blockNumber = byronEbBlock.getHeader().getConsensusData().getDifficulty().longValue();

            EventMetadata eventMetadata = EventMetadata.builder()
                    .mainnet(storeProperties.isMainnet())
                    .protocolMagic(storeProperties.getProtocolMagic())
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
        lastReceivedBlockTime = System.currentTimeMillis();
        log.info("Writing genesis block to cursor -->");
        cursorService.setCursor(new Cursor(genesisBlockEvent.getSlot(), genesisBlockEvent.getBlockHash(), 0L, null, genesisBlockEvent.getEra()));
        if (genesisBlockEvent.getEra().getValue() > Era.Byron.getValue()) { //If Genesis block is not byron era. Possible for preview and local devnet
            eraService.saveEra(genesisBlockEvent.getEra(), 0, genesisBlockEvent.getBlockHash(), 0);
        }
    }

    private void stopSyncOnError() {
        setError();
        if (blockRangeSync != null)
            blockRangeSync.stop();
        if (blockSync != null)
            blockSync.stop();
    }

    public void stop() {
        scheduledToStop.set(true);
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

        //Publish Pre-rollback event
        PreRollbackEvent preRollbackEvent = PreRollbackEvent
                .builder()
                .rollbackTo(point)
                .currentPoint(currentPoint)
                .currentBlock(currentBlockNum)
                .build();
        log.info("Publishing pre-rollback event : " + preRollbackEvent);
        publisher.publishEvent(preRollbackEvent);

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

    @Override
    public void onParsingError(BlockParseRuntimeException e) {
        log.error("Block parsing error", e);
        if (storeProperties.isContinueOnParseError()) {
            log.info("Continue on parse error is enabled. Continuing sync ...");
            String cbor = null;
            if (e.getBlockCbor() != null) {
                try {
                    cbor = HexUtil.encodeHexString(e.getBlockCbor());
                } catch (Exception ex) {
                    log.error("Error encoding block cbor to hex string", ex);
                }
            }
            ErrorEvent errorEvent = ErrorEvent.builder()
                    .block(e.getBlockNumber())
                    .errorCode(ErrorCode.BLOCK_PARSE_ERROR.name())
                    .reason(e.getMessage())
                    .details(cbor)
                    .build();
            publisher.publishEvent(errorEvent);
        } else {
            stopSyncOnError();
            throw new RuntimeException(e);
        }
    }

    public synchronized void startFetch(Point from, Point to) {
        isError.set(false);
        scheduledToStop.set(false);

        stopKeepAliveThread();
        blockRangeSync.restart(this);
        blockRangeSync.fetch(from, to);
        syncMode = false;
        cursorService.setSyncMode(syncMode);

        startKeepAliveThread(syncMode);
    }

    public synchronized void startSync(Point from) {
        isError.set(false);
        scheduledToStop.set(false);

        stopKeepAliveThread();
        blockSync.startSync(from, this);
        syncMode = true;
        cursorService.setSyncMode(syncMode);
        startKeepAliveThread(syncMode);
    }

    public synchronized void shutdown() {
        blockRangeSync.stop();
    }

    public synchronized void shutdownSync() {
        blockSync.stop();
    }

    public boolean isRunning() {
        if (syncMode) {
            return blockSync.isRunning();
        }

        return blockRangeSync.isRunning();
    }

    public int getLastKeepAliveResponseCookie() {
        if (syncMode) {
            return blockSync.getLastKeepAliveResponseCookie();
        }

        return blockRangeSync.getLastKeepAliveResponseCookie();
    }

    public long getLastKeepAliveResponseTime() {
        if (syncMode) {
            return blockSync.getLastKeepAliveResponseTime();
        }

        return blockRangeSync.getLastKeepAliveResponseTime();
    }

    public boolean isScheduledToStop() {
        return scheduledToStop.get();
    }

    public boolean isError() {
        return isError.get();
    }

    private void setError() {
        isError.set(true);
    }

    public synchronized void reset() {
        byronBlockEventPublisher.reset();
        postShelleyBlockEventPublisher.reset();
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

    private synchronized void startKeepAliveThread(boolean syncMode) {
        stopKeepAliveThread();
        keepAliveThread = Thread.ofVirtual().unstarted(() -> {
            int interval = storeProperties.getKeepAliveInterval();
            while (true) {
                try {
                    Thread.sleep(interval);
                    int randomNo = getRandomNumber(0, 60000);

                    if (log.isDebugEnabled())
                        log.debug("Sending keep alive : " + randomNo);

                    if (syncMode)
                        blockSync.sendKeepAliveMessage(randomNo);
                    else
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

    //For shelley and later eras, this method is used to detect new epoch
    private boolean detectIfNewEpoch(Integer epoch, long slot) {
        // If the previous epoch is null, check the cursor table for the previous epoch.
        // However, if the previous epoch is null and it's the first block and the network is starting from a non-Byron era,
        // then prevCursor will be returned for slot(-1) or the genesis entry. In this case, we will consider it as a new epoch.
        if (previousEpoch == null) {
            var prevCursor = cursorService.getPreviousCursor(slot);
            if (prevCursor.isPresent() && prevCursor.get().getSlot() != -1) { //Previous cursor is not genesis entry
                previousEpoch = eraService.getEpochNo(prevCursor.get().getEra(), prevCursor.get().getSlot());
            }
        }

        if (previousEpoch == null ||  epoch == previousEpoch + 1) {
            return true;
        } else
            return false;
    }

    private boolean detectIfNewEpochByronEra(Integer epoch) {
        if (previousEpoch == null ||  epoch == previousEpoch + 1) {
            return true;
        } else
            return false;
    }

    private void publishEpochChangeEvent(EventMetadata eventMetadata) {
        EpochChangeEvent epochChangeEvent = EpochChangeEvent.builder()
                .metadata(eventMetadata)
                .previousEpoch(previousEpoch)
                .epoch(eventMetadata.getEpochNumber())
                .previousEra(previousEra)
                .era(eventMetadata.getEra())
                .build();
        publisher.publishEvent(epochChangeEvent);
    }

    @Override
    public void intersactNotFound(Tip tip) {
        log.error("Intersection not found. Current tip: {}", tip);
        
        // Publish restart event
        RequiredSyncRestartEvent restartEvent = RequiredSyncRestartEvent.builder()
                .reason("IntersectionNotFound")
                .errorCode("INTERSECTION_NOT_FOUND")
                .timestamp(System.currentTimeMillis())
                .source("BlockFetchService")
                .details(String.format("Intersect not found. Current Tip: %s", tip))
                .build();
        
        publisher.publishEvent(restartEvent);
    }
}
