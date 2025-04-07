package com.bloxbean.cardano.yaci.store.events.internal;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class CommitEvent {
    private EventMetadata metadata;
    private List<BatchEvent> blockCaches;
}
