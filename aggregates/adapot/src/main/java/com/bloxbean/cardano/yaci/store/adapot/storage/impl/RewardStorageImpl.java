package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.UnclaimedRewardRestRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;

@RequiredArgsConstructor
public class RewardStorageImpl implements RewardStorage {
    private final InstantRewardRepository instantRewardRepository;
    private final RewardRestRepository rewardRestRepository;
    private final RewardRepository rewardRepository;
    private final UnclaimedRewardRestRepository unclaimedRewardRestRepository;
    private final Mapper mapper;
    private final DSLContext dsl;

    @Override
    public void saveInstantRewards(List<InstantReward> rewards) {
        instantRewardRepository.saveAll(rewards.stream().map(mapper::toInstantRewardEntity).toList());
    }

    @Override
    public void saveRewardRest(List<RewardRest> rewards) {
        rewardRestRepository.saveAll(rewards.stream().map(mapper::toRewardRestEntity).toList());
    }

    @Override
    public void saveRewards(List<Reward> rewards) {
        var inserts = rewards.stream()
                .map(reward -> dsl.insertInto(REWARD)
                        .set(REWARD.ADDRESS, reward.getAddress())
                        .set(REWARD.EARNED_EPOCH, reward.getEarnedEpoch())
                        .set(REWARD.TYPE, reward.getType().toString())
                        .set(REWARD.POOL_ID, reward.getPoolId())
                        .set(REWARD.AMOUNT, reward.getAmount())
                        .set(REWARD.SPENDABLE_EPOCH, reward.getSpendableEpoch())
                        .set(REWARD.SLOT, reward.getSlot())
                        .onDuplicateKeyUpdate()
                        .set(REWARD.ADDRESS, reward.getAddress())
                        .set(REWARD.EARNED_EPOCH, reward.getEarnedEpoch())
                        .set(REWARD.TYPE, reward.getType().toString())
                        .set(REWARD.POOL_ID, reward.getPoolId())
                        .set(REWARD.AMOUNT, reward.getAmount())
                        .set(REWARD.SPENDABLE_EPOCH, reward.getSpendableEpoch())
                        .set(REWARD.SLOT, reward.getSlot())).toList();

        dsl.batch(inserts).execute();
    }

    @Override
    public void saveUnclaimedRewardRest(List<UnclaimedRewardRest> unclaimedRewards) {
        unclaimedRewardRestRepository.saveAll(unclaimedRewards.stream().map(mapper::toUnclaimedRewardRestEntity).toList());
    }

    @Override
    public List<UnclaimedRewardRest> findUnclaimedRewardRest(int spendableEpoch) {
        return unclaimedRewardRestRepository.findBySpendableEpoch(spendableEpoch)
                .stream().map(mapper::toUnclaimedRewardRest)
                .toList();
    }

    @Override
    public void deleteLeaderMemberRewards(int epoch) {
        rewardRepository.deleteLeaderMemberRewards(epoch);
    }

    @Override
    public int deleteInstantRewardsBySlotGreaterThan(long slot) {
        return instantRewardRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteRewardsBySlotGreaterThan(long slot) {
        return rewardRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteRewardRestsBySlotGreaterThan(long slot) {
        return rewardRestRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteUnclaimedRewardsBySlotGreaterThan(long slot) {
        return unclaimedRewardRestRepository.deleteBySlotGreaterThan(slot);
    }

}
