package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.domain.RewardAmt;
import com.bloxbean.cardano.yaci.store.events.domain.RewardEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.domain.event.PoolRetiredEvent;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolCertificateStorageReader;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PoolDepositRefundProcessorTest {
    private static final String POOL_ID = "19cec83f40645921cdacbe5ceb89e716356efb2071989ac17095233a";
    private static final String OLD_REWARD_ACCOUNT = "stake1uynlrvy59zgcu9z5hah8cm9c3c6yfahx5ul46rhxgst56jg0ry80w";
    private static final String NEW_REWARD_ACCOUNT = "stake1u906vxaha82ywx443m3gaulq0ct6l5xv2qmwzdlrqe6n7xs6tmymq";
    private static final BigInteger POOL_DEPOSIT = BigInteger.valueOf(500_000_000L);

    @Mock
    private PoolCertificateStorageReader poolCertificateStorageReader;
    @Mock
    private PoolStorage poolStorage;
    @Mock
    private StakingCertificateStorageReader stakingCertificateStorageReader;
    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private PoolDepositRefundProcessor processor;

    @Test
    void refundGoesToRewardAccountOfReRegistration_whenOlderUpdateExists() {
        //UPDATE in epoch 265 (before first retirement), REGISTRATION again in epoch 278, retired in 285
        var oldUpdate = poolCert(PoolStatusType.UPDATE, "update-tx", 265, 30_000_000L);
        var reRegistration = poolCert(PoolStatusType.REGISTRATION, "rereg-tx", 278, 40_000_000L);
        when(poolStorage.findRecentPoolUpdate(POOL_ID, 284)).thenReturn(Optional.of(oldUpdate));
        when(poolStorage.findRecentPoolRegistration(POOL_ID, 284)).thenReturn(Optional.of(reRegistration));
        when(poolCertificateStorageReader.findPoolRegistration("rereg-tx", 0))
                .thenReturn(Optional.of(PoolRegistration.builder().rewardAccount(NEW_REWARD_ACCOUNT).build()));
        when(stakingCertificateStorageReader.getRegistrationByStakeAddress(eq(NEW_REWARD_ACCOUNT), anyLong()))
                .thenReturn(Optional.of(StakeRegistrationDetail.builder().type(CertificateType.STAKE_REGISTRATION).build()));

        processor.handlePoolRetiredEvent(retiredEvent(285));

        assertThat(publishedRewards()).singleElement()
                .satisfies(r -> assertThat(r.getAddress()).isEqualTo(NEW_REWARD_ACCOUNT));
    }

    @Test
    void refundGoesToRewardAccountOfUpdate_whenUpdateIsAfterRegistration() {
        var registration = poolCert(PoolStatusType.REGISTRATION, "reg-tx", 209, 10_000_000L);
        var update = poolCert(PoolStatusType.UPDATE, "update-tx", 265, 30_000_000L);
        when(poolStorage.findRecentPoolUpdate(POOL_ID, 274)).thenReturn(Optional.of(update));
        when(poolStorage.findRecentPoolRegistration(POOL_ID, 274)).thenReturn(Optional.of(registration));
        when(poolCertificateStorageReader.findPoolRegistration("update-tx", 0))
                .thenReturn(Optional.of(PoolRegistration.builder().rewardAccount(OLD_REWARD_ACCOUNT).build()));
        when(stakingCertificateStorageReader.getRegistrationByStakeAddress(eq(OLD_REWARD_ACCOUNT), anyLong()))
                .thenReturn(Optional.of(StakeRegistrationDetail.builder().type(CertificateType.STAKE_REGISTRATION).build()));

        processor.handlePoolRetiredEvent(retiredEvent(275));

        assertThat(publishedRewards()).singleElement()
                .satisfies(r -> assertThat(r.getAddress()).isEqualTo(OLD_REWARD_ACCOUNT));
    }

    private Pool poolCert(PoolStatusType status, String txHash, int epoch, long slot) {
        return Pool.builder()
                .poolId(POOL_ID)
                .status(status)
                .txHash(txHash)
                .txIndex(0)
                .certIndex(0)
                .epoch(epoch)
                .slot(slot)
                .build();
    }

    private PoolRetiredEvent retiredEvent(int epoch) {
        var retiredPool = Pool.builder()
                .poolId(POOL_ID)
                .status(PoolStatusType.RETIRED)
                .amount(POOL_DEPOSIT)
                .build();
        return PoolRetiredEvent.builder()
                .metadata(EventMetadata.builder().epochNumber(epoch).slot(50_000_000L).build())
                .retiredPools(List.of(retiredPool))
                .build();
    }

    private List<RewardAmt> publishedRewards() {
        var captor = ArgumentCaptor.forClass(RewardEvent.class);
        verify(publisher).publishEvent(captor.capture());
        return captor.getValue().getRewards();
    }
}
