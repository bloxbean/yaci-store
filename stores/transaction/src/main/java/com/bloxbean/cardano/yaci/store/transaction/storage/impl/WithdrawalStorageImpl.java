package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.transaction.storage.WithdrawalStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class WithdrawalStorageImpl implements WithdrawalStorage {
    private final static String PLUGIN_TRANSACTION_WITHDRAWAL_SAVE = "transaction.withdrawal.save";

    private final WithdrawalRepository withdrawalRepository;
    private final TxnMapper mapper;

    @Override
    @Plugin(key = PLUGIN_TRANSACTION_WITHDRAWAL_SAVE)
    public void save(List<Withdrawal> withdrawals) {
        withdrawalRepository.saveAll(withdrawals.stream()
                .map(mapper::toWithdrawalEntity).collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(Long slot) {
        return withdrawalRepository.deleteBySlotGreaterThan(slot);
    }
}
