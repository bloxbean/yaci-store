package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.util.AddressUtil;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.utxo.domain.Address;
import com.bloxbean.cardano.yaci.store.utxo.domain.PtrAddress;
import com.bloxbean.cardano.yaci.store.utxo.jooq.Tables;
import com.bloxbean.cardano.yaci.store.utxo.storage.AddressStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.PTR_ADDRESS;

@RequiredArgsConstructor
@Slf4j
public class AddressStorageImpl implements AddressStorage {
    private final DSLContext dsl;
    private final StoreProperties storeProperties;

    @Transactional
    @Override
    public void save(Collection<Address> addresses) {
        if (storeProperties.isParallelWrite()
                && addresses.size() > storeProperties.getWriteThreadDefaultBatchSize()) {
            int partitionSize = getPartitionSize(addresses.size());
            ListUtil.partitionAndApply(addresses.stream().toList(), partitionSize, this::saveBatch);
        } else {
            saveBatch(addresses);
        }
    }

    @Transactional
    @Override
    public void savePtrAddress(Collection<PtrAddress> ptrAddresses) {
        var inserts = ptrAddresses.stream()
                .map(ptrAddress -> {
                    return dsl.insertInto(PTR_ADDRESS)
                            .set(PTR_ADDRESS.ADDRESS, ptrAddress.getAddress())
                            .set(PTR_ADDRESS.STAKE_ADDRESS, ptrAddress.getStakeAddress())
                            .onDuplicateKeyIgnore();
                }).toList();

        dsl.batch(inserts).execute();
    }

    @Override
    public List<PtrAddress> findPtrAddresses() {
        return dsl.selectFrom(PTR_ADDRESS)
                .fetch()
                .map(record -> new PtrAddress(
                        record.getAddress(),
                        record.getStakeAddress()
                ));
    }

    @Transactional
    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return dsl.deleteFrom(ADDRESS)
                .where(ADDRESS.SLOT.gt(slot))
                .execute();
    }

    private void saveBatch(Collection<Address> addresses) {
        LocalDateTime localDateTime = LocalDateTime.now();

        var inserts = addresses.stream()
                .map(address -> {
                    var addressTuple = AddressUtil.getAddress(address.getAddress());
                    return dsl.insertInto(ADDRESS)
                            .set(Tables.ADDRESS.ADDRESS_, addressTuple._1)
                            .set(Tables.ADDRESS.ADDR_FULL, addressTuple._2)
                            .set(Tables.ADDRESS.PAYMENT_CREDENTIAL, address.getPaymentCredential())
                            .set(Tables.ADDRESS.STAKE_ADDRESS, address.getStakeAddress())
                            .set(Tables.ADDRESS.STAKE_CREDENTIAL, address.getStakeCredential())
                            .set(Tables.ADDRESS.SLOT, address.getSlot())
                            .set(Tables.ADDRESS.UPDATE_DATETIME, localDateTime)
                            .onDuplicateKeyIgnore();
                }).toList();

        dsl.batch(inserts).execute();
    }

    private int getPartitionSize(int totalSize) {
        int partitionSize = totalSize;
        if (totalSize > storeProperties.getWriteThreadDefaultBatchSize()) {
            partitionSize = totalSize / storeProperties.getWriteThreadCount();
            log.info("\tAddress Partition size : {}", partitionSize);
        } else {
            log.info("\tAddress Partition size : {}", partitionSize);
        }
        return partitionSize;
    }
}
