package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.InstantRewardSnapshotService;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.RewardAmt;
import com.bloxbean.cardano.yaci.store.events.domain.RewardEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
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
    private final InstantRewardSnapshotService instantRewardSnapshotService;


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
    public void handleInstantRewardSnapshot(PreEpochTransitionEvent epochTransitionCommitEvent) {
        if (epochTransitionCommitEvent.getEra() == Era.Byron)
            return;

        //TODO -- Handle null previous epoch due to restart
        if (epochTransitionCommitEvent.getPreviousEpoch() == null) {
            return;
        }

        int snapshotEpoch = epochTransitionCommitEvent.getEpoch() - 1;
        if (snapshotEpoch < 0)
            return;

        if (epochTransitionCommitEvent.getPreviousEpoch() == null) {
            log.error("Previous epoch is null. Cannot take instant reward snapshot");
            return;
        }

        instantRewardSnapshotService.takeInstantRewardSnapshot(epochTransitionCommitEvent.getMetadata(), epochTransitionCommitEvent.getPreviousEpoch());
        log.info("Instant reward snapshot taken for epoch : {}", snapshotEpoch);
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = rewardStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} rewards records", count);

        //TODO -- What about rewards
    }

}
