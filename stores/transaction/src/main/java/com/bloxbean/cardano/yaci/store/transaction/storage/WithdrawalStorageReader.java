package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;

import java.util.List;

public interface WithdrawalStorageReader {
    List<Withdrawal> getWithdrawals(int page, int count, Order order);
    List<Withdrawal> getWithdrawalsByTxHash(String txHash);
    List<Withdrawal> getWithdrawalsByAddress(String address, int page, int count, Order order);
}
