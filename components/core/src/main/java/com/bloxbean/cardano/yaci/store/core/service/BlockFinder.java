package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BlockFinder {
    private BlockRangeSync blockRangeSync;

    public BlockFinder(BlockRangeSync blockRangeSync) {
        this.blockRangeSync = blockRangeSync;
        log.info("BlockFinder initialized >>");
    }

    public synchronized boolean blockExists(Point point) {
        try {
            AtomicBoolean blockExists = new AtomicBoolean(false);
            CountDownLatch countDownLatch = new CountDownLatch(1);

            blockRangeSync.restart(new BlockChainDataListener() {
                @Override
                public void onByronBlock(ByronMainBlock byronBlock) {
                    BlockChainDataListener.super.onByronBlock(byronBlock);
                    blockExists.set(true);
                    countDownLatch.countDown();
                }

                @Override
                public void onByronEbBlock(ByronEbBlock byronEbBlock) {
                    blockExists.set(true);
                    countDownLatch.countDown();
                }

                @Override
                public void onBlock(Block block) {
                    blockExists.set(true);
                    countDownLatch.countDown();
                }

                @Override
                public void noBlockFound(Point from, Point to) {
                    blockExists.set(false);
                    countDownLatch.countDown();
                }

            });

            blockRangeSync.fetch(point, point);
            try {
                countDownLatch.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Error waiting for blockExists", e);
                throw new IllegalStateException("Unable verify if block exists. Server could not be started. Point: " + point);
            }

            return blockExists.get();
        } finally {
            blockRangeSync.stop();
        }
    }
}
