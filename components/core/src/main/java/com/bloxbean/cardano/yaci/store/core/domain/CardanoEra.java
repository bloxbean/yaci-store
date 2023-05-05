package com.bloxbean.cardano.yaci.store.core.domain;

import com.bloxbean.cardano.yaci.core.model.Era;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardanoEra {
    private Era era;
    private long startSlot;
    private long block;
    private String blockHash;
}
