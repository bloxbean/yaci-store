package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.*;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.UnclaimedRewardRestRepository;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.RewardInfoType;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.WITHDRAWAL;

@RequiredArgsConstructor
public class RewardStorageReaderImpl implements RewardStorageReader {
    private final InstantRewardRepository instantRewardRepository;
    private final RewardRepository rewardRepository;
    private final RewardRestRepository rewardRestRepository;
    private final UnclaimedRewardRestRepository unclaimedRewardRestRepository;
    private final Mapper mapper;
    private final DSLContext dsl;

    @Override
    public List<InstantReward> findInstantRewardByEarnedEpoch(Integer epoch, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return instantRewardRepository.findByEarnedEpoch(epoch, sortedBySlot).stream().map(mapper::toInstantReward).toList();
    }

    @Override
    public List<InstantReward> findInstantRewardByEarnedEpochAndType(Integer epoch, InstantRewardType rewardType, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return instantRewardRepository.findByEarnedEpochAndType(epoch, rewardType, sortedBySlot).stream().map(mapper::toInstantReward).toList();
    }

    @Override
    public BigInteger findTotalInstantRewardByEarnedEpochAndType(Integer epoch, InstantRewardType rewardType) {
        return instantRewardRepository.findTotalAmountByEarnedEpoch(epoch, rewardType);
    }

    @Override
    public List<Reward> findRewardsByEarnedEpoch(Integer epoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRepository.findByEarnedEpoch(epoch, pagable)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsBySpendableEpoch(Integer epoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRepository.findBySpendableEpoch(epoch, pagable)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsByAddress(String address, int page, int count, Order order) {
        Pageable sortedBySlot =
                getPagableBySlotOrder(page, count, order);

        return rewardRepository.findByAddress(address, sortedBySlot)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsByAddressAndEarnedEpoch(String address, Integer epoch) {
        return rewardRepository.findByAddressAndEarnedEpoch(address, epoch)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsByAddressAndSpendableEpoch(String address, Integer epoch) {
        return rewardRepository.findByAddressAndSpendableEpoch(address, epoch)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    //-- Reward Rest
    @Override
    public List<RewardRest> findRewardRestByEarnedEpoch(Integer epoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRestRepository.findByEarnedEpoch(epoch, pagable)
                .stream()
                .map(rewardEntity -> mapper.toRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<RewardRest> findRewardRestBySpendableEpoch(Integer epoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRestRepository.findBySpendableEpoch(epoch, pagable)
                .stream()
                .map(rewardRestEntity -> mapper.toRewardRest(rewardRestEntity))
                .toList();
    }

    @Override
    public List<RewardRest> findRewardRestByAddress(String address, int page, int count, Order order) {
        Pageable sortedBySlot = getPagableBySlotOrder(page, count, order);

        return rewardRestRepository.findByAddress(address, sortedBySlot)
                .stream()
                .map(rewardEntity -> mapper.toRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<RewardRest> findRewardRestByAddressAndEarnedEpoch(String address, Integer earnedEpoch) {
        return rewardRestRepository.findByAddressAndEarnedEpoch(address, earnedEpoch)
                .stream()
                .map(rewardEntity -> mapper.toRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<RewardRest> findRewardRestByAddressAndSpendableEpoch(String address, Integer spendableEpoch) {
        return rewardRestRepository.findByAddressAndSpendableEpoch(address, spendableEpoch)
                .stream()
                .map(rewardEntity -> mapper.toRewardRest(rewardEntity))
                .toList();
    }

    //-- Unclaimed Reward Rest
    @Override
    public List<UnclaimedRewardRest> findUnclaimedRewardRestByEarnedEpoch(Integer epoch, int page, int count, Order order) {
        Pageable sortedBySlot = getPagableBySlotOrder(page, count, order);

        return unclaimedRewardRestRepository.findByEarnedEpoch(epoch, sortedBySlot)
                .stream()
                .map(rewardEntity -> mapper.toUnclaimedRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<UnclaimedRewardRest> findUnclaimedRewardRestBySpendableEpoch(Integer epoch, int page, int count, Order order) {
        Pageable sortedBySlot = getPagableBySlotOrder(page, count, order);

        return unclaimedRewardRestRepository.findBySpendableEpoch(epoch, sortedBySlot)
                .stream()
                .map(rewardEntity -> mapper.toUnclaimedRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsByPoolHashAndSpendableEpoch(String poolHash, Integer spendableEpoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRepository.findByPoolIdAndSpendableEpoch(poolHash, spendableEpoch, pagable)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    @Override
    public List<RewardInfo> findUnwithdrawnRewardsByAddresses(List<String> addresses, int page, int count) {
        int offset = page * count;

        var query = dsl.with("max_withdrawals").as(
                        dsl.select(
                                        WITHDRAWAL.ADDRESS,
                                        DSL.max(WITHDRAWAL.SLOT).as("max_withdrawal_slot")
                                )
                                .from(WITHDRAWAL)
                                .where(WITHDRAWAL.ADDRESS.in(addresses))
                                .groupBy(WITHDRAWAL.ADDRESS)
                )
                .with("pool_rewards").as(
                        dsl.select(
                                        REWARD.ADDRESS.as("address"),
                                        REWARD.EARNED_EPOCH.as("earned_epoch"),
                                        REWARD.SPENDABLE_EPOCH.as("spendable_epoch"),
                                        REWARD.AMOUNT.as("amount"),
                                        REWARD.POOL_ID.as("pool_id"),
                                        REWARD.SLOT.as("slot"),
                                        DSL.val("pool_reward").as("reward_category"),
                                        REWARD.TYPE.cast(String.class).as("reward_type")
                                )
                                .from(REWARD)
                                .leftJoin(DSL.table("max_withdrawals"))
                                .on(REWARD.ADDRESS.eq(DSL.field("max_withdrawals.address", String.class)))
                                .where(REWARD.ADDRESS.in(addresses)
                                        .and(REWARD.SLOT.gt(
                                                DSL.coalesce(
                                                        DSL.field("max_withdrawals.max_withdrawal_slot", Long.class),
                                                        DSL.val(0L)
                                                )
                                        ))
                                )
                )
                .with("reward_rests").as(
                        dsl.select(
                                        REWARD_REST.ADDRESS.as("address"),
                                        REWARD_REST.EARNED_EPOCH.as("earned_epoch"),
                                        REWARD_REST.SPENDABLE_EPOCH.as("spendable_epoch"),
                                        REWARD_REST.AMOUNT.as("amount"),
                                        DSL.val((String) null).as("pool_id"),
                                        REWARD_REST.SLOT.as("slot"),
                                        DSL.val("reward_rest").as("reward_category"),
                                        REWARD_REST.TYPE.cast(String.class).as("reward_type")
                                )
                                .from(REWARD_REST)
                                .leftJoin(DSL.table("max_withdrawals"))
                                .on(REWARD_REST.ADDRESS.eq(DSL.field("max_withdrawals.address", String.class)))
                                .where(REWARD_REST.ADDRESS.in(addresses)
                                        .and(REWARD_REST.SLOT.gt(
                                                DSL.coalesce(
                                                        DSL.field("max_withdrawals.max_withdrawal_slot", Long.class),
                                                        DSL.val(0L)
                                                )
                                        ))
                                )
                )
                .with("instant_rewards").as(
                        dsl.select(
                                        INSTANT_REWARD.ADDRESS.as("address"),
                                        INSTANT_REWARD.EARNED_EPOCH.as("earned_epoch"),
                                        INSTANT_REWARD.SPENDABLE_EPOCH.as("spendable_epoch"),
                                        INSTANT_REWARD.AMOUNT.as("amount"),
                                        DSL.val((String) null).as("pool_id"),
                                        INSTANT_REWARD.SLOT.as("slot"),
                                        DSL.val("instant_reward").as("reward_category"),
                                        INSTANT_REWARD.TYPE.cast(String.class).as("reward_type")
                                )
                                .from(INSTANT_REWARD)
                                .leftJoin(DSL.table("max_withdrawals"))
                                .on(INSTANT_REWARD.ADDRESS.eq(DSL.field("max_withdrawals.address", String.class)))
                                .where(INSTANT_REWARD.ADDRESS.in(addresses)
                                        .and(INSTANT_REWARD.SLOT.gt(
                                                DSL.coalesce(
                                                        DSL.field("max_withdrawals.max_withdrawal_slot", Long.class),
                                                        DSL.val(0L)
                                                )
                                        ))
                                )
                )
                .selectFrom(
                        dsl.selectFrom(DSL.table("pool_rewards"))
                                .unionAll(dsl.selectFrom(DSL.table("reward_rests")))
                                .unionAll(dsl.selectFrom(DSL.table("instant_rewards")))
                                .asTable("combined")
                )
                .orderBy(DSL.field("slot").desc())
                .limit(count)
                .offset(offset);

        return query.fetch()
                .stream()
                .map(record -> {
                    String address = record.get("address", String.class);
                    Integer earnedEpoch = record.get("earned_epoch", Integer.class);
                    Integer spendableEpoch = record.get("spendable_epoch", Integer.class);
                    java.math.BigInteger amount = record.get("amount", java.math.BigInteger.class);
                    String poolId = record.get("pool_id", String.class);

                    String rewardTypeStr = record.get("reward_type", String.class);
                    String rewardCategory = record.get("reward_category", String.class);

                    RewardInfoType rewardType = getRewardInfoType(rewardCategory, rewardTypeStr);
                    return new RewardInfo(address, earnedEpoch, spendableEpoch, amount, poolId, rewardType);
                })
                .toList();
    }

    private static RewardInfoType getRewardInfoType(String rewardCategory, String rewardTypeStr) {
        RewardInfoType rewardType = null;

        switch (rewardCategory) {
            case "pool_reward" -> {
                if (RewardType.member.name().equals(rewardTypeStr)) {
                    rewardType = RewardInfoType.pool_member;
                } else if (RewardType.leader.name().equals(rewardTypeStr)) {
                    rewardType = RewardInfoType.pool_leader;
                } else if (RewardType.refund.name().equals(rewardTypeStr)) {
                    rewardType = RewardInfoType.pool_deposit_refund;
                }
            }
            case "reward_rest" -> {
                if (RewardRestType.treasury.name().equals(rewardTypeStr)) {
                    rewardType = RewardInfoType.treasury_withdrawal;
                } else if (RewardRestType.proposal_refund.name().equals(rewardTypeStr)) {
                    rewardType = RewardInfoType.proposal_deposit_refund;
                }
            }
            case "instant_reward" -> {
                if (InstantRewardType.treasury.name().equals(rewardTypeStr)) {
                    rewardType = RewardInfoType.treasury;
                } else if (InstantRewardType.reserves.name().equals(rewardTypeStr)) {
                    rewardType = RewardInfoType.reserves;
                }
            }
            default -> throw new IllegalArgumentException("Unknown reward category: " + rewardCategory);
        }

        return rewardType;
    }

    private static Pageable getPagableBySlotOrder(int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count,  order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());
        return sortedBySlot;
    }

}
