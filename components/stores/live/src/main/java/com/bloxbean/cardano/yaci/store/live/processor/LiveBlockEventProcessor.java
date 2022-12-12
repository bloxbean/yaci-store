package com.bloxbean.cardano.yaci.store.live.processor;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.live.BlocksWebSocketHandler;
import com.bloxbean.cardano.yaci.store.live.cache.BlockCache;
import com.bloxbean.cardano.yaci.store.live.dto.BlockData;
import com.bloxbean.cardano.yaci.store.live.mapper.BlockDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;

@Component
@Slf4j
public class LiveBlockEventProcessor {
    private DecimalFormat df = new DecimalFormat("0.00");

    @Autowired
    private BlockDataMapper blockDataMapper;

    @Autowired
    private BlocksWebSocketHandler socketHandler;

    private BlockCache blockCache;


    public LiveBlockEventProcessor(BlockCache blockCache) {
        this.blockCache = blockCache;
        if (log.isDebugEnabled())
            log.debug("LiveBlockEventProcessor started !!!");
    }

    @EventListener
    public void handleBlockHeaderEvent(BlockHeaderEvent blockHeaderEvent) {
        //If sync mode ignore
        //As this live mode is supported only for fully sync mode
        if (log.isDebugEnabled())
            log.debug("Inside handleBlockEvent >> " + blockHeaderEvent.getMetadata().isSyncMode());
        if (!blockHeaderEvent.getMetadata().isSyncMode())
            return;

        BlockHeader blockHeader = blockHeaderEvent.getBlockHeader();
        if (log.isDebugEnabled()) {
            log.debug("Live Blk # : " + blockHeader.getHeaderBody().getBlockNumber() + ",  Size : " + df.format(blockHeader.getHeaderBody().getBlockBodySize() / 1024.0) + " KB");
        }

        BlockData blockData = blockDataMapper.blockHeaderToBlockData(blockHeader);
        blockData.setNTx(blockHeaderEvent.getMetadata().getNoOfTxs());
//        blockData.setFee(); //TODO -- Get total fee from somewhere
        blockCache.addBlock(blockData);
        try {
            socketHandler.broadcastBlockData(blockData);
        } catch (IOException e) {
            log.error("Error in broadcast : ", e);
        }
    }
}
