package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceHistoryCleanupHelper {
    @Value("${store.account.balance-cleanup-slot-count:43200}")
    private int cleanupSlotCount; //TODO: Make it configurable

    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountStoreProperties accountStoreProperties;

    private final AtomicLong deleteCountAddrBal = new AtomicLong(0);
    private final AtomicLong deleteCountStakeBal = new AtomicLong(0);

    public void deleteAddressBalanceBeforeConfirmedSlot(List<Pair<String, String>> addressUnits, long currentSlot) {
        if (!accountStoreProperties.isHistoryCleanupEnabled())
            return;

        long slot = currentSlot - cleanupSlotCount;
        if(slot < 0)
            return;

        addressUnits.forEach(addressUnit -> {
            int delCount = accountBalanceStorage.deleteAddressBalanceBeforeSlotExceptTop(addressUnit.getFirst(), addressUnit.getSecond(), slot);
            deleteCountAddrBal.addAndGet(delCount);
        });

        long count = deleteCountAddrBal.get();
        if (count > 0 && count % 10000 == 0)
            log.info("Total address balances deleted: " + count);
    }

    public void deleteStakeBalanceBeforeConfirmedSlot(List<String> addresses, long currentSlot) {
        if (!accountStoreProperties.isHistoryCleanupEnabled())
            return;

        long slot = currentSlot - cleanupSlotCount;
        if(slot < 0)
            return;

        addresses.forEach(address -> {
            int delCount = accountBalanceStorage.deleteStakeBalanceBeforeSlotExceptTop(address,  slot);
            deleteCountStakeBal.addAndGet(delCount);
        });

        long count = deleteCountStakeBal.get();
        if (count > 0 && count % 10000 == 0)
            log.info("Total stake addr balances deleted: " + count);
    }
}
