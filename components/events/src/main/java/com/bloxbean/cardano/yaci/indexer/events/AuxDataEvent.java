package com.bloxbean.cardano.yaci.indexer.events;

import com.bloxbean.cardano.yaci.indexer.events.model.TxAuxData;
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
