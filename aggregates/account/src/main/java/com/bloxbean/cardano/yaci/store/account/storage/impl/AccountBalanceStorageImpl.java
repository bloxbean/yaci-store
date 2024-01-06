package com.bloxbean.cardano.yaci.store.account.storage.impl;

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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.ADDRESS_BALANCE;
import static org.jooq.impl.DSL.field;

@RequiredArgsConstructor
@Slf4j
public class AccountBalanceStorageImpl implements AccountBalanceStorage {
    private final AddressBalanceRepository addressBalanceRepository;
    private final StakeBalanceRepository stakeBalanceRepository;
    private final AccountMapper mapper = AccountMapper.INSTANCE;
    private final DSLContext dsl;

    @Value("${store.account.write-batch-size:1000}")
    private int batchSize = 1000;

    @Value("${store.account.parallel-write:false}")
    private boolean parallelWrite = false;

    @Override
    public Optional<AddressBalance> getAddressBalance(String address, long slot) {
        return addressBalanceRepository.findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(address, slot)
                .map(mapper::toAddressBalance);
    }

    @Override
    public Optional<AddressBalance> getAddressBalanceByTime(String address, long time) {
        if (time == 0)
            throw new IllegalArgumentException("Time cannot be 0");
        return addressBalanceRepository.findTopByAddressAndBlockTimeIsLessThanEqualOrderByBlockTimeDesc(address, time)
                .map(mapper::toAddressBalance);

    }

    @Override
    public Optional<AddressBalance> getAddressBalance(String address) {
        return addressBalanceRepository.findLatestAddressBalanceByAddress(address).stream()
                .map(mapper::toAddressBalance)
                .findFirst();
    }

    @Transactional
    @Override
    public void saveAddressBalances(@NonNull List<AddressBalance> addressBalances) {
        List<AddressBalanceEntity> entities = addressBalances.stream().map(mapper::toAddressBalanceEntity)
                .toList();

        if (parallelWrite) {
            log.info("Writing address balances in parallel : Batch size : {}", batchSize);
            ListUtil.partitionAndApplyInParallel(entities, batchSize, addressBalanceRepository::saveAll);
        } else {
            addressBalanceRepository.saveAll(entities);
        }
    }

    @Override
    public int deleteAddressBalanceBeforeSlotExceptTop(String address, long slot) {
        //Find the latest address balance before the slot and delete all address balances before that
        return addressBalanceRepository.findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(address, slot)
                .map(addressBalanceEntity -> addressBalanceRepository.deleteByAddressAndSlotLessThan(address, addressBalanceEntity.getSlot() - 1)).orElse(0);
    }

    @Override
    public int deleteAddressBalanceBySlotGreaterThan(Long slot) {
        return addressBalanceRepository.deleteBySlotGreaterThan(slot);
    }

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

        return stakeBalanceRepository.findTopByAddressAndBlockTimeIsLessThanEqualOrderByBlockTimeDesc(address,time)
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

        if (parallelWrite) {
            ListUtil.partitionAndApplyInParallel(entities, batchSize, stakeBalanceRepository::saveAll);
        } else {
            stakeBalanceRepository.saveAll(entities);
        }
    }

    @Override
    public int deleteStakeBalanceBeforeSlotExceptTop(String address, long slot) {
        //Find the latest stake address balance before the slot and delete all address balances before that
        return stakeBalanceRepository.findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(address, slot)
                .map(addressBalanceEntity -> stakeBalanceRepository.deleteAllBeforeSlot(address, addressBalanceEntity.getSlot() - 1)).orElse(0);
    }

    @Override
    public int deleteStakeAddressBalanceBySlotGreaterThan(Long slot) {
        return stakeBalanceRepository.deleteBySlotGreaterThan(slot);
    }

    //TODO -- This query is very slow. Need to optimize
    @Override
    public List<AddressBalance> getAddressesByAsset(String unit, int page, int count, Order sort) {

        Pageable pageable = PageRequest.of(page, count)
                .withSort(sort.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "address");

        // Create an alias for the outer table
        var outerAddressBalance = ADDRESS_BALANCE.as("outer_ab");
        var query = dsl
                .select(outerAddressBalance.fields())
                .from(outerAddressBalance)
                .where(field(outerAddressBalance.AMOUNTS).cast(String.class).contains("\"unit\": \"" + unit + "\""))
                .groupBy(outerAddressBalance.ADDRESS, outerAddressBalance.SLOT)
                .having(outerAddressBalance.SLOT.eq(
                        DSL.select(DSL.max(ADDRESS_BALANCE.SLOT))
                                .from(ADDRESS_BALANCE)
                                .where(field(ADDRESS_BALANCE.AMOUNTS).cast(String.class).contains("\"unit\": \"" + unit + "\""))
                                .and(ADDRESS_BALANCE.ADDRESS.eq(outerAddressBalance.ADDRESS))
                ))
                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: Ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressBalance.class);
    }

}
