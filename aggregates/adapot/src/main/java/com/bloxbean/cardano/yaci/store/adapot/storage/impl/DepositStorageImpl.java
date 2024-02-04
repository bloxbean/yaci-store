package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.Deposit;
import com.bloxbean.cardano.yaci.store.adapot.storage.DepositStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.AdaPotMapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.DepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DepositStorageImpl implements DepositStorage {

    private final DepositRepository depositRepository;
    private final AdaPotMapper adaPotMapper;

    @Override
    public void save(List<Deposit> deposit) {
        if (deposit == null || deposit.isEmpty())
            return;

        depositRepository.saveAll(deposit.stream()
                .map(adaPotMapper::toDepositEntity).collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return depositRepository.deleteBySlotGreaterThan(slot);
    }
}
