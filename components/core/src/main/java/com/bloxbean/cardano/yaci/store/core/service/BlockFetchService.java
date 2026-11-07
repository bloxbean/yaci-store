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
    // When the current session was started. The health check gives a new session the full block-receive delay
    // before it treats it as stalled, even if the last block was received long ago.
    private volatile long lastSessionStartTime;

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
        if (point.getSlot() == 0 && point.getHash() == null) {
            log.warn("Rollback point doesn't seem to be valid. Ignoring rollback event to slot. " + point);
            return;
        }

        if (currentPoint == null) {
            log.info("Nothing stored yet. Nothing to roll back for rollback event to " + point);
            return;
        }

        // A node never rolls back more than k blocks. Note that k counts blocks: comparing it with a distance in
        // slots, as before, refused every rollback deeper than ~36 minutes on mainnet.
        String refusal = checkRollbackDepth(point, currentPoint, currentBlockNum);
        if (refusal != null) {
            // Ignoring the rollback and carrying on would write the blocks that follow it on top of newer data.
            // Stop this session instead; the restart resumes from the cursor.
            log.error("Refusing rollback to {} from cursor {} (block {}): {}. Restarting the sync from the cursor.",
                    point, currentPoint, currentBlockNum, refusal);
            stopSyncOnError();
            publisher.publishEvent(RequiredSyncRestartEvent.builder()
                    .reason("RollbackTooDeep")
                    .errorCode("ROLLBACK_TOO_DEEP")
                    .timestamp(System.currentTimeMillis())
                    .source("BlockFetchService")
                    .details(String.format("Rollback to %s, cursor at %s: %s", point, currentPoint, refusal))
                    .build());
            return;
        }

        cursorService.rollback(point.getSlot());
        restoreEpochStateFromCursor();

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

    /**
     * Returns why a rollback must be refused, or null if it is fine.
     * <p>
     * The depth is counted in blocks, using the block number stored for the rollback point in the cursor table.
     * The cursor table has one row per block in chain-sync, but only one row per batch for blocks written by the
     * range fetch, and old rows are cleaned up. When the point has no row, fall back to slots: 3k/f slots is the
     * window in which the chain normally has k blocks (129600 slots on mainnet and preprod).
     */
    String checkRollbackDepth(Point point, Point currentPoint, long currentBlockNum) {
        long k = securityParam();
        Optional<Cursor> pointCursor = point.getHash() != null
                ? cursorService.getCursorByBlockHash(point.getHash())
                : Optional.empty();

        if (pointCursor.isPresent() && pointCursor.get().getBlock() != null && currentBlockNum >= 0) {
            long depthBlocks = currentBlockNum - pointCursor.get().getBlock();
            return depthBlocks > k
                    ? String.format("%d blocks deep, more than k = %d", depthBlocks, k)
                    : null;
        }

        long depthSlots = currentPoint.getSlot() - point.getSlot();
        long maxSlots = maxRollbackSlots();
        return depthSlots > maxSlots
                ? String.format("rollback point not in the cursor table and %d slots deep, more than 3k/f = %d slots",
                        depthSlots, maxSlots)
                : null;
    }

    private long securityParam() {
        int k = genesisConfig.getSecurityParam();
        return k > 0 ? k : DEFAULT_SECURITY_PARAM;
    }

    long maxRollbackSlots() {
        long k = securityParam();
        double f = genesisConfig.getActiveSlotsCoeff();
        if (f <= 0)
            f = 0.05;
        return (long) Math.ceil(3 * k / f);
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
        lastSessionStartTime = System.currentTimeMillis();
        syncMode = false;
        cursorService.setSyncMode(syncMode);

        startKeepAliveThread(syncMode);
    }

    public synchronized void startSync(Point from) {
        isError.set(false);
        scheduledToStop.set(false);

        stopKeepAliveThread();
        blockSync.startSync(from, this);
        lastSessionStartTime = System.currentTimeMillis();
        syncMode = true;
        cursorService.setSyncMode(syncMode);
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

    // yaci's BlockSync and BlockRangeSync create their connection in start(). Before the first start (for example
    // while the startup is still waiting for the node) these getters throw NullPointerException. Answer "not
    // running" instead, so the health check can still decide what to do.

    public boolean isRunning() {
        try {
            return syncMode ? blockSync.isRunning() : blockRangeSync.isRunning();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public int getLastKeepAliveResponseCookie() {
        try {
            return syncMode ? blockSync.getLastKeepAliveResponseCookie() : blockRangeSync.getLastKeepAliveResponseCookie();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public long getLastKeepAliveResponseTime() {
        try {
            return syncMode ? blockSync.getLastKeepAliveResponseTime() : blockRangeSync.getLastKeepAliveResponseTime();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public boolean isScheduledToStop() {
        return scheduledToStop.get();
    }

    // True while StartService.start() runs. The connection is not up yet, so the health check must not judge it.
    private volatile boolean starting;

    public boolean isStarting() {
        return starting;
    }

    public void setStarting(boolean starting) {
        this.starting = starting;
    }

    /**
     * Called when a restart stopped the sync but could not start it again (for example, the database was down).
     * Clears the stop flag so the health check does not mistake this for a deliberate stop, and sets the error
     * flag so it schedules another restart.
     */
    public void markRestartFailed() {
        scheduledToStop.set(false);
        isError.set(true);
    }

    /**
     * Clears the error left by a previous session or a failed restart, before a new start.
     */
    public void clearError() {
        isError.set(false);
    }

    public long getLastSessionStartTime() {
        return lastSessionStartTime;
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
