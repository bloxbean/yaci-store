package com.bloxbean.cardano.yaci.store.utxo.storage;

import com.bloxbean.cardano.yaci.store.utxo.domain.InvalidTransaction;

public interface InvalidTransactionStorage {
    InvalidTransaction save(InvalidTransaction invalidTransaction);
    int deleteBySlotGreaterThan(Long slot);
}
