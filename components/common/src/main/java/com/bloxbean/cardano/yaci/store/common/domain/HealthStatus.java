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
    private long lastReceivedBlockTime;

    /**
     * Time since last block was received in milliseconds
     */
    private long timeSinceLastBlock;

    /**
     * Indicates if blocks are being received within the configured threshold
     */
    private boolean isReceivingBlocks;

    /**
     * The configured block receive delay threshold in milliseconds
     */
    private long blockReceiveDelayThreshold;
}
