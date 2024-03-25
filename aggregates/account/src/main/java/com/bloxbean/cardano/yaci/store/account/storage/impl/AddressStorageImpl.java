package com.bloxbean.cardano.yaci.store.account.storage.impl;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.Address;
import com.bloxbean.cardano.yaci.store.account.storage.AddressStorage;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;

import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.ADDRESS;
import static com.bloxbean.cardano.yaci.store.account.util.AddressUtil.getAddress;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddressStorageImpl implements AddressStorage {
    private final DSLContext dsl;
    private final AccountStoreProperties accountStoreProperties;

    @Transactional
    @Override
    public void save(Set<Address> addresses) {
        if (accountStoreProperties.isParallelWrite()
                && addresses.size() > accountStoreProperties.getWriteThreadDefaultBatchSize()) {
            int partitionSize = getPartitionSize(addresses.size());
            ListUtil.partitionAndApplyInParallel(addresses.stream().toList(), partitionSize, this::saveBatch);
        } else {
            saveBatch(addresses);
        }
    }

    private void saveBatch(Collection<Address> addresses) {
        LocalDateTime localDateTime = LocalDateTime.now();

        var inserts = addresses.stream()
                .map(address -> {
                    var addressTuple = getAddress(address.getAddress());
                    return dsl.insertInto(ADDRESS)
                            .set(ADDRESS.ADDRESS_, addressTuple._1)
                            .set(ADDRESS.ADDR_FULL, addressTuple._2)
                            .set(ADDRESS.PAYMENT_CREDENTIAL, address.getPaymentCredential())
                            .set(ADDRESS.STAKE_ADDRESS, address.getStakeAddress())
                            .set(ADDRESS.STAKE_CREDENTIAL, address.getStakeCredential())
                            .set(ADDRESS.UPDATE_DATETIME, localDateTime)
                            .onDuplicateKeyIgnore();
                }).toList();

        dsl.batch(inserts).execute();
    }

    private int getPartitionSize(int totalSize) {
        int partitionSize = totalSize;
        if (totalSize > accountStoreProperties.getWriteThreadDefaultBatchSize()) {
            partitionSize = totalSize / accountStoreProperties.getWriteThreadCount();
            log.info("\tAddress Partition size : {}", partitionSize);
        } else {
            log.info("\tAddress Partition size : {}", partitionSize);
        }
        return partitionSize;
    }
}
