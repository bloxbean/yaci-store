package com.bloxbean.cardano.yaci.store.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.helper.model.StartPoint;
import com.bloxbean.cardano.yaci.store.configuration.StoreConfig;
import com.bloxbean.cardano.yaci.store.domain.Cursor;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartService {
    private final ApplicationEventPublisher publisher;
    private final BlockFetchService blockFetchService;
    private final TipFinderService tipFinderService;
    private final GenesisBlockFinder genesisBlockFinder;
    private final CursorService cursorService;
    private final StoreConfig storeConfig;

    private boolean alreadyStarted;

    public void start() {
        if (alreadyStarted)
            throw new RuntimeException("StartService has already been started");
        alreadyStarted = true;
        log.info("Application is ready. Let's start the sync process ...");
        Point from = null;
        Integer era = null;
        Long blockNumber = 0L;
        Optional<Cursor> optional = cursorService.getCursor();
        if (optional.isPresent()) {
            log.info("Last block in DB : " + optional.get().getBlock());
            from = new Point(optional.get().getSlot(), optional.get().getBlockHash());
            era = null;
            blockNumber = optional.get().getBlock();
        } else {
            if (storeConfig.getSyncStartSlot() == 0 || storeConfig.getSyncStartBlockHash() == null
                    || storeConfig.getSyncStartBlockHash().isEmpty()) {
                Optional<StartPoint> startPoint = genesisBlockFinder.getGenesisAndFirstBlock();
                if (startPoint.isPresent()) {
                    //Save genesis block
                    GenesisBlockEvent genesisBlockEvent = GenesisBlockEvent.builder()
                            .blockHash(startPoint.get().getGenesisBlock().getHash())
                            .slot(startPoint.get().getGenesisBlock().getSlot())
                            .block(0)
                            .build();
                    publisher.publishEvent(genesisBlockEvent);
                    from = startPoint.get().getFirstBlock();
                } else
                    throw new IllegalStateException("Genesis points not found. From point could not be decided.");
            } else {
                from = new Point(storeConfig.getSyncStartSlot(), storeConfig.getSyncStartBlockHash());
            }
        }

        //Reset cursor
        cursorService.setCursor(new Cursor(from.getSlot(), from.getHash(), blockNumber));
        Tip tip = tipFinderService.getTip().block();

        Point to = null;
        if (storeConfig.getSyncStopSlot() != 0) {
            to = new Point(storeConfig.getSyncStopSlot(), storeConfig.getSyncStopBlockHash());
        } else {
            to = tip.getPoint();
            storeConfig.setPrimaryInstance(true);
        }

        log.info("From >> " + from);
        log.info("TO >> " + to);

        long diff = tip.getPoint().getSlot() - from.getSlot();
        if (storeConfig.getPrimaryInstance()) {
            if (diff > 7200) {
                log.info("Start BlockRangeSync");
                blockFetchService.startFetch(from, to);
            } else {
                log.info("Start BlockSync");
                blockFetchService.startSync(from);
            }
        } else {
            blockFetchService.startFetch(from, to);
        }
    }
}
