package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnWitnessEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnWitnessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION_WITNESS;

@RequiredArgsConstructor
@Slf4j
public class TransactionWitnessStorageImpl implements TransactionWitnessStorage {
    private final static String PLUGIN_TRANSACTION_WITNESS_SAVE = "transaction.witness.save";

    private final TxnWitnessRepository txnWitnessRepository;
    private final TxnMapper mapper;
    private final DSLContext dsl;

    @Override
    @Plugin(key = PLUGIN_TRANSACTION_WITNESS_SAVE)
    public void saveAll(List<TxnWitness> txnWitnesses) {
        List<TxnWitnessEntity> txnWitnessEntities = txnWitnesses.stream().map(mapper::toTxnWitnessEntity).toList();
        txnWitnessRepository.saveAll(txnWitnessEntities);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txnWitnessRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    @Transactional
    public int deleteBySlotLessThan(long slot) {
        return dsl.deleteFrom(TRANSACTION_WITNESS).where(TRANSACTION_WITNESS.SLOT.lessThan(slot)).execute();
    }
}
