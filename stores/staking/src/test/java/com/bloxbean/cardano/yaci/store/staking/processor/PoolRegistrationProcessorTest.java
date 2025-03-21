package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.yaci.core.model.PoolParams;
import com.bloxbean.cardano.yaci.core.model.Relay;
import com.bloxbean.cardano.yaci.core.model.certs.PoolRegistration;
import com.bloxbean.cardano.yaci.core.model.certs.PoolRetirement;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolCertificateStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PoolRegistrationProcessorTest {
    @Mock
    private PoolCertificateStorage poolStorage;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private StoreProperties storeProperties;

    @InjectMocks
    private PoolRegistrationProcessor poolRegistrationProcessor;

    @Captor
    ArgumentCaptor<List<com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration>> poolRegistrationCaptor;

    @Captor
    ArgumentCaptor<List<com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement>> poolRetirementCaptor;

    @Test
    void processPoolRegistration_WhenCertTypeIsPoolRegistration() {
        PoolRegistration poolRegistrationCert = PoolRegistration.builder()
                .poolParams(PoolParams.builder()
                        .operator("e14bee3dc87c4b9e00703513c2122eb2dc53c4db19862c16228ca9d1")
                        .vrfKeyHash("40983f267a1571ef3a6e300b85b6dd1501af2dfa74e478c56bd366190149a598")
                        .pledge(BigInteger.ZERO)
                        .cost(BigInteger.valueOf(340000000))
                        .margin("3/40")
                        .rewardAccount("e028bcf2cdaafbe7352a4f09cbc8c1277a7247d19355772edf4b89cae6")
                        .poolOwners(Set.of("28bcf2cdaafbe7352a4f09cbc8c1277a7247d19355772edf4b89cae6"))
                        .relays(List.of(
                                Relay.builder()
                                        .port(6000)
                                        .ipv4("34.0.9.23")
                                        .ipv6(null)
                                        .dnsName(null)
                                        .build()
                        ))
                        .poolMetadataUrl("https://raw.githubusercontent.com/zjavax/ada/main/baidu.json")
                        .poolMetadataHash("8c9fd4b8725ea8f442392e9ebb13255815f48538d2e6506f8ebdb8ba4c084b8c")
                        .build())
                .build();

        CertificateEvent certificateEvent =
                new CertificateEvent(eventMetadata(),
                        List.of(TxCertificates.builder()
                                .txHash("0f229fd9cdad8f306c16b226cf459818251af4b05efd8d83c75b609d5eb78dc3")
                                .certificates(
                                        List.of(poolRegistrationCert))
                                .build()));

        poolRegistrationProcessor.processPoolRegistration(certificateEvent);

        verify(poolStorage, times(1)).savePoolRegistrations(poolRegistrationCaptor.capture());
        verify(poolStorage, never()).savePoolRetirements(any());
        assertThat(poolRegistrationCaptor.getValue()).hasSize(1);

        com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration poolRegistrationSaved = poolRegistrationCaptor.getValue().get(0);

        assertThat(poolRegistrationSaved.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(poolRegistrationSaved.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(poolRegistrationSaved.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(poolRegistrationSaved.getBlockHash()).isEqualTo(eventMetadata().getBlockHash());
        assertThat(poolRegistrationSaved.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(poolRegistrationSaved.getTxHash()).isEqualTo("0f229fd9cdad8f306c16b226cf459818251af4b05efd8d83c75b609d5eb78dc3");
        assertThat(poolRegistrationSaved.getCertIndex()).isEqualTo(0);
        assertThat(poolRegistrationSaved.getVrfKeyHash()).isEqualTo(poolRegistrationCert.getPoolParams().getVrfKeyHash());
        assertThat(poolRegistrationSaved.getPledge()).isEqualTo(poolRegistrationCert.getPoolParams().getPledge());
        assertThat(poolRegistrationSaved.getCost()).isEqualTo(poolRegistrationCert.getPoolParams().getCost());
        assertThat(poolRegistrationSaved.getMargin()).isEqualTo(0.075);
        assertThat(poolRegistrationSaved.getRewardAccount()).isEqualTo("stake_test1uq5teukd4ta7wdf2fuyuhjxpyaa8y373jd2hwtklfwyu4es7clcde");
        assertThat(poolRegistrationSaved.getPoolOwners()).isEqualTo(poolRegistrationCert.getPoolParams().getPoolOwners());
        assertThat(poolRegistrationSaved.getRelays()).isEqualTo(poolRegistrationCert.getPoolParams().getRelays());
        assertThat(poolRegistrationSaved.getMetadataUrl()).isEqualTo(poolRegistrationCert.getPoolParams().getPoolMetadataUrl());
        assertThat(poolRegistrationSaved.getMetadataHash()).isEqualTo(poolRegistrationCert.getPoolParams().getPoolMetadataHash());
    }

    @Test
    void processPoolRegistration_WhenCertTypeIsPoolRetirement() {
        PoolRetirement poolRetirementCert = PoolRetirement.builder()
                .poolKeyHash("efe2f45e00f4e31a30a268e6d7b43c676b1a60a402e03606f04e1122")
                .epoch(80)
                .build();
        CertificateEvent certificateEvent =
                new CertificateEvent(eventMetadata(),
                        List.of(TxCertificates.builder()
                                .txHash("0f229fd9cdad8f306c16b226cf459818251af4b05efd8d83c75b609d5eb78dc3")
                                .certificates(
                                        List.of(poolRetirementCert))
                                .build()));

        poolRegistrationProcessor.processPoolRegistration(certificateEvent);

        verify(poolStorage, times(1)).savePoolRetirements(poolRetirementCaptor.capture());
        verify(poolStorage, never()).savePoolRegistrations(any());
        assertThat(poolRetirementCaptor.getValue()).hasSize(1);

        com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement poolRetirementSaved = poolRetirementCaptor.getValue().get(0);

        assertThat(poolRetirementSaved.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(poolRetirementSaved.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(poolRetirementSaved.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(poolRetirementSaved.getBlockHash()).isEqualTo(eventMetadata().getBlockHash());
        assertThat(poolRetirementSaved.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(poolRetirementSaved.getRetirementEpoch()).isEqualTo(poolRetirementCert.getEpoch());
        assertThat(poolRetirementSaved.getCertIndex()).isEqualTo(0);
        assertThat(poolRetirementSaved.getTxHash()).isEqualTo("0f229fd9cdad8f306c16b226cf459818251af4b05efd8d83c75b609d5eb78dc3");
    }

    @Test
    void handleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(78650212, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(78650198, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .build();

        poolRegistrationProcessor.handleRollbackEvent(rollbackEvent);

        verify(poolStorage, times(1)).deleteRegistrationsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        verify(poolStorage, times(1)).deleteRetirementsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .mainnet(false)
                .epochNumber(80)
                .slotLeader("8ffb4c8e648c0662f2a91157c92feaa95f1a3d2728eaea8257b3d8d9")
                .block(1133167)
                .blockHash("5f834500d2e4dde1bc07feb8e00cd320c53f26fa41749f2e2b2bd0a81fa833f7")
                .blockTime(1688706377)
                .prevBlockHash("8ca8294dd773b27a1202858aad91259dd35989eebc490a1dcff3c536653a182d")
                .slot(33023177)
                .epochSlot(119648)
                .noOfTxs(1)
                .syncMode(false)
                .remotePublish(false)
                .build();
    }
}
