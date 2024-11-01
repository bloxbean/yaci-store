package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.reward.service.RewardCalcJobManager;
import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.internal.EpochTransitionCommitEvent;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ReadOnly(false)
@Slf4j
public class AdaPotProcessor {
    private final EraService eraService;
    private final AdaPotService adaPotService;
    private final TransactionStorageReader transactionStorageReader;
    private final RewardCalcJobManager rewardCalcJobManager;

    @EventListener
    @Transactional
    public void processAdaPotDuringEpochTransition(EpochTransitionCommitEvent epochTransitionCommitEvent) {

        //TODO -- Handle null previous epoch due to restart
        if (epochTransitionCommitEvent.getPreviousEpoch() == null) {
            return;
        }

        if (epochTransitionCommitEvent.getEra() == Era.Byron)
            return;

        //Create AdaPot for the epoch
        adaPotService.createAdaPot(epochTransitionCommitEvent.getMetadata());

        //Update Fee pot
        var totalFeeInEpoch = transactionStorageReader.getTotalFee(epochTransitionCommitEvent.getMetadata().getEpochNumber() - 1); //Prev epoch
        log.info("Total fee in epoch {} : {}", epochTransitionCommitEvent.getEpoch() - 1, totalFeeInEpoch);

        //Update total fee in the epoch
        adaPotService.updateEpochFee(epochTransitionCommitEvent.getMetadata().getEpochNumber(), totalFeeInEpoch);

        triggerEpochTransitionJobs(epochTransitionCommitEvent);
    }

    private void triggerEpochTransitionJobs(EpochTransitionCommitEvent epochTransitionCommitEvent) {
        Integer nonByronEpoch = eraService.getFirstNonByronEpoch().orElse(null);

        if (nonByronEpoch == null || epochTransitionCommitEvent.getEpoch() < nonByronEpoch) {
            log.info("Epoch : {} is Byron era. Skipping reward calculation", epochTransitionCommitEvent.getEpoch());
            return;
        }

        //Calculate epoch rewards
        Integer epoch = epochTransitionCommitEvent.getEpoch();
        rewardCalcJobManager.triggerRewardCalcJob(epoch, epochTransitionCommitEvent.getMetadata().getSlot());
    }
}
