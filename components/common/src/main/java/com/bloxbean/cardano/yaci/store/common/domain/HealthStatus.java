package com.bloxbean.cardano.yaci.store.common.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthStatus {
    private boolean isConnectionAlive;

    /**
     * This is to indicate if the service is scheduled to stop
     */
    private boolean isScheduleToStop;

    /**
     * This is to indicate if there is any error
     */
    private boolean isError;

    private int lastKeepAliveResponseCookie;
    private long lastKeepAliveResponseTime;
}
