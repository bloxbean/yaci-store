package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.domain.*;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.event.PoolRetiredEvent;
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

import static com.bloxbean.cardano.yaci.store.adapot.AdaPotConfiguration.STORE_ADAPOT_ENABLED;

//TODO -- Remove this later if not required
@Component
@RequiredArgsConstructor
@EnableIf(value = STORE_ADAPOT_ENABLED, defaultValue = false)
@Slf4j
public class PoolDepositRefundProcessor {

    private final PoolCertificateStorageReader poolCertificateStorageReader;
    private final PoolStorage poolStorage;
    private final StakingCertificateStorageReader stakingCertificateStorageReader;
    private final ApplicationEventPublisher publisher;

    //Handles pool deposit refund for retire pool during epoch change
    //PoolRetiredEvent is published during PreEpochTransitionEvent
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
                //refundToTreasury = refundToTreasury.add(poolDeposit);
                log.info("Pool reward account is not registered. Sending the deposit to treasury {}", rewardAccount);
            } else {
                //send the deposit to the pool owner
                var rewardAmt = RewardAmt.builder()
                        .rewardType(RewardType.refund)
                        .address(rewardAccount)
                        .poolId(pool.getPoolId())
                        .amount(poolDeposit)
                        .build();
                rewardAmts.add(rewardAmt);
            }
        }

        //Publish reward event to process refund
        var rewardEvent = RewardEvent.builder()
                .metadata(poolRetiredEvent.getMetadata())
                .earnedEpoch(poolRetiredEvent.getMetadata().getEpochNumber())
                .spendableEpoch(poolRetiredEvent.getMetadata().getEpochNumber())
                .rewards(rewardAmts)
                .build();
        publisher.publishEvent(rewardEvent);
    }

}
