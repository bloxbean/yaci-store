package com.bloxbean.cardano.yaci.store.account.storage.impl;

import com.bloxbean.cardano.yaci.store.account.domain.AddressTxAmount;
import com.bloxbean.cardano.yaci.store.account.storage.AddressTxAmountStorage;
import com.bloxbean.cardano.yaci.store.account.storage.impl.mapper.AggrMapper;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressTxAmountEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.AddressTxAmountRepository;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.account.jooq.tables.AddressTxAmount.ADDRESS_TX_AMOUNT;

@RequiredArgsConstructor
@Slf4j
public class AddressTxAmountStorageImpl implements AddressTxAmountStorage {
    private final AddressTxAmountRepository addressTxAmountRepository;
    private final DSLContext dsl;
    private final StoreProperties storeProperties;

    private final AggrMapper aggrMapper = AggrMapper.INSTANCE;

    @PostConstruct
    public void postConstruct() {
        this.dsl.settings().setBatchSize(storeProperties.getJooqWriteBatchSize());
    }

    @Override
    @Transactional
    public void save(List<AddressTxAmount> addressTxAmount) {
        var addressTxAmtEntities = addressTxAmount.stream()
                .map(addressTxAmount1 -> aggrMapper.toAddressTxAmountEntity(addressTxAmount1))
                .toList();

        if (storeProperties.isParallelWrite()
                && addressTxAmtEntities.size() > storeProperties.getWriteThreadDefaultBatchSize()) {
            int partitionSize = getPartitionSize(addressTxAmtEntities.size());
            ListUtil.partitionAndApply(addressTxAmtEntities, partitionSize, this::saveBatch);
        } else {
            saveBatch(addressTxAmtEntities);
        }
    }

    private void saveBatch(List<AddressTxAmountEntity> addressTxAmountEntities) {
        var inserts = addressTxAmountEntities.stream()
                .map(addressTxAmount -> dsl.insertInto(ADDRESS_TX_AMOUNT)
                        .set(ADDRESS_TX_AMOUNT.ADDRESS, addressTxAmount.getAddress())
                        .set(ADDRESS_TX_AMOUNT.UNIT, addressTxAmount.getUnit())
                        .set(ADDRESS_TX_AMOUNT.TX_HASH, addressTxAmount.getTxHash())
                        .set(ADDRESS_TX_AMOUNT.SLOT, addressTxAmount.getSlot())
                        .set(ADDRESS_TX_AMOUNT.QUANTITY, addressTxAmount.getQuantity())
                        .set(ADDRESS_TX_AMOUNT.ADDR_FULL, addressTxAmount.getAddrFull())
                        .set(ADDRESS_TX_AMOUNT.STAKE_ADDRESS, addressTxAmount.getStakeAddress())
                        .set(ADDRESS_TX_AMOUNT.BLOCK, addressTxAmount.getBlockNumber())
                        .set(ADDRESS_TX_AMOUNT.BLOCK_TIME, addressTxAmount.getBlockTime())
                        .set(ADDRESS_TX_AMOUNT.EPOCH, addressTxAmount.getEpoch())
                        .onDuplicateKeyUpdate()
                        .set(ADDRESS_TX_AMOUNT.SLOT, addressTxAmount.getSlot())
                        .set(ADDRESS_TX_AMOUNT.QUANTITY, addressTxAmount.getQuantity())
                        .set(ADDRESS_TX_AMOUNT.ADDR_FULL, addressTxAmount.getAddrFull())
                        .set(ADDRESS_TX_AMOUNT.STAKE_ADDRESS, addressTxAmount.getStakeAddress())
                        .set(ADDRESS_TX_AMOUNT.BLOCK, addressTxAmount.getBlockNumber())
                        .set(ADDRESS_TX_AMOUNT.BLOCK_TIME, addressTxAmount.getBlockTime())
                        .set(ADDRESS_TX_AMOUNT.EPOCH, addressTxAmount.getEpoch())).toList();
        dsl.batch(inserts).execute();

        /**
        dsl.batched(c -> {
            for (var addressTxAmount : addressTxAmountEntities) {
                c.dsl().insertInto(ADDRESS_TX_AMOUNT)
                        .set(ADDRESS_TX_AMOUNT.ADDRESS, addressTxAmount.getAddress())
                        .set(ADDRESS_TX_AMOUNT.UNIT, addressTxAmount.getUnit())
                        .set(ADDRESS_TX_AMOUNT.TX_HASH, addressTxAmount.getTxHash())
                        .set(ADDRESS_TX_AMOUNT.SLOT, addressTxAmount.getSlot())
                        .set(ADDRESS_TX_AMOUNT.QUANTITY, addressTxAmount.getQuantity())
                        .set(ADDRESS_TX_AMOUNT.ADDR_FULL, addressTxAmount.getAddrFull())
                        .set(ADDRESS_TX_AMOUNT.STAKE_ADDRESS, addressTxAmount.getStakeAddress())
                        .set(ADDRESS_TX_AMOUNT.BLOCK, addressTxAmount.getBlockNumber())
                        .set(ADDRESS_TX_AMOUNT.BLOCK_TIME, addressTxAmount.getBlockTime())
                        .set(ADDRESS_TX_AMOUNT.EPOCH, addressTxAmount.getEpoch())
                        .onDuplicateKeyUpdate()
                        .set(ADDRESS_TX_AMOUNT.SLOT, addressTxAmount.getSlot())
                        .set(ADDRESS_TX_AMOUNT.QUANTITY, addressTxAmount.getQuantity())
                        .set(ADDRESS_TX_AMOUNT.ADDR_FULL, addressTxAmount.getAddrFull())
                        .set(ADDRESS_TX_AMOUNT.STAKE_ADDRESS, addressTxAmount.getStakeAddress())
                        .set(ADDRESS_TX_AMOUNT.BLOCK, addressTxAmount.getBlockNumber())
                        .set(ADDRESS_TX_AMOUNT.BLOCK_TIME, addressTxAmount.getBlockTime())
                        .set(ADDRESS_TX_AMOUNT.EPOCH, addressTxAmount.getEpoch())
                        .execute();
            }
        });
        **/
    }

    @Override
    public int deleteAddressBalanceBySlotGreaterThan(Long slot) {
        return addressTxAmountRepository.deleteAddressBalanceBySlotGreaterThan(slot);
    }

    private int getPartitionSize(int totalSize) {
        int partitionSize = totalSize;
        if (totalSize > storeProperties.getWriteThreadDefaultBatchSize()) {
            partitionSize = totalSize / storeProperties.getWriteThreadCount();
            if (log.isDebugEnabled())
                log.debug("\tAddress Tx Amt Partition size : {}", partitionSize);
        } else {
            if (log.isDebugEnabled())
                log.debug("\tAddress Tx Amt Partition size : {}", partitionSize);
        }
        return partitionSize;
    }
}
