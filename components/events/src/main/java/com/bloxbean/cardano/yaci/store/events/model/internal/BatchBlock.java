package com.bloxbean.cardano.yaci.store.events.model.internal;

import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BatchBlock {
    private EventMetadata eventMetadata;
    private Block block;
    private List<Transaction> transactions;
}
