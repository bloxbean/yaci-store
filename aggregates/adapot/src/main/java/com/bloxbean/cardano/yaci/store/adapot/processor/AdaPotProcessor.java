package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdaPotProcessor {
    private final AdaPotService adaPotService;
    private final DepositEventProcessor depositEventProcessor;
    private final FeePotProcessor feePotProcessor;
    private final UtxoPotProcessor utxoPotProcessor;
    private final EraService eraService;
    private final TransactionStorageReader transactionStorageReader;

    private Integer nonByronEpoch;

    @EventListener
    @Transactional
    public void processAdaPotForBlockBatch(CommitEvent commitEvent) {
        var totalDepositAmount = depositEventProcessor.getBatchDepositAmount();
        var totalRefundAmount = depositEventProcessor.getBatchRefundAmount(); //negative

        var netDeposit = totalDepositAmount.add(totalRefundAmount);

        var totalFeeInBlockBatch = feePotProcessor.getTotalFeeInBatch();
        var netUtxoInBlockBatch = utxoPotProcessor.getNetUtxoAmount();

        if (log.isDebugEnabled())
            log.debug("Total deposit amount : {}, Total refund amount : {}, Total fee : {}", totalDepositAmount, totalRefundAmount, netDeposit, totalFeeInBlockBatch);

        var existingAdaPot = adaPotService.getAdaPot(commitEvent.getMetadata().getEpochNumber());

        BigInteger totalFee;
        if (existingAdaPot.getEpochBoundary()) { //If prev adapot is from epoch boundary, reset fee in pot
            totalFee = BigInteger.ZERO.add(totalFeeInBlockBatch);
        } else {
            totalFee = existingAdaPot.getFees().add(totalFeeInBlockBatch);
        }

        adaPotService.updateAdaPotDeposit(commitEvent.getMetadata(), existingAdaPot, netDeposit, totalFee, netUtxoInBlockBatch, false);

        depositEventProcessor.reset();
        feePotProcessor.reset();
        utxoPotProcessor.reset();
    }


    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAdaPotDuringEpochTransition(PreEpochTransitionEvent epochTransitionCommitEvent) {

        //TODO -- Handle null previous epoch due to restart
        if (epochTransitionCommitEvent.getPreviousEpoch() == null) {
            return;
        }

        //Update retired pool refunds
        BigInteger poolRefundAmount = depositEventProcessor.getPoolRefundAmount();
        BigInteger refundToTreasury = depositEventProcessor.getRefundToTreasury();

        var existingAdaPot = adaPotService.getAdaPot(epochTransitionCommitEvent.getMetadata().getEpochNumber());

        //Update Fee pot
        var totalFeeInEpoch = transactionStorageReader.getTotalFee(epochTransitionCommitEvent.getMetadata().getEpochNumber() - 1); //Prev epoch
        log.info("Total fee in epoch {} : {}", epochTransitionCommitEvent.getEpoch() - 1, totalFeeInEpoch);

        //Total fee in the epoch
        adaPotService.updateAdaPotDeposit(epochTransitionCommitEvent.getMetadata(), existingAdaPot, poolRefundAmount, totalFeeInEpoch, BigInteger.ZERO, refundToTreasury, true);
        log.info("AdaPot updated for epoch transition : {}", epochTransitionCommitEvent.getMetadata().getEpochNumber());

    }
}
