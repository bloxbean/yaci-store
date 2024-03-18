package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnEntityJpa;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnEntityRepository;
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
public class TransactionStorageReaderImpl implements TransactionStorageReader {
    private final TxnEntityRepository txnEntityRepository;
    private final TxnMapper mapper;
    private final DSLContext dsl;

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

        List<Txn> transactions = query.fetch().into(TxnEntityJpa.class)
                .stream().map(mapper::toTxn).collect(Collectors.toList());

        return transactions;

// TODO -- this is the old code using JPA repository where count query takes too long
//        return txnEntityRepository.findAll(sortedBySlot)
//                .map(mapper::toTxn);
    }

    @Override
    public List<Txn> getTransactionsByBlockHash(String blockHash) {
        return txnEntityRepository.findAllByBlockHash(blockHash)
                .stream()
                .map(mapper::toTxn).collect(Collectors.toList());
    }

    @Override
    public List<Txn> getTransactionsByBlockNumber(long blockNumber) {
        return txnEntityRepository.findAllByBlockNumber(blockNumber)
                .stream()
                .map(mapper::toTxn).collect(Collectors.toList());
    }

}
