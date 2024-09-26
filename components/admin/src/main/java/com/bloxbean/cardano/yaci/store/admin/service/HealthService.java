package com.bloxbean.cardano.yaci.store.admin.service;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.BlockFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "store",
        name = "read-only-mode",
        havingValue = "false",
        matchIfMissing = true
)
@RequiredArgsConstructor
@Slf4j
public class HealthService {
    private final BlockFetchService blockFetchService;

    public HealthStatus getHealthStatus() {

        return HealthStatus.builder()
                .isConnectionAlive(blockFetchService.isRunning())
                .isScheduleToStop(blockFetchService.isScheduledToStop())
                .isError(blockFetchService.isError())
                .lastKeepAliveResponseCookie(blockFetchService.getLastKeepAliveResponseCookie())
                .lastKeepAliveResponseTime(blockFetchService.getLastKeepAliveResponseTime())
                .lastReceivedBlockTime(blockFetchService.getLastReceivedBlockTime())
                .build();
    }
}
