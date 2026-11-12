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
import java.util.Map;
import java.util.LinkedHashMap;
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

    //The last block accepted in this session: written, or handed to a parallel batch. The next block must build on
    //it, otherwise a block was skipped or the order is wrong. A block at or below it that was already accepted is a
    //re-delivery and is skipped, so each block is processed once.
    //A null hash means there is nothing to check the link against, e.g. right after a block was skipped as a parse
    //error. A block number of -1 means it is not known.
    private volatile String expectedPrevBlockHash;
    private volatile long lastAcceptedBlockNumber = -1;
    //True when the last accepted block was handed to a parallel batch. Chain sync never writes such a batch.
    private volatile boolean lastAcceptedInBatch;
    //A range fetch from a block that is not processed yet (a fresh start) delivers that block first. Until a block is
    //accepted (or skipped as a recorded parse error), the first block must be this one, so a lost first block is not
    //missed. Null when there is no such requirement.
    private volatile String requiredFirstBlockHash;
    //Hashes accepted in this session, so a re-delivered block can be recognised before its batch is written
    private final Map<String, Boolean> recentAcceptedHashes = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > RECENT_ACCEPTED_HASHES;
        }
    });
    private static final int RECENT_ACCEPTED_HASHES = 5000;

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
            if (!acceptBlock(blockHeader.getHeaderBody().getBlockNumber(),
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
                if (parallelMode) {
                    postShelleyBlockEventPublisher.processBlocksInParallel();
                }

                //Publish epoch change event first and then process first block of the epoch
                publishEpochChangeEvent(eventMetadata);

                //Now process first block
                log.info("Processing first block of the epoch {}, block {}", eventMetadata.getEpochNumber(), eventMetadata.getBlock());
                //Published on the sequential path, so it must not be marked parallel (it is never part of a batch)
                postShelleyBlockEventPublisher.publishBlockEvents(eventMetadata.toBuilder().parallelMode(false).build(), block, transactions);
                blockAccepted(eventMetadata.getBlock(), eventMetadata.getBlockHash(), false);
            } else {
                if (parallelMode) {
                    //Handed to a batch. If the batch fails to write, the sync stops and restarts from the cursor.
                    postShelleyBlockEventPublisher.publishBlockEventsInParallel(eventMetadata, block, transactions);
                } else {
                    postShelleyBlockEventPublisher.publishBlockEvents(eventMetadata, block, transactions);
                }
                blockAccepted(eventMetadata.getBlock(), eventMetadata.getBlockHash(), parallelMode);
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
        if (scheduledToStop.get()) {
            log.debug("Stopping BlockFetchService as scheduled");
            return;
        }

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
            if (!acceptBlock(blockNumber, byronBlock.getHeader().getBlockHash(), byronBlock.getHeader().getPrevBlock()))
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
                if (parallelMode) {
                    byronBlockEventPublisher.processBlocksInParallel();
                }

                log.info("Publish EpochChangeEvent >>>");
                //Publish epoch change event
                publishEpochChangeEvent(eventMetadata);

                log.info("Processing first block of the epoch {}, block {}", eventMetadata.getEpochNumber(), eventMetadata.getBlock());
                //Process single block (First block of epoch)
                byronBlockEventPublisher.publishBlockEvents(eventMetadata.toBuilder().parallelMode(false).build(), byronBlock, Collections.emptyList());
                blockAccepted(eventMetadata.getBlock(), eventMetadata.getBlockHash(), false);

            } else {
                if (parallelMode) {
                    //Handed to a batch. If the batch fails to write, the sync stops and restarts from the cursor.
                    byronBlockEventPublisher.publishBlockEventsInParallel(eventMetadata, byronBlock, Collections.emptyList());
                } else {
                    byronBlockEventPublisher.publishBlockEvents(eventMetadata, byronBlock, Collections.emptyList());
                }
                blockAccepted(eventMetadata.getBlock(), eventMetadata.getBlockHash(), parallelMode);
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
        if (scheduledToStop.get()) {
            log.debug("Stopping BlockFetchService as scheduled");
            return;
        }

        checkError();
        lastReceivedBlockTime = System.currentTimeMillis();
        try {
            final long absoluteSlot = genesisConfig.absoluteSlot(Era.Byron,
                    byronEbBlock.getHeader().getConsensusData().getEpoch(), 0);
            final long epochNumber = byronEbBlock.getHeader().getConsensusData().getEpoch();
            final long blockTime = eraService.blockTime(Era.Byron, absoluteSlot);

            long blockNumber = byronEbBlock.getHeader().getConsensusData().getDifficulty().longValue();
            if (!acceptBlock(blockNumber, byronEbBlock.getHeader().getBlockHash(), byronEbBlock.getHeader().getPrevBlock()))
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

            //The EB block writes the cursor itself. Write the Byron blocks still waiting in a batch first, or the cursor
            //would move past blocks that are not written yet.
            byronBlockEventPublisher.processBlocksInParallel();
            publisher.publishEvent(new ByronEbBlockEvent(eventMetadata, byronEbBlock));

            //Finally Set the cursor
            cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(),
                    eventMetadata.getBlock(), eventMetadata.getPrevBlockHash(), eventMetadata.getEra()));
            blockAccepted(eventMetadata.getBlock(), eventMetadata.getBlockHash(), false);
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
        restoreEpochStateFromCursor(cursorService.getCursor());
        log.info("Restored epoch state after rollback. previousEpoch: {}, previousEra: {}",
                previousEpoch, previousEra);
        //The next block must build on the rollback point. A point after the last stored block means yaci holds a
        //block that was never stored. Keep the old expectation so the next block fails the link check and sync
        //restarts from the cursor.
        if (point.getSlot() <= currentPoint.getSlot()) {
            resetAcceptedBlock(point.getHash(), cursorService.getCursor().map(Cursor::getBlock).orElse(null));
            //Chain sync continues right after the rollback point, so the next block must build on it, even when its
            //block number is not known (no cursor left)
            expectedPrevBlockHash = point.getHash();
        } else
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

    private void restoreEpochStateFromCursor(Optional<Cursor> cursorOptional) {
        cursorOptional.ifPresentOrElse(cursor -> {
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
    }

    /**
     * A start block that is already processed is skipped, so it never sets the epoch state. Take it from that block's
     * cursor instead. Otherwise an in-process restart keeps the epoch of the old session, which can be ahead of the
     * start block, and the first block of the next epoch would not be seen as a new epoch.
     */
    private void restoreEpochStateForStart(Point from, Long fromBlockNumber) {
        if (fromBlockNumber == null || from.getHash() == null)
            return;
        restoreEpochStateFromCursor(cursorService.getCursorByBlockHash(from.getHash()));
        log.info("Restored epoch state from start block {}. previousEpoch: {}, previousEra: {}",
                fromBlockNumber, previousEpoch, previousEra);
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
                            startSync(new Point(slot, blockHash), cursor.getBlock());
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
            //Only the block after the last accepted one may be skipped, or a Byron epoch boundary block with the same
            //number. Otherwise another block is missing before the failed one: fetch again from the last stored block.
            Long failedBlockNumber = e.getBlockNumber(); //null when yaci could not decode the header either
            long lastNumber = lastAcceptedBlockNumber;
            if (failedBlockNumber != null && failedBlockNumber > 0 && lastNumber >= 0
                    && failedBlockNumber != lastNumber + 1 && failedBlockNumber != lastNumber) {
                log.error("Block {} could not be parsed, but the last block accepted is {}. A block is missing before " +
                        "it. Stopping sync to fetch again from the last stored block.", failedBlockNumber, lastNumber);
                stopSyncAndRefetch("ParseErrorAfterMissingBlock", "BLOCK_NOT_LINKED",
                        String.format("Block %d could not be parsed, last block accepted is %d",
                                failedBlockNumber, lastNumber));
                return;
            }

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

            //The skipped block's hash is not known, so accept the next block without the link check. If it was the
            //first block of a fresh range fetch, the recorded skip stands in for it.
            requiredFirstBlockHash = null;
            expectedPrevBlockHash = null;
            //The next block must be the one after the skipped block, or a Byron epoch boundary block with the same
            //number. If the skipped block's number is not known, take it as the block after the last accepted one:
            //then the next block may be the one after it (or, for a skipped Byron epoch boundary block, the one after
            //the last accepted block), and any other number is a missing block.
            Long skippedBlockNumber = e.getBlockNumber(); //null when yaci could not decode the header either
            if (skippedBlockNumber != null && skippedBlockNumber > 0)
                lastAcceptedBlockNumber = skippedBlockNumber;
            else if (lastNumber >= 0)
                lastAcceptedBlockNumber = lastNumber + 1;
            else
                lastAcceptedBlockNumber = -1;
        } else {
            stopSyncOnError();
            throw new RuntimeException(e);
        }
    }

    /**
     * Decides whether a block is processed. In range fetch and in chain sync, a block must build on the last block
     * accepted in this session.
     * <ul>
     *     <li>It builds on it: process it.</li>
     *     <li>It is at or below the last accepted block and was already accepted (in this session, or it is in the
     *     cursor table, so it is fully written): it is a re-delivery. Skip it, without publishing, so it is processed
     *     only once.</li>
     *     <li>Otherwise a block was skipped, or blocks came in the wrong order: stop the sync without storing this
     *     block and restart from the cursor, the last stored block.</li>
     * </ul>
     *
     * @return true if the block must be processed
     */
    private boolean acceptBlock(long blockNumber, String blockHash, String prevBlockHash) {
        String requiredFirst = requiredFirstBlockHash;
        if (requiredFirst != null) {
            if (requiredFirst.equals(blockHash))
                return true;

            log.error("The first block of the range fetch must be the start block {}, but block {} ({}) came first. " +
                    "Stopping sync to fetch again from the last stored block.", requiredFirst, blockNumber, blockHash);
            stopSyncAndRefetch("FirstBlockNotStartBlock", "BLOCK_NOT_LINKED",
                    String.format("First block %d (%s) of the range fetch is not the start block %s",
                            blockNumber, blockHash, requiredFirst));
            return false;
        }

        String expected = expectedPrevBlockHash;
        long lastNumber = lastAcceptedBlockNumber;

        //Chain sync writes each block at once and never flushes a parallel batch. A previous block that went to a
        //batch in this session (the range-to-sync startup race) is lost, even though this block links to it.
        boolean previousBlockLost = syncMode && lastAcceptedInBatch;

        if (!previousBlockLost && expected != null && expected.equals(prevBlockHash))
            return true;

        if (lastNumber >= 0 && blockNumber <= lastNumber) {
            if (isAlreadyAccepted(blockHash)) {
                log.info("Block {} ({}) was already processed. Skipping the re-delivered block.", blockNumber, blockHash);
                return false;
            }
            //After a skipped parse error the hash is unknown. A Byron epoch boundary block has the same number as
            //the block before it.
            if (expected == null && !previousBlockLost && blockNumber == lastNumber)
                return true;
        } else if (expected == null && !previousBlockLost) {
            //Nothing to check the hash against: the block after a skipped parse error, which must be the next block,
            //or a first block with no known start (the start point has no hash). A fresh range fetch checks its first
            //block against the start block above.
            if (lastNumber < 0 || blockNumber == lastNumber + 1)
                return true;
        }

        log.error("Block {} ({}) does not build on the last block accepted (block {}{}). Expected previous block hash {}, " +
                "found {}. Stopping sync to fetch again from the last stored block.",
                blockNumber, blockHash, lastNumber, previousBlockLost ? ", left in a batch that chain sync never writes" : "",
                expected, prevBlockHash);
        stopSyncAndRefetch("BlockNotLinkedToPreviousBlock", "BLOCK_NOT_LINKED",
                String.format("Block %d (%s) has previous block hash %s, expected %s",
                        blockNumber, blockHash, prevBlockHash, expected));
        return false;
    }

    //Stops the sync and asks for a restart from the cursor, the last stored block
    private void stopSyncAndRefetch(String reason, String errorCode, String details) {
        stopSyncOnError();

        publisher.publishEvent(RequiredSyncRestartEvent.builder()
                .reason(reason)
                .errorCode(errorCode)
                .timestamp(System.currentTimeMillis())
                .source("BlockFetchService")
                .details(details)
                .build());
    }

    private boolean isAlreadyAccepted(String blockHash) {
        if (blockHash == null)
            return false;
        if (recentAcceptedHashes.containsKey(blockHash))
            return true;
        //Publishers write a cursor row only after the block (or its batch) is fully written. The placeholder cursor of a
        //fresh start has no previous hash and does not count.
        return cursorService.getCursorByBlockHash(blockHash)
                .filter(cursor -> cursor.getPrevBlockHash() != null)
                .isPresent();
    }

    private void blockAccepted(long blockNumber, String blockHash, boolean inBatch) {
        requiredFirstBlockHash = null;
        expectedPrevBlockHash = blockHash;
        lastAcceptedBlockNumber = blockNumber;
        lastAcceptedInBatch = inBatch;
        recentAcceptedHashes.put(blockHash, Boolean.TRUE);
    }

    /**
     * Sets the block the next block must build on.
     *
     * @param blockNumber the block number if this block is already processed, or null if it is not (for example,
     *                    the first block of a fresh start, which the range fetch delivers and which must be processed)
     */
    private void resetAcceptedBlock(String blockHash, Long blockNumber) {
        recentAcceptedHashes.clear();
        lastAcceptedInBatch = false;
        requiredFirstBlockHash = null;
        if (blockNumber != null && blockHash != null) {
            expectedPrevBlockHash = blockHash;
            lastAcceptedBlockNumber = blockNumber;
            recentAcceptedHashes.put(blockHash, Boolean.TRUE);
        } else {
            expectedPrevBlockHash = null;
            lastAcceptedBlockNumber = -1;
        }
    }

    public synchronized void startFetch(Point from, Point to) {
        startFetch(from, to, null);
    }

    /**
     * @param fromBlockNumber the block number of {@code from} if that block is already processed (the cursor), or
     *                        null. The range fetch delivers {@code from} itself again: it is skipped only when it is
     *                        already processed.
     */
    public synchronized void startFetch(Point from, Point to, Long fromBlockNumber) {
        isError.set(false);
        scheduledToStop.set(false);
        resetAcceptedBlock(from.getHash(), fromBlockNumber);
        //The range fetch delivers the start block first. When it is not processed yet, it must not be lost.
        if (fromBlockNumber == null)
            requiredFirstBlockHash = from.getHash();
        restoreEpochStateForStart(from, fromBlockNumber);

        stopKeepAliveThread();
        //Set the mode before fetching starts, because the first blocks can arrive before fetch() returns
        syncMode = false;
        cursorService.setSyncMode(syncMode);
        blockRangeSync.restart(this);
        blockRangeSync.fetch(from, to);

        startKeepAliveThread(syncMode);
    }

    public synchronized void startSync(Point from) {
        startSync(from, null);
    }

    /**
     * @param fromBlockNumber the block number of {@code from} if that block is already processed (the cursor), or
     *                        null. Chain sync delivers the blocks after {@code from}, so the first one must build on it.
     */
    public synchronized void startSync(Point from, Long fromBlockNumber) {
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
        resetAcceptedBlock(from.getHash(), fromBlockNumber);
        expectedPrevBlockHash = from.getHash();
        restoreEpochStateForStart(from, fromBlockNumber);
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
