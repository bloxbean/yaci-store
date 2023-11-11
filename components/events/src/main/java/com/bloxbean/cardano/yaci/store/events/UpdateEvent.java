package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.store.events.domain.TxUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateEvent {
    private EventMetadata metadata;
    private List<TxUpdate> updates;
}
