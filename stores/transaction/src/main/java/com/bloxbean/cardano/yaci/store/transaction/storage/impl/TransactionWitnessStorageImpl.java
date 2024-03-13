package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnWitnessEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnWitnessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class TransactionWitnessStorageImpl implements TransactionWitnessStorage {
    private final TxnWitnessRepository txnWitnessRepository;
    private final TxnMapper mapper;

    @Override
    public void saveAll(List<TxnWitness> txnWitnesses) {
        List<TxnWitnessEntity> txnWitnessEntities = txnWitnesses.stream().map(mapper::toTxnWitnessEntity).toList();
        txnWitnessRepository.saveAll(txnWitnessEntities);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txnWitnessRepository.deleteBySlotGreaterThan(slot);
    }
}
