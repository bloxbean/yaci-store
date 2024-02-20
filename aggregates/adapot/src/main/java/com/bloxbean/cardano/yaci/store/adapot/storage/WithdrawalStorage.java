package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.Withdrawal;

import java.util.List;

public interface WithdrawalStorage {
    void save(List<Withdrawal> withdrawals);
    int deleteBySlotGreaterThan(Long slot);
}
