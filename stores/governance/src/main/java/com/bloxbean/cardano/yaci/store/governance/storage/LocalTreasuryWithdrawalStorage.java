package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalTreasuryWithdrawal;

import java.util.List;

public interface LocalTreasuryWithdrawalStorage {
    void saveAll(List<LocalTreasuryWithdrawal> localTreasuryWithdrawals);

    int deleteBySlotGreaterThan(long slot);
}
