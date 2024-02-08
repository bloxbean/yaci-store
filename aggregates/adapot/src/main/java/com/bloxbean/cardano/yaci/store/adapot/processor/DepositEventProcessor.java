package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.event.PoolRetiredEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.event.StakingDepositEvent;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositEventProcessor {
    private final AdaPotService adaPotService;
    private final DepositParamService depositParamService;

    @EventListener
    @Transactional
    public void handleStakingDepositEvent(StakingDepositEvent stakingDepositEvent) {
        var metadata = stakingDepositEvent.getMetadata();
        //Check if there is any existing value for this epoch
        var prevAdaPot = adaPotService.getAdaPot(metadata.getEpochNumber());

        var keyDeposit = depositParamService.getKeyDeposit(metadata.getEpochNumber());

        BigInteger totalRegDeposit = BigInteger.ZERO;
        BigInteger totalKeyRegRefund = BigInteger.ZERO;
        if (stakingDepositEvent.getStakeKeyRegistrationCount() > 0)
            totalRegDeposit = keyDeposit.multiply(BigInteger.valueOf(stakingDepositEvent.getStakeKeyRegistrationCount()));

        if (stakingDepositEvent.getStakeKeyDeRegistrationCount() > 0)
            totalKeyRegRefund = keyDeposit.multiply(BigInteger.valueOf(stakingDepositEvent.getStakeKeyDeRegistrationCount()));

        var totalDepositAmount = totalRegDeposit.subtract(totalKeyRegRefund);

        var poolDeposit = depositParamService.getPoolDeposit(metadata.getEpochNumber());
        //sum deposit amount in poolDeposits
        if (stakingDepositEvent.getStakePoolRegistrationCount() > 0) {
            var totalPoolDeposit = poolDeposit.multiply(BigInteger.valueOf(stakingDepositEvent.getStakePoolRegistrationCount()));
            totalDepositAmount = totalDepositAmount.add(totalPoolDeposit);
        }

        if (log.isDebugEnabled())
            log.debug("Total deposit amount : {}", totalDepositAmount);

        adaPotService.updateAdaPotDeposit(metadata, prevAdaPot, totalDepositAmount, false);
    }

    //Handles pool deposit refund for retire pool durin epoch change
    @EventListener
    @Transactional
    public void handlePoolRetiredEvent(PoolRetiredEvent poolRetiredEvent) {
        log.info("Processing pool deposit events for retired pools. Total # of retired pools {}", poolRetiredEvent.getRetiredPools().size());
        if (poolRetiredEvent.getRetiredPools().isEmpty())
            return;

        var refundAmt = poolRetiredEvent.getRetiredPools()
                .stream().map(p -> p.getAmount())
                .reduce(BigInteger::add)
                .orElse(BigInteger.ZERO);

        var metadata = poolRetiredEvent.getMetadata();
        var adaPot = adaPotService.getAdaPot(metadata.getEpochNumber());

        adaPotService.updateAdaPotDeposit(metadata, adaPot, refundAmt, true);
    }


    @EventListener
    @Transactional
    public void rollback(RollbackEvent rollbackEvent) {
        int count = adaPotService.rollbackAdaPot(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} adaPot records", count);
    }

}
