package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.transaction.storage.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.InvalidTransactionEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.InvalidTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class InvalidTransactionStorageImpl implements InvalidTransactionStorage {
    private final InvalidTransactionRepository repository;
    private final TxnMapper mapper;

    @Override
    public InvalidTransaction save(InvalidTransaction invalidTransaction) {
        InvalidTransactionEntity entity =
                repository.save(mapper.toInvalidTransactionEntity(invalidTransaction));
        return mapper.toInvalidTransaction(entity);
    }

    @Override
    public int deleteBySlotGreaterThan(Long slot) {
        return repository.deleteBySlotGreaterThan(slot);
    }
}
