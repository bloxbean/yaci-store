package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionEvent {
    private EventMetadata metadata;
    private List<Transaction> transactions;
}
