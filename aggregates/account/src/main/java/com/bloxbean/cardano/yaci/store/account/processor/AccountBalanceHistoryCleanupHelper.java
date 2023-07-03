package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.client.util.Tuple;
import com.bloxbean.cardano.yaci.store.account.AccountStoreConfiguration;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceHistoryCleanupHelper {
    private static int confirmedSlotCount = 2160 * 20; //TODO: Make it configurable
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountStoreConfiguration accountStoreConfiguration;

    private final AtomicLong deleteCountAddrBal = new AtomicLong(0);
    private final AtomicLong deleteCountStakeBal = new AtomicLong(0);

    @Transactional
    public void deleteAddressBalanceBeforeConfirmedSlot(List<Tuple<String, String>> addressUnits, long currentSlot) {
        if (!accountStoreConfiguration.isHistoryCleanupEnabled())
            return;

        long slot = currentSlot - confirmedSlotCount;
        if(slot < 0)
            return;

        addressUnits.forEach(addressUnit -> {
            int delCount = accountBalanceStorage.deleteAddressBalanceBeforeSlotExceptTop(addressUnit._1, addressUnit._2, slot);
            deleteCountAddrBal.addAndGet(delCount);
        });

        long count = deleteCountAddrBal.get();
        if (count > 0 && count % 10000 == 0)
            log.info("Total address balances deleted: " + count);
    }

    @Transactional
    public void deleteStakeBalanceBeforeConfirmedSlot(List<Tuple<String, String>> addressUnits, long currentSlot) {
        if (!accountStoreConfiguration.isHistoryCleanupEnabled())
            return;

        long slot = currentSlot - confirmedSlotCount;
        if(slot < 0)
            return;

        addressUnits.forEach(addressUnit -> {
            int delCount = accountBalanceStorage.deleteStakeBalanceBeforeSlotExceptTop(addressUnit._1, addressUnit._2, slot);
            deleteCountStakeBal.addAndGet(delCount);
        });

        long count = deleteCountStakeBal.get();
        if (count > 0 && count % 10000 == 0)
            log.info("Total stake addr balances deleted: " + count);
    }
}
