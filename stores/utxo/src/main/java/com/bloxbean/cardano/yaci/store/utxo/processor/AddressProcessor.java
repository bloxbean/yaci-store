package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.yaci.core.util.HexUtil;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.cache.GuavaCache;
import com.bloxbean.cardano.yaci.store.utxo.cache.NoCache;
import com.bloxbean.cardano.yaci.store.utxo.domain.Address;
import com.bloxbean.cardano.yaci.store.common.cache.Cache;
import com.bloxbean.cardano.yaci.store.common.cache.MVMapCache;
import com.bloxbean.cardano.yaci.store.common.cache.MVStoreFactory;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.AddressStorage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.utxo.UtxoStoreConfiguration.STORE_UTXO_ENABLED;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_UTXO_ENABLED)
@Slf4j
public class AddressProcessor {
    private final AddressStorage addressStorage;
    private final UtxoStoreProperties utxoStoreProperties;
    private final StoreProperties storeProperties;

    private Cache<String, String> cache;
    private List<SlotAddresses> slotAddressesCache = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    public void init() {
        log.info("-- Address Processor Initialized with address cache size: {}, expiry: {} min"
                , utxoStoreProperties.getAddressCacheSize(), utxoStoreProperties.getAddressCacheExpiryAfterAccess());

        if (utxoStoreProperties.isAddressCacheEnabled() && !storeProperties.isMvstoreEnabled()) {
            cache = new GuavaCache<>(utxoStoreProperties.getAddressCacheSize(),
                    utxoStoreProperties.getAddressCacheExpiryAfterAccess());
            log.info("<< Using Guava Cache for Address >>");
        } else if (!utxoStoreProperties.isAddressCacheEnabled()) {
            log.info("<< Address Cache is disabled >>");
            cache = new NoCache<>();
        }
    }

    @EventListener
    public void handleAddress(AddressUtxoEvent addressUtxoEvent) {
        if (!utxoStoreProperties.isSaveAddress()) {
            return;
        }

        if (cache == null && utxoStoreProperties.isAddressCacheEnabled() && storeProperties.isMvstoreEnabled()) {
            cache = new MVMapCache<>(MVStoreFactory.getInstance().getStore(), "address");
            log.info("<< Using MVStore Cache for Address >>");
        }

        LinkedHashSet<Address> addressesInBlock = new LinkedHashSet<>();
        //Get Addresses
        addressUtxoEvent.getTxInputOutputs()
                .stream().flatMap(txInputOutput -> txInputOutput.getOutputs().stream())
                .filter(addressUtxo -> cache.get(addressUtxo.getOwnerAddr()) == null)
                .forEach(addressUtxo -> {
                    var address = Address.builder()
                            .address(addressUtxo.getOwnerAddr())
                            .stakeAddress(addressUtxo.getOwnerStakeAddr())
                            .paymentCredential(addressUtxo.getOwnerPaymentCredential())
                            .stakeCredential(addressUtxo.getOwnerStakeCredential())
                            .slot(addressUtxo.getSlot())
                            .build();

                    if (!addressesInBlock.contains(address)) {
                        addressesInBlock.add(address);
                    }
                });

        slotAddressesCache.add(new SlotAddresses(addressUtxoEvent.getMetadata().getSlot(), addressesInBlock));
    }

    @EventListener
    @Transactional
    public void handleCommit(CommitEvent commitEvent) {
        if (!utxoStoreProperties.isSaveAddress()) {
            return;
        }

        try {
            List<Address> addresses = slotAddressesCache.stream()
                    .sorted(Comparator.comparingLong(slotAddresses -> slotAddresses.slot()))
                    .flatMap(slotAddresses -> slotAddresses.address.stream())
                    .distinct()
                    .collect(toList());

            if (addresses.size() > 0) {
                long t1 = System.currentTimeMillis();
                addressStorage.save(addresses);

                if (!commitEvent.getMetadata().isSyncMode()) { //Store only for initial sync
                    addresses.forEach(address -> cache.put(address.getAddress(), ""));
                }

                long t2 = System.currentTimeMillis();
                log.info("Address save size : {}, time: {} ms", addresses.size(), (t2 - t1));
                log.info("Address Cache Size: {}", cache.size());
            }
        } finally {
            slotAddressesCache.clear();
        }
    }

    @EventListener
    @Transactional
    public void handleGenesisBalances(GenesisBlockEvent genesisBlockEvent) {
        if (!utxoStoreProperties.isSaveAddress()) {
            return;
        }

        var genesisBalances = genesisBlockEvent.getGenesisBalances();
        if (genesisBalances == null || genesisBalances.isEmpty()) {
            log.info("No genesis balances found");
            return;
        }

        Set<Address> addresses = genesisBalances.stream().map(genesisBalance -> {
            String paymentCredential = null;
            String stakeCredential = null;
            String stakeAddress = null;
            try {
                com.bloxbean.cardano.client.address.Address address = new com.bloxbean.cardano.client.address.Address(genesisBalance.getAddress());
                paymentCredential = address.getPaymentCredential().map(credential -> HexUtil.encodeHexString(credential.getBytes()))
                        .orElse(null);

                stakeCredential = address.getDelegationCredential().map(delegCred -> HexUtil.encodeHexString(delegCred.getBytes()))
                        .orElse(null);
                stakeAddress = address.getDelegationCredential().map(delegCred -> AddressProvider.getStakeAddress(address).toBech32())
                        .orElse(null);
            } catch (Exception e) {
                //Not a valid shelley address
            }

            return Address.builder()
                    .address(genesisBalance.getAddress())
                    .paymentCredential(paymentCredential)
                    .stakeCredential(stakeCredential)
                    .stakeAddress(stakeAddress)
                    .build();
        }).collect(Collectors.toSet());

        addressStorage.save(addresses);
    }

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        if (cache != null)
            cache.clear();

        slotAddressesCache.clear();

        long count = addressStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} address records", count);
    }

    record SlotAddresses(long slot, LinkedHashSet<Address> address) {};
}
