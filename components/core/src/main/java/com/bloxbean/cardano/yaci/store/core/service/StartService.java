package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.helper.model.StartPoint;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.PreRollbackEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ReadOnly(false)
@RequiredArgsConstructor
@Slf4j
public class StartService {
    private final ApplicationEventPublisher publisher;
    private final BlockFetchService blockFetchService;
    private final TipFinderService tipFinderService;
    private final GenesisBlockFinder genesisBlockFinder;
    private final CursorService cursorService;
    private final InitService initService;
    private final StoreProperties storeProperties;

    private final GenesisConfig genesisConfig;

    @Value("${store.cardano.protocol-magic}")
    private long protocolMagic;

    private volatile boolean alreadyStarted;
    // Incremented by every stop(), so the auto-restart can tell whether someone else stopped the sync while it
    // was waiting between its own stop and start.
    private long stopCount;

    public synchronized void start() {
        if (alreadyStarted)
            throw new RuntimeException("StartService has already been started");

        try {
            doStart();
        } catch (RuntimeException e) {
            // Leave the service startable: a later start() must not fail with "already been started".
            alreadyStarted = false;
            throw e;
        }
    }

    private void doStart() {

        log.info("###### Genesis Config #####");
        log.info("Epoch Length            : " + genesisConfig.getEpochLength());
        log.info("ByronEra Slot Duration  : " + genesisConfig.slotDuration(Era.Byron));
        log.info("ShelleyEra Slot Length  : " + genesisConfig.slotDuration(Era.Shelley));
        log.info("Byron Slots per Epoch   : " + genesisConfig.slotsPerEpoch(Era.Byron));
        log.info("Shelley Slots per Epoch : " + genesisConfig.slotsPerEpoch(Era.Shelley));
        log.info("Start time              : " + genesisConfig.getStartTime(protocolMagic));
        log.info("Max Lovelace Supply     : " + genesisConfig.getMaxLovelaceSupply());
        log.info("###########################");

        // A failed restart leaves the error flag set so the health check retries. Clear it before starting:
        // the startup path (e.g. writing the genesis cursor) refuses to run while it is set.
        blockFetchService.clearError();
        initService.init();
        blockFetchService.reset();

        alreadyStarted = true;
        log.info("Application is ready. Let's start the sync process ...");
        Point from = null;
        String prevBlockHash = null;
        Era era = null;
        Long blockNumber = 0L;
        //Block number of the start point if it is a block that is already processed (resuming from the cursor)
        Long processedBlockNumber = null;
        Optional<Cursor> optional = cursorService.getStartCursor();
        if (optional.isPresent()) {
            log.info("Last block in DB : " + optional.get().getBlock());
            from = new Point(optional.get().getSlot(), optional.get().getBlockHash());
            //Keep the cursor's own previous hash. A null one marks the placeholder written below on a fresh start.
            prevBlockHash = optional.get().getPrevBlockHash();
            era = optional.get().getEra();
            blockNumber = optional.get().getBlock();
            //A fresh start saves a placeholder cursor at its start point before that block is processed (see
            //"Reset cursor" below), with no previous hash. If the first block never got written, the placeholder
            //is all there is: the block must still be processed, not skipped as already done.
            if (optional.get().getPrevBlockHash() != null)
                processedBlockNumber = optional.get().getBlock();
        } else {
            if (storeProperties.getSyncStartSlot() == 0 || storeProperties.getSyncStartBlockhash() == null
                    || storeProperties.getSyncStartBlockhash().isEmpty()) {
                Optional<StartPoint> startPoint = genesisBlockFinder.getGenesisAndFirstBlock();
                if (startPoint.isPresent()) {
                    String genesisHash = startPoint.get().getGenesisHash();
                    if (genesisHash == null) {
                        log.info("Genesis hash is null. So genesis block's hash will be set from " +
                                "store.cardano.default-genesis-hash property");
                        genesisHash = storeProperties.getDefaultGenesisHash();
                    }

                    //Save genesis block
                    GenesisBlockEvent genesisBlockEvent = GenesisBlockEvent.builder()
                            .blockHash(genesisHash)
                            .blockTime(genesisConfig.getStartTime(protocolMagic))
                            .genesisBalances(genesisConfig.getGenesisBalances())
                            .genesisStaking(genesisConfig.getGenesisStaking())
                            .era(startPoint.get().getFirstBlockEra()) //Set first block era to set start era
                            .slot(-1) //dummy value
                            .block(-1) //dummy value
                            .epoch(0)
                            .protocolMagic(protocolMagic)
                            .build();
                    publisher.publishEvent(genesisBlockEvent);
                    from = startPoint.get().getFirstBlock();
                    era = startPoint.get().getFirstBlockEra();
                } else
                    throw new IllegalStateException("Genesis points not found. From point could not be decided.");
            } else {
                from = new Point(storeProperties.getSyncStartSlot(), storeProperties.getSyncStartBlockhash());
                blockNumber = storeProperties.getSyncStartByronBlockNumber();
            }
        }

        //Reset cursor
        cursorService.setCursor(new Cursor(from.getSlot(), from.getHash(), blockNumber, prevBlockHash, era));
        Tip tip = tipFinderService.getTip().block();

        Point to;
        if (storeProperties.getSyncStopSlot() != 0) {
            to = new Point(storeProperties.getSyncStopSlot(), storeProperties.getSyncStopBlockhash());
        } else {
            to = tip.getPoint();
            storeProperties.setPrimaryInstance(true);
        }

        log.info("From >> " + from);
        log.info("TO >> " + to);

        log.debug("Block diff configuration to start sync >>> " + storeProperties.getBlockDiffToStartSyncProtocol());
        long diff = tip.getPoint().getSlot() - from.getSlot();
        //BlockFetch requests the range from the from block inclusive, so the from block is processed again.
        //ChainSync starts after the intersection, so it is not.
        boolean fetchRange = !storeProperties.isPrimaryInstance() || diff > storeProperties.getBlockDiffToStartSyncProtocol();

        //Send a rollback event to rollback data after this slot, and at this slot for stores
        //that cannot overwrite the from block's rows when it is processed again (For BlockFetch).
        if (from.getSlot() > 0) {
            //Publish pre-rollback event to clean data
            PreRollbackEvent preRollbackEvent = PreRollbackEvent.builder()
                    .rollbackTo(new Point(from.getSlot(), from.getHash()))
                    .currentBlock(tip.getBlock())
                    .currentPoint(new Point(tip.getPoint().getSlot(), tip.getPoint().getHash()))
                    .build();
            log.info("Publishing pre-rollback event to clean data after restart >>> " + preRollbackEvent);
            publisher.publishEvent(preRollbackEvent);

            //Publish rollback event to clean data
            RollbackEvent rollbackEvent = RollbackEvent.builder()
                    .rollbackTo(new Point(from.getSlot(), from.getHash()))
                    .currentBlock(tip.getBlock())
                    .currentPoint(new Point(tip.getPoint().getSlot(), tip.getPoint().getHash()))
                    //BlockFetchService skips the from block when it is already processed (#1313), so it is processed
                    //again only on a range fetch from a point that is not processed yet
                    .rollbackPointReprocessed(fetchRange && processedBlockNumber == null)
                    .build();
            log.info("Publishing rollback event to clean data after restart >>> " + rollbackEvent);
            publisher.publishEvent(rollbackEvent);
        }

        if (blockNumber != null && blockNumber > 0L)
            publisher.publishEvent(new PreSyncEvent(blockNumber));

        if (fetchRange) {
            if (storeProperties.isPrimaryInstance())
                log.info("Start BlockRangeSync");
            blockFetchService.startFetch(from, to, processedBlockNumber);
        } else {
            log.info("Start BlockSync");
            blockFetchService.startSync(from, processedBlockNumber);
        }
    }

    public synchronized void stop() {
        log.info("Stopping the StartService >>>> ");
        stopCount++;
        // Always record the stop, even when no session is running (for example, after a restart that failed).
        // Otherwise the health check would see "not running, not stopped" and start the sync again.
        try {
            blockFetchService.stop();
        } catch (Exception e) {
            log.debug("Error stopping blockFetchService", e);
        }

        alreadyStarted = false;
    }

    /**
     * First half of an auto-restart: stops the sync and returns a token for {@link #startAfterRestart(long)}.
     */
    public synchronized long stopForRestart() {
        stop();
        return stopCount;
    }

    /**
     * Second half of an auto-restart. Starts the sync again, unless someone else stopped or started it since
     * {@link #stopForRestart()}: a deliberate stop (admin API, snapshot job) must stay stopped, and a sync that is
     * already running must not get a second session.
     * <p>
     * If the start fails (for example, the database is down), the service is left in a state the health check
     * sees as an error, not as a deliberate stop, so the restart is tried again (#959). Then the exception is
     * rethrown.
     *
     * @return true if the sync was started
     */
    public synchronized boolean startAfterRestart(long stopToken) {
        if (stopCount != stopToken) {
            log.info("The sync was stopped again while the restart was waiting. Leaving it stopped.");
            return false;
        }
        if (alreadyStarted) {
            log.info("The sync was started by someone else while the restart was waiting. Nothing to do.");
            return false;
        }

        try {
            start();
            return true;
        } catch (RuntimeException e) {
            blockFetchService.markRestartFailed();
            throw e;
        }
    }

    /**
     * True when the sync stopped because of an error and was not stopped on purpose since, so it should be
     * restarted.
     */
    public boolean isSyncFailed() {
        return blockFetchService.isError() && !blockFetchService.isScheduledToStop();
    }

    public boolean isStarted() {
        return alreadyStarted;
    }
}
