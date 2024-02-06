package com.bloxbean.cardano.yaci.store.core.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthStatus {
    private boolean isConnectionAlive;
    private int lastKeepAliveResponseCookie;
    private long lastKeepAliveResponseTime;
}
