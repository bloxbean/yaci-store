package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class BlockFinder {
    private final BlockSync blockSync;
    // Once connected, how long to wait for the node's answer, even when the call itself has no limit.
    // Not final so tests can shorten it.
    long answerTimeoutMs = NodeCalls.DEFAULT_TIMEOUT_MS;

    public BlockFinder(BlockSync blockSync) {
        this.blockSync = blockSync;
        log.info("BlockFinder initialized >>");
    }

    public boolean blockExists(Point point) {
        return blockExists(point, NodeCalls.DEFAULT_TIMEOUT_MS);
    }

    /**
     * Asks the node whether the block at {@code point} is on its chain.
     *
     * @param timeoutMs how long to wait for the connection and the answer, 0 for no limit on the connection. The
     *                  answer itself is never waited for longer than {@link #answerTimeoutMs}.
     * @return true if the node found the point, false if the node said it is not on its chain
     * @throws NodeNotAnsweringException if the node did not answer in time. A missing answer is not a "no": treating
     *                                   it as one would make the caller step back to an older cursor and delete
     *                                   newer ones.
     */
    public synchronized boolean blockExists(Point point, long timeoutMs) {
        CompletableFuture<Boolean> answer = new CompletableFuture<>();
        BlockChainDataListener listener = new BlockChainDataListener() {
            @Override
            public void intersactFound(Tip tip, Point point) {
                log.info("Intersection found: " + point);
                answer.complete(true);
            }

            @Override
            public void intersactNotFound(Tip tip) {
                log.info("Intersection not found: " + point);
                answer.complete(false);
            }
        };

        try {
            return NodeCalls.call("check if block " + point + " exists", timeoutMs, () -> {
                blockSync.startSync(point, listener);
                try {
                    return answer.get(answerTimeoutMs, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    throw new NodeNotAnsweringException("The node did not answer within " + (answerTimeoutMs / 1000)
                            + " s: check if block " + point + " exists");
                }
            }, () -> {
                // Also releases the worker if yaci swallowed the interrupt and returned without an answer
                answer.cancel(true);
                blockSync.stop();
            });
        } catch (CancellationException e) {
            throw new NodeNotAnsweringException("No answer from the node. Could not check if block " + point + " exists");
        } finally {
            try {
                blockSync.stop();
            } catch (Exception e) {
                log.debug("Error stopping the block finder connection", e);
            }
        }
    }
}
