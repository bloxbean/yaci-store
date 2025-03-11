package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.adapot.job.AdaPotJobManager;
import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.EpochTransitionCommitEvent;
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
    private final AdaPotProperties adaPotProperties;
    private final EraService eraService;
    private final AdaPotService adaPotService;
    private final AdaPotJobManager adaPotJobManager;

    @EventListener
    @Transactional
    public void processAdaPotDuringEpochTransition(EpochTransitionCommitEvent epochTransitionCommitEvent) {
        if (!adaPotProperties.isEnabled())
            return;

        //TODO -- Handle null previous epoch due to restart
        //For custom network, epoch 0 can be directly at era > shelley. so no previous epoch and we should
        //consider epoch 0 as well
        if (epochTransitionCommitEvent.getPreviousEpoch() == null && epochTransitionCommitEvent.getEpoch() > 0) {
            return;
        }

        if (epochTransitionCommitEvent.getEra() == Era.Byron)
            return;

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
        adaPotJobManager.triggerRewardCalcJob(epoch, epochTransitionCommitEvent.getMetadata().getSlot());
    }

    @EventListener
    @Transactional
    public void rollback(RollbackEvent rollbackEvent) {
        if (!adaPotProperties.isEnabled())
            return;

        int count = adaPotService.rollbackAdaPot(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} adaPot records", count);
    }
}
