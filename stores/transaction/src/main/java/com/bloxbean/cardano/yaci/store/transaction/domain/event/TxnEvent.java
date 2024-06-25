package com.bloxbean.cardano.yaci.store.transaction.domain.event;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Events with list of processed transactions. If there are invalid transactions in the block, a separate event will be
 * published with the invalid transactions.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TxnEvent {
    private EventMetadata metadata;
    private List<Txn> txnList;
}
