package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl;

import com.bloxbean.cardano.yaci.core.util.Constants;
import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.BFAccountStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.AmountsJsonUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.BlockfrostDialectUtil;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.Cursor;
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
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;

@Component
@RequiredArgsConstructor
@Slf4j
public class BFAccountStorageReaderImpl implements BFAccountStorageReader {

    private final DSLContext dsl;
    private final UtxoStoreProperties utxoStoreProperties;

    @Override
    public Optional<AccountInfo> getAccountInfo(String stakeAddress) {
        var registrations = dsl.select(STAKE_REGISTRATION.TYPE, STAKE_REGISTRATION.EPOCH, STAKE_REGISTRATION.SLOT,
                        STAKE_REGISTRATION.TX_INDEX, STAKE_REGISTRATION.CERT_INDEX)
                .from(STAKE_REGISTRATION)
                .where(STAKE_REGISTRATION.ADDRESS.eq(stakeAddress))
                .orderBy(STAKE_REGISTRATION.SLOT.asc(), STAKE_REGISTRATION.TX_INDEX.asc(), STAKE_REGISTRATION.CERT_INDEX.asc())
                .fetch();

        if (registrations.isEmpty()) {
            return Optional.empty();
        }

        var firstReg = registrations.get(0);
        var lastReg = registrations.get(registrations.size() - 1);

        String lastType = lastReg != null ? lastReg.get(STAKE_REGISTRATION.TYPE) : null;
        boolean registered = lastType != null
                && lastType.toUpperCase().contains("REGISTRATION")
                && !lastType.toUpperCase().contains("DEREGISTRATION");

        // active_epoch = epoch of the most recent effective registration (not deregistration)
        Integer activeEpoch = null;
        for (int i = registrations.size() - 1; i >= 0; i--) {
            var registration = registrations.get(i);
            String type = registration.get(STAKE_REGISTRATION.TYPE);
            if (type != null
                    && type.toUpperCase().contains("REGISTRATION")
                    && !type.toUpperCase().contains("DEREGISTRATION")) {
                activeEpoch = registration.get(STAKE_REGISTRATION.EPOCH);
                break;
            }
        }
        if (activeEpoch == null) {
            activeEpoch = firstReg.get(STAKE_REGISTRATION.EPOCH);
        }

        Long currentCycleSlot = registered && lastReg != null ? lastReg.get(STAKE_REGISTRATION.SLOT) : null;
        Integer currentCycleTxIndex = registered && lastReg != null ? lastReg.get(STAKE_REGISTRATION.TX_INDEX) : null;
        Integer currentCycleCertIndex = registered && lastReg != null ? lastReg.get(STAKE_REGISTRATION.CERT_INDEX) : null;

        BigInteger controlledAmount = BigInteger.ZERO;
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

        String poolId = null;
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

        BigInteger rewardsSum = BigInteger.ZERO;
        // Sum all three reward sources: staking rewards + instant rewards + leftover pool rewards
        var rewardSum = dsl.select(DSL.sum(REWARD.AMOUNT).cast(BigDecimal.class))
                .from(REWARD)
                .where(REWARD.ADDRESS.eq(stakeAddress))
                .fetchOne(0, BigDecimal.class);
        if (rewardSum != null) {
            rewardsSum = rewardSum.toBigInteger();
        }
        var rewardRestSum = dsl.select(DSL.sum(REWARD_REST.AMOUNT).cast(BigDecimal.class))
                .from(REWARD_REST)
                .where(REWARD_REST.ADDRESS.eq(stakeAddress))
                .fetchOne(0, BigDecimal.class);
        if (rewardRestSum != null) {
            rewardsSum = rewardsSum.add(rewardRestSum.toBigInteger());
        }
        var instantRewardSum = dsl.select(DSL.sum(INSTANT_REWARD.AMOUNT).cast(BigDecimal.class))
                .from(INSTANT_REWARD)
                .where(INSTANT_REWARD.ADDRESS.eq(stakeAddress))
                .fetchOne(0, BigDecimal.class);
        if (instantRewardSum != null) {
            rewardsSum = rewardsSum.add(instantRewardSum.toBigInteger());
        }

        BigInteger withdrawalsSum = BigInteger.ZERO;
        var wdSum = dsl.select(DSL.sum(WITHDRAWAL.AMOUNT).cast(BigDecimal.class))
                .from(WITHDRAWAL)
                .where(WITHDRAWAL.ADDRESS.eq(stakeAddress))
                .fetchOne(0, BigDecimal.class);
        if (wdSum != null) {
            withdrawalsSum = wdSum.toBigInteger();
        }

        BigInteger reservesSum = BigInteger.ZERO;
        BigInteger treasurySum = BigInteger.ZERO;
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

        // controlled_amount = spendable UTXOs + unclaimed rewards (rewards - withdrawals)
        BigInteger withdrawable = rewardsSum.subtract(withdrawalsSum);
        if (withdrawable.compareTo(BigInteger.ZERO) < 0) withdrawable = BigInteger.ZERO;
        BigInteger totalControlled = controlledAmount.add(withdrawable);

        String drepId = null;
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

        boolean active = registered
                && activeEpoch != null
                && dsl.fetchExists(
                dsl.selectOne()
                        .from(EPOCH_STAKE)
                        .where(activeEpochStakeCondition(stakeAddress, activeEpoch))
        );

        return Optional.of(new AccountInfo(stakeAddress, active, registered, activeEpoch,
                totalControlled, rewardsSum, withdrawalsSum, reservesSum, treasurySum, poolId, drepId));
    }

    /**
     * Rows of {@code epoch_stake} that make {@code stakeAddress} active from {@code activeEpoch} on.
     *
     * <p>{@code epoch_stake} is range-partitioned on {@code epoch}, so a filter on
     * {@code address} and {@code active_epoch} alone prunes nothing: Postgres plans and probes
     * every epoch partition (about 1,445 on preview), which is seconds on a fresh connection
     * and holds thousands of locks (#1200). Both writers store {@code active_epoch = epoch + 2}:
     * {@code StakeSnapshotService} binds {@code activeEpoch = epoch + 2}, and
     * {@code GenesisPoolProcessor} writes {@code epoch = E - 1, activeEpoch = E + 1}. So
     * {@code active_epoch >= N} is the same filter as {@code epoch >= N - 2}, and stating it on
     * the partition key lets the planner keep only the newest partitions.
     *
     * <p>The bound is not clamped at zero: on a devnet that starts at epoch 0, the genesis
     * delegators' rows carry {@code epoch = -1}, and {@code epoch >= 0} would skip them.
     */
    static Condition activeEpochStakeCondition(String stakeAddress, int activeEpoch) {
        return EPOCH_STAKE.ADDRESS.eq(stakeAddress)
                .and(EPOCH_STAKE.EPOCH.ge(activeEpoch - 2));
    }

    @Override
    public List<AccountReward> findRewards(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
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
    }

    @Override
    public List<AccountHistory> findHistory(String stakeAddress, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
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
                        STAKE_REGISTRATION.DEPOSIT, TRANSACTION.BLOCK_TIME, TRANSACTION.BLOCK)
                .from(STAKE_REGISTRATION)
                .leftJoin(TRANSACTION).on(TRANSACTION.TX_HASH.eq(STAKE_REGISTRATION.TX_HASH))
                .where(STAKE_REGISTRATION.ADDRESS.eq(stakeAddress))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(rec -> {
                    Long deposit = rec.get(STAKE_REGISTRATION.DEPOSIT);
                    return new AccountRegistration(
                            rec.get(STAKE_REGISTRATION.TX_HASH),
                            rec.get(STAKE_REGISTRATION.TYPE),
                            rec.get(STAKE_REGISTRATION.SLOT),
                            rec.get(TRANSACTION.BLOCK_TIME),
                            rec.get(TRANSACTION.BLOCK),
                            deposit == null ? null : BigInteger.valueOf(deposit));
                });
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
        if (utxoStoreProperties.isSaveAddress()) {
            var addressRecords = dsl.select(ADDRESS.ADDR_FULL)
                    .from(ADDRESS)
                    .where(ADDRESS.STAKE_ADDRESS.eq(stakeAddress))
                    .orderBy(order == Order.desc ? ADDRESS.ADDR_FULL.desc() : ADDRESS.ADDR_FULL.asc())
                    .limit(count)
                    .offset(offset)
                    .fetch(ADDRESS.ADDR_FULL);

            if (!addressRecords.isEmpty()) {
                return addressRecords.stream()
                        .map(AccountAddress::new)
                        .toList();
            }
        }

        return dsl.selectDistinct(ADDRESS_UTXO.OWNER_ADDR)
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                .orderBy(order == Order.desc ? ADDRESS_UTXO.OWNER_ADDR.desc() : ADDRESS_UTXO.OWNER_ADDR.asc())
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
                    .and(unitField.ne(Constants.LOVELACE))
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
                    if (!Constants.LOVELACE.equals(e.getKey())) {
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
            var receivedHashesQuery = dsl.select(ADDRESS_UTXO.TX_HASH.as("tx_hash"))
                    .from(ADDRESS_UTXO)
                    .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress));

            var spentHashesQuery = dsl.select(TX_INPUT.SPENT_TX_HASH.as("tx_hash"))
                    .from(ADDRESS_UTXO)
                    .join(TX_INPUT)
                    .on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH)
                            .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)))
                    .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress));

            long txCount = dsl.fetchCount(receivedHashesQuery.union(spentHashesQuery));

            Map<String, BigInteger> receivedMap = new LinkedHashMap<>();
            Map<String, BigInteger> sentMap = new LinkedHashMap<>();

            try (Cursor<?> outputCursor = dsl.select(
                            ADDRESS_UTXO.LOVELACE_AMOUNT,
                            ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts"))
                    .from(ADDRESS_UTXO)
                    .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                    .fetchLazy()) {
                for (Record rec : outputCursor) {
                    Long lovelace = rec.get(ADDRESS_UTXO.LOVELACE_AMOUNT);
                    if (lovelace != null && lovelace > 0) {
                        receivedMap.merge(Constants.LOVELACE, BigInteger.valueOf(lovelace), BigInteger::add);
                    }
                    Map<String, BigInteger> assets = AmountsJsonUtil.toQuantityByUnit(rec.get("amounts", String.class));
                    for (Map.Entry<String, BigInteger> e : assets.entrySet()) {
                        if (!Constants.LOVELACE.equals(e.getKey()) && e.getValue() != null && e.getValue().compareTo(BigInteger.ZERO) > 0) {
                            receivedMap.merge(e.getKey(), e.getValue(), BigInteger::add);
                        }
                    }
                }
            }

            try (Cursor<?> inputCursor = dsl.select(
                            ADDRESS_UTXO.LOVELACE_AMOUNT,
                            ADDRESS_UTXO.AMOUNTS.cast(String.class).as("amounts"))
                    .from(ADDRESS_UTXO)
                    .join(TX_INPUT)
                    .on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH)
                            .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)))
                    .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                    .fetchLazy()) {
                for (Record rec : inputCursor) {
                    Long lovelace = rec.get(ADDRESS_UTXO.LOVELACE_AMOUNT);
                    if (lovelace != null && lovelace > 0) {
                        sentMap.merge(Constants.LOVELACE, BigInteger.valueOf(lovelace), BigInteger::add);
                    }
                    Map<String, BigInteger> assets = AmountsJsonUtil.toQuantityByUnit(rec.get("amounts", String.class));
                    for (Map.Entry<String, BigInteger> e : assets.entrySet()) {
                        if (!Constants.LOVELACE.equals(e.getKey()) && e.getValue() != null && e.getValue().compareTo(BigInteger.ZERO) > 0) {
                            sentMap.merge(e.getKey(), e.getValue(), BigInteger::add);
                        }
                    }
                }
            }

            return Optional.of(new AccountAddressesTotal(stakeAddress, receivedMap, sentMap, txCount));
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
                        amounts.put(Constants.LOVELACE, BigInteger.valueOf(lovelace));
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

    /**
     * Returns the transactions of a stake address the way Blockfrost's
     * {@code /accounts/{stake_address}/transactions} does: one row per (transaction, address of the
     * account), built from the outputs the transaction created (received side) and the outputs it
     * spent (sent side). Rows are ordered by the position of the transaction, then by the position of
     * the earliest matching output, and paged directly.
     * <p>
     * The account's outputs are read once and shared by both sides. The sent side takes its block
     * from {@code tx_input}, so the transaction's index in its block is only looked up for the rows
     * that need it: those in the {@code from}/{@code to} boundary blocks, and those in the blocks
     * of the requested page.
     */
    @Override
    public List<AccountTransaction> findTransactions(String stakeAddress, int page, int count, Order order, String from, String to) {
        BlockRef fromRef = parseBlockRef(from);
        BlockRef toRef = parseBlockRef(to);

        var utxos = DSL.name("account_utxo").as(dsl.select(
                        ADDRESS_UTXO.OWNER_ADDR.as("address"),
                        ADDRESS_UTXO.TX_HASH.as("tx_hash"),
                        ADDRESS_UTXO.OUTPUT_INDEX.as("output_index"),
                        ADDRESS_UTXO.BLOCK.as("block"),
                        ADDRESS_UTXO.TX_INDEX.as("tx_index"),
                        ADDRESS_UTXO.BLOCK_TIME.as("block_time"),
                        outputPosition().as("output_position"))
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress)));
        Field<String> utxoAddress = utxos.field("address", String.class);
        Field<String> utxoTxHash = utxos.field("tx_hash", String.class);
        Field<Long> utxoBlock = utxos.field("block", Long.class);
        Field<Long> utxoPosition = utxos.field("output_position", Long.class);

        var received = dsl.select(utxoAddress, utxoTxHash, utxoBlock,
                        utxos.field("tx_index", Integer.class),
                        utxos.field("block_time", Long.class),
                        utxoPosition)
                .from(utxos)
                .where(blockRangeCondition(utxoBlock, fromRef, toRef));

        // The sent side's tx index is unknown here (null); it is resolved below where it matters.
        var spent = dsl.select(utxoAddress,
                        TX_INPUT.SPENT_TX_HASH,
                        TX_INPUT.SPENT_AT_BLOCK,
                        DSL.castNull(Integer.class),
                        TX_INPUT.SPENT_BLOCK_TIME,
                        utxoPosition)
                .from(utxos)
                .join(TX_INPUT).on(TX_INPUT.TX_HASH.eq(utxoTxHash)
                        .and(TX_INPUT.OUTPUT_INDEX.eq(utxos.field("output_index", Integer.class))))
                .where(blockRangeCondition(TX_INPUT.SPENT_AT_BLOCK, fromRef, toRef));

        // Group by (tx, address) only: a group can hold rows from both sides, so block/tx index/time
        // are aggregated rather than grouped on.
        Table<?> rows = received.unionAll(spent).asTable("account_tx_rows");
        Field<Long> rowsBlock = DSL.max(rows.field("block", Long.class));
        Field<Integer> rowsTxIndex = DSL.max(rows.field("tx_index", Integer.class));
        var grouped = dsl.select(
                        rows.field("address", String.class).as("address"),
                        rows.field("tx_hash", String.class).as("tx_hash"),
                        rowsBlock.as("block"),
                        boundaryTxIndex(rows.field("tx_hash", String.class), rowsBlock, rowsTxIndex, fromRef, toRef)
                                .as("tx_index"),
                        DSL.max(rows.field("block_time", Long.class)).as("block_time"),
                        DSL.min(rows.field("output_position", Long.class)).as("output_position"))
                .from(rows)
                .groupBy(rows.field("address"), rows.field("tx_hash"))
                .asTable("account_tx_grouped");
        Field<Long> groupedBlock = grouped.field("block", Long.class);
        var accountTxs = DSL.name("account_tx").as(dsl.select(grouped.fields())
                .from(grouped)
                .where(txIndexRangeCondition(groupedBlock, grouped.field("tx_index", Integer.class), fromRef, toRef)));

        Field<String> address = accountTxs.field("address", String.class);
        Field<String> txHash = accountTxs.field("tx_hash", String.class);
        Field<Long> block = accountTxs.field("block", Long.class);
        Field<Long> blockTime = accountTxs.field("block_time", Long.class);
        Field<Long> firstOutputPosition = accountTxs.field("output_position", Long.class);
        Field<Integer> txIndex = DSL.coalesce(resolveTxIndex(txHash, accountTxs.field("tx_index", Integer.class)), 0);

        // Order by block alone to find the last block of the page; only rows up to that block need
        // their tx index to be sorted exactly.
        boolean desc = order == Order.desc;
        int offset = Math.max(page, 0) * count;
        Field<Long> lastPageBlock = DSL.field(dsl.select(block)
                .from(accountTxs)
                .orderBy(desc ? block.desc() : block.asc())
                .limit(1)
                .offset(offset + count - 1));
        Condition pageBlocks = desc
                ? block.ge(DSL.coalesce(lastPageBlock, -1L))
                : block.le(DSL.coalesce(lastPageBlock, Long.MAX_VALUE));

        return dsl.with(utxos)
                .with(accountTxs)
                .select(address, txHash, block, txIndex, blockTime)
                .from(accountTxs)
                .where(pageBlocks)
                .orderBy(desc ? block.desc() : block.asc(),
                        desc ? txIndex.desc() : txIndex.asc(),
                        desc ? firstOutputPosition.desc() : firstOutputPosition.asc())
                .limit(count)
                .offset(offset)
                .fetch(rec -> new AccountTransaction(
                        rec.get(address),
                        rec.get(txHash),
                        rec.get(txIndex) != null ? rec.get(txIndex) : 0L,
                        rec.get(block) != null ? rec.get(block) : 0L,
                        rec.get(blockTime) != null ? rec.get(blockTime) : 0L));
    }

    /**
     * Index of the transaction in its block, read from {@code transaction} only when it is not
     * already known. A CASE keeps the lookup lazy: a join would probe {@code transaction} for every row.
     */
    private Field<Integer> resolveTxIndex(Field<String> txHash, Field<Integer> txIndex) {
        return DSL.when(txIndex.isNull(), DSL.field(dsl.select(TRANSACTION.TX_INDEX)
                        .from(TRANSACTION)
                        .where(TRANSACTION.TX_HASH.eq(txHash))))
                .otherwise(txIndex);
    }

    /**
     * Resolves the tx index only for the rows in a {@code from}/{@code to} block that names a tx index;
     * the index range of the other rows is already decided by their block.
     */
    private Field<Integer> boundaryTxIndex(Field<String> txHash, Field<Long> block, Field<Integer> txIndex,
                                           BlockRef from, BlockRef to) {
        List<Long> boundaryBlocks = new ArrayList<>();
        if (from != null && from.txIndex() != null) {
            boundaryBlocks.add(from.block());
        }
        if (to != null && to.txIndex() != null) {
            boundaryBlocks.add(to.block());
        }
        if (boundaryBlocks.isEmpty()) {
            return txIndex;
        }
        return DSL.when(block.in(boundaryBlocks), resolveTxIndex(txHash, txIndex)).otherwise(txIndex);
    }

    /**
     * On-chain position of an address_utxo output (block, tx index, output index) packed into one
     * number. Plays the role of db-sync's {@code tx_out.id}, which Blockfrost uses to pick and order
     * the rows of a transaction.
     */
    private Field<Long> outputPosition() {
        return ADDRESS_UTXO.BLOCK.mul(1L << 32)
                .plus(DSL.coalesce(ADDRESS_UTXO.TX_INDEX, 0).cast(Long.class).mul(1L << 16))
                .plus(ADDRESS_UTXO.OUTPUT_INDEX.cast(Long.class));
    }

    /**
     * Inclusive block range of Blockfrost's from/to parameters, ignoring their ":txIndex" part.
     */
    private Condition blockRangeCondition(Field<Long> block, BlockRef from, BlockRef to) {
        Condition condition = DSL.trueCondition();
        if (from != null) {
            condition = condition.and(block.ge(from.block()));
        }
        if (to != null) {
            condition = condition.and(block.le(to.block()));
        }
        return condition;
    }

    /**
     * The ":txIndex" part of Blockfrost's inclusive "block[:txIndex]" from/to parameters, applied to
     * rows already inside the block range.
     */
    private Condition txIndexRangeCondition(Field<Long> block, Field<Integer> txIndex, BlockRef from, BlockRef to) {
        Field<Integer> index = DSL.coalesce(txIndex, 0);
        Condition condition = DSL.trueCondition();
        if (from != null && from.txIndex() != null) {
            condition = condition.and(block.ne(from.block()).or(index.ge(from.txIndex())));
        }
        if (to != null && to.txIndex() != null) {
            condition = condition.and(block.ne(to.block()).or(index.le(to.txIndex())));
        }
        return condition;
    }

    private BlockRef parseBlockRef(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split(":", -1);
        try {
            long block = Long.parseLong(parts[0]);
            Integer txIndex = parts.length > 1 && !parts[1].isBlank() ? Integer.parseInt(parts[1]) : null;
            if (parts.length <= 2 && block >= 0 && (txIndex == null || txIndex >= 0)) {
                return new BlockRef(block, txIndex);
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        log.warn("Invalid block reference: {}", value);
        return null;
    }

    private record BlockRef(long block, Integer txIndex) {
    }
}
