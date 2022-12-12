package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.store.events.domain.TxAuxData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuxDataEvent {
    private EventMetadata metadata;
    private List<TxAuxData> txAuxDataList;
}
