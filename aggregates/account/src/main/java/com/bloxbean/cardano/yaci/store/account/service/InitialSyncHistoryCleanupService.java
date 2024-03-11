package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.AddressBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitialSyncHistoryCleanupService {
    @Value("${store.account.balance-cleanup-slot-count:43200}")
    private int cleanupSlotCount; //TODO: Make it configurable

    private final AddressBalanceRepository addressBalanceRepository;
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountStoreProperties accountStoreProperties;

    @Transactional
    public void deletePreviousBalances(List<AddressBalance> addressBalances, List<StakeAddressBalance> stakeAddressBalances) {
        if (!accountStoreProperties.isHistoryCleanupEnabled())
            return;

        //TODO : Add logic to keep history for certain duration

        long startTime = System.currentTimeMillis();
//        var delAddrBalanceFuture = CompletableFuture.supplyAsync(() -> {
            log.info("Deleting previous balances ");
            if (!addressBalances.isEmpty())
                accountBalanceStorage.deleteAddressBalance(addressBalances);
//            return true;
//        });

//        var deleteStakeAddrBalanceFuture = CompletableFuture.supplyAsync(() -> {
//            log.info("Deleting previous stake balances ");
            if (!addressBalances.isEmpty())
                accountBalanceStorage.deleteStakeAddressBalance(stakeAddressBalances);
//            return true;
//        });

//        CompletableFuture.allOf(delAddrBalanceFuture, deleteStakeAddrBalanceFuture).join();
        long endTime = System.currentTimeMillis();
        log.info("\tTotal Address balances deleted : {}", addressBalances.size());
        log.info("\tTotal Stake Address balances deleted : {}", stakeAddressBalances.size());
        log.info("\tDeleted previous balances in {} ms", (endTime - startTime));
    }

}
