package com.bloxbean.cardano.yaci.store.extensions.assetstore.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import com.bloxbean.cardano.yaci.store.core.service.SyncStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Readiness health indicator for on-chain sync progress.
 * <p>
 * Reports whether the indexer has caught up to the chain tip. A pod that is still
 * syncing historical blocks reports {@code OUT_OF_SERVICE} so that traffic is not
 * routed to it prematurely.
 * Ported from cf-token-metadata-registry's {@code OnchainReadinessHealthIndicator}.
 * <p>
 * Uses yaci-store's built-in {@link SyncStatusService} to determine sync percentage
 * and whether the node is fully synced (within 10 blocks of tip).
 * <p>
 * The consuming application decides which probe group this belongs to.
 *
 * <ul>
 *   <li>{@code UP} — fully synced to chain tip</li>
 *   <li>{@code OUT_OF_SERVICE} — still syncing, or not receiving blocks, or scheduled to stop</li>
 *   <li>{@code DOWN} — connection lost or sync error</li>
 *   <li>{@code UNKNOWN} — block fetcher not yet initialized</li>
 * </ul>
 */
@Component("assetStoreOnchainReadiness")
@RequiredArgsConstructor
public class OnchainReadinessHealthIndicator implements HealthIndicator {

    private final HealthService healthService;
    private final SyncStatusService syncStatusService;

    @Override
    public Health health() {
        HealthStatus status;
        try {
            status = healthService.getHealthStatus();
        } catch (NullPointerException e) {
            return Health.unknown()
                    .withDetail("syncStatus", "Block fetcher not initialized")
                    .build();
        }

        Health.Builder builder = new Health.Builder()
                .withDetail("connectionAlive", status.isConnectionAlive())
                .withDetail("receivingBlocks", status.isReceivingBlocks())
                .withDetail("error", status.isError())
                .withDetail("timeSinceLastBlockMs", status.getTimeSinceLastBlock());

        if (status.isScheduleToStop()) {
            return builder.outOfService()
                    .withDetail("syncStatus", "Scheduled to stop")
                    .build();
        }

        if (status.isError() || !status.isConnectionAlive()) {
            return builder.down()
                    .withDetail("syncStatus", "Connection lost or sync error")
                    .build();
        }

        if (!status.isReceivingBlocks()) {
            return builder.outOfService()
                    .withDetail("syncStatus", "Not receiving blocks")
                    .build();
        }

        com.bloxbean.cardano.yaci.store.common.domain.SyncStatus syncStatus = syncStatusService.getSyncStatus();
        builder.withDetail("syncPercentage", String.format(java.util.Locale.US, "%.2f%%", syncStatus.syncPercentage()));

        if (!syncStatus.synced()) {
            return builder.outOfService()
                    .withDetail("syncStatus", "Syncing")
                    .build();
        }

        return builder.up()
                .withDetail("syncStatus", "Synced")
                .build();
    }
}
