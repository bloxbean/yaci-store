package com.bloxbean.cardano.yaci.store.blockfrost.account.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;
import java.util.Optional;

public interface BFAccountStorageReader {
    Optional<AccountInfo> getAccountInfo(String stakeAddress);
    List<AccountReward> findRewards(String stakeAddress, int page, int count, Order order);
    List<AccountHistory> findHistory(String stakeAddress, int page, int count, Order order);
    List<AccountDelegation> findDelegations(String stakeAddress, int page, int count, Order order);
    List<AccountRegistration> findRegistrations(String stakeAddress, int page, int count, Order order);
    List<AccountWithdrawal> findWithdrawals(String stakeAddress, int page, int count, Order order);
    List<AccountMir> findMIRs(String stakeAddress, int page, int count, Order order);
    List<AccountAddress> findAddresses(String stakeAddress, int page, int count, Order order);
    List<AccountAddressAsset> findAddressAssets(String stakeAddress, int page, int count, Order order);
    Optional<AccountAddressesTotal> getAddressesTotal(String stakeAddress);
    List<AccountUtxo> findUtxos(String stakeAddress, int page, int count, Order order);
    List<AccountTransaction> findTransactions(String stakeAddress, int page, int count, Order order, String from, String to);
}
