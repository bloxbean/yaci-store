package com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.api.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.model.TxnEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.repository.TxnEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.transaction.jooq.tables.Transaction.TRANSACTION;

@RequiredArgsConstructor
@Slf4j
public class TransactionStorageImpl implements TransactionStorage {
    private final TxnEntityRepository txnEntityRepository;
    private final TxnMapper mapper;
    private final DSLContext dsl;

    @Override
    public void saveAll(List<Txn> txnList) {
        List<TxnEntity> txnEntities = txnList.stream().map(mapper::toTxnEntity).collect(Collectors.toList());
        txnEntityRepository.saveAll(txnEntities);
    }

    @Override
    public Optional<Txn> getTransactionByTxHash(String txHash) {
        return txnEntityRepository.findByTxHash(txHash)
                .map(mapper::toTxn);
    }

    @Override
    public List<Txn> getTransactions(int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        var query = dsl
                .select()
                .from(TRANSACTION)
                .orderBy(order.equals(Order.desc) ? TRANSACTION.SLOT.desc() : TRANSACTION.SLOT.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<Txn> transactions = query.fetch().into(TxnEntity.class)
                .stream().map(mapper::toTxn).collect(Collectors.toList());

        return transactions;

// TODO -- this is the old code using JPA repository where count query takes too long
//        return txnEntityRepository.findAll(sortedBySlot)
//                .map(mapper::toTxn);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txnEntityRepository.deleteBySlotGreaterThan(slot);
    }
}
