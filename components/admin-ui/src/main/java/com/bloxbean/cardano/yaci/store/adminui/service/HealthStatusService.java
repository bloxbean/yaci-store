package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.HealthStatusDto;
import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthStatusService {
    private final HealthService healthService;

    public HealthStatusDto getHealthStatus() {
        HealthStatus health = healthService.getHealthStatus();

        return HealthStatusDto.builder()
                .connectionAlive(health.isConnectionAlive())
                .receivingBlocks(health.isReceivingBlocks())
                .scheduledToStop(health.isScheduleToStop())
                .error(health.isError())
                .lastReceivedBlockTime(health.getLastReceivedBlockTime())
                .timeSinceLastBlock(health.getTimeSinceLastBlock())
                .blockReceiveDelayThreshold(health.getBlockReceiveDelayThreshold())
                .build();
    }
}
