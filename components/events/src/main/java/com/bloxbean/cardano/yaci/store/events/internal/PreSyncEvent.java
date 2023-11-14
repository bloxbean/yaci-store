package com.bloxbean.cardano.yaci.store.events.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * This is an internal event. It is published to perform any necessary initialization or pending processes that need to
 * be completed before the actual sync process begins. This is only published if the startBlock > 0
 */
@Getter
@AllArgsConstructor
@Builder
public class PreSyncEvent {
    private Long startBlock;
}
