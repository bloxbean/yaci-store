package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.InstantRewardSnapshotService;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.*;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RewardProcessor {
    private final AdaPotProperties adaPotProperties;
    private final RewardStorage rewardStorage;
    private final InstantRewardSnapshotService instantRewardSnapshotService;
    private final AdaPotJobStorage rewardCalcJobStorage;

    @EventListener
    @Transactional
    public void handleRewardEvent(RewardEvent rewardEvent) {
        if (!adaPotProperties.isEnabled())
            return;

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
        if (!adaPotProperties.isEnabled())
            return;

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
    public void handleRewardRestEvent(RewardRestEvent rewardRestEvent) {
        if (!adaPotProperties.isEnabled())
            return;

        List<RewardRestAmt> rewardRestAmts = rewardRestEvent.getRewards();

        if (rewardRestAmts == null || rewardRestAmts.isEmpty())
            return;

        var rewards = rewardRestAmts.stream()
                .map(rewardRestAmt -> {
                    var reward = new RewardRest();
                    reward.setAddress(rewardRestAmt.getAddress());
                    reward.setAmount(rewardRestAmt.getAmount());
                    reward.setType(rewardRestAmt.getType());
                    reward.setEarnedEpoch(rewardRestEvent.getEarnedEpoch());
                    reward.setSpendableEpoch(rewardRestEvent.getSpendableEpoch());
                    reward.setSlot(rewardRestEvent.getSlot());

                    return reward;
                }).toList();

        rewardStorage.saveRewardRest(rewards);
    }

    @EventListener
    @Transactional
    public void handleUnclaimedRewardRestEvent(UnclaimedRewardRestEvent unclaimedRewardRestEvent) {
        if (!adaPotProperties.isEnabled())
            return;

        List<RewardRestAmt> unclaimedRewardRestAmts = unclaimedRewardRestEvent.getRewards();

        if (unclaimedRewardRestAmts == null || unclaimedRewardRestAmts.isEmpty())
            return;

        var rewards = unclaimedRewardRestAmts.stream()
                .map(rewardRestAmt -> {
                    var reward = new UnclaimedRewardRest();
                    reward.setAddress(rewardRestAmt.getAddress());
                    reward.setAmount(rewardRestAmt.getAmount());
                    reward.setType(rewardRestAmt.getType());
                    reward.setEarnedEpoch(unclaimedRewardRestEvent.getEarnedEpoch());
                    reward.setSpendableEpoch(unclaimedRewardRestEvent.getSpendableEpoch());
                    reward.setSlot(unclaimedRewardRestEvent.getSlot());

                    return reward;
                }).toList();

        rewardStorage.saveUnclaimedRewardRest(rewards);
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        if (!adaPotProperties.isEnabled())
            return;

        int count = rewardStorage.deleteInstantRewardsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} instant rewards records", count);

        count = rewardStorage.deleteRewardsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} rewards records", count);

        count = rewardStorage.deleteRewardRestsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} reward_rest records", count);

        count = rewardStorage.deleteUnclaimedRewardsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} unclaimed rewards records", count);

        count = rewardCalcJobStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} reward calculation jobs", count);
    }

}
