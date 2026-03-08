package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.BFAccountStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.AmountsJsonUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.BlockfrostDialectUtil;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.EPOCH_STAKE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;
import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.DELEGATION;
import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.STAKE_REGISTRATION;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.TRANSACTION;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.WITHDRAWAL;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;

@Component
@RequiredArgsConstructor
@Slf4j
public class BFAccountStorageReaderImpl implements BFAccountStorageReader {

    private final DSLContext dsl;

    @Override
    public Optional<AccountInfo> getAccountInfo(String stakeAddress) {
        var registrations = dsl.select(STAKE_REGISTRATION.TYPE, STAKE_REGISTRATION.EPOCH)
                .from(STAKE_REGISTRATION)
                .where(STAKE_REGISTRATION.ADDRESS.eq(stakeAddress))
                .orderBy(STAKE_REGISTRATION.SLOT.asc())
                .fetch();

        if (registrations.isEmpty()) {
            return Optional.empty();
        }

        var lastReg = dsl.select(STAKE_REGISTRATION.TYPE)
                .from(STAKE_REGISTRATION)
                .where(STAKE_REGISTRATION.ADDRESS.eq(stakeAddress))
                .orderBy(STAKE_REGISTRATION.SLOT.desc())
                .limit(1)
                .fetchOne();

        String lastType = lastReg != null ? lastReg.get(STAKE_REGISTRATION.TYPE) : null;
        boolean active = lastType != null
                && lastType.toUpperCase().contains("REGISTRATION")
                && !lastType.toUpperCase().contains("DEREGISTRATION");
        Integer activeEpoch = registrations.get(0).get(STAKE_REGISTRATION.EPOCH);

        BigInteger controlledAmount = BigInteger.ZERO;
        try {
            Condition unspentUtxo = DSL.notExists(
                    dsl.selectOne().from(TX_INPUT)
                            .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                            .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
            );
            var sumRec = dsl.select(DSL.sum(ADDRESS_UTXO.LOVELACE_AMOUNT.cast(BigDecimal.class)))
                    .from(ADDRESS_UTXO)
                    .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                    .and(unspentUtxo)
                    .fetchOne(0, BigDecimal.class);
            if (sumRec != null) {
                controlledAmount = sumRec.toBigInteger();
            }
        } catch (DataAccessException e) {
            log.warn("Could not fetch controlled amount for {}: {}", stakeAddress, e.getMessage());
        }

        String poolId = null;
        try {
            var delRec = dsl.select(DELEGATION.POOL_ID)
                    .from(DELEGATION)
                    .where(DELEGATION.ADDRESS.eq(stakeAddress))
                    .orderBy(DELEGATION.SLOT.desc())
                    .limit(1)
                    .fetchOne();
            if (delRec != null) {
                poolId = delRec.get(DELEGATION.POOL_ID);
            }
        } catch (DataAccessException e) {
            log.warn("Could not fetch pool delegation for {}: {}", stakeAddress, e.getMessage());
        }

        BigInteger rewardsSum = BigInteger.ZERO;
        try {
            var rewardSum = dsl.select(DSL.sum(REWARD.AMOUNT).cast(BigDecimal.class))
                    .from(REWARD)
                    .where(REWARD.ADDRESS.eq(stakeAddress))
                    .fetchOne(0, BigDecimal.class);
            if (rewardSum != null) {
                rewardsSum = rewardSum.toBigInteger();
            }
        } catch (DataAccessException e) {
            log.warn("Could not fetch rewards sum for {}: {}", stakeAddress, e.getMessage());
        }

        BigInteger withdrawalsSum = BigInteger.ZERO;
        try {
            var wdSum = dsl.select(DSL.sum(WITHDRAWAL.AMOUNT).cast(BigDecimal.class))
                    .from(WITHDRAWAL)
                    .where(WITHDRAWAL.ADDRESS.eq(stakeAddress))
                    .fetchOne(0, BigDecimal.class);
            if (wdSum != null) {
                withdrawalsSum = wdSum.toBigInteger();
            }
        } catch (DataAccessException e) {
            log.warn("Could not fetch withdrawals sum for {}: {}", stakeAddress, e.getMessage());
        }

        BigInteger reservesSum = BigInteger.ZERO;
        BigInteger treasurySum = BigInteger.ZERO;
        try {
            Table<?> mirTable = DSL.table(DSL.name("mir"));
            Field<String> mirAddress = DSL.field(DSL.name("mir", "address"), String.class);
            Field<String> mirPot = DSL.field(DSL.name("mir", "pot"), String.class);
            Field<BigDecimal> mirAmount = DSL.field(DSL.name("mir", "amount"), BigDecimal.class);

            var mirResults = dsl.select(mirPot, DSL.sum(mirAmount).as("total"))
                    .from(mirTable)
                    .where(mirAddress.eq(stakeAddress))
                    .groupBy(mirPot)
                    .fetch();

            for (Record mirRec : mirResults) {
                String pot = mirRec.get(mirPot);
                BigDecimal total = mirRec.get("total", BigDecimal.class);
                if (total == null) continue;
                if ("RESERVES".equalsIgnoreCase(pot)) {
                    reservesSum = total.toBigInteger();
                } else if ("TREASURY".equalsIgnoreCase(pot)) {
                    treasurySum = total.toBigInteger();
                }
            }
        } catch (DataAccessException e) {
            log.warn("Could not fetch MIR data for {}: {}", stakeAddress, e.getMessage());
        }

        // controlled_amount = spendable UTXOs + unclaimed rewards (rewards - withdrawals)
        BigInteger withdrawable = rewardsSum.subtract(withdrawalsSum);
        if (withdrawable.compareTo(BigInteger.ZERO) < 0) withdrawable = BigInteger.ZERO;
        BigInteger totalControlled = controlledAmount.add(withdrawable);

        return Optional.of(new AccountInfo(stakeAddress, active, active, activeEpoch,
                totalControlled, rewardsSum, withdrawalsSum, reservesSum, treasurySum, poolId));
    }

    @Override
    public List<AccountReward> findRewards(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        try {
            SortField<?> orderBy = order == Order.desc ? REWARD.EARNED_EPOCH.desc() : REWARD.EARNED_EPOCH.asc();
            return dsl.select(REWARD.EARNED_EPOCH, REWARD.AMOUNT, REWARD.POOL_ID, REWARD.TYPE)
                    .from(REWARD)
                    .where(REWARD.ADDRESS.eq(stakeAddress))
                    .orderBy(orderBy)
                    .limit(count)
                    .offset(offset)
                    .fetch(rec -> new AccountReward(
                            rec.get(REWARD.EARNED_EPOCH),
                            rec.get(REWARD.AMOUNT),
                            rec.get(REWARD.POOL_ID),
                            rec.get(REWARD.TYPE)));
        } catch (DataAccessException e) {
            log.warn("Could not fetch rewards for {} (adapot may be disabled): {}", stakeAddress, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<AccountHistory> findHistory(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        try {
            SortField<?> orderBy = order == Order.desc ? EPOCH_STAKE.ACTIVE_EPOCH.desc() : EPOCH_STAKE.ACTIVE_EPOCH.asc();
            return dsl.select(EPOCH_STAKE.ACTIVE_EPOCH, EPOCH_STAKE.AMOUNT, EPOCH_STAKE.POOL_ID)
                    .from(EPOCH_STAKE)
                    .where(EPOCH_STAKE.ADDRESS.eq(stakeAddress))
                    .orderBy(orderBy)
                    .limit(count)
                    .offset(offset)
                    .fetch(rec -> new AccountHistory(
                            rec.get(EPOCH_STAKE.ACTIVE_EPOCH) != null ? rec.get(EPOCH_STAKE.ACTIVE_EPOCH) : 0,
                            rec.get(EPOCH_STAKE.AMOUNT),
                            rec.get(EPOCH_STAKE.POOL_ID)));
        } catch (DataAccessException e) {
            log.warn("Could not fetch history for {} (adapot may be disabled): {}", stakeAddress, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<AccountDelegation> findDelegations(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        SortField<?> orderBy = order == Order.desc ? DELEGATION.SLOT.desc() : DELEGATION.SLOT.asc();

        // BF computes delegation amount as the sum of the delegation tx's own outputs
        // belonging to this stake address (not historical UTXO balance)
        var delegTxOutputSum = dsl.select(DSL.sum(ADDRESS_UTXO.LOVELACE_AMOUNT.cast(BigDecimal.class)))
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.TX_HASH.eq(DELEGATION.TX_HASH))
                .and(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(DELEGATION.ADDRESS))
                .asField("amount");

        return dsl.select(DELEGATION.TX_HASH, DELEGATION.EPOCH, DELEGATION.POOL_ID, DELEGATION.SLOT,
                        TRANSACTION.BLOCK_TIME, TRANSACTION.BLOCK, delegTxOutputSum)
                .from(DELEGATION)
                .leftJoin(TRANSACTION).on(TRANSACTION.TX_HASH.eq(DELEGATION.TX_HASH))
                .where(DELEGATION.ADDRESS.eq(stakeAddress))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(rec -> {
                    BigDecimal amt = rec.get("amount", BigDecimal.class);
                    return new AccountDelegation(
                            rec.get(DELEGATION.EPOCH) != null ? rec.get(DELEGATION.EPOCH) + 2 : 0,
                            rec.get(DELEGATION.TX_HASH),
                            amt != null ? amt.toBigInteger() : BigInteger.ZERO,
                            rec.get(DELEGATION.POOL_ID),
                            rec.get(DELEGATION.SLOT),
                            rec.get(TRANSACTION.BLOCK_TIME),
                            rec.get(TRANSACTION.BLOCK));
                });
    }

    @Override
    public List<AccountRegistration> findRegistrations(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        SortField<?> orderBy = order == Order.desc ? STAKE_REGISTRATION.SLOT.desc() : STAKE_REGISTRATION.SLOT.asc();
        return dsl.select(STAKE_REGISTRATION.TX_HASH, STAKE_REGISTRATION.TYPE, STAKE_REGISTRATION.SLOT,
                        TRANSACTION.BLOCK_TIME, TRANSACTION.BLOCK)
                .from(STAKE_REGISTRATION)
                .leftJoin(TRANSACTION).on(TRANSACTION.TX_HASH.eq(STAKE_REGISTRATION.TX_HASH))
                .where(STAKE_REGISTRATION.ADDRESS.eq(stakeAddress))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(rec -> new AccountRegistration(
                        rec.get(STAKE_REGISTRATION.TX_HASH),
                        rec.get(STAKE_REGISTRATION.TYPE),
                        rec.get(STAKE_REGISTRATION.SLOT),
                        rec.get(TRANSACTION.BLOCK_TIME),
                        rec.get(TRANSACTION.BLOCK)));
    }

    @Override
    public List<AccountWithdrawal> findWithdrawals(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        SortField<?> orderBy = order == Order.desc ? WITHDRAWAL.SLOT.desc() : WITHDRAWAL.SLOT.asc();
        return dsl.select(WITHDRAWAL.TX_HASH, WITHDRAWAL.AMOUNT)
                .from(WITHDRAWAL)
                .where(WITHDRAWAL.ADDRESS.eq(stakeAddress))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(rec -> new AccountWithdrawal(
                        rec.get(WITHDRAWAL.TX_HASH),
                        rec.get(WITHDRAWAL.AMOUNT)));
    }

    @Override
    public List<AccountMir> findMIRs(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        try {
            Table<?> mirTable = DSL.table(DSL.name("mir"));
            Field<String> mirAddress = DSL.field(DSL.name("mir", "address"), String.class);
            Field<String> mirTxHash = DSL.field(DSL.name("mir", "tx_hash"), String.class);
            Field<Long> mirSlot = DSL.field(DSL.name("mir", "slot"), Long.class);
            Field<BigDecimal> mirAmount = DSL.field(DSL.name("mir", "amount"), BigDecimal.class);

            SortField<?> orderBy = order == Order.desc ? mirSlot.desc() : mirSlot.asc();

            return dsl.select(mirTxHash, DSL.sum(mirAmount).cast(BigDecimal.class).as("total_amount"))
                    .from(mirTable)
                    .where(mirAddress.eq(stakeAddress))
                    .groupBy(mirTxHash, mirSlot)
                    .orderBy(orderBy)
                    .limit(count)
                    .offset(offset)
                    .fetch(rec -> {
                        BigDecimal total = rec.get("total_amount", BigDecimal.class);
                        return new AccountMir(
                                rec.get(mirTxHash),
                                total != null ? total.toBigInteger() : BigInteger.ZERO);
                    });
        } catch (DataAccessException e) {
            log.warn("Could not fetch MIRs for {} (mir store may be disabled): {}", stakeAddress, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<AccountAddress> findAddresses(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        return dsl.selectDistinct(ADDRESS_UTXO.OWNER_ADDR)
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                .orderBy(ADDRESS_UTXO.OWNER_ADDR.asc())
                .limit(count)
                .offset(offset)
                .fetch(rec -> new AccountAddress(rec.get(ADDRESS_UTXO.OWNER_ADDR)));
    }

    @Override
    public List<AccountAddressAsset> findAddressAssets(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;

        Condition unspentCondition = DSL.notExists(
                dsl.selectOne()
                        .from(TX_INPUT)
                        .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                        .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
        );

        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            Field<?> amountsField = ADDRESS_UTXO.AMOUNTS;
            Table<?> amountTable = DSL.table(
                    "jsonb_to_recordset({0}::jsonb) as amt(unit text, quantity numeric)",
                    amountsField
            );
            Field<String> unitField = DSL.field("amt.unit", String.class);
            Field<BigDecimal> quantityField = DSL.field("amt.quantity", BigDecimal.class);

            return dsl.select(unitField.as("unit"), DSL.sum(quantityField).cast(BigDecimal.class).as("total_quantity"))
                    .from(ADDRESS_UTXO)
                    .join(DSL.lateral(amountTable)).on(DSL.trueCondition())
                    .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                    .and(unspentCondition)
                    .and(unitField.ne("lovelace"))
                    .groupBy(unitField)
                    .orderBy(unitField.asc())
                    .limit(count)
                    .offset(offset)
                    .fetch(rec -> {
                        BigDecimal qty = rec.get("total_quantity", BigDecimal.class);
                        return new AccountAddressAsset(
                                rec.get("unit", String.class),
                                qty != null ? qty.toBigInteger() : BigInteger.ZERO);
                    });
        } else {
            var rows = dsl.select(ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts"))
                    .from(ADDRESS_UTXO)
                    .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                    .and(unspentCondition)
                    .fetch();

            Map<String, BigInteger> totals = new TreeMap<>();
            for (Record rec : rows) {
                Map<String, BigInteger> unitQty = AmountsJsonUtil.toQuantityByUnit(rec.get("amounts", String.class));
                for (Map.Entry<String, BigInteger> e : unitQty.entrySet()) {
                    if (!"lovelace".equals(e.getKey())) {
                        totals.merge(e.getKey(), e.getValue(), BigInteger::add);
                    }
                }
            }

            return totals.entrySet().stream()
                    .skip(offset)
                    .limit(count)
                    .map(e -> new AccountAddressAsset(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Optional<AccountAddressesTotal> getAddressesTotal(String stakeAddress) {
        try {
            var rows = dsl.select(ADDRESS_TX_AMOUNT.UNIT, ADDRESS_TX_AMOUNT.QUANTITY, ADDRESS_TX_AMOUNT.TX_HASH)
                    .from(ADDRESS_TX_AMOUNT)
                    .where(ADDRESS_TX_AMOUNT.STAKE_ADDRESS.eq(stakeAddress))
                    .fetch();

            Map<String, BigInteger> receivedMap = new HashMap<>();
            Map<String, BigInteger> sentMap = new HashMap<>();
            Set<String> txHashes = new HashSet<>();

            for (Record rec : rows) {
                String unit = rec.get(ADDRESS_TX_AMOUNT.UNIT);
                BigInteger qty = rec.get(ADDRESS_TX_AMOUNT.QUANTITY);
                String txHash = rec.get(ADDRESS_TX_AMOUNT.TX_HASH);

                if (txHash != null) txHashes.add(txHash);
                if (qty == null) continue;

                if (qty.compareTo(BigInteger.ZERO) > 0) {
                    receivedMap.merge(unit, qty, BigInteger::add);
                } else if (qty.compareTo(BigInteger.ZERO) < 0) {
                    sentMap.merge(unit, qty.abs(), BigInteger::add);
                }
            }

            return Optional.of(new AccountAddressesTotal(stakeAddress, receivedMap, sentMap, txHashes.size()));
        } catch (DataAccessException e) {
            log.warn("Could not fetch addresses total for {}: {}", stakeAddress, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<AccountUtxo> findUtxos(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        SortField<?> slotOrder = order == Order.desc ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc();
        SortField<?> idxOrder = order == Order.desc ? ADDRESS_UTXO.OUTPUT_INDEX.desc() : ADDRESS_UTXO.OUTPUT_INDEX.asc();

        Condition unspentCondition = DSL.notExists(
                dsl.selectOne()
                        .from(TX_INPUT)
                        .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                        .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
        );

        return dsl.select(
                        ADDRESS_UTXO.OWNER_ADDR,
                        ADDRESS_UTXO.TX_HASH,
                        ADDRESS_UTXO.OUTPUT_INDEX,
                        ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts"),
                        ADDRESS_UTXO.LOVELACE_AMOUNT,
                        ADDRESS_UTXO.BLOCK_HASH,
                        ADDRESS_UTXO.DATA_HASH,
                        ADDRESS_UTXO.INLINE_DATUM,
                        ADDRESS_UTXO.REFERENCE_SCRIPT_HASH
                )
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                .and(unspentCondition)
                .orderBy(slotOrder, idxOrder)
                .limit(count)
                .offset(offset)
                .fetch(rec -> {
                    Map<String, BigInteger> amounts = AmountsJsonUtil.toQuantityByUnit(rec.get("amounts", String.class));
                    Long lovelace = rec.get(ADDRESS_UTXO.LOVELACE_AMOUNT);
                    if (lovelace != null && lovelace != 0) {
                        amounts.put("lovelace", BigInteger.valueOf(lovelace));
                    }
                    return new AccountUtxo(
                            rec.get(ADDRESS_UTXO.OWNER_ADDR),
                            rec.get(ADDRESS_UTXO.TX_HASH),
                            rec.get(ADDRESS_UTXO.OUTPUT_INDEX),
                            amounts,
                            rec.get(ADDRESS_UTXO.BLOCK_HASH),
                            rec.get(ADDRESS_UTXO.DATA_HASH),
                            rec.get(ADDRESS_UTXO.INLINE_DATUM),
                            rec.get(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH));
                });
    }

    @Override
    public List<AccountTransaction> findTransactions(String stakeAddress, int page, int count, Order order, String from, String to) {
        int offset = Math.max(page, 0) * count;
        SortField<?> slotOrder = order == Order.desc ? ADDRESS_TX_AMOUNT.SLOT.desc() : ADDRESS_TX_AMOUNT.SLOT.asc();

        Condition condition = ADDRESS_TX_AMOUNT.STAKE_ADDRESS.eq(stakeAddress);
        if (from != null && !from.isBlank()) {
            try {
                long fromBlock = Long.parseLong(from.split(":")[0]);
                condition = condition.and(ADDRESS_TX_AMOUNT.BLOCK.ge(fromBlock));
            } catch (NumberFormatException e) {
                log.warn("Invalid 'from' block reference: {}", from);
            }
        }
        if (to != null && !to.isBlank()) {
            try {
                long toBlock = Long.parseLong(to.split(":")[0]);
                condition = condition.and(ADDRESS_TX_AMOUNT.BLOCK.le(toBlock));
            } catch (NumberFormatException e) {
                log.warn("Invalid 'to' block reference: {}", to);
            }
        }

        return dsl.selectDistinct(
                        ADDRESS_TX_AMOUNT.ADDRESS,
                        ADDRESS_TX_AMOUNT.TX_HASH,
                        ADDRESS_TX_AMOUNT.SLOT,
                        ADDRESS_TX_AMOUNT.BLOCK,
                        ADDRESS_TX_AMOUNT.BLOCK_TIME,
                        TRANSACTION.TX_INDEX
                )
                .from(ADDRESS_TX_AMOUNT)
                .leftJoin(TRANSACTION).on(TRANSACTION.TX_HASH.eq(ADDRESS_TX_AMOUNT.TX_HASH))
                .where(condition)
                .orderBy(slotOrder, TRANSACTION.TX_INDEX.asc(), ADDRESS_TX_AMOUNT.ADDRESS.asc())
                .limit(count)
                .offset(offset)
                .fetch(rec -> new AccountTransaction(
                        rec.get(ADDRESS_TX_AMOUNT.ADDRESS),
                        rec.get(ADDRESS_TX_AMOUNT.TX_HASH),
                        rec.get(TRANSACTION.TX_INDEX) != null ? rec.get(TRANSACTION.TX_INDEX).longValue() : 0L,
                        rec.get(ADDRESS_TX_AMOUNT.BLOCK) != null ? rec.get(ADDRESS_TX_AMOUNT.BLOCK) : 0L,
                        rec.get(ADDRESS_TX_AMOUNT.BLOCK_TIME) != null ? rec.get(ADDRESS_TX_AMOUNT.BLOCK_TIME) : 0L));
    }
}
