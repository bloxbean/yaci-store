package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.certs.RegDrepCert;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.certs.UnregDrepCert;
import com.bloxbean.cardano.yaci.core.model.certs.UpdateDrepCert;
import com.bloxbean.cardano.yaci.core.model.governance.Anchor;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.DRepRegistrationStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DRepRegistrationProcessorTest {
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private DRepRegistrationStorage dRepRegistrationStorage;
    @InjectMocks
    private DRepRegistrationProcessor dRepRegistrationProcessor;
    @Captor
    private ArgumentCaptor<List<DRepRegistration>> dRepRegistrationsCaptor;

    @Test
    void testHandleDRepRegistration_WhenCertIsRegDRepCert() {
        final String txHash = "498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f";
        final CertificateEvent certificateEvent = CertificateEvent.builder()
                .metadata(eventMetadata())
                .txCertificatesList(List.of(TxCertificates.builder()
                        .txHash(txHash)
                        .certificates(List.of(
                                RegDrepCert.builder()
                                        .drepCredential(Credential.builder()
                                                .hash("d6d84c6a5b05cb8f89d24e9d46926975fa1dc08a58b3c26e96c06df7")
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .build())
                                        .coin(BigInteger.valueOf(2000000))
                                        .anchor(Anchor.builder()
                                                .anchor_data_hash("1111111111111111111111111111111111111111111111111111111111111111")
                                                .anchor_url("https://bit.ly/3zCH2HL")
                                                .build())
                                        .build()
                        ))
                        .build()))
                .build();

        dRepRegistrationProcessor.handleDRepRegistration(certificateEvent);

        verify(dRepRegistrationStorage).saveAll(dRepRegistrationsCaptor.capture());
        assertThat(dRepRegistrationsCaptor.getValue()).hasSize(1);
        DRepRegistration savedDRepRegistration = dRepRegistrationsCaptor.getValue().get(0);

        assertThat(savedDRepRegistration.getTxHash()).isEqualTo(txHash);
        assertThat(savedDRepRegistration.getDeposit()).isEqualTo(2000000);
        assertThat(savedDRepRegistration.getDrepHash()).isEqualTo("d6d84c6a5b05cb8f89d24e9d46926975fa1dc08a58b3c26e96c06df7");
        assertThat(savedDRepRegistration.getDrepId()).isEqualTo(GovId.drepFromKeyHash(HexUtil.decodeHexString("d6d84c6a5b05cb8f89d24e9d46926975fa1dc08a58b3c26e96c06df7")));
        assertThat(savedDRepRegistration.getAnchorUrl()).isEqualTo("https://bit.ly/3zCH2HL");
        assertThat(savedDRepRegistration.getAnchorHash()).isEqualTo("1111111111111111111111111111111111111111111111111111111111111111");
        assertThat(savedDRepRegistration.getCertIndex()).isEqualTo(0);
        assertThat(savedDRepRegistration.getCredType()).isEqualTo(StakeCredType.ADDR_KEYHASH);
        assertThat(savedDRepRegistration.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(savedDRepRegistration.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(savedDRepRegistration.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
    }

    @Test
    void testHandleDRepRegistration_WhenCertIsUnRegDRepCert() {
        final String txHash = "498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f";
        final CertificateEvent certificateEvent = CertificateEvent.builder()
                .metadata(eventMetadata())
                .txCertificatesList(List.of(TxCertificates.builder()
                        .txHash(txHash)
                        .certificates(List.of(
                                UnregDrepCert.builder()
                                        .drepCredential(Credential.builder()
                                                .hash("adae9fa43726611f1c2c29f798f223316a084a3f54f8c3bf4fb8d410")
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .build())
                                        .coin(BigInteger.valueOf(2000000))
                                        .build()
                        ))
                        .build()))
                .build();

        dRepRegistrationProcessor.handleDRepRegistration(certificateEvent);

        verify(dRepRegistrationStorage).saveAll(dRepRegistrationsCaptor.capture());
        assertThat(dRepRegistrationsCaptor.getValue()).hasSize(1);
        DRepRegistration savedDRepRegistration = dRepRegistrationsCaptor.getValue().get(0);

        assertThat(savedDRepRegistration.getTxHash()).isEqualTo(txHash);
        assertThat(savedDRepRegistration.getDeposit()).isEqualTo(2000000);
        assertThat(savedDRepRegistration.getDrepHash()).isEqualTo("adae9fa43726611f1c2c29f798f223316a084a3f54f8c3bf4fb8d410");
        assertThat(savedDRepRegistration.getDrepId()).isEqualTo(GovId.drepFromKeyHash(HexUtil.decodeHexString("adae9fa43726611f1c2c29f798f223316a084a3f54f8c3bf4fb8d410")));
        assertThat(savedDRepRegistration.getAnchorUrl()).isNull();
        assertThat(savedDRepRegistration.getAnchorHash()).isNull();
        assertThat(savedDRepRegistration.getCertIndex()).isEqualTo(0);
        assertThat(savedDRepRegistration.getCredType()).isEqualTo(StakeCredType.ADDR_KEYHASH);
        assertThat(savedDRepRegistration.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(savedDRepRegistration.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(savedDRepRegistration.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(savedDRepRegistration.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
    }

    @Test
    void testHandleDRepRegistration_WhenCertIsUpdateDRepCert() {
        final String txHash = "498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f";
        final CertificateEvent certificateEvent = CertificateEvent.builder()
                .metadata(eventMetadata())
                .txCertificatesList(List.of(TxCertificates.builder()
                        .txHash(txHash)
                        .certificates(List.of(
                                UpdateDrepCert.builder()
                                        .drepCredential(Credential.builder()
                                                .hash("b3b5ff08207450a2d7aab00f206c6d25078ccee35b9c407afd941e95")
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .build())
                                        .anchor(Anchor.builder()
                                                .anchor_data_hash("1111111111111111111111111111111111111111111111111111111111111111")
                                                .anchor_url("https://bit.ly/3zCH2HL")
                                                .build())
                                        .build()
                        ))
                        .build()))
                .build();

        dRepRegistrationProcessor.handleDRepRegistration(certificateEvent);

        verify(dRepRegistrationStorage).saveAll(dRepRegistrationsCaptor.capture());
        assertThat(dRepRegistrationsCaptor.getValue()).hasSize(1);
        DRepRegistration savedDRepRegistration = dRepRegistrationsCaptor.getValue().get(0);

        assertThat(savedDRepRegistration.getTxHash()).isEqualTo(txHash);
        assertThat(savedDRepRegistration.getDeposit()).isNull();
        assertThat(savedDRepRegistration.getDrepHash()).isEqualTo("b3b5ff08207450a2d7aab00f206c6d25078ccee35b9c407afd941e95");
        assertThat(savedDRepRegistration.getDrepId()).isEqualTo(GovId.drepFromKeyHash(HexUtil.decodeHexString("b3b5ff08207450a2d7aab00f206c6d25078ccee35b9c407afd941e95")));
        assertThat(savedDRepRegistration.getAnchorUrl()).isEqualTo("https://bit.ly/3zCH2HL");
        assertThat(savedDRepRegistration.getAnchorHash()).isEqualTo("1111111111111111111111111111111111111111111111111111111111111111");
        assertThat(savedDRepRegistration.getCertIndex()).isEqualTo(0);
        assertThat(savedDRepRegistration.getCredType()).isEqualTo(StakeCredType.ADDR_KEYHASH);
        assertThat(savedDRepRegistration.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(savedDRepRegistration.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(savedDRepRegistration.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(savedDRepRegistration.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
    }

    @Test
    void testHandleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(78650212, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(78650198, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .build();

        dRepRegistrationProcessor.handleRollbackEvent(rollbackEvent);
        verify(dRepRegistrationStorage, times(1)).deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .epochNumber(50)
                .block(100L)
                .blockTime(99999L)
                .slot(10000L)
                .build();
    }
}
