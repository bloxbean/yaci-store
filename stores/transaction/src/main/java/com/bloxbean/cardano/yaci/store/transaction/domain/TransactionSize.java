package com.bloxbean.cardano.yaci.store.transaction.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionSize {

    int size;
    int scriptSize;

}
