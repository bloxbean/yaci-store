package com.bloxbean.cardano.yaci.store.core.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import com.bloxbean.cardano.yaci.store.core.service.SyncStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Spring Boot Actuator health indicator reporting how close yaci-store is to
 * the Cardano chain tip.
 *
 * <p>This is a <b>readiness-style</b> probe — it answers "is yaci-store caught
 * up enough to serve traffic". A pod that is still indexing historical blocks
 * reports {@code OUT_OF_SERVICE} so that load balancers and Kubernetes
 * readiness probes don't route traffic to it prematurely. Connection liveness
 * (without sync progress) is reported separately by
 * {@link NodeHealthIndicator}.
 *
 * <p>Sync progress is determined by {@link SyncStatusService}, which considers
 * the indexer "synced" when it is within 10 blocks of the tip.
 *
 * <p>Status mapping:
 * <ul>
 *   <li>{@code UP} — fully synced to chain tip</li>
 *   <li>{@code OUT_OF_SERVICE} — still syncing, or not receiving blocks, or scheduled to stop</li>
 *   <li>{@code DOWN} — connection lost or sync error</li>
 *   <li>{@code UNKNOWN} — block fetcher not yet initialised at the time of the probe</li>
 * </ul>
 *
 * <p>Conditional on {@link HealthService}: in read-only mode
 * ({@code store.read-only-mode=true}) neither {@code HealthService} nor
 * {@code SyncStatusService} is created (both are {@code @ReadOnly(false)}),
 * so this indicator is silently skipped instead of failing on a missing
 * dependency.
 */
@Component("nodeSync")
@ConditionalOnBean(HealthService.class)
@RequiredArgsConstructor
public class NodeSyncIndicator implements HealthIndicator {

    private static final String DETAIL_SYNC_STATUS = "syncStatus";

    private final HealthService healthService;
    private final SyncStatusService syncStatusService;

    @Override
    public Health health() {
        HealthStatus status;
        try {
            status = healthService.getHealthStatus();
        } catch (NullPointerException e) {
            return Health.unknown()
                    .withDetail(DETAIL_SYNC_STATUS, "Block fetcher not initialized")
                    .build();
        }

        Health.Builder builder = new Health.Builder()
                .withDetail("connectionAlive", status.isConnectionAlive())
                .withDetail("receivingBlocks", status.isReceivingBlocks())
                .withDetail("error", status.isError())
                .withDetail("timeSinceLastBlockMs", status.getTimeSinceLastBlock());

        if (status.isScheduleToStop()) {
            return builder.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, "Scheduled to stop")
                    .build();
        }

        if (status.isError() || !status.isConnectionAlive()) {
            return builder.down()
                    .withDetail(DETAIL_SYNC_STATUS, "Connection lost or sync error")
                    .build();
        }

        if (!status.isReceivingBlocks()) {
            return builder.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, "Not receiving blocks")
                    .build();
        }

        SyncStatus syncStatus = syncStatusService.getSyncStatus();
        builder.withDetail("syncPercentage",
                String.format(Locale.US, "%.2f%%", syncStatus.syncPercentage()));

        if (!syncStatus.synced()) {
            return builder.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, "Syncing")
                    .build();
        }

        return builder.up()
                .withDetail(DETAIL_SYNC_STATUS, "Synced")
                .build();
    }
}
