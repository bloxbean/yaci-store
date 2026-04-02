package com.bloxbean.cardano.yaci.store.extensions.assetstore.health;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service.SyncStatus;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service.TokenMetadataSyncCronJob;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service.TokenMetadataSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Health indicator for CIP-26 offchain metadata sync.
 * <p>
 * Reports the status of the background GitHub token registry sync job.
 * Ported from cf-token-metadata-registry's {@code OffchainSyncHealthIndicator}.
 * <p>
 * The consuming application decides which probe group (startup, liveness, readiness)
 * this indicator belongs to via {@code management.endpoint.health.group.*} configuration.
 *
 * <ul>
 *   <li>{@code UP} — sync completed successfully or running as an external job</li>
 *   <li>{@code OUT_OF_SERVICE} — sync in progress or not yet started</li>
 *   <li>{@code DOWN} — sync encountered an error</li>
 * </ul>
 *
 * <p>Conditional on {@link TokenMetadataSyncCronJob} — this indicator is only registered when
 * the CIP-26 sync cron job is active. In read-only mode or when CIP-26 is disabled, the cron
 * job bean is absent and this indicator is silently skipped (reporting sync status without an
 * active sync job would be misleading).
 */
@Component("assetStoreOffchainSync")
@ConditionalOnBean(TokenMetadataSyncCronJob.class)
@RequiredArgsConstructor
public class OffchainSyncHealthIndicator implements HealthIndicator {

    private static final String DETAIL_SYNC_STATUS = "syncStatus";

    private final TokenMetadataSyncService tokenMetadataSyncService;

    @Override
    public Health health() {
        SyncStatus syncStatus = tokenMetadataSyncService.getSyncStatus();
        if (syncStatus == null) {
            return Health.unknown()
                    .withDetail(DETAIL_SYNC_STATUS, "Not initialized")
                    .build();
        }

        String statusText = syncStatus.getStatus().toString();

        return switch (syncStatus.getStatus()) {
            case SYNC_DONE, SYNC_IN_EXTRA_JOB -> Health.up()
                    .withDetail(DETAIL_SYNC_STATUS, statusText)
                    .build();
            case SYNC_IN_PROGRESS, SYNC_NOT_STARTED -> Health.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, statusText)
                    .build();
            case SYNC_ERROR -> Health.down()
                    .withDetail(DETAIL_SYNC_STATUS, statusText)
                    .build();
        };
    }
}
