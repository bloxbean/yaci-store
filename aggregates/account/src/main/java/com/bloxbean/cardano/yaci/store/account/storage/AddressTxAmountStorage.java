package com.bloxbean.cardano.yaci.store.account.storage;

import com.bloxbean.cardano.yaci.store.account.domain.AddressTxAmount;

import java.util.List;

public interface AddressTxAmountStorage {
    void save(List<AddressTxAmount> addressTxAmount);
    int deleteAddressBalanceBySlotGreaterThan(Long slot);
}
