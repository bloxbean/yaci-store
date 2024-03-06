package com.bloxbean.cardano.yaci.store.account.storage.impl;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.storage.impl.mapper.AccountMapper;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.AddressBalanceRepository;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.StakeBalanceRepository;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.ADDRESS_BALANCE;
import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.STAKE_ADDRESS_BALANCE;

@RequiredArgsConstructor
@Slf4j
public class AccountBalanceStorageImpl implements AccountBalanceStorage {
    private final AddressBalanceRepository addressBalanceRepository;
    private final StakeBalanceRepository stakeBalanceRepository;
    private final AccountMapper mapper = AccountMapper.INSTANCE;
    private final DSLContext dsl;

    private final AccountStoreProperties accountStoreProperties;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @Value("${store.account.enable-jpa-insert:false}")
    private boolean enableJPAInsert = false;

    @PostConstruct
    public void postConstruct() {
        this.dsl.settings().setBatchSize(accountStoreProperties.getJooqWriteBatchSize());

        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Optional<AddressBalance> getAddressBalance(String address, String unit, long slot) {
        return addressBalanceRepository.findTopByAddressAndUnitAndSlotIsLessThanEqualOrderBySlotDesc(address, unit, slot)
                .map(mapper::toAddressBalance);
    }

    @Override
    public Optional<AddressBalance> getAddressBalanceByTime(String address, String unit, long time) {
        if (time == 0)
            throw new IllegalArgumentException("Time cannot be 0");
        return addressBalanceRepository.findTopByAddressAndUnitAndBlockTimeIsLessThanEqualOrderByBlockTimeDesc(address, unit, time)
                .map(mapper::toAddressBalance);
    }

    @Override
    public List<AddressBalance> getAddressBalance(String address) {
        return addressBalanceRepository.findLatestAddressBalanceByAddress(address).stream()
                .map(mapper::toAddressBalance)
                .toList();
    }

    @Transactional
    @Override
    public void saveAddressBalances(@NonNull List<AddressBalance> addressBalances) {
        List<AddressBalanceEntity> entities = addressBalances.stream().map(mapper::toAddressBalanceEntity)
                .toList();

        if (accountStoreProperties.isParallelWrite()) {
            log.info("Writing address balances in parallel. Using {}, Per Thread Batch size : {} ", enableJPAInsert ? "JPA" : "JOOQ", accountStoreProperties.getPerThreadBatchSize());
            if (!enableJPAInsert)
                log.info("JOOQ Insert Batch Size {}", accountStoreProperties.getJooqWriteBatchSize());

            ListUtil.partitionAndApplyInParallel(entities, accountStoreProperties.getPerThreadBatchSize(), this::saveAddrBalanceBatch);
        } else {
            saveAddrBalanceBatch(entities);
        }
    }

    private void saveAddrBalanceBatch(List<AddressBalanceEntity> addressBalanceEntities) {
        if (enableJPAInsert) {
            if (log.isTraceEnabled())
                log.trace("Inserting address balances using JPA batch");

            transactionTemplate.execute(status -> {
                addressBalanceRepository.saveAll(addressBalanceEntities);
                return null;
            });
        } else {
            if (log.isTraceEnabled())
                log.trace("Inserting address balances using JOOQ batch");

            transactionTemplate.execute(status -> {
                saveAddrBalanceBatchJOOQ(addressBalanceEntities);
                return null;
            });
        }
    }

    private void saveAddrBalanceBatchJOOQ(List<AddressBalanceEntity> addressBalanceEntities) {
        LocalDateTime localDateTime = LocalDateTime.now();

        dsl.batched(c -> {
            for (var addressBalance : addressBalanceEntities) {
                c.dsl().insertInto(ADDRESS_BALANCE)
                        .set(ADDRESS_BALANCE.ADDRESS, addressBalance.getAddress())
                        .set(ADDRESS_BALANCE.UNIT, addressBalance.getUnit())
                        .set(ADDRESS_BALANCE.SLOT, addressBalance.getSlot())
                        .set(ADDRESS_BALANCE.QUANTITY, addressBalance.getQuantity())
                        .set(ADDRESS_BALANCE.ADDR_FULL, addressBalance.getAddrFull())
                        .set(ADDRESS_BALANCE.POLICY, addressBalance.getPolicy())
                        .set(ADDRESS_BALANCE.ASSET_NAME, addressBalance.getAssetName())
                        .set(ADDRESS_BALANCE.PAYMENT_CREDENTIAL, addressBalance.getPaymentCredential())
                        .set(ADDRESS_BALANCE.STAKE_ADDRESS, addressBalance.getStakeAddress())
                        .set(ADDRESS_BALANCE.BLOCK_HASH, addressBalance.getBlockHash())
                        .set(ADDRESS_BALANCE.BLOCK, addressBalance.getBlockNumber())
                        .set(ADDRESS_BALANCE.BLOCK_TIME, addressBalance.getBlockTime())
                        .set(ADDRESS_BALANCE.EPOCH, addressBalance.getEpoch())
                        .set(ADDRESS_BALANCE.UPDATE_DATETIME, localDateTime)
                        .onDuplicateKeyUpdate()
                        .set(ADDRESS_BALANCE.QUANTITY, addressBalance.getQuantity())
                        .set(ADDRESS_BALANCE.ADDR_FULL, addressBalance.getAddrFull())
                        .set(ADDRESS_BALANCE.POLICY, addressBalance.getPolicy())
                        .set(ADDRESS_BALANCE.ASSET_NAME, addressBalance.getAssetName())
                        .set(ADDRESS_BALANCE.PAYMENT_CREDENTIAL, addressBalance.getPaymentCredential())
                        .set(ADDRESS_BALANCE.STAKE_ADDRESS, addressBalance.getStakeAddress())
                        .set(ADDRESS_BALANCE.BLOCK_HASH, addressBalance.getBlockHash())
                        .set(ADDRESS_BALANCE.BLOCK, addressBalance.getBlockNumber())
                        .set(ADDRESS_BALANCE.BLOCK_TIME, addressBalance.getBlockTime())
                        .set(ADDRESS_BALANCE.EPOCH, addressBalance.getEpoch())
                        .set(ADDRESS_BALANCE.UPDATE_DATETIME, localDateTime)
                        .execute();
            }
        });

    }

    @Transactional
    @Override
    public int deleteAddressBalanceBeforeSlotExceptTop(String address, String unit, long slot) {
        //Find the latest address balance before the slot and delete all address balances before that
        return addressBalanceRepository.findTopByAddressAndUnitAndSlotIsLessThanEqualOrderBySlotDesc(address, unit, slot)
                .map(addressBalanceEntity -> addressBalanceRepository.deleteAllBeforeSlot(address, unit, addressBalanceEntity.getSlot() - 1)).orElse(0);
    }

    @Transactional
    @Override
    public int deleteAddressBalanceBySlotGreaterThan(Long slot) {
        return addressBalanceRepository.deleteBySlotGreaterThan(slot);
    }

    @Transactional
    @Override
    public int deleteAddressBalanceByBlockGreaterThan(Long block) {
        return addressBalanceRepository.deleteByBlockNumberGreaterThan(block);
    }

    @Override
    public Optional<StakeAddressBalance> getStakeAddressBalance(String address, long slot) {
        return stakeBalanceRepository.findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(address, slot)
                .map(mapper::toStakeBalance);
    }

    @Override
    public Optional<StakeAddressBalance> getStakeAddressBalanceByTime(String address, long time) {
        if (time == 0)
            throw new IllegalArgumentException("Time cannot be 0");

        return stakeBalanceRepository.findTopByAddressAndBlockTimeIsLessThanEqualOrderByBlockTimeDesc(address, time)
                .map(mapper::toStakeBalance);
    }

    @Override
    public Optional<StakeAddressBalance> getStakeAddressBalance(String address) {
        return stakeBalanceRepository.findLatestAddressBalanceByAddress(address)
                .map(mapper::toStakeBalance);
    }

    @Transactional
    @Override
    public void saveStakeAddressBalances(List<StakeAddressBalance> stakeBalances) {
        List<StakeAddressBalanceEntity> entities = stakeBalances.stream().map(mapper::toStakeBalanceEntity)
                .toList();

        if (accountStoreProperties.isParallelWrite()) {
            ListUtil.partitionAndApplyInParallel(entities, accountStoreProperties.getPerThreadBatchSize(), this::saveStakeBalanceBatch);
        } else {
            saveStakeBalanceBatch(entities);
        }
    }

    private void saveStakeBalanceBatch(List<StakeAddressBalanceEntity> stakeAddressBalances) {
        if (enableJPAInsert) {
            if (log.isTraceEnabled())
                log.trace("Inserting stake address balances using JPA batch");

            transactionTemplate.execute(status -> {
                stakeBalanceRepository.saveAll(stakeAddressBalances);
                return null;
            });
        } else {
            if (log.isTraceEnabled())
                log.trace("Inserting stake address balances using JOOQ batch");

            transactionTemplate.execute(status -> {
                saveStakeBalanceBatchJOOQ(stakeAddressBalances);
                return null;
            });
        }
    }

    private void saveStakeBalanceBatchJOOQ(List<StakeAddressBalanceEntity> stakeAddressBalances) {
        LocalDateTime localDateTime = LocalDateTime.now();
        dsl.batched(c -> {
            for (var stakeAddrBalance : stakeAddressBalances) {
                c.dsl().insertInto(STAKE_ADDRESS_BALANCE)
                        .set(STAKE_ADDRESS_BALANCE.ADDRESS, stakeAddrBalance.getAddress())
                        .set(STAKE_ADDRESS_BALANCE.SLOT, stakeAddrBalance.getSlot())
                        .set(STAKE_ADDRESS_BALANCE.QUANTITY, stakeAddrBalance.getQuantity())
                        .set(STAKE_ADDRESS_BALANCE.STAKE_CREDENTIAL, stakeAddrBalance.getStakeCredential())
                        .set(STAKE_ADDRESS_BALANCE.BLOCK_HASH, stakeAddrBalance.getBlockHash())
                        .set(STAKE_ADDRESS_BALANCE.BLOCK, stakeAddrBalance.getBlockNumber())
                        .set(STAKE_ADDRESS_BALANCE.BLOCK_TIME, stakeAddrBalance.getBlockTime())
                        .set(STAKE_ADDRESS_BALANCE.EPOCH, stakeAddrBalance.getEpoch())
                        .set(STAKE_ADDRESS_BALANCE.UPDATE_DATETIME, localDateTime)
                        .onDuplicateKeyUpdate()
                        .set(STAKE_ADDRESS_BALANCE.QUANTITY, stakeAddrBalance.getQuantity())
                        .set(STAKE_ADDRESS_BALANCE.STAKE_CREDENTIAL, stakeAddrBalance.getStakeCredential())
                        .set(STAKE_ADDRESS_BALANCE.BLOCK_HASH, stakeAddrBalance.getBlockHash())
                        .set(STAKE_ADDRESS_BALANCE.BLOCK, stakeAddrBalance.getBlockNumber())
                        .set(STAKE_ADDRESS_BALANCE.BLOCK_TIME, stakeAddrBalance.getBlockTime())
                        .set(STAKE_ADDRESS_BALANCE.EPOCH, stakeAddrBalance.getEpoch())
                        .set(STAKE_ADDRESS_BALANCE.UPDATE_DATETIME, localDateTime)
                        .execute();
            }
        });
    }

    @Transactional
    @Override
    public int deleteStakeBalanceBeforeSlotExceptTop(String address, long slot) {
        //Find the latest stake address balance before the slot and delete all address balances before that
        return stakeBalanceRepository.findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(address, slot)
                .map(addressBalanceEntity -> stakeBalanceRepository.deleteAllBeforeSlot(address, addressBalanceEntity.getSlot() - 1)).orElse(0);
    }

    @Transactional
    @Override
    public int deleteStakeAddressBalanceBySlotGreaterThan(Long slot) {
        return stakeBalanceRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public List<AddressBalance> getAddressesByAsset(String unit, int page, int count, Order sort) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, sort == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        return addressBalanceRepository.findLatestAddressBalanceByUnit(unit, sortedBySlot).stream()
                .map(mapper::toAddressBalance)
                .toList();
    }

}
