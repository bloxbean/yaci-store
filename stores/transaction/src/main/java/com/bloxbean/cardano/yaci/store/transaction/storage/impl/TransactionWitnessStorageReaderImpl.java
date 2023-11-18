package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnWitnessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class TransactionWitnessStorageReaderImpl implements TransactionWitnessStorageReader {
    private final TxnWitnessRepository txnWitnessRepository;
    private final TxnMapper mapper;


    @Override
    public List<TxnWitness> getTransactionWitnesses(String txHash) {
        return txnWitnessRepository.findByTxHash(txHash)
                .stream().map(mapper::toTxnWitness).toList();

    }
}
