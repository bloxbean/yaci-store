package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION;

@RequiredArgsConstructor
@Slf4j
public class TransactionStorageImpl implements TransactionStorage {
    private final static String PLUGIN_TRANSACTION_SAVE = "transaction.save";

    private final TxnEntityRepository txnEntityRepository;
    private final TxnMapper mapper;
    private final DSLContext dsl;

    @Override
    @Plugin(key = PLUGIN_TRANSACTION_SAVE)
    public void saveAll(List<Txn> txnList) {
        List<TxnEntity> txnEntities = txnList.stream().map(mapper::toTxnEntity).collect(Collectors.toList());
        txnEntityRepository.saveAll(txnEntities);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txnEntityRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    @Transactional
    public int deleteBySlotLessThan(long slot) {
        return dsl.deleteFrom(TRANSACTION).where(TRANSACTION.SLOT.lessThan(slot)).execute();
    }
}
