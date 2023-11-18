package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.utxo.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.utxo.storage.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.InvalidTransactionEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.InvalidTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class InvalidTransactionStorageImpl implements InvalidTransactionStorage {
    private final InvalidTransactionRepository repository;
    private final UtxoMapper mapper = UtxoMapper.INSTANCE;

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
