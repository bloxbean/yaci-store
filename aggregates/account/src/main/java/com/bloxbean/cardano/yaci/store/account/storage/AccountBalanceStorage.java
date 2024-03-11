package com.bloxbean.cardano.yaci.store.account.storage;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Storage for address balance and stake address balance
 */
public interface AccountBalanceStorage {
    /**
     * Get address balance for the given address, unit at the given slot
     * @param address address
     * @param unit unit
     * @param slot slot
     * @return AddressBalance
     */
    Optional<AddressBalance> getAddressBalance(String address, String unit, long slot);

    //only used during full syc
    List<AddressBalance> getAllAddressBalances(String address, String unit, long slot);

    /**
     * Get address balance for the given address, unit at the given time in sec
     * @param address address
     * @param unit unit
     * @param time time in seconds
     * @return AddressBalance
     */
    Optional<AddressBalance> getAddressBalanceByTime(String address, String unit, long time);

    /**
     * Get latest address balance for the given address at highest slot
     * @param address address
     * @return List of AddressBalance
     */
    List<AddressBalance> getAddressBalance(String address);

    /**
     * Save address balances
     * @param addressBalances
     */
    void saveAddressBalances(List<AddressBalance> addressBalances);

    /**
     * Delete all address balances before the slot except the top one
     * This is called to clean history data
     * @param address address
     * @param unit unit
     * @param slot slot
     * @return number of records deleted
     */
    int deleteAddressBalanceBeforeSlotExceptTop(String address, String unit, long slot);

    /**
     * Delete all address balances after the slot
     * This is used for rollback event
     * @param slot slot
     * @return number of records deleted
     */
    int deleteAddressBalanceBySlotGreaterThan(Long slot);

    /**
     * Delete all address balances after the block
     * @param block - block
     * @return number of records deleted
     */
    int deleteAddressBalanceByBlockGreaterThan(Long block);

    /**
     * Get stake address balance for the given address, unit at the given slot
     * @param address stake address
     * @param slot slot
     * @return StakeAddressBalance
     */
    Optional<StakeAddressBalance> getStakeAddressBalance(String address, long slot);

    List<StakeAddressBalance> getAllStakeAddressBalances(String address, long slot);

    /**
     * Get stake address balance for the given address, at the given time in sec
     * @param address stake address
     * @param time time in sec
     * @return StakeAddressBalance
     */
    Optional<StakeAddressBalance> getStakeAddressBalanceByTime(String address, long time);

    /**
     * Get latest stake address balance for the given address at highest slot
     * @param address address
     * @return List of StakeAddressBalance
     */
    Optional<StakeAddressBalance> getStakeAddressBalance(String address);

    /**
     * Save stake address balances
     * @param stakeBalances
     */
    void saveStakeAddressBalances(List<StakeAddressBalance> stakeBalances);

    /**
     * Delete all stake address balances before the slot except the top one
     * This is called to clean history data
     * @param address address
     * @param slot slot
     * @return number of records deleted
     */
    int deleteStakeBalanceBeforeSlotExceptTop(String address, long slot);

    /**
     * Delete all stake address balances after the slot
     * This is used for rollback event
     * @param slot  slot
     * @return number of records deleted
     */
    int deleteStakeAddressBalanceBySlotGreaterThan(Long slot);

    default void deleteAddressBalance(List<AddressBalance> addressBalances) {}

    default void deleteStakeAddressBalance(List<StakeAddressBalance> stakeAddressBalances) {}

    //Optional -- Required for controller

    /**
     * Get address balance for the given unit.
     * @param unit unit (policyId + assetName)
     * @param page page
     * @param count count
     * @param sort sort order.
     * @return List of AddressBalance
     */
    List<AddressBalance> getAddressesByAsset(String unit, int page, int count, Order sort);

}
