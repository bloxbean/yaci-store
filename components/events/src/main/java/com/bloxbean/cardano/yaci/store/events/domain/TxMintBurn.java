package com.bloxbean.cardano.yaci.store.events.domain;

import com.bloxbean.cardano.yaci.core.model.Amount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TxMintBurn {
    private String txHash;
    private List<Amount> amounts;
}
