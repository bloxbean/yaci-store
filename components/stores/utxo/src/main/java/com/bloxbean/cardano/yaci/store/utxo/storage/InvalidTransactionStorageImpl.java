package com.bloxbean.cardano.yaci.store.utxo.storage;

import com.bloxbean.cardano.yaci.store.utxo.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.utxo.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.model.InvalidTransactionEntity;
import com.bloxbean.cardano.yaci.store.utxo.repository.InvalidTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvalidTransactionStorageImpl implements InvalidTransactionStorage{
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
