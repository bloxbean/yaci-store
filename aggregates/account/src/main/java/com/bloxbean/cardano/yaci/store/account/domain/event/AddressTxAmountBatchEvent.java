package com.bloxbean.cardano.yaci.store.account.domain.event;

import com.bloxbean.cardano.yaci.store.account.domain.AddressTxAmount;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class AddressTxAmountBatchEvent {
    private EventMetadata metadata;
    private List<AddressTxAmount> addressTxAmountList;
}
