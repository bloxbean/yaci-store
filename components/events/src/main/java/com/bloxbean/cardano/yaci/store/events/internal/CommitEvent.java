package com.bloxbean.cardano.yaci.store.events.internal;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class CommitEvent<T> {
    private EventMetadata metadata;
    private List<T> blockCaches;
}
