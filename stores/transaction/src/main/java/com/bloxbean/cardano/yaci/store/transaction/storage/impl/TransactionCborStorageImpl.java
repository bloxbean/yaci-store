package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnCbor;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionCborStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnCborEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnCborRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class TransactionCborStorageImpl implements TransactionCborStorage {

    private final TxnCborRepository txnCborRepository;
    private final TransactionStoreProperties transactionStoreProperties;

    @Override
    @Transactional
    public void save(List<TxnCbor> txnCborList) {
        if (!transactionStoreProperties.isSaveCbor() || txnCborList == null || txnCborList.isEmpty()) {
            return;
        }

        List<TxnCborEntity> cborEntities = txnCborList.stream()
                .filter(txnCbor -> txnCbor.getCborData() != null && txnCbor.getCborData().length > 0)
                .map(txnCbor -> TxnCborEntity.builder()
                        .txHash(txnCbor.getTxHash())
                        .cborData(txnCbor.getCborData())
                        .cborSize(txnCbor.getCborSize())
                        .slot(txnCbor.getSlot())
                        .build())
                .collect(Collectors.toList());

        if (cborEntities.isEmpty()) {
            return;
        }

        txnCborRepository.saveAll(cborEntities);
    }

    @Override
    @Transactional
    public int deleteBySlotGreaterThan(long slot) {
        if (!transactionStoreProperties.isSaveCbor()) {
            return 0;
        }
        return txnCborRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    @Transactional
    public int deleteBySlotLessThan(long slot) {
        if (!transactionStoreProperties.isSaveCbor()) {
            return 0;
        }
        return txnCborRepository.deleteBySlotLessThan(slot);
    }
}
