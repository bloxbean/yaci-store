package com.bloxbean.cardano.yaci.store.account.storage;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * Saves the current address balances to current address balance table.
     *
     * @param addressBalances a list of AddressBalance objects representing the current balances
     *                        for various addresses and units.
     */
    void saveCurrentAddressBalances(List<AddressBalance> addressBalances);

    /**
     * Delete all address balances before the slot except the top one
     * This method is called when {@link #isBatchDeleteSupported()} returns false.
     * This is called to clean history data
     * <p></p>
     * This is deprecated and should not be used. Use {@link #deleteAddressBalanceBeforeSlotExceptTop(List, long)}
     *
     * @param address address
     * @param unit unit
     * @param slot slot
     * @return number of records deleted
     */
    @Deprecated
    int deleteAddressBalanceBeforeSlotExceptTop(String address, String unit, long slot);

    /**
     * Delete all address balance records before the slot except the top one. This method is called when batch delete is supported.
     * {@link #isBatchDeleteSupported()}
     * This is called to clean history data
     * @param addresUnits List of Pairs of address and unit
     * @param slot slot
     * @return number of records deleted
     */
    int deleteAddressBalanceBeforeSlotExceptTop(List<Pair<String,String>> addresUnits, long slot);

    /**
     * Check if batch delete is supported.
     * If supported, {@link #deleteAddressBalanceBeforeSlotExceptTop(List, long)} will be called during history cleanup.
     * @return
     */
    default boolean isBatchDeleteSupported() {
        return false;
    }

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
     * Save current stake address balances to the current balance table
     * @param stakeBalances
     */
    void saveCurrentStakeAddressBalances(List<StakeAddressBalance> stakeBalances);

    /**
     * Delete all stake address balances before the slot except the top one
     * This is called to clean history data
     * <p></p>
     * This method is deprecated and should not be used. Use {@link #deleteStakeBalanceBeforeSlotExceptTop(List, long)}
     * @param address address
     * @param slot slot
     * @return number of records deleted
     */
    int deleteStakeBalanceBeforeSlotExceptTop(String address, long slot);

    /**
     * Delete all stake address balances before the slot except the top. This method is called when batch delete is supported.
     * {@link #isBatchDeleteSupported()}
     * This is called to clean history data
     * @param addresses List of addresses
     * @param slot slot
     * @return number of records deleted
     */
    int deleteStakeBalanceBeforeSlotExceptTop(List<String> addresses, long slot);

    /**
     * Delete all stake address balances after the slot
     * This is used for rollback event
     * @param slot  slot
     * @return number of records deleted
     */
    int deleteStakeAddressBalanceBySlotGreaterThan(Long slot);

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

    /*******************************************************************************************
     * The following methods are used for current address balance table refresh during rollback
     *******************************************************************************************/

    /**
     * Retrieves a list of address balances where the slot is greater than the specified value. This is typically used
     * to identify records that need to be deleted during a rollback operation.
     *
     * @param slot the slot value
     * @return a list of AddressBalance objects that match the specified slot criteria.
     */
    List<AddressBalance> getAddressBalanceBySlotGreaterThan(Long slot);

    /**
     * Retrieves a list of stake address balances where the slot is greater than the specified value. This is typically used
     * to identify records that need to be deleted during a rollback operation.
     *
     * @param slot the slot value
     * @return a list of StakeAddressBalance objects that match the specified slot criteria.
     */
    List<StakeAddressBalance> getStakeAddressBalanceBySlotGreaterThan(Long slot);

    /**
     * Refreshes the current address balance for a given address and set of units at a specified slot.
     * This is typically used during a rollback operation to ensure that the current balance is accurate.
     * @param address
     * @param units
     * @param slot rollback slot
     */
    void refreshCurrentAddressBalance(String address, Set<String> units, Long slot);

    /**
     * Refreshes the current stake address balance for a given list of addresses at a specified slot.
     * This is typically used during a rollback operation to ensure that the current balance is accurate.
     * @param address
     * @param slot rollback slot
     */
    void refreshCurrentStakeAddressBalance(List<String> address, Long slot);

    /**
     * Get current address balances for the given address from current balance table
     * @param address
     * @return List of AddressBalance
     */
    List<AddressBalance> getCurrentAddressBalance(String address);

    /**
     * Get current stake address balance for the given address from current balance table
     * @param address
     * @return StakeAddressBalance
     */
    Optional<StakeAddressBalance> getCurrentStakeAddressBalance(String address);
}
