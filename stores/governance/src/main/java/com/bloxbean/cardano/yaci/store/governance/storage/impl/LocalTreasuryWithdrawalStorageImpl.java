package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalTreasuryWithdrawal;
import com.bloxbean.cardano.yaci.store.governance.storage.LocalTreasuryWithdrawalStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalTreasuryWithdrawalMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalTreasuryWithdrawalRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LocalTreasuryWithdrawalStorageImpl implements LocalTreasuryWithdrawalStorage {
    private final LocalTreasuryWithdrawalRepository localTreasuryWithdrawalRepository;
    private final LocalTreasuryWithdrawalMapper localTreasuryWithdrawalMapper;

    @Override
    public void saveAll(List<LocalTreasuryWithdrawal> localTreasuryWithdrawals) {
        localTreasuryWithdrawalRepository.saveAll(localTreasuryWithdrawals.stream()
                .map(localTreasuryWithdrawalMapper::toLocalTreasuryWithdrawalEntity)
                .toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return localTreasuryWithdrawalRepository.deleteBySlotGreaterThan(slot);
    }
}
