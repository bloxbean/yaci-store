package com.bloxbean.cardano.yaci.store.adapot.event.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * This is an internal event to do required steps before actual reward calculation and governance calculation in
 * background job. For example: Proposal refund processing
 */
@Getter
@AllArgsConstructor
@Builder
public class PreAdaPotJobProcessingEvent {
    private int epoch;
    private long slot;
}
