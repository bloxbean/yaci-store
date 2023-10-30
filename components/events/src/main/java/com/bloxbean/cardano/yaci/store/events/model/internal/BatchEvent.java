package com.bloxbean.cardano.yaci.store.events.model.internal;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;

public interface BatchEvent {
    EventMetadata getMetadata();
}
