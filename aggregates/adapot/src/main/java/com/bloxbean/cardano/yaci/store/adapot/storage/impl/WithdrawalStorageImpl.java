package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.adapot.storage.WithdrawalStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class WithdrawalStorageImpl implements WithdrawalStorage {
    private final WithdrawalRepository withdrawalRepository;
    private final Mapper mapper;

    @Override
    public void save(List<Withdrawal> withdrawals) {
        withdrawalRepository.saveAll(withdrawals.stream()
                .map(mapper::toWithdrawalEntity).collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(Long slot) {
        return withdrawalRepository.deleteBySlotGreaterThan(slot);
    }
}
