package com.bloxbean.cardano.yaci.store.account.storage;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;

import java.util.List;
import java.util.Optional;

public interface AccountBalanceStorage {
    Optional<AddressBalance> getAddressBalance(String address, String unit, long slot);
    List<AddressBalance> getAddressBalance(String address);

    void saveAddressBalances(List<AddressBalance> addressBalances);

    int deleteAddressBalanceBySlotGreaterThan(Long slot);

    Optional<StakeAddressBalance> getAddressStakeBalance(String address, String unit, long slot);
    List<StakeAddressBalance> getStakeAddressBalance(String address);

    void saveStakeAddressBalances(List<StakeAddressBalance> stakeBalances);

    int deleteStakeAddressBalanceBySlotGreaterThan(Long slot);
}
