package com.bloxbean.cardano.yaci.store.core.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Spring Boot Actuator health indicator reporting the state of the Cardano node
 * connection (n2n / n2c, depending on configuration).
 *
 * <p>This is a <b>liveness-style</b> probe — it answers "is yaci-store currently
 * able to talk to a Cardano node and receive blocks". It does <b>not</b> evaluate
 * sync progress; for the readiness equivalent see {@link CardanoNodeSyncHealthIndicator}.
 *
 * <p>Status mapping:
 * <ul>
 *   <li>{@code UP} — connected and receiving blocks</li>
 *   <li>{@code OUT_OF_SERVICE} — connected but not receiving blocks, or sync is scheduled to stop</li>
 *   <li>{@code DOWN} — connection lost or sync error</li>
 *   <li>{@code UNKNOWN} — block fetcher not yet initialised at the time of the probe</li>
 * </ul>
 *
 * <p>Conditional on {@link HealthService}: in read-only mode
 * ({@code store.read-only-mode=true}) the {@code HealthService} bean is not
 * created (it is {@code @ReadOnly(false)}), so this indicator is silently
 * skipped instead of failing on a missing dependency.
 */
@Component("cardanoNodeConnection")
@ConditionalOnBean(HealthService.class)
@RequiredArgsConstructor
public class CardanoNodeConnectionHealthIndicator implements HealthIndicator {

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
