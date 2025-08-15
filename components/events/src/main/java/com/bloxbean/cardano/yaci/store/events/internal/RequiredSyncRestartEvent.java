package com.bloxbean.cardano.yaci.store.events.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Internal event published when sync needs to be restarted due to recoverable errors.
 * This event triggers the RequiredRestartProcessor to handle sync restart with proper backpressure.
 */
@Getter
@AllArgsConstructor
@Builder
public class RequiredSyncRestartEvent {
    private final String reason;
    private final String errorCode;
    private final long timestamp;
    private final String source;
    private final String details;
}