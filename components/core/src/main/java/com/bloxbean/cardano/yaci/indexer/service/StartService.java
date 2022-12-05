package com.bloxbean.cardano.yaci.indexer.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.helper.model.StartPoint;
import com.bloxbean.cardano.yaci.indexer.blocks.entity.BlockEntity;
import com.bloxbean.cardano.yaci.indexer.blocks.repository.BlockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class StartService {

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private BlockFetchService blockFetchService;

    @Autowired
    private TipFinderService tipFinderService;

    @Autowired
    private GenesisBlockFinder genesisBlockFinder;

    @Autowired
    private CursorService cursorService;

    @Value("${cardano.sync_start_slot:0}")
    private long syncStartSlot;

    @Value("${cardano.sync_start_blockhash:null}")
    private String syncStartBlockHash;

//    @Value("${cardano.known_slot}")
//    private long knownSlot;
//    @Value("${cardano.known_block_hash}")
//    private String knownBlockHash;
//
//    @Value("${cardano.first_block_slot}")
//    private long firstBlockSlot;
//
//    @Value("${cardano.first_block_hash}")
//    private String firstBlockHash;

    public StartService() {
    }

    @EventListener
    public void initialize(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Application is ready. Let's start the sync process ...");
        Point from = null;
        Integer era = null;
        long blockNumber = 0;
        Optional<BlockEntity> optional = blockRepository.findTopByOrderByBlockDesc();
        if (optional.isPresent()) {
            log.info("Last block in DB : " + optional.get().getBlock());
            from = new Point(optional.get().getSlot(), optional.get().getBlockHash());
            era = optional.get().getEra();
            blockNumber = optional.get().getBlock();
        } else {
            if (syncStartSlot == 0 || syncStartBlockHash == null || syncStartBlockHash.isEmpty()) {
                // from = new Point(firstBlockSlot, firstBlockHash);
                Optional<StartPoint> startPoint = genesisBlockFinder.getGenesisAndFirstBlock();
                if (startPoint.isPresent()) {
                    //Save genesis block
                    BlockEntity genesisBlock = new BlockEntity();
                    genesisBlock.setBlock(0);
                    genesisBlock.setBlockHash(startPoint.get().getGenesisBlock().getHash());
                    genesisBlock.setSlot(startPoint.get().getGenesisBlock().getSlot());
                    blockRepository.save(genesisBlock);

                    from = startPoint.get().getFirstBlock();
                } else
                    throw new IllegalStateException("Genesis points not found. From point could not be decided.");
            } else {
                from = new Point(syncStartSlot, syncStartBlockHash);
            }
        }

        //Reset cursor
        cursorService.setEra(era);
        cursorService.setSlot(from.getSlot());
        cursorService.setBlockNumber(blockNumber);
        cursorService.setBlockHash(from.getHash());

        Tip tip = tipFinderService.getTip().block();
        Point to = tip.getPoint();

        log.info("From >> " + from);
        log.info("TO >> " + to);

        long diff = tip.getPoint().getSlot() - from.getSlot();
        if (diff > 7200) {
            log.info("Start BlockRangeSync");
            blockFetchService.startFetch(from, to);
        } else {
            log.info("Start BlockSync");
            blockFetchService.startSync(from);
        }
    }
}
