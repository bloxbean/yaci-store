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

        // Consider blocks are being received if:
        // - No blocks received yet (lastReceivedBlockTime == 0) OR
        // - Time since last block is within threshold
        boolean isReceivingBlocks = (lastReceivedBlockTime == 0) || (timeSinceLastReceivedBlock <= blockReceiveDelayThreshold);

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
