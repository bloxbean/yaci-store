package com.bloxbean.cardano.yaci.store.events.internal;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PreCommitEvent {
    private EventMetadata metadata;
}
