package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.utxo.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.InvalidTransactionEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.InvalidTransactionRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.InvalidTransactionStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
public class InvalidTransactionStorageImpl implements InvalidTransactionStorage {
    private final InvalidTransactionRepository repository;
    private final UtxoMapper mapper;

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
