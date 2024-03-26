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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddressProcessor {
    private final AddressStorage addressStorage;
    private final AccountStoreProperties accountStoreProperties;

    private Cache<String, String> cache;
    private Map<String, Address> addresseCache = new ConcurrentHashMap<>();

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
        addressUtxoEvent.getTxInputOutputs()
                .stream().flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .filter(addressUtxo -> cache.getIfPresent(addressUtxo.getOwnerAddr()) == null)
                .forEach(addressUtxo -> {
                    var address = Address.builder()
                            .address(addressUtxo.getOwnerAddr())
                            .stakeAddress(addressUtxo.getOwnerStakeAddr())
                            .paymentCredential(addressUtxo.getOwnerPaymentCredential())
                            .stakeCredential(addressUtxo.getOwnerStakeCredential())
                            .build();

                    addresseCache.putIfAbsent(address.getAddress(), address);
                });
    }

    @EventListener
    @Transactional
    public void handleCommit(CommitEvent commitEvent) {
        try {
            if (addresseCache.size() > 0) {
                long t1 = System.currentTimeMillis();
                addressStorage.save(addresseCache.values());

                if (!commitEvent.getMetadata().isSyncMode()) { //Store only for initial sync
                    addresseCache.values()
                            .forEach(address -> cache.put(address.getAddress(), ""));
                }

                long t2 = System.currentTimeMillis();
                log.info("Address save size : {}, time: {} ms", addresseCache.size(), (t2 - t1));
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
