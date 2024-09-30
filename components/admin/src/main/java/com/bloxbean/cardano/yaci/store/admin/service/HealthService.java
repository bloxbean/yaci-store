package com.bloxbean.cardano.yaci.store.admin.service;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.BlockFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@ReadOnly(false)
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
