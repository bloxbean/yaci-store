package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnEntityJpa;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class TransactionStorageImpl implements TransactionStorage {
    private final TxnEntityRepository txnEntityRepository;
    private final TxnMapper mapper;
    private final DSLContext dsl;

    @Override
    public void saveAll(List<Txn> txnList) {
        List<TxnEntityJpa> txnEntities = txnList.stream().map(mapper::toTxnEntity).collect(Collectors.toList());
        txnEntityRepository.saveAll(txnEntities);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txnEntityRepository.deleteBySlotGreaterThan(slot);
    }
}
