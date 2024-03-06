package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.transaction.domain.InvalidTransaction;

public interface InvalidTransactionStorage {
    InvalidTransaction save(InvalidTransaction invalidTransaction);
    int deleteBySlotGreaterThan(Long slot);
}
