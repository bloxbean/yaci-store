package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;

import java.util.List;

public interface WithdrawalStorage {
    void save(List<Withdrawal> withdrawals);
    int deleteBySlotGreaterThan(Long slot);
}
