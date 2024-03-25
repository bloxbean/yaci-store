package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.Address;
import com.bloxbean.cardano.yaci.store.account.storage.AddressStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddressProcessor {
    private final AddressStorage addressStorage;
    private final AccountStoreProperties accountStoreProperties;

    private Cache<String, String> cache;
    private Set<Address> addresseCache = Collections.synchronizedSet(new HashSet<>());

    @PostConstruct
    public void init() {
        log.info("-- Address Processor Initialized with address cache size: {}, expiry: {} min"
                , accountStoreProperties.getAddressCacheSize(), accountStoreProperties.getAddressCacheExpiryAfterAccess());
        cache = CacheBuilder.newBuilder()
                .maximumSize(accountStoreProperties.getAddressCacheSize())
                .expireAfterAccess(accountStoreProperties.getAddressCacheExpiryAfterAccess(),
                        TimeUnit.MINUTES)
                .build();
    }

    @EventListener
    public void handleAddress(AddressUtxoEvent addressUtxoEvent) {
        //Get Addresses
        var addresses = addressUtxoEvent.getTxInputOutputs()
                .stream().flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .filter(addressUtxo -> cache.getIfPresent(addressUtxo.getOwnerAddr()) == null)
                .map(addressUtxo -> Address.builder()
                        .address(addressUtxo.getOwnerAddr())
                        .stakeAddress(addressUtxo.getOwnerStakeAddr())
                        .paymentCredential(addressUtxo.getOwnerPaymentCredential())
                        .stakeCredential(addressUtxo.getOwnerStakeCredential())
                        .build())
                .toList();

        if (addresses.isEmpty())
            return;

        addresseCache.addAll(addresses);
    }

    @EventListener
    @Transactional
    public void handleCommit(CommitEvent commitEvent) {
        try {
            if (addresseCache.size() > 0) {
                long t1 = System.currentTimeMillis();
                addressStorage.save(addresseCache);

                if (!commitEvent.getMetadata().isSyncMode()) { //Store only for initial sync
                    addresseCache.stream()
                            .forEach(address -> cache.put(address.getAddress(), ""));
                }

                long t2 = System.currentTimeMillis();
                log.info("Address save size : {}, time: {} ms", addresseCache.size(),  (t2 - t1));
                log.info("Address Cache Size: {}", cache.size());
            }
        } finally {
            addresseCache.clear();
        }
    }

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        addresseCache.clear();
        cache.invalidateAll();
    }
}
