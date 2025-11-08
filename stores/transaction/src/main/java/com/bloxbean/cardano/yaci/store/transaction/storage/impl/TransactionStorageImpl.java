package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnCbor;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnCborEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnCborRepository;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION_CBOR;

@RequiredArgsConstructor
@Slf4j
public class TransactionStorageImpl implements TransactionStorage {
    private final static String PLUGIN_TRANSACTION_SAVE = "transaction.save";

    private final TxnEntityRepository txnEntityRepository;
    private final TxnCborRepository txnCborRepository;
    private final TxnMapper mapper;
    private final DSLContext dsl;
    private final TransactionStoreProperties transactionStoreProperties;

    @Override
    @Plugin(key = PLUGIN_TRANSACTION_SAVE)
    @Transactional
    public void saveAll(List<Txn> txnList) {
        List<TxnEntity> txnEntities = txnList.stream()
                .map(mapper::toTxnEntity)
                .collect(Collectors.toList());
        txnEntityRepository.saveAll(txnEntities);
    }

    @Override
    @Transactional
    public int deleteBySlotGreaterThan(long slot) {
        if (transactionStoreProperties.isSaveCbor()) {
            txnCborRepository.deleteBySlotGreaterThan(slot);
        }
        
        return txnEntityRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    @Transactional
    public int deleteBySlotLessThan(long slot) {
        if (transactionStoreProperties.isSaveCbor()) {
            dsl.deleteFrom(TRANSACTION_CBOR)
                    .where(TRANSACTION_CBOR.SLOT.lessThan(slot))
                    .execute();
        }
        return dsl.deleteFrom(TRANSACTION)
                .where(TRANSACTION.SLOT.lessThan(slot))
                .execute();
    }
    
    @Override
    public void saveCbor(List<TxnCbor> txnCborList) {
        List<TxnCborEntity> cborEntities = txnCborList.stream()
                .filter(txnCbor -> txnCbor.getCborData() != null && txnCbor.getCborData().length > 0)
                .map(txnCbor -> TxnCborEntity.builder()
                        .txHash(txnCbor.getTxHash())
                        .cborData(txnCbor.getCborData())
                        .cborSize(txnCbor.getCborSize())
                        .slot(txnCbor.getSlot())
                        .build())
                .collect(Collectors.toList());
        
        if (!cborEntities.isEmpty()) {
            txnCborRepository.saveAll(cborEntities);
        }
    }
}
