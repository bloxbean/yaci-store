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
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
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
    private final ErrorStorage errorStorage;

    //Written by the thread that starts the sync and read by the yaci network thread
    private volatile boolean syncMode;
    private AtomicBoolean isError = new AtomicBoolean(false);
    private AtomicBoolean scheduledToStop = new AtomicBoolean(false);
    private Thread keepAliveThread;

    //Hash of the last block written. In sync mode the next block must link to it, otherwise a block was skipped.
    //Null means there is nothing to check against, e.g. right after a block was skipped as a parse error.
    private volatile String expectedPrevBlockHash;

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

        final BlockHeader blockHeader = block.getHeader();
        EventMetadata eventMetadata = null;

        //Everything below can fail on a database error (era/cursor lookups, saving the block). Any failure must stop
        //the sync. If it escapes to yaci instead, yaci reports it as a parse error and moves on to the next block.
        try {
            if (!isLinkedToPreviousBlock(blockHeader.getHeaderBody().getBlockNumber(),
                    blockHeader.getHeaderBody().getBlockHash(), blockHeader.getHeaderBody().getPrevHash()))
                return;

            byronBlockEventPublisher.processBlocksInParallel();

            final long slot = blockHeader.getHeaderBody().getSlot();
            final boolean newEra = eraService.checkIfNewEra(era, blockHeader);
            final int epochNumber = eraService.getEpochNo(era, slot);
            final int epochSlot = eraService.getShelleyEpochSlot(slot);
            final long blockTime = eraService.blockTime(era, slot);
            final String slotLeader = SlotLeaderUtil.getShelleySlotLeader(blockHeader.getHeaderBody().getIssuerVkey());
            final boolean newEpoch = detectIfNewEpoch(epochNumber, slot);

            //paralleMode is true when not fully synced and parallel processing is enabled
            boolean parallelMode = !syncMode && storeProperties.isEnableParallelProcessing();

            eventMetadata = EventMetadata.builder()
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

            if (newEra || newEpoch) { //Also add epoch change
                log.info("Publish EpochChangeEvent >>>");
                //Incase of era change. Process all pending blocks and then process era change block first
                postShelleyBlockEventPublisher.processBlocksInParallel();

                //Publish epoch change event first and then process first block of the epoch
                publishEpochChangeEvent(eventMetadata);

                //Now process first block
                log.info("Processing first block of the epoch {}, block {}", eventMetadata.getEpochNumber(), eventMetadata.getBlock());
                //Published on the sequential path, so it must not be marked parallel (it is never part of a batch)
                postShelleyBlockEventPublisher.publishBlockEvents(eventMetadata.toBuilder().parallelMode(false).build(), block, transactions);
                expectedPrevBlockHash = eventMetadata.getBlockHash();
            } else {
                if (parallelMode) {
                    //Not stored yet, so the expected previous block stays as it is
                    postShelleyBlockEventPublisher.publishBlockEventsInParallel(eventMetadata, block, transactions);
                } else {
                    //Blocks still waiting in a batch must be written first, or they are never written
                    postShelleyBlockEventPublisher.processBlocksInParallel();
                    postShelleyBlockEventPublisher.publishBlockEvents(eventMetadata, block, transactions);
                    expectedPrevBlockHash = eventMetadata.getBlockHash();
                }
            }

            previousEpoch = eventMetadata.getEpochNumber();
            previousEra = eventMetadata.getEra();

        } catch (Throwable e) {
            log.error("Error saving : " + (eventMetadata != null ? eventMetadata : blockHeader.getHeaderBody()), e);
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
            if (!isLinkedToPreviousBlock(blockNumber, byronBlock.getHeader().getBlockHash(), byronBlock.getHeader().getPrevBlock()))
                return;

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
                byronBlockEventPublisher.processBlocksInParallel();

                log.info("Publish EpochChangeEvent >>>");
                //Publish epoch change event
                publishEpochChangeEvent(eventMetadata);

                log.info("Processing first block of the epoch {}, block {}", eventMetadata.getEpochNumber(), eventMetadata.getBlock());
                //Process single block (First block of epoch)
                byronBlockEventPublisher.publishBlockEvents(eventMetadata.toBuilder().parallelMode(false).build(), byronBlock, Collections.emptyList());
                expectedPrevBlockHash = eventMetadata.getBlockHash();

            } else {
                if (parallelMode) {
                    //Not stored yet, so the expected previous block stays as it is
                    byronBlockEventPublisher.publishBlockEventsInParallel(eventMetadata, byronBlock, Collections.emptyList());
                } else {
                    //Blocks still waiting in a batch must be written first, or they are never written
                    byronBlockEventPublisher.processBlocksInParallel();
                    byronBlockEventPublisher.publishBlockEvents(eventMetadata, byronBlock, Collections.emptyList());
                    expectedPrevBlockHash = eventMetadata.getBlockHash();
                }
            }

            previousEpoch = eventMetadata.getEpochNumber();
            previousEra = eventMetadata.getEra();

        } catch (Throwable e) {
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
            if (!isLinkedToPreviousBlock(blockNumber, byronEbBlock.getHeader().getBlockHash(), byronEbBlock.getHeader().getPrevBlock()))
                return;

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
            expectedPrevBlockHash = eventMetadata.getBlockHash();
        } catch (Throwable e) {
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
        shutdownRangeSync();
        shutdownSync();
    }

    public void stop() {
        scheduledToStop.set(true);
        shutdownRangeSync();
        shutdownSync();
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
        restoreEpochStateFromCursor();
        //The next block must build on the rollback point. A point after the last stored block means yaci holds a
        //block that was never stored. Keep the old expectation so the next block fails the link check and sync
        //restarts from the cursor.
        if (point.getSlot() <= currentPoint.getSlot())
            expectedPrevBlockHash = point.getHash();
        else
            log.warn("Rollback point {} is after the last stored block {}", point, currentPoint);

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

    private void restoreEpochStateFromCursor() {
        cursorService.getCursor().ifPresentOrElse(cursor -> {
            previousEra = cursor.getEra();
            // Byron blocks carry their epoch, but the cursor does not. AdaPot starts after Byron,
            // so leave it unset and let the next Byron block provide the epoch if this ever occurs.
            previousEpoch = cursor.getEra() == null || cursor.getEra() == Era.Byron
                    ? null
                    : eraService.getEpochNo(cursor.getEra(), cursor.getSlot());
        }, () -> {
            previousEpoch = null;
            previousEra = null;
        });

        log.info("Restored epoch state after rollback. previousEpoch: {}, previousEra: {}",
                previousEpoch, previousEra);
    }

    @Override
    public void batchDone() {

        if (!syncMode) {
            log.info("Batch Done >>>");

            if (storeProperties.isPrimaryInstance()) {
                //Stop BlockRange Sync before starting BlockSync
                shutdownRangeSync();

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
        //yaci also reports exceptions thrown by onBlock as parse errors. onBlock stops the sync before it throws,
        //so this is a block that could not be stored, not one that could not be decoded. Never skip it.
        if (isError.get()) {
            log.error("Sync is stopped after a failure to store block {}. The block is not skipped.", e.getBlockNumber());
            return;
        }

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

            //A block is only skipped once the error row that records the skip is saved
            try {
                errorStorage.save(errorEvent);
            } catch (Exception ex) {
                log.error("Could not record parse error for block {}. Stopping sync instead of skipping the block.",
                        e.getBlockNumber(), ex);
                stopSyncOnError();
                throw new RuntimeException(e);
            }

            //The skipped block's hash is not known, so accept the next block without the link check
            expectedPrevBlockHash = null;
        } else {
            stopSyncOnError();
            throw new RuntimeException(e);
        }
    }

    /**
     * In sync mode, checks that a block builds on the last block written. If it does not, a block was skipped:
     * stop the sync without storing this block and restart from the cursor, the last stored block.
     */
    private boolean isLinkedToPreviousBlock(long blockNumber, String blockHash, String prevBlockHash) {
        String expected = expectedPrevBlockHash;
        if (!syncMode || expected == null || expected.equals(prevBlockHash))
            return true;

        log.error("Block {} ({}) does not build on the last block written. Expected previous block hash {}, found {}. " +
                "Stopping sync to fetch again from the last stored block.", blockNumber, blockHash, expected, prevBlockHash);
        stopSyncOnError();

        publisher.publishEvent(RequiredSyncRestartEvent.builder()
                .reason("BlockNotLinkedToPreviousBlock")
                .errorCode("BLOCK_NOT_LINKED")
                .timestamp(System.currentTimeMillis())
                .source("BlockFetchService")
                .details(String.format("Block %d (%s) has previous block hash %s, expected %s",
                        blockNumber, blockHash, prevBlockHash, expected))
                .build());
        return false;
    }

    public synchronized void startFetch(Point from, Point to) {
        isError.set(false);
        scheduledToStop.set(false);

        stopKeepAliveThread();
        //Set the mode before fetching starts, because the first blocks can arrive before fetch() returns
        syncMode = false;
        cursorService.setSyncMode(syncMode);
        blockRangeSync.restart(this);
        blockRangeSync.fetch(from, to);

        startKeepAliveThread(syncMode);
    }

    public synchronized void startSync(Point from) {
        isError.set(false);
        scheduledToStop.set(false);

        stopKeepAliveThread();
        //Set the mode before connecting. BlockSync.startSync waits for the handshake, and blocks can arrive
        //during that wait. A block seen with syncMode still false would go to a batch that is never written.
        syncMode = true;
        cursorService.setSyncMode(syncMode);
        //Sync restarts from the last stored block, so blocks still waiting in a batch are fetched again. Drop them,
        //or they would be written a second time when the batch is flushed.
        reset();
        //The first block from chain sync must build on the start point
        expectedPrevBlockHash = from.getHash();
        blockSync.startSync(from, this);
        startKeepAliveThread(syncMode);
    }

    public synchronized void shutdown() {
        blockRangeSync.stop();
    }

    public synchronized void shutdownRangeSync() {
        try {
            if (blockRangeSync != null)
                blockRangeSync.stop();
        } catch (Exception e) {
            log.error("Error stopping blockRangeSync", e);
        }
    }

    public synchronized void shutdownSync() {
        try {
            if (blockSync != null)
                blockSync.stop();
        } catch (Exception e) {
            log.error("Error stopping blockSync", e);
        }
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

                    if (log.isDebugEnabled()) {
                        log.debug("Sending keep alive : " + randomNo);
                    }

                    if (syncMode) {
                        blockSync.sendKeepAliveMessage(randomNo);
                        if (log.isDebugEnabled())
                            log.debug("Response from keep alive : " + blockSync.getLastKeepAliveResponseCookie());
                    } else {
                        blockRangeSync.sendKeepAliveMessage(randomNo);
                        if (log.isDebugEnabled())
                            log.debug("Response from keep alive : " + blockRangeSync.getLastKeepAliveResponseCookie());
                    }

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
