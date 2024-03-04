package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardAccountStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardAccountEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardAccountRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RewardAccountStorageImpl implements RewardAccountStorage {
    private final RewardAccountRepository rewardAccountRepository;
    private final Mapper mapper;

    @Override
    public void addReward(List<Reward> rewards) {
        List<RewardAccountEntity> newRewardAccEntities = new ArrayList<>();
        for (Reward reward : rewards) { //For each reward update
            var recentRewardAcc = rewardAccountRepository.findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(reward.getAddress(), reward.getSlot()).orElse(null);

            var newRewardAccountEntity = new RewardAccountEntity();
            newRewardAccountEntity.setAddress(reward.getAddress());
            newRewardAccountEntity.setSlot(reward.getSlot());
            newRewardAccountEntity.setEpoch(reward.getEarnedEpoch());
            newRewardAccountEntity.setBlockNumber(reward.getBlockNumber());
            newRewardAccountEntity.setBlockTime(reward.getBlockTime());

            if (recentRewardAcc != null) {
                newRewardAccountEntity.setAmount(recentRewardAcc.getAmount().add(reward.getAmount()));
                newRewardAccountEntity.setWithdrawable(recentRewardAcc.getWithdrawable());
            } else {
                newRewardAccountEntity.setAmount(reward.getAmount());
                newRewardAccountEntity.setWithdrawable(BigInteger.ZERO);
            }

            newRewardAccEntities.add(newRewardAccountEntity);
        }

        if (!newRewardAccEntities.isEmpty()) {
            rewardAccountRepository.saveAll(newRewardAccEntities);
        }

    }

    @Override
    public void withdrawReward(List<Withdrawal> withdrawals) {
        List<RewardAccountEntity> updatedRewardAccountEntities = new ArrayList<>();

        for (Withdrawal withdrawal : withdrawals) { //For each reward update
            var recentRewardAcc = rewardAccountRepository
                    .findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(withdrawal.getAddress(), withdrawal.getSlot())
                    .orElse(null);

            RewardAccountEntity updatedRewardAcc;
            if (recentRewardAcc != null) {
                var amount = recentRewardAcc.getAmount().subtract(withdrawal.getAmount());
                updatedRewardAcc = new RewardAccountEntity();
                updatedRewardAcc.setAddress(withdrawal.getAddress());
                updatedRewardAcc.setAmount(amount);
                updatedRewardAcc.setSlot(withdrawal.getSlot());
                updatedRewardAcc.setEpoch(withdrawal.getEpoch());
                updatedRewardAcc.setBlockNumber(withdrawal.getBlockNumber());
                updatedRewardAcc.setBlockTime(withdrawal.getBlockTime());
            } else {
                //throw new RuntimeException("No reward account found for withdrawal"); //TODO
                updatedRewardAcc = new RewardAccountEntity();
                updatedRewardAcc.setAddress(withdrawal.getAddress());
                updatedRewardAcc.setAmount(withdrawal.getAmount().negate()); //TODO
                updatedRewardAcc.setSlot(withdrawal.getSlot());
                updatedRewardAcc.setEpoch(withdrawal.getEpoch());
                updatedRewardAcc.setBlockNumber(withdrawal.getBlockNumber());
                updatedRewardAcc.setBlockTime(withdrawal.getBlockTime());
            }

            updatedRewardAccountEntities.add(updatedRewardAcc);
        }

        if (!updatedRewardAccountEntities.isEmpty()) {
            rewardAccountRepository.saveAll(updatedRewardAccountEntities);
        }
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return rewardAccountRepository.deleteBySlotGreaterThan(slot);
    }
}
