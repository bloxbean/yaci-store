package com.bloxbean.cardano.yaci.store.extensions.assetstore.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the Cardano node connection.
 * <p>
 * Checks that the on-chain connection is alive and blocks are being received.
 * Does NOT check sync progress — use {@link OnchainReadinessHealthIndicator} for that.
 * Ported from cf-token-metadata-registry's {@code OnchainConnectionHealthIndicator}.
 * <p>
 * The consuming application decides which probe group this belongs to.
 *
 * <ul>
 *   <li>{@code UP} — connected and receiving blocks</li>
 *   <li>{@code OUT_OF_SERVICE} — connected but not receiving blocks, or scheduled to stop</li>
 *   <li>{@code DOWN} — connection lost or sync error</li>
 *   <li>{@code UNKNOWN} — block fetcher not yet initialized</li>
 * </ul>
 */
@Component("assetStoreOnchainConnection")
@RequiredArgsConstructor
public class OnchainConnectionHealthIndicator implements HealthIndicator {

    private static final String DETAIL_SYNC_STATUS = "syncStatus";

    private final HealthService healthService;

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
                .withDetail("error", status.isError());

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

        if (status.isScheduleToStop()) {
            return builder.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, "Scheduled to stop")
                    .build();
        }

        return builder.up()
                .withDetail(DETAIL_SYNC_STATUS, "Connected")
                .build();
    }
}
