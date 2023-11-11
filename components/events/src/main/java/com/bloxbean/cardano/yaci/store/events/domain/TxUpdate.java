package com.bloxbean.cardano.yaci.store.events.domain;

import com.bloxbean.cardano.yaci.core.model.Update;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TxUpdate {
    private String txHash;
    private Update update;
}
