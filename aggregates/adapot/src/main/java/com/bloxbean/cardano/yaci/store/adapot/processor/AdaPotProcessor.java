package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.EpochTransitionCommitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdaPotProcessor {
    private final AdaPotService adaPotService;
    private final DepositEventProcessor depositEventProcessor;
    private final FeePotProcessor feePotProcessor;

    @EventListener
    @Transactional
    public void processAdaPotForBlockBatch(CommitEvent commitEvent) {
        var totalDepositAmount = depositEventProcessor.getBatchDepositAmount();
        var totalRefundAmount = depositEventProcessor.getBatchRefundAmount(); //negative

        var netDeposit = totalDepositAmount.add(totalRefundAmount);

        var totalFeeInBlockBatch = feePotProcessor.getTotalFeeInBatch();

        if (log.isDebugEnabled())
            log.debug("Total deposit amount : {}, Total refund amount : {}, Total fee : {}", totalDepositAmount, totalRefundAmount, netDeposit, totalFeeInBlockBatch);

        var existingAdaPot = adaPotService.getAdaPot(commitEvent.getMetadata().getEpochNumber());

        BigInteger totalFee;
        if (existingAdaPot.getEpochBoundary()) { //If prev adapot is from epoch boundary, reset fee in pot
            totalFee = BigInteger.ZERO.add(totalFeeInBlockBatch);
        } else {
            totalFee = existingAdaPot.getFees().add(totalFeeInBlockBatch);
        }

        adaPotService.updateAdaPotDeposit(commitEvent.getMetadata(), existingAdaPot, netDeposit, totalFee, false);

        depositEventProcessor.reset();
        feePotProcessor.reset();
    }

    @EventListener
    @Transactional
    public void processAdaPotDuringEpochTransition(EpochTransitionCommitEvent epochTransitionCommitEvent) {
        //Update retired pool refunds
        BigInteger poolRefundAmount = depositEventProcessor.getPoolRefundAmount();

        var existingAdaPot = adaPotService.getAdaPot(epochTransitionCommitEvent.getMetadata().getEpochNumber());
        //Total fee in the epoch
        adaPotService.updateAdaPotDeposit(epochTransitionCommitEvent.getMetadata(), existingAdaPot, poolRefundAmount, existingAdaPot.getFees(), true);

        log.info("AdaPot updated for epoch transition : {}", epochTransitionCommitEvent.getMetadata().getEpochNumber());
    }
}
