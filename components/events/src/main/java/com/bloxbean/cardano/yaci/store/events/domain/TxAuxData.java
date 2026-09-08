package com.bloxbean.cardano.yaci.store.events.domain;

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
    /** Index of the transaction within its block (0-based), counting invalid transactions. */
    private Integer txIndex;
    private AuxData auxData;
}
