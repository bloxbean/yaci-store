package com.bloxbean.cardano.yaci.store.governance.storage.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalTreasuryWithdrawal;

import java.util.List;

public interface LocalTreasuryWithdrawalStorage {
    void saveAll(List<LocalTreasuryWithdrawal> localTreasuryWithdrawals);

    int deleteBySlotGreaterThan(long slot);
}
