package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.Deposit;

import java.util.List;

public interface DepositStorage {
    void save(List<Deposit> deposit);

    int deleteBySlotGreaterThan(long slot);
}
