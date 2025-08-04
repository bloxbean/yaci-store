package com.bloxbean.cardano.yaci.store.admin.service;

import com.bloxbean.cardano.yaci.store.core.service.StartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(value = "store.admin.auto-recovery-enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class AutoRecoveryStartService {
    private final HealthService healthService;
    private final StartService startService;
    private AtomicBoolean waitingToStart = new AtomicBoolean(false);

    @Scheduled(fixedRateString = "${store.admin.health-check-interval}", initialDelay = 120, timeUnit = TimeUnit.SECONDS)
    public void checkHealthAndStart() {
        var healthStatus = healthService.getHealthStatus();

        if (healthStatus.isScheduleToStop())
            return;

        if(healthStatus.isConnectionAlive() && !healthStatus.isError()) {
            if (log.isDebugEnabled())
                log.debug("Connection is alive.");
            return;
        }

        if(waitingToStart.get()) { //Already waiting to start
            return;
        }

        //Schedule to start the service
        Thread.startVirtualThread(() -> {
            waitingToStart.set(true);
            log.info("Waiting for 10 seconds before restarting the sync process ...");
            try {
                TimeUnit.SECONDS.sleep(10); //TODO -- Configurable
            } catch (InterruptedException e) {
            }
            synchronized (this) {
                try {
                    startService.stop();
                    startService.start();
                } catch (Exception e) {
                    log.error("Error starting the service", e);
                }
            }

            waitingToStart.set(false);
        });
    }

}
