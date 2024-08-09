package com.bloxbean.cardano.yaci.store.events.internal;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class EpochTransitionCommitEvent {
    private EventMetadata metadata;
    private Integer previousEpoch;
    private Integer epoch;
    private Era previousEra;
    private Era era;
}
