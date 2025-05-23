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
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.common.util.ListUtil.partition;
import static org.jooq.impl.DSL.*;

@Slf4j
public class AccountBalanceStorageImpl implements AccountBalanceStorage {
    private final AddressBalanceRepository addressBalanceRepository;
    private final StakeBalanceRepository stakeBalanceRepository;
    private final DSLContext dsl;
    private final StoreProperties storeProperties;
    private final AccountStoreProperties accountStoreProperties;

    private final AccountMapper mapper = AccountMapper.INSTANCE;

    @Value("${store.account.enable-jpa-insert:false}")
    private boolean enableJPAInsert = false;

    private Map<Pair<String, String>, Long> addressBalanceKeysToDeleteCache = new ConcurrentHashMap<>();
    private Map<String, Long> stakeBalanceKeysToDeleteCache = new ConcurrentHashMap<>();

    public AccountBalanceStorageImpl(AddressBalanceRepository addressBalanceRepository, StakeBalanceRepository stakeBalanceRepository,
                                     DSLContext dsl, StoreProperties storeProperties, AccountStoreProperties accountStoreProperties) {
        this.addressBalanceRepository = addressBalanceRepository;
        this.stakeBalanceRepository = stakeBalanceRepository;
        this.dsl = dsl;
        this.storeProperties = storeProperties;
        this.accountStoreProperties = accountStoreProperties;

        init();
    }

    public void init() {
        this.dsl.settings().setBatchSize(storeProperties.getJooqWriteBatchSize());
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

        if (storeProperties.isParallelWrite()) {
            log.info("\tWriting address balances in parallel. Using {}, Per Thread Batch size : {} ", enableJPAInsert ? "JPA" : "JOOQ", storeProperties.getWriteThreadDefaultBatchSize());
            if (!enableJPAInsert)
                log.info("\tInsert Batch Size -- {}", storeProperties.getJooqWriteBatchSize());

            int partitionSize = getPartitionSize(entities.size());
            ListUtil.partitionAndApply(entities, partitionSize, this::saveAddrBalanceBatch);
        } else {
            saveAddrBalanceBatch(entities);
        }
    }

    private void saveAddrBalanceBatch(List<AddressBalanceEntity> addressBalanceEntities) {
        if (enableJPAInsert) {
            if (log.isTraceEnabled())
                log.trace("Inserting address balances using JPA batch");

            addressBalanceRepository.saveAll(addressBalanceEntities);
        } else {
            if (log.isTraceEnabled())
                log.trace("Inserting address balances using JOOQ batch");

            saveAddrBalanceBatchJOOQ(addressBalanceEntities);
        }
    }

    private void saveAddrBalanceBatchJOOQ(List<AddressBalanceEntity> addressBalanceEntities) {
        LocalDateTime localDateTime = LocalDateTime.now();

        var inserts = addressBalanceEntities.stream()
                .map(addressBalance ->   dsl.insertInto(ADDRESS_BALANCE)
                                .set(ADDRESS_BALANCE.ADDRESS, addressBalance.getAddress())
                                .set(ADDRESS_BALANCE.UNIT, addressBalance.getUnit())
                                .set(ADDRESS_BALANCE.SLOT, addressBalance.getSlot())
                                .set(ADDRESS_BALANCE.QUANTITY, addressBalance.getQuantity())
                                .set(ADDRESS_BALANCE.ADDR_FULL, addressBalance.getAddrFull())
                                .set(ADDRESS_BALANCE.BLOCK, addressBalance.getBlockNumber())
                                .set(ADDRESS_BALANCE.BLOCK_TIME, addressBalance.getBlockTime())
                                .set(ADDRESS_BALANCE.EPOCH, addressBalance.getEpoch())
                                .set(ADDRESS_BALANCE.UPDATE_DATETIME, localDateTime)
                                .onDuplicateKeyUpdate()
                                .set(ADDRESS_BALANCE.QUANTITY, addressBalance.getQuantity())
                                .set(ADDRESS_BALANCE.ADDR_FULL, addressBalance.getAddrFull())
                                .set(ADDRESS_BALANCE.BLOCK, addressBalance.getBlockNumber())
                                .set(ADDRESS_BALANCE.BLOCK_TIME, addressBalance.getBlockTime())
                                .set(ADDRESS_BALANCE.EPOCH, addressBalance.getEpoch())
                                .set(ADDRESS_BALANCE.UPDATE_DATETIME, localDateTime)).toList();

        dsl.batch(inserts).execute();
    }

    @Transactional
    @Override
    public void saveCurrentAddressBalances(List<AddressBalance> addressBalances) {
        LocalDateTime localDateTime = LocalDateTime.now();

        List<AddressBalanceEntity> entities = addressBalances.stream().map(mapper::toAddressBalanceEntity)
                .toList();

        int batchSize = storeProperties.getWriteThreadDefaultBatchSize();

        var addressBalanceRecords = entities.stream()
                .map(addrBalance -> {
                    var addressBalanceRec = dsl.newRecord(ADDRESS_BALANCE_CURRENT);
                    addressBalanceRec.setAddress(addrBalance.getAddress());
                    addressBalanceRec.setUnit(addrBalance.getUnit());
                    addressBalanceRec.setQuantity(addrBalance.getQuantity());
                    addressBalanceRec.setAddrFull(addrBalance.getAddrFull());
                    addressBalanceRec.setSlot(addrBalance.getSlot());
                    addressBalanceRec.setBlock(addrBalance.getBlockNumber());
                    addressBalanceRec.setBlockTime(addrBalance.getBlockTime());
                    addressBalanceRec.setEpoch(addrBalance.getEpoch());
                    addressBalanceRec.setUpdateDatetime(localDateTime);
                    return addressBalanceRec;
                });

        try {
            dsl.loadInto(ADDRESS_BALANCE_CURRENT)
                    .onDuplicateKeyUpdate()
                    //.bulkAfter(batchSize)
                    .batchAfter(batchSize)
                    .commitAfter(batchSize)
                    .loadRecords(addressBalanceRecords)
                    .fields(ADDRESS_BALANCE_CURRENT.ADDRESS,
                            ADDRESS_BALANCE_CURRENT.UNIT,
                            ADDRESS_BALANCE_CURRENT.QUANTITY,
                            ADDRESS_BALANCE_CURRENT.ADDR_FULL,
                            ADDRESS_BALANCE_CURRENT.SLOT,
                            ADDRESS_BALANCE_CURRENT.BLOCK,
                            ADDRESS_BALANCE_CURRENT.BLOCK_TIME,
                            ADDRESS_BALANCE_CURRENT.EPOCH,
                            ADDRESS_BALANCE_CURRENT.UPDATE_DATETIME)
                    .execute();
        } catch (IOException e) {
            log.error("Error while inserting address balance current records", e);
        }
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
    public int deleteAddressBalanceBeforeSlotExceptTop(List<Pair<String, String>> addresses, long slot) {
        //Add to cache and delete once the cache size reaches a threshold
        addresses.forEach(addressUnit -> {
            addressBalanceKeysToDeleteCache.put(addressUnit, slot);
        });

        return 0;
    }

    @Override
    public boolean isBatchDeleteSupported() {
        return true;
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

        if (storeProperties.isParallelWrite()) {
            int partitionSize = getPartitionSize(entities.size());
            ListUtil.partitionAndApply(entities, partitionSize, this::saveStakeBalanceBatch);
        } else {
            saveStakeBalanceBatch(entities);
        }
    }

    private void saveStakeBalanceBatch(List<StakeAddressBalanceEntity> stakeAddressBalances) {
        if (enableJPAInsert) {
            if (log.isTraceEnabled())
                log.trace("\tInserting stake address balances using JPA batch");

            stakeBalanceRepository.saveAll(stakeAddressBalances);
        } else {
            if (log.isTraceEnabled())
                log.trace("\tInserting stake address balances using JOOQ batch");

            saveStakeBalanceBatchJOOQ(stakeAddressBalances);
        }
    }

    private void saveStakeBalanceBatchJOOQ(List<StakeAddressBalanceEntity> stakeAddressBalances) {
        LocalDateTime localDateTime = LocalDateTime.now();
        var inserts = stakeAddressBalances.stream()
                        .map(stakeAddrBalance -> dsl.insertInto(STAKE_ADDRESS_BALANCE)
                                .set(STAKE_ADDRESS_BALANCE.ADDRESS, stakeAddrBalance.getAddress())
                                .set(STAKE_ADDRESS_BALANCE.SLOT, stakeAddrBalance.getSlot())
                                .set(STAKE_ADDRESS_BALANCE.QUANTITY, stakeAddrBalance.getQuantity())
                                .set(STAKE_ADDRESS_BALANCE.BLOCK, stakeAddrBalance.getBlockNumber())
                                .set(STAKE_ADDRESS_BALANCE.BLOCK_TIME, stakeAddrBalance.getBlockTime())
                                .set(STAKE_ADDRESS_BALANCE.EPOCH, stakeAddrBalance.getEpoch())
                                .set(STAKE_ADDRESS_BALANCE.UPDATE_DATETIME, localDateTime)
                                .onDuplicateKeyUpdate()
                                .set(STAKE_ADDRESS_BALANCE.QUANTITY, stakeAddrBalance.getQuantity())
                                .set(STAKE_ADDRESS_BALANCE.BLOCK, stakeAddrBalance.getBlockNumber())
                                .set(STAKE_ADDRESS_BALANCE.BLOCK_TIME, stakeAddrBalance.getBlockTime())
                                .set(STAKE_ADDRESS_BALANCE.EPOCH, stakeAddrBalance.getEpoch())
                                .set(STAKE_ADDRESS_BALANCE.UPDATE_DATETIME, localDateTime)
                        ).toList();

        dsl.batch(inserts).execute();
    }

    @Transactional
    @Override
    public void saveCurrentStakeAddressBalances(List<StakeAddressBalance> stakeBalances) {
        LocalDateTime localDateTime = LocalDateTime.now();

        int batchSize = storeProperties.getWriteThreadDefaultBatchSize();

        var stakeAddressBalanceRecords = stakeBalances.stream()
                .map(stakeAddrBalance -> {
                    var stakeAddressBalanceRec = dsl.newRecord(STAKE_ADDRESS_BALANCE_CURRENT);
                    stakeAddressBalanceRec.setAddress(stakeAddrBalance.getAddress());
                    stakeAddressBalanceRec.setQuantity(stakeAddrBalance.getQuantity());
                    stakeAddressBalanceRec.setSlot(stakeAddrBalance.getSlot());
                    stakeAddressBalanceRec.setBlock(stakeAddrBalance.getBlockNumber());
                    stakeAddressBalanceRec.setBlockTime(stakeAddrBalance.getBlockTime());
                    stakeAddressBalanceRec.setEpoch(stakeAddrBalance.getEpoch());
                    stakeAddressBalanceRec.setUpdateDatetime(localDateTime);
                    return stakeAddressBalanceRec;
                });

        try {
            dsl.loadInto(STAKE_ADDRESS_BALANCE_CURRENT)
                    .onDuplicateKeyUpdate()
                    //.bulkAfter(batchSize)
                    .batchAfter(batchSize)
                    .commitAfter(batchSize)
                    .loadRecords(stakeAddressBalanceRecords)
                    .fields(STAKE_ADDRESS_BALANCE_CURRENT.ADDRESS,
                            STAKE_ADDRESS_BALANCE_CURRENT.QUANTITY,
                            STAKE_ADDRESS_BALANCE_CURRENT.SLOT,
                            STAKE_ADDRESS_BALANCE_CURRENT.BLOCK,
                            STAKE_ADDRESS_BALANCE_CURRENT.BLOCK_TIME,
                            STAKE_ADDRESS_BALANCE_CURRENT.EPOCH,
                            STAKE_ADDRESS_BALANCE_CURRENT.UPDATE_DATETIME)
                    .execute();
        } catch (IOException e) {
            log.error("Error while inserting stake address balance current records", e);
        }
    }

    @Transactional
    @Override
    public int deleteStakeBalanceBeforeSlotExceptTop(String address, long slot) {
        //Find the latest stake address balance before the slot and delete all address balances before that
        return stakeBalanceRepository.findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(address, slot)
                .map(addressBalanceEntity -> stakeBalanceRepository.deleteAllBeforeSlot(address, addressBalanceEntity.getSlot() - 1)).orElse(0);
    }

    @Override
    public int deleteStakeBalanceBeforeSlotExceptTop(List<String> addresses, long slot) {
        //Add to cache and delete once the cache size reaches a threshold
        addresses.forEach(address -> {
            stakeBalanceKeysToDeleteCache.put(address, slot);
        });

        return 0;
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

    @EventListener
    @Transactional
    public void handleBalanceDeleteOnCommit(CommitEvent commitEvent) {
        //Delete address balances if cache size reaches a threshold
        if (commitEvent.getMetadata().isSyncMode()) {
            performBalanceDelete(true); //Ignore threshold check. Delete always
        } else {
            performBalanceDelete(false);
        }
    }

    private long performBalanceDelete(boolean ignoreThresholdCheck) {
        long t1 = System.currentTimeMillis();

        if (!ignoreThresholdCheck && addressBalanceKeysToDeleteCache.size() < accountStoreProperties.getBalanceCleanupBatchThreshold())
            return 0;

        log.info("<< Start: Deleting balances : cache size {} >>",addressBalanceKeysToDeleteCache.size() + stakeBalanceKeysToDeleteCache.size());

        var addressBalanceDeleteQueries = addressBalanceKeysToDeleteCache.entrySet().stream()
                .map(pairLongEntry -> dsl.deleteFrom(ADDRESS_BALANCE)
                        .where(ADDRESS_BALANCE.ADDRESS.eq(pairLongEntry.getKey().getFirst()))
                        .and(ADDRESS_BALANCE.UNIT.eq(pairLongEntry.getKey().getSecond()))
                        .and(ADDRESS_BALANCE.SLOT.lt(pairLongEntry.getValue())));

        var stakeBalanceDeleteQueries = stakeBalanceKeysToDeleteCache.entrySet().stream()
                .map(entry -> dsl.deleteFrom(STAKE_ADDRESS_BALANCE)
                        .where(
                                STAKE_ADDRESS_BALANCE.ADDRESS.eq(entry.getKey())
                                        .and(STAKE_ADDRESS_BALANCE.SLOT.lt(entry.getValue()))
                        ));

        var allDeleteQueries = Stream.concat(addressBalanceDeleteQueries, stakeBalanceDeleteQueries).toList();

        int[] deletedRows = dsl.batch(allDeleteQueries).execute();

        int totalCount = Arrays.stream(deletedRows).sum();

        long t2 = System.currentTimeMillis();
        log.info("<< End: Deleted balance rows: {}, Time taken: {} ms >>", totalCount, (t2 - t1));

        addressBalanceKeysToDeleteCache.clear();
        stakeBalanceKeysToDeleteCache.clear();

        return totalCount;
    }

    @Transactional
    @Override
    public void refreshCurrentAddressBalance(String address, Set<String> units, Long slot) {
        if (units == null || units.isEmpty()) {
            log.warn("No units found for address: {}", address);
            return;
        }

        List<String> unitList = new ArrayList<>(units);

        List<List<String>> unitChunks = partition(unitList, 20);

        for (List<String> unitChunk : unitChunks) {
            rollbackForAddressUnits(address, unitChunk, slot);
        }

    }

    private void rollbackForAddressUnits(String address, List<String> units, Long slot) {
        // Table aliases
        var ab = ADDRESS_BALANCE.as("ab");
        var abc = ADDRESS_BALANCE_CURRENT;
        var ranked = name("ranked");
        var latest = name("latest_balances");

        // Step 1: CTE for latest balances with row_number()
        var latestBalancesQuery = dsl
                .select()
                .from(
                        select(ab.fields())
                                .select(rowNumber().over()
                                        .partitionBy(ab.ADDRESS, ab.UNIT)
                                        .orderBy(ab.SLOT.desc())
                                        .as("rn"))
                                .from(ab)
                                .where(ab.ADDRESS.eq(address))
                                .and(ab.UNIT.in(units))
                                .and(ab.SLOT.le(slot))
                                .asTable(ranked)
                )
                .where(field("rn", Integer.class).eq(1));
                //.asTable(latest);

        Table<Record> latestTable = latestBalancesQuery.asTable(latest);
        CommonTableExpression<?> latestBalancesCte = name("latest_balances").as(latestBalancesQuery);

        // Step 2: Delete existing entries
        dsl.deleteFrom(abc)
                .where(abc.ADDRESS.eq(address))
                .and(abc.UNIT.in(units))
                .execute();

        // Step 3: Upsert back into current balance table
        dsl.with(latestBalancesCte)
                .insertInto(abc)
                .columns(
                        abc.ADDRESS,
                        abc.UNIT,
                        abc.QUANTITY,
                        abc.ADDR_FULL,
                        abc.SLOT,
                        abc.BLOCK,
                        abc.BLOCK_TIME,
                        abc.EPOCH,
                        abc.UPDATE_DATETIME
                )
                .select(
                        select(
                                latestTable.field("address", String.class),
                                latestTable.field("unit", String.class),
                                latestTable.field("quantity", BigInteger.class),
                                latestTable.field("addr_full", String.class),
                                latestTable.field("slot", Long.class),
                                latestTable.field("block", Long.class),
                                latestTable.field("block_time", Long.class),
                                latestTable.field("epoch", Integer.class),
                                latestTable.field("update_datetime", LocalDateTime.class)
                        ).from(latest)
                )
                .execute();
    }

    @Transactional
    @Override
    public void refreshCurrentStakeAddressBalance(List<String> stakeAddresses, Long slot) {
        List<List<String>> addressChunks = partition(stakeAddresses, 20);

        for (List<String> chunk : addressChunks) {
            rollbackForStakeAddresses(chunk, slot);
        }
    }

    private void rollbackForStakeAddresses(List<String> addresses, Long slot) {
        var sab = STAKE_ADDRESS_BALANCE.as("sab");
        var sabc = STAKE_ADDRESS_BALANCE_CURRENT;
        var ranked = name("ranked");
        var latest = name("latest_balances");

        // Step 1: Build CTE for latest balances
        var latestBalancesQuery = dsl
                .select()
                .from(
                        select(sab.fields())
                                .select(
                                        rowNumber().over()
                                                .partitionBy(sab.ADDRESS)
                                                .orderBy(sab.SLOT.desc())
                                                .as("rn")
                                )
                                .from(sab)
                                .where(sab.ADDRESS.in(addresses))
                                .and(sab.SLOT.le(slot))
                                .asTable(ranked)
                )
                .where(field("rn", Integer.class).eq(1));
                //.asTable(latest);

        Table<Record> latestTable = latestBalancesQuery.asTable(latest);
        CommonTableExpression<?> latestBalancesCte = name("latest_balances").as(latestBalancesQuery);


        // Step 2: Delete old current records for addresses
        dsl.deleteFrom(sabc)
                .where(sabc.ADDRESS.in(addresses))
                .execute();

        // Step 3: Insert or update latest balance
        dsl.with(latestBalancesCte)
                .insertInto(sabc)
                .columns(
                        sabc.ADDRESS,
                        sabc.QUANTITY,
                        sabc.SLOT,
                        sabc.BLOCK,
                        sabc.BLOCK_TIME,
                        sabc.EPOCH,
                        sabc.UPDATE_DATETIME
                )
                .select(
                        select(
                                latestTable.field("address", String.class),
                                latestTable.field("quantity", BigInteger.class),
                                latestTable.field("slot", Long.class),
                                latestTable.field("block", Long.class),
                                latestTable.field("block_time", Long.class),
                                latestTable.field("epoch", Integer.class),
                                latestTable.field("update_datetime", LocalDateTime.class)
                        ).from(latest)
                )
                .execute();
    }

    @Override
    public List<AddressBalance> getAddressBalanceBySlotGreaterThan(Long slot) {
        return dsl.select()
                .from(ADDRESS_BALANCE)
                .where(ADDRESS_BALANCE.SLOT.gt(slot))
                .orderBy(ADDRESS_BALANCE.SLOT.asc())
                .fetchInto(AddressBalance.class);
    }

    @Override
    public List<StakeAddressBalance> getStakeAddressBalanceBySlotGreaterThan(Long slot) {
        return dsl.select()
                .from(STAKE_ADDRESS_BALANCE)
                .where(STAKE_ADDRESS_BALANCE.SLOT.gt(slot))
                .orderBy(STAKE_ADDRESS_BALANCE.SLOT.asc())
                .fetchInto(StakeAddressBalance.class);
    }

    @Override
    public List<AddressBalance> getCurrentAddressBalance(String address) {
        return dsl.select()
                .from(ADDRESS_BALANCE_CURRENT)
                .where(ADDRESS_BALANCE_CURRENT.ADDRESS.eq(address))
                .fetchInto(AddressBalance.class);
    }

    @Override
    public Optional<StakeAddressBalance> getCurrentStakeAddressBalance(String address) {
        return dsl.select()
                .from(STAKE_ADDRESS_BALANCE_CURRENT)
                .where(STAKE_ADDRESS_BALANCE_CURRENT.ADDRESS.eq(address))
                .fetchOptionalInto(StakeAddressBalance.class);
    }

    private int getPartitionSize(int totalSize) {
        int partitionSize = totalSize;
        if (totalSize > storeProperties.getWriteThreadDefaultBatchSize()) {
            partitionSize = totalSize / storeProperties.getWriteThreadCount();
            log.info("\tPartition size : {}", partitionSize);
        } else {
            log.info("\tPartition size : {}", partitionSize);
        }
        return partitionSize;
    }
}
