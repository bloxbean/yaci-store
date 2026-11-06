package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@ReadOnly(false)
@RequiredArgsConstructor
@Slf4j
public class HealthService {
    private final BlockFetchService blockFetchService;
    private final StoreProperties storeProperties;

    public HealthStatus getHealthStatus() {
        long currentTime = System.currentTimeMillis();
        long lastReceivedBlockTime = blockFetchService.getLastReceivedBlockTime();
        long blockReceiveDelayThreshold = getBlockReceiveDelayThreshold();
        long timeSinceLastReceivedBlock = (lastReceivedBlockTime == 0) ? 0 : (currentTime - lastReceivedBlockTime);

        // A new session gets the full threshold to receive its first block, counted from when it started.
        // Without this, a restart that has not received a block yet is judged by the time of the last block
        // before the outage and is restarted again at the next check.
        long lastActivityTime = Math.max(lastReceivedBlockTime, blockFetchService.getLastSessionStartTime());

        // Consider blocks are being received if:
        // - No session started and no block received yet OR
        // - Time since the last block, or since the session started, is within the threshold
        boolean isReceivingBlocks = (lastActivityTime == 0) || (currentTime - lastActivityTime <= blockReceiveDelayThreshold);

        boolean isConnectionAlive = blockFetchService.isRunning();

        return HealthStatus.builder()
                .isConnectionAlive(isConnectionAlive)
                .isScheduleToStop(blockFetchService.isScheduledToStop())
                .isError(blockFetchService.isError())
                .lastKeepAliveResponseCookie(blockFetchService.getLastKeepAliveResponseCookie())
                .lastKeepAliveResponseTime(blockFetchService.getLastKeepAliveResponseTime())
                .lastReceivedBlockTime(lastReceivedBlockTime)
                .timeSinceLastBlock(timeSinceLastReceivedBlock)
                .isReceivingBlocks(isReceivingBlocks)
                .blockReceiveDelayThreshold(blockReceiveDelayThreshold)
                .build();
    }

    private long getBlockReceiveDelayThreshold() {
        // Convert seconds to milliseconds
        return storeProperties.getBlockReceiveDelaySeconds() * 1000L;
    }
}
