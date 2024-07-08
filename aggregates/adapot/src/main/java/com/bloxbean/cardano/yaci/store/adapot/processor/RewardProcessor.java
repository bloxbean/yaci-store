package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardAmt;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardEvent;
import com.bloxbean.cardano.yaci.store.events.domain.RewardAmt;
import com.bloxbean.cardano.yaci.store.events.domain.RewardEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RewardProcessor {
    private final RewardStorage rewardStorage;

    @EventListener
    @Transactional
    public void handleInstantRewardEvent(InstantRewardEvent rewardEvent) {
        var metadata = rewardEvent.getMetadata();
        List<InstantRewardAmt> rewardAmts = rewardEvent.getRewards();

        if (rewardAmts == null || rewardAmts.isEmpty())
            return;

        var rewards = rewardAmts.stream()
                        .map(rewardAmt -> {
                            //TODO - check if it's true for MIR rewards or refund
                            int epoch = metadata.getEpochNumber() + 1;

                            var reward = new InstantReward();
                            reward.setAddress(rewardAmt.getAddress());
                            reward.setAmount(rewardAmt.getAmount());
                            reward.setType(rewardAmt.getRewardType());
                            reward.setTxHash(rewardAmt.getTxHash());
                            reward.setSlot(metadata.getSlot());
                            reward.setEarnedEpoch(metadata.getEpochNumber());
                            reward.setSpendableEpoch(epoch);
                            reward.setBlockNumber(metadata.getBlock());
                            reward.setBlockTime(metadata.getBlockTime());

                            return reward;
                        }).toList();

        rewardStorage.saveInstantRewards(rewards);
    }

    @EventListener
    @Transactional
    public void handleRewardEvent(RewardEvent rewardEvent) {
        var metadata = rewardEvent.getMetadata();
        List<RewardAmt> rewardAmts = rewardEvent.getRewards();

        if (rewardAmts == null || rewardAmts.isEmpty())
            return;

        var rewards = rewardAmts.stream()
                        .map(rewardAmt -> {
                            var reward = new Reward();
                            reward.setAddress(rewardAmt.getAddress());
                            reward.setAmount(rewardAmt.getAmount());
                            reward.setType(rewardAmt.getRewardType());
                            reward.setPoolId(rewardAmt.getPoolId());
                            reward.setEarnedEpoch(rewardEvent.getEarnedEpoch());
                            reward.setSpendableEpoch(rewardEvent.getSpendableEpoch());
                            reward.setSlot(metadata.getSlot());

                            return reward;
                        }).toList();

        rewardStorage.saveRewards(rewards);
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = rewardStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} rewards records", count);

        //TODO -- What about rewards
    }

}
