package com.bloxbean.cardano.yaci.store.admin.service;

import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ReadOnly(false)
@ConditionalOnProperty(value = "store.admin.auto-recovery-enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class AutoRecoveryStartService {
    private final HealthService healthService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRateString = "${store.admin.health-check-interval:120}", initialDelay = 120, timeUnit = TimeUnit.SECONDS)
    public void checkHealthAndStart() {
        var healthStatus = healthService.getHealthStatus();

        if (healthStatus.isScheduleToStop())
            return;

        if(healthStatus.isConnectionAlive() && !healthStatus.isError()) {
            if (log.isDebugEnabled())
                log.debug("Connection is alive.");
            return;
        }

        // Publish restart event - let RequiredRestartProcessor handle the restart
        RequiredSyncRestartEvent restartEvent = RequiredSyncRestartEvent.builder()
                .reason("HealthCheckFailed")
                .errorCode("HEALTH_CHECK_FAILED")
                .timestamp(System.currentTimeMillis())
                .source("AutoRecoveryStartService")
                .details("Connection not alive or sync error detected")
                .build();

        eventPublisher.publishEvent(restartEvent);
        log.info("Published restart event due to health check failure");
    }

}
