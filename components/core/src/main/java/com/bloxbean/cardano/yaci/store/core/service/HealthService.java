package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.core.domain.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
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
                .build();
    }
}
