package com.bloxbean.cardano.yaci.indexer.events.domain;

import com.bloxbean.cardano.yaci.core.model.AuxData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TxAuxData {
    private String txHash;
    private AuxData auxData;
}
