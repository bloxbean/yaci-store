package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.store.events.domain.TxMintBurn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MintBurnEvent {
    private EventMetadata metadata;
    private List<TxMintBurn> txMintBurns;
}
