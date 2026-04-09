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

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.EPOCH_STAKE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.INSTANT_REWARD;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD_REST;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.DELEGATION_VOTE;
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
    private static final String TX_SIDE_IN = "in";
    private static final String TX_SIDE_OUT = "out";

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

        var lastReg = dsl.select(STAKE_REGISTRATION.TYPE, STAKE_REGISTRATION.EPOCH, STAKE_REGISTRATION.SLOT,
                        STAKE_REGISTRATION.TX_INDEX, STAKE_REGISTRATION.CERT_INDEX)
                .from(STAKE_REGISTRATION)
                .where(STAKE_REGISTRATION.ADDRESS.eq(stakeAddress))
                .orderBy(STAKE_REGISTRATION.SLOT.desc(), STAKE_REGISTRATION.TX_INDEX.desc(), STAKE_REGISTRATION.CERT_INDEX.desc())
                .limit(1)
                .fetchOne();

        String lastType = lastReg != null ? lastReg.get(STAKE_REGISTRATION.TYPE) : null;
        boolean registered = lastType != null
                && lastType.toUpperCase().contains("REGISTRATION")
                && !lastType.toUpperCase().contains("DEREGISTRATION");

        // active_epoch = epoch of the most recent effective registration (not deregistration)
        var lastActiveReg = dsl.select(STAKE_REGISTRATION.EPOCH, STAKE_REGISTRATION.SLOT,
                        STAKE_REGISTRATION.TX_INDEX, STAKE_REGISTRATION.CERT_INDEX)
                .from(STAKE_REGISTRATION)
                .where(STAKE_REGISTRATION.ADDRESS.eq(stakeAddress))
                .and(DSL.upper(STAKE_REGISTRATION.TYPE).contains("REGISTRATION"))
                .and(DSL.upper(STAKE_REGISTRATION.TYPE).notContains("DEREGISTRATION"))
                .orderBy(STAKE_REGISTRATION.SLOT.desc(), STAKE_REGISTRATION.TX_INDEX.desc(), STAKE_REGISTRATION.CERT_INDEX.desc())
                .limit(1)
                .fetchOne();
        Integer activeEpoch = lastActiveReg != null ? lastActiveReg.get(STAKE_REGISTRATION.EPOCH)
                : registrations.get(0).get(STAKE_REGISTRATION.EPOCH);

        Long currentCycleSlot = registered && lastReg != null ? lastReg.get(STAKE_REGISTRATION.SLOT) : null;
        Integer currentCycleTxIndex = registered && lastReg != null ? lastReg.get(STAKE_REGISTRATION.TX_INDEX) : null;
        Integer currentCycleCertIndex = registered && lastReg != null ? lastReg.get(STAKE_REGISTRATION.CERT_INDEX) : null;

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
            // Pool delegation belongs to the current registration cycle even before it is fully active in the
            // latest epoch snapshot, so use the registration cycle gate instead of the active flag.
            if (registered && currentCycleSlot != null) {
                var delRec = dsl.select(DELEGATION.POOL_ID)
                        .from(DELEGATION)
                        .where(DELEGATION.ADDRESS.eq(stakeAddress))
                        .and(isAtOrAfterCurrentCycle(
                                DELEGATION.SLOT,
                                DELEGATION.TX_INDEX,
                                DELEGATION.CERT_INDEX,
                                currentCycleSlot,
                                currentCycleTxIndex,
                                currentCycleCertIndex))
                        .orderBy(DELEGATION.SLOT.desc(), DELEGATION.TX_INDEX.desc(), DELEGATION.CERT_INDEX.desc())
                        .limit(1)
                        .fetchOne();
                if (delRec != null) {
                    poolId = delRec.get(DELEGATION.POOL_ID);
                }
            }
        } catch (DataAccessException e) {
            log.warn("Could not fetch pool delegation for {}: {}", stakeAddress, e.getMessage());
        }

        BigInteger rewardsSum = BigInteger.ZERO;
        try {
            // Sum all three reward sources: staking rewards + instant rewards + leftover pool rewards
            var rewardSum = dsl.select(DSL.sum(REWARD.AMOUNT).cast(BigDecimal.class))
                    .from(REWARD)
                    .where(REWARD.ADDRESS.eq(stakeAddress))
                    .fetchOne(0, BigDecimal.class);
            if (rewardSum != null) {
                rewardsSum = rewardSum.toBigInteger();
            }
            try {
                var rewardRestSum = dsl.select(DSL.sum(REWARD_REST.AMOUNT).cast(BigDecimal.class))
                        .from(REWARD_REST)
                        .where(REWARD_REST.ADDRESS.eq(stakeAddress))
                        .fetchOne(0, BigDecimal.class);
                if (rewardRestSum != null) {
                    rewardsSum = rewardsSum.add(rewardRestSum.toBigInteger());
                }
            } catch (DataAccessException e) {
                log.warn("Could not fetch reward_rest for {}: {}", stakeAddress, e.getMessage());
            }
            try {
                var instantRewardSum = dsl.select(DSL.sum(INSTANT_REWARD.AMOUNT).cast(BigDecimal.class))
                        .from(INSTANT_REWARD)
                        .where(INSTANT_REWARD.ADDRESS.eq(stakeAddress))
                        .fetchOne(0, BigDecimal.class);
                if (instantRewardSum != null) {
                    rewardsSum = rewardsSum.add(instantRewardSum.toBigInteger());
                }
            } catch (DataAccessException e) {
                log.warn("Could not fetch instant_reward for {}: {}", stakeAddress, e.getMessage());
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

        String drepId = null;
        try {
            // Vote delegation is scoped to the current registration cycle, not to the latest active epoch stake.
            if (registered && currentCycleSlot != null) {
                var drepRec = dsl.select(DELEGATION_VOTE.DREP_ID, DELEGATION_VOTE.DREP_TYPE)
                        .from(DELEGATION_VOTE)
                        .where(DELEGATION_VOTE.ADDRESS.eq(stakeAddress))
                        .and(isAtOrAfterCurrentCycle(
                                DELEGATION_VOTE.SLOT,
                                DELEGATION_VOTE.TX_INDEX,
                                DELEGATION_VOTE.CERT_INDEX,
                                currentCycleSlot,
                                currentCycleTxIndex,
                                currentCycleCertIndex))
                        .orderBy(DELEGATION_VOTE.SLOT.desc(), DELEGATION_VOTE.TX_INDEX.desc(), DELEGATION_VOTE.CERT_INDEX.desc())
                        .limit(1)
                        .fetchOne();
                if (drepRec != null) {
                    drepId = normalizeDrepId(drepRec.get(DELEGATION_VOTE.DREP_ID), drepRec.get(DELEGATION_VOTE.DREP_TYPE));
                }
            }
        } catch (DataAccessException e) {
            log.warn("Could not fetch drep delegation for {}: {}", stakeAddress, e.getMessage());
        }

        boolean active = false;
        try {
            active = registered
                    && activeEpoch != null
                    && dsl.fetchExists(
                    dsl.selectOne()
                            .from(EPOCH_STAKE)
                            .where(EPOCH_STAKE.ADDRESS.eq(stakeAddress))
                            .and(EPOCH_STAKE.ACTIVE_EPOCH.ge(activeEpoch))
            );
        } catch (DataAccessException e) {
            log.warn("Could not fetch active epoch stake for {}: {}", stakeAddress, e.getMessage());
            active = registered && poolId != null;
        }

        return Optional.of(new AccountInfo(stakeAddress, active, registered, activeEpoch,
                totalControlled, rewardsSum, withdrawalsSum, reservesSum, treasurySum, poolId, drepId));
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
        List<SortField<?>> orderBy = order == Order.desc
                ? List.of(DELEGATION.SLOT.desc(), DELEGATION.TX_INDEX.desc(), DELEGATION.CERT_INDEX.desc())
                : List.of(DELEGATION.SLOT.asc(), DELEGATION.TX_INDEX.asc(), DELEGATION.CERT_INDEX.asc());

        // Older pointer-address outputs may not have owner_stake_addr populated.
        // Use the stake-attributed sum when available; otherwise fall back to the
        // full delegation transaction output sum.
        var delegationOutput = ADDRESS_UTXO.as("delegation_output");
        var knownAccountOutput = ADDRESS_UTXO.as("known_account_output");
        Field<BigDecimal> attributedDelegTxOutputSum = dsl.select(DSL.sum(delegationOutput.LOVELACE_AMOUNT.cast(BigDecimal.class)))
                .from(delegationOutput)
                .where(delegationOutput.TX_HASH.eq(DELEGATION.TX_HASH))
                .and(
                        delegationOutput.OWNER_STAKE_ADDR.eq(DELEGATION.ADDRESS)
                                .or(DSL.coalesce(delegationOutput.OWNER_STAKE_ADDR, "").eq("")
                                        .and(delegationOutput.OWNER_ADDR.in(
                                                dsl.selectDistinct(knownAccountOutput.OWNER_ADDR)
                                                        .from(knownAccountOutput)
                                                        .where(knownAccountOutput.OWNER_STAKE_ADDR.eq(DELEGATION.ADDRESS))
                                                        .and(knownAccountOutput.OWNER_ADDR.isNotNull())
                                        )))
                )
                .asField("attributed_amount");
        Field<BigDecimal> allDelegTxOutputSum = dsl.select(DSL.sum(delegationOutput.LOVELACE_AMOUNT.cast(BigDecimal.class)))
                .from(delegationOutput)
                .where(delegationOutput.TX_HASH.eq(DELEGATION.TX_HASH))
                .asField("fallback_amount");

        return dsl.select(DELEGATION.TX_HASH, DELEGATION.EPOCH, DELEGATION.POOL_ID, DELEGATION.SLOT,
                        TRANSACTION.BLOCK_TIME, TRANSACTION.BLOCK, attributedDelegTxOutputSum, allDelegTxOutputSum)
                .from(DELEGATION)
                .leftJoin(TRANSACTION).on(TRANSACTION.TX_HASH.eq(DELEGATION.TX_HASH))
                .where(DELEGATION.ADDRESS.eq(stakeAddress))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(rec -> {
                    BigDecimal attributedAmount = rec.get("attributed_amount", BigDecimal.class);
                    BigDecimal fallbackAmount = rec.get("fallback_amount", BigDecimal.class);
                    BigDecimal amt = attributedAmount != null ? attributedAmount : fallbackAmount;
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

    private Condition isAtOrAfterCurrentCycle(Field<Long> slotField, Field<Integer> txIndexField, Field<Integer> certIndexField,
                                              Long cycleSlot, Integer cycleTxIndex, Integer cycleCertIndex) {
        if (cycleSlot == null) {
            return DSL.trueCondition();
        }

        int txIndex = cycleTxIndex != null ? cycleTxIndex : 0;
        int certIndex = cycleCertIndex != null ? cycleCertIndex : 0;
        Field<Integer> txIndexOrZero = DSL.coalesce(txIndexField, 0);
        Field<Integer> certIndexOrZero = DSL.coalesce(certIndexField, 0);

        return slotField.gt(cycleSlot)
                .or(slotField.eq(cycleSlot).and(txIndexOrZero.gt(txIndex)))
                .or(slotField.eq(cycleSlot).and(txIndexOrZero.eq(txIndex)).and(certIndexOrZero.ge(certIndex)));
    }

    private String normalizeDrepId(String drepId, String drepType) {
        if (drepId != null && !drepId.isBlank()) {
            return drepId;
        }

        if ("ABSTAIN".equalsIgnoreCase(drepType)) {
            return "drep_always_abstain";
        }

        if ("NO_CONFIDENCE".equalsIgnoreCase(drepType)) {
            return "drep_always_no_confidence";
        }

        return null;
    }

    @Override
    public List<AccountRegistration> findRegistrations(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        List<SortField<?>> orderBy = order == Order.desc
                ? List.of(STAKE_REGISTRATION.SLOT.desc(), STAKE_REGISTRATION.TX_INDEX.desc(), STAKE_REGISTRATION.CERT_INDEX.desc())
                : List.of(STAKE_REGISTRATION.SLOT.asc(), STAKE_REGISTRATION.TX_INDEX.asc(), STAKE_REGISTRATION.CERT_INDEX.asc());
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
        return dsl.select(WITHDRAWAL.TX_HASH, WITHDRAWAL.AMOUNT, WITHDRAWAL.SLOT, WITHDRAWAL.BLOCK_TIME, WITHDRAWAL.BLOCK)
                .from(WITHDRAWAL)
                .where(WITHDRAWAL.ADDRESS.eq(stakeAddress))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(rec -> new AccountWithdrawal(
                        rec.get(WITHDRAWAL.TX_HASH),
                        rec.get(WITHDRAWAL.AMOUNT),
                        rec.get(WITHDRAWAL.SLOT),
                        rec.get(WITHDRAWAL.BLOCK_TIME),
                        rec.get(WITHDRAWAL.BLOCK)));
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
            // Received: all outputs (UTXOs) belonging to this stake address
            // Each ADDRESS_UTXO row represents an output; lovelace + multi-asset amounts are received
            var outputRows = dsl.select(
                            ADDRESS_UTXO.TX_HASH,
                            ADDRESS_UTXO.LOVELACE_AMOUNT,
                            ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts"))
                    .from(ADDRESS_UTXO)
                    .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                    .fetch();

            // Sent: all inputs (spent UTXOs) belonging to this stake address
            // TX_INPUT.TX_HASH = the utxo's creating tx hash (matches ADDRESS_UTXO.TX_HASH)
            // TX_INPUT.SPENT_TX_HASH = the spending tx hash
            var inputRows = dsl.select(
                            TX_INPUT.SPENT_TX_HASH.as("spending_tx_hash"),
                            ADDRESS_UTXO.LOVELACE_AMOUNT,
                            ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts"))
                    .from(ADDRESS_UTXO)
                    .join(TX_INPUT)
                    .on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH)
                            .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)))
                    .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                    .fetch();

            Map<String, BigInteger> receivedMap = new LinkedHashMap<>();
            Map<String, BigInteger> sentMap = new LinkedHashMap<>();
            Set<String> txHashes = new HashSet<>();

            for (Record rec : outputRows) {
                String txHash = rec.get(ADDRESS_UTXO.TX_HASH);
                if (txHash != null) txHashes.add(txHash);

                Long lovelace = rec.get(ADDRESS_UTXO.LOVELACE_AMOUNT);
                if (lovelace != null && lovelace > 0) {
                    receivedMap.merge("lovelace", BigInteger.valueOf(lovelace), BigInteger::add);
                }
                Map<String, BigInteger> assets = AmountsJsonUtil.toQuantityByUnit(rec.get("amounts", String.class));
                for (Map.Entry<String, BigInteger> e : assets.entrySet()) {
                    if (!"lovelace".equals(e.getKey()) && e.getValue() != null && e.getValue().compareTo(BigInteger.ZERO) > 0) {
                        receivedMap.merge(e.getKey(), e.getValue(), BigInteger::add);
                    }
                }
            }

            for (Record rec : inputRows) {
                String spendingTxHash = rec.get("spending_tx_hash", String.class);
                if (spendingTxHash != null) txHashes.add(spendingTxHash);

                Long lovelace = rec.get(ADDRESS_UTXO.LOVELACE_AMOUNT);
                if (lovelace != null && lovelace > 0) {
                    sentMap.merge("lovelace", BigInteger.valueOf(lovelace), BigInteger::add);
                }
                Map<String, BigInteger> assets = AmountsJsonUtil.toQuantityByUnit(rec.get("amounts", String.class));
                for (Map.Entry<String, BigInteger> e : assets.entrySet()) {
                    if (!"lovelace".equals(e.getKey()) && e.getValue() != null && e.getValue().compareTo(BigInteger.ZERO) > 0) {
                        sentMap.merge(e.getKey(), e.getValue(), BigInteger::add);
                    }
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

        // Parse block range filters
        Long fromBlock = null;
        Long toBlock = null;
        if (from != null && !from.isBlank()) {
            try { fromBlock = Long.parseLong(from.split(":")[0]); }
            catch (NumberFormatException e) { log.warn("Invalid 'from' block reference: {}", from); }
        }
        if (to != null && !to.isBlank()) {
            try { toBlock = Long.parseLong(to.split(":")[0]); }
            catch (NumberFormatException e) { log.warn("Invalid 'to' block reference: {}", to); }
        }
        final Long fromBlockFinal = fromBlock;
        final Long toBlockFinal = toBlock;

        Condition receiveCondition = ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress);
        if (fromBlockFinal != null) receiveCondition = receiveCondition.and(ADDRESS_UTXO.BLOCK.ge(fromBlockFinal));
        if (toBlockFinal != null) receiveCondition = receiveCondition.and(ADDRESS_UTXO.BLOCK.le(toBlockFinal));

        List<AccountTransactionCandidate> candidates = new ArrayList<>();

        var receivingTxs = dsl.selectDistinct(
                        ADDRESS_UTXO.OWNER_ADDR,
                        ADDRESS_UTXO.TX_HASH,
                        ADDRESS_UTXO.SLOT,
                        ADDRESS_UTXO.BLOCK,
                        ADDRESS_UTXO.BLOCK_TIME,
                        TRANSACTION.TX_INDEX)
                .from(ADDRESS_UTXO)
                .join(TRANSACTION).on(TRANSACTION.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                .where(receiveCondition);

        Condition spendCondition = ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress);
        if (fromBlockFinal != null) spendCondition = spendCondition.and(TRANSACTION.BLOCK.ge(fromBlockFinal));
        if (toBlockFinal != null) spendCondition = spendCondition.and(TRANSACTION.BLOCK.le(toBlockFinal));

        Field<String> spentTxHashField = TX_INPUT.SPENT_TX_HASH.as(ADDRESS_UTXO.TX_HASH.getName());

        var spendingTxs = dsl.selectDistinct(
                        ADDRESS_UTXO.OWNER_ADDR,
                        spentTxHashField,
                        TRANSACTION.SLOT,
                        TRANSACTION.BLOCK,
                        TRANSACTION.BLOCK_TIME,
                        TRANSACTION.TX_INDEX)
                .from(ADDRESS_UTXO)
                .join(TX_INPUT).on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH)
                        .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)))
                .join(TRANSACTION).on(TRANSACTION.TX_HASH.eq(TX_INPUT.SPENT_TX_HASH))
                .where(spendCondition);

        candidates.addAll(receivingTxs.fetch(rec -> new AccountTransactionCandidate(
                rec.get(ADDRESS_UTXO.OWNER_ADDR),
                rec.get(ADDRESS_UTXO.TX_HASH),
                rec.get(ADDRESS_UTXO.SLOT) != null ? rec.get(ADDRESS_UTXO.SLOT) : 0L,
                rec.get(ADDRESS_UTXO.BLOCK) != null ? rec.get(ADDRESS_UTXO.BLOCK) : 0L,
                rec.get(ADDRESS_UTXO.BLOCK_TIME) != null ? rec.get(ADDRESS_UTXO.BLOCK_TIME) : 0L,
                rec.get(TRANSACTION.TX_INDEX) != null ? rec.get(TRANSACTION.TX_INDEX).longValue() : 0L,
                TX_SIDE_OUT
        )));

        candidates.addAll(spendingTxs.fetch(rec -> new AccountTransactionCandidate(
                rec.get(ADDRESS_UTXO.OWNER_ADDR),
                rec.get(spentTxHashField),
                rec.get(TRANSACTION.SLOT) != null ? rec.get(TRANSACTION.SLOT) : 0L,
                rec.get(TRANSACTION.BLOCK) != null ? rec.get(TRANSACTION.BLOCK) : 0L,
                rec.get(TRANSACTION.BLOCK_TIME) != null ? rec.get(TRANSACTION.BLOCK_TIME) : 0L,
                rec.get(TRANSACTION.TX_INDEX) != null ? rec.get(TRANSACTION.TX_INDEX).longValue() : 0L,
                TX_SIDE_IN
        )));

        Comparator<AccountTransactionCandidate> comparator = Comparator
                .comparingLong(AccountTransactionCandidate::slot);
        if (order == Order.desc) {
            comparator = comparator.reversed();
        }
        comparator = comparator
                .thenComparing(AccountTransactionCandidate::txHash)
                .thenComparing(AccountTransactionCandidate::address);

        candidates.sort(comparator);

        Map<String, List<AccountTransactionCandidate>> groupedByTx = new LinkedHashMap<>();
        for (AccountTransactionCandidate candidate : candidates) {
            groupedByTx.computeIfAbsent(candidate.txHash(), ignored -> new ArrayList<>()).add(candidate);
        }

        Comparator<AccountTransactionCandidate> txPositionComparator = Comparator
                .comparingLong(AccountTransactionCandidate::slot)
                .thenComparingLong(AccountTransactionCandidate::txIndex)
                .thenComparing(AccountTransactionCandidate::txHash);

        Map<String, AccountTransactionCandidate> lastCandidateByAddress = candidates.stream()
                .collect(Collectors.toMap(
                        AccountTransactionCandidate::address,
                        candidate -> candidate,
                        (left, right) -> txPositionComparator.compare(left, right) >= 0 ? left : right
                ));

        Map<String, Long> addressTxCounts = candidates.stream()
                .collect(Collectors.groupingBy(
                        AccountTransactionCandidate::address,
                        Collectors.mapping(AccountTransactionCandidate::txHash,
                                Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))
                ));

        List<AccountTransactionCandidate> normalized = new ArrayList<>();
        for (List<AccountTransactionCandidate> txCandidates : groupedByTx.values()) {
            normalized.addAll(normalizeTransactionCandidates(txCandidates, lastCandidateByAddress, addressTxCounts));
        }

        int fromIndex = Math.min(offset, normalized.size());
        int toIndex = Math.min(fromIndex + count, normalized.size());

        return normalized.subList(fromIndex, toIndex).stream()
                .map(candidate -> new AccountTransaction(
                        candidate.address(),
                        candidate.txHash(),
                        candidate.txIndex(),
                        candidate.blockHeight(),
                        candidate.blockTime()))
                .toList();
    }

    private List<AccountTransactionCandidate> normalizeTransactionCandidates(List<AccountTransactionCandidate> txCandidates,
                                                                             Map<String, AccountTransactionCandidate> lastCandidateByAddress,
                                                                             Map<String, Long> addressTxCounts) {
        Map<String, AccountTransactionCandidate> displayRows = new LinkedHashMap<>();
        for (AccountTransactionCandidate candidate : txCandidates) {
            displayRows.putIfAbsent(candidate.address(), candidate);
        }

        long outputCount = txCandidates.stream()
                .filter(candidate -> TX_SIDE_OUT.equals(candidate.side()))
                .map(AccountTransactionCandidate::address)
                .distinct()
                .count();
        long inputCount = txCandidates.stream()
                .filter(candidate -> TX_SIDE_IN.equals(candidate.side()))
                .map(AccountTransactionCandidate::address)
                .distinct()
                .count();

        // Blockfrost collapses a narrow historical-address pattern where a terminal receiving
        // address appears alongside a single spending address in the same transaction.
        if (outputCount == 1 && inputCount == 1) {
            String inputAddress = txCandidates.stream()
                    .filter(candidate -> TX_SIDE_IN.equals(candidate.side()))
                    .map(AccountTransactionCandidate::address)
                    .findFirst()
                    .orElse(null);
            String outputAddress = txCandidates.stream()
                    .filter(candidate -> TX_SIDE_OUT.equals(candidate.side()))
                    .map(AccountTransactionCandidate::address)
                    .findFirst()
                    .orElse(null);

            if (inputAddress != null && outputAddress != null && !Objects.equals(inputAddress, outputAddress)) {
                AccountTransactionCandidate outputRow = displayRows.get(outputAddress);
                AccountTransactionCandidate lastOutputRow = lastCandidateByAddress.get(outputAddress);

                if (outputRow != null && lastOutputRow != null && Objects.equals(lastOutputRow.txHash(), outputRow.txHash())) {
                    AccountTransactionCandidate inputRow = displayRows.get(inputAddress);
                    if (inputRow != null) {
                        return List.of(inputRow);
                    }
                }
            }
        }

        // Blockfrost also collapses a rare split-input pattern where two stake-owned inputs fund a
        // one-off receiving address that never appears in any other transaction.
        if (outputCount == 1 && inputCount == 2) {
            String outputAddress = txCandidates.stream()
                    .filter(candidate -> TX_SIDE_OUT.equals(candidate.side()))
                    .map(AccountTransactionCandidate::address)
                    .findFirst()
                    .orElse(null);

            if (outputAddress != null && addressTxCounts.getOrDefault(outputAddress, 0L) == 1L) {
                return txCandidates.stream()
                        .filter(candidate -> TX_SIDE_IN.equals(candidate.side()))
                        .map(candidate -> displayRows.get(candidate.address()))
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(AccountTransactionCandidate::address))
                        .limit(1)
                        .toList();
            }
        }

        return new ArrayList<>(displayRows.values());
    }

    private record AccountTransactionCandidate(
            String address,
            String txHash,
            long slot,
            long blockHeight,
            long blockTime,
            long txIndex,
            String side
    ) {
    }
}
