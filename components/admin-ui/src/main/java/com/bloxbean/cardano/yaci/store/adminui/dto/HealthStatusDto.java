package com.bloxbean.cardano.yaci.store.adminui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusDto {
    private boolean connectionAlive;
    private boolean receivingBlocks;
    private boolean scheduledToStop;
    private boolean error;
    private long lastReceivedBlockTime;
    private long timeSinceLastBlock;
    private long blockReceiveDelayThreshold;
}
