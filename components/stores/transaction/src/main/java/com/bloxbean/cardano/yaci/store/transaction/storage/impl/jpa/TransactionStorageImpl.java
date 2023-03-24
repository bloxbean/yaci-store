package com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.api.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.model.TxnEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.repository.TxnEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionStorageImpl implements TransactionStorage {
    private final TxnEntityRepository txnEntityRepository;
    private final TxnMapper mapper;

    @Override
    public void saveAll(List<Txn> txnList) {
        List<TxnEntity> txnEntities = txnList.stream().map(mapper::toTxnEntity).collect(Collectors.toList());
        txnEntityRepository.saveAll(txnEntities);
    }

    @Override
    public Optional<Txn> findByTxHash(String txHash) {
        return txnEntityRepository.findByTxHash(txHash)
                .map(mapper::toTxn);
    }

    @Override
    public Page<Txn> findAll(Pageable sortedBySlot) {
        return txnEntityRepository.findAll(sortedBySlot)
                .map(mapper::toTxn);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txnEntityRepository.deleteBySlotGreaterThan(slot);
    }
}
