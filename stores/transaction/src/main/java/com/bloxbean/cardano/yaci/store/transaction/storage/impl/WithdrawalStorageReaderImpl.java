package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.transaction.storage.WithdrawalStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
public class WithdrawalStorageReaderImpl implements WithdrawalStorageReader {
    private final WithdrawalRepository withdrawalRepository;
    private final TxnMapper mapper;

    @Override
    public List<Withdrawal> getWithdrawals(int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC, "slot");

        return withdrawalRepository.findAllWithdrawals(pageable)
                .stream().map(mapper::toWithdrawal).toList();
    }

    @Override
    public List<Withdrawal> getWithdrawalsByTxHash(String txHash) {
        return withdrawalRepository.findByTxHash(txHash)
                .stream().map(mapper::toWithdrawal).toList();
    }

    @Override
    public List<Withdrawal> getWithdrawalsByAddress(String address, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC, "slot");

        return withdrawalRepository.findByAddress(address, pageable)
                .stream().map(mapper::toWithdrawal).toList();
    }
}
