package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.RewardAmt;
import com.bloxbean.cardano.yaci.store.events.domain.RewardEvent;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.event.PoolRetiredEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.event.StakingDepositEvent;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolCertificateStorageReader;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositEventProcessor {
    private final AdaPotService adaPotService;
    private final DepositParamService depositParamService;

    private final PoolCertificateStorageReader poolCertificateStorageReader;
    private final PoolStorage poolStorage;
    private final StakingCertificateStorageReader stakingCertificateStorageReader;
    private final ApplicationEventPublisher publisher;

    private BigInteger batchDepositAmount = BigInteger.ZERO;
    private BigInteger batchRefundAmount = BigInteger.ZERO;
    private BigInteger poolRefundAmount = BigInteger.ZERO;
    private BigInteger refundToTreasury = BigInteger.ZERO;

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

        batchDepositAmount = totalDepositAmount;
    }

    //Handles pool deposit refund for retire pool during epoch change
    @EventListener
    @Transactional
    public void handlePoolRetiredEvent(PoolRetiredEvent poolRetiredEvent) {
        log.info("Processing pool deposit events for retired pools. Total # of retired pools {}", poolRetiredEvent.getRetiredPools().size());
        if (poolRetiredEvent.getRetiredPools().isEmpty())
            return;

        BigInteger refundAmt = BigInteger.ZERO;
        List<RewardAmt> rewardAmts = new ArrayList<>();
        //Process pool deposit refund
        for (Pool pool: poolRetiredEvent.getRetiredPools()) {
            //TODO -- check if we can get pool params from the pool registration certificate in one call
            var recentPoolUpdate = poolStorage.findRecentPoolUpdate(pool.getPoolId(), poolRetiredEvent.getMetadata().getEpochNumber() - 1); //in previous epoch
            if (recentPoolUpdate.isEmpty()) {
                recentPoolUpdate = poolStorage.findRecentPoolRegistration(pool.getPoolId(), poolRetiredEvent.getMetadata().getEpochNumber() - 1); //in previous epoch
            }

            if (recentPoolUpdate.isEmpty()) {
                throw new IllegalStateException("No recent pool registration or update found for pool : " + pool.getPoolId());
            }

            var recentPool = recentPoolUpdate.get();

            //For each retired pool, process the refund
            var poolRegistration = poolCertificateStorageReader.findPoolRegistration(recentPool.getTxHash(), recentPool.getCertIndex())
                    .orElseThrow(() -> new IllegalStateException("Pool registration details not found for pool : "
                            + pool.getPoolId() + ", txHash : " + pool.getTxHash()
                            + ", certIndex : " + pool.getCertIndex()));

            var poolDeposit = pool.getAmount();

            refundAmt = refundAmt.add(poolDeposit.negate());
            var rewardAccount = poolRegistration.getRewardAccount();

            //check if the reward account is still registered, if not send the deposit to reserve
            var regCert = stakingCertificateStorageReader
                    .getRegistrationByStakeAddress(rewardAccount, poolRetiredEvent.getMetadata().getSlot())
                    .orElse(null);

            if (regCert == null || regCert.getType() == CertificateType.STAKE_DEREGISTRATION
                    || regCert.getType() == CertificateType.UNREG_CERT) { //reward account not found or deregistered

                //send the deposit to treasury
                refundToTreasury = refundToTreasury.add(poolDeposit);
                log.info("Pool reward account is not registered. Sending the deposit to treasury {}", rewardAccount);
            } else {
                //send the deposit to the pool owner
                var rewardAmt = RewardAmt.builder()
                        .rewardType(RewardType.refund)
                        .address(rewardAccount)
                        .amount(poolDeposit)
                        .build();
                rewardAmts.add(rewardAmt);
            }
        }

        poolRefundAmount = refundAmt;

        //Publish reward event to process refund
        publisher.publishEvent(new RewardEvent(poolRetiredEvent.getMetadata(), rewardAmts));
    }

    public BigInteger getBatchDepositAmount() {
        return batchDepositAmount;
    }

    public BigInteger getBatchRefundAmount() {
        return batchRefundAmount;
    }

    public BigInteger getPoolRefundAmount() {
        return poolRefundAmount;
    }

    public BigInteger getRefundToTreasury() {
        return refundToTreasury;
    }

    public void reset() {
        batchDepositAmount = BigInteger.ZERO;
        batchRefundAmount = BigInteger.ZERO;
        poolRefundAmount = BigInteger.ZERO;
        refundToTreasury = BigInteger.ZERO;
    }

    @EventListener
    @Transactional
    public void rollback(RollbackEvent rollbackEvent) {
        int count = adaPotService.rollbackAdaPot(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} adaPot records", count);
    }

}
