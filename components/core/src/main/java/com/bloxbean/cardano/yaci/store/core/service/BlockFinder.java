package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BlockFinder {
    private BlockSync blockSync;

    public BlockFinder(BlockSync blockSync) {
        this.blockSync = blockSync;
        log.info("BlockFinder initialized >>");
    }

    public synchronized boolean blockExists(Point point) {
        try {
            AtomicBoolean blockExists = new AtomicBoolean(false);
            CountDownLatch countDownLatch = new CountDownLatch(1);

            blockSync.startSync(point, new BlockChainDataListener() {
                @Override
                public void intersactFound(Tip tip, Point point) {
                    log.info("Intersection found: " + point);
                    blockExists.set(true);
                    countDownLatch.countDown();
                }

                @Override
                public void intersactNotFound(Tip tip) {
                    log.info("Intersection not found: " + point);
                    blockExists.set(false);
                    countDownLatch.countDown();
                }
            });

            try {
                countDownLatch.await(40, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Error waiting for blockExists", e);
                throw new IllegalStateException("Unable verify if block exists. Server could not be started. Point: " + point);
            }

            return blockExists.get();
        } finally {
            blockSync.stop();
        }
    }
}
