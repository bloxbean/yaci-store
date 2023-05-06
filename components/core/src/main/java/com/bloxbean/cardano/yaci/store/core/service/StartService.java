package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.helper.model.StartPoint;
import com.bloxbean.cardano.yaci.store.core.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final StoreProperties storeProperties;

    private final GenesisConfig genesisConfig;

    @Value("${store.cardano.protocol-magic}")
    private long protocolMagic;

    private boolean alreadyStarted;

    public void start() {
        if (alreadyStarted)
            throw new RuntimeException("StartService has already been started");

        log.info("###### Genesis Config #####");
        log.info("Epoch Length            : " + genesisConfig.getEpochLength());
        log.info("ByronEra Slot Duration  : " + genesisConfig.slotDuration(Era.Byron));
        log.info("ShelleyEra Slot Length  : " + genesisConfig.slotDuration(Era.Shelley));
        log.info("Byron Slots per Epoch   : " + genesisConfig.slotsPerEpoch(Era.Byron));
        log.info("Shelley Slots per Epoch : " + genesisConfig.slotsPerEpoch(Era.Shelley));
        log.info("Start time              : " + genesisConfig.getStartTime(protocolMagic));
        log.info("Max Lovelace Supply     : " + genesisConfig.getMaxLovelaceSupply());
        log.info("###########################");

        alreadyStarted = true;
        log.info("Application is ready. Let's start the sync process ...");
        Point from = null;
        String prevBlockHash = null;
        Era era = null;
        Long blockNumber = 0L;
        Optional<Cursor> optional = cursorService.getCursor();
        if (optional.isPresent()) {
            log.info("Last block in DB : " + optional.get().getBlock());
            from = new Point(optional.get().getSlot(), optional.get().getBlockHash());
            prevBlockHash = optional.get().getBlockHash();
            era = optional.get().getEra();
            blockNumber = optional.get().getBlock();
        } else {
            if (storeProperties.getSyncStartSlot() == 0 || storeProperties.getSyncStartBlockhash() == null
                    || storeProperties.getSyncStartBlockhash().isEmpty()) {
                Optional<StartPoint> startPoint = genesisBlockFinder.getGenesisAndFirstBlock();
                if (startPoint.isPresent()) {
                    //Save genesis block
                    GenesisBlockEvent genesisBlockEvent = GenesisBlockEvent.builder()
                            .blockHash(startPoint.get().getGenesisBlock().getHash())
                            .slot(startPoint.get().getGenesisBlock().getSlot())
                            .blockTime(genesisConfig.getStartTime(protocolMagic))
                            .block(0)
                            .era(startPoint.get().getGenesisBlockEra())
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

        long diff = tip.getPoint().getSlot() - from.getSlot();
        if (storeProperties.isPrimaryInstance()) {
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
