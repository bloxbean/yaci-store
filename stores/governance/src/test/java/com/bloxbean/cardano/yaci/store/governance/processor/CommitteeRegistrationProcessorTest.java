package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.certs.AuthCommitteeHotCert;
import com.bloxbean.cardano.yaci.core.model.certs.ResignCommitteeColdCert;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.governance.Anchor;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeDeRegistrationStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeRegistrationStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CommitteeRegistrationProcessorTest {

    @Mock
    private CommitteeDeRegistrationStorage committeeDeRegistrationStorage;
    @Mock
    private CommitteeRegistrationStorage committeeRegistrationStorage;
    @InjectMocks
    private CommitteeRegistrationProcessor committeeRegistrationProcessor;
    @Captor
    private ArgumentCaptor<List<CommitteeDeRegistration>> committeeDeRegistrationsCaptor;
    @Captor
    private ArgumentCaptor<List<CommitteeRegistration>> committeeRegistrationsCaptor;

    @Test
    void testHandleCommitteeRegistration_WhenCertIsResignCommitteeColdCert() {
        final String txHash = "498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f";
        final CertificateEvent certificateEvent = CertificateEvent.builder()
                .metadata(eventMetadata())
                .txCertificatesList(List.of(TxCertificates.builder()
                        .txHash(txHash)
                        .certificates(List.of(
                                ResignCommitteeColdCert.builder()
                                        .committeeColdCredential(Credential.builder()
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .hash("50b0f0893285e237641df982bf8cbd277a5788bd81e1014eb9e8e207")
                                                .build())
                                        .anchor(Anchor.builder()
                                                .anchor_data_hash("cd4ca39b59b7c1133635fb97668bde1f28934af4dfd524fdeacda0d47ffb8bf2")
                                                .anchor_url("http://bit.ly/3QFMhii?index=0")
                                                .build())
                                        .build()
                        ))
                        .build()))
                .build();

        committeeRegistrationProcessor.handleCommitteeRegistration(certificateEvent);

        verify(committeeDeRegistrationStorage).saveAll(committeeDeRegistrationsCaptor.capture());
        assertThat(committeeDeRegistrationsCaptor.getValue()).hasSize(1);
        CommitteeDeRegistration savedCommitteeDeRegistration = committeeDeRegistrationsCaptor.getValue().get(0);

        assertThat(savedCommitteeDeRegistration.getTxHash()).isEqualTo(txHash);
        assertThat(savedCommitteeDeRegistration.getAnchorHash()).isEqualTo("cd4ca39b59b7c1133635fb97668bde1f28934af4dfd524fdeacda0d47ffb8bf2");
        assertThat(savedCommitteeDeRegistration.getAnchorUrl()).isEqualTo("http://bit.ly/3QFMhii?index=0");
        assertThat(savedCommitteeDeRegistration.getColdKey()).isEqualTo("50b0f0893285e237641df982bf8cbd277a5788bd81e1014eb9e8e207");
        assertThat(savedCommitteeDeRegistration.getCredType()).isEqualTo(StakeCredType.ADDR_KEYHASH);
        assertThat(savedCommitteeDeRegistration.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(savedCommitteeDeRegistration.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(savedCommitteeDeRegistration.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(savedCommitteeDeRegistration.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
    }

    @Test
    void testHandleCommitteeRegistration_WhenCertIsAuthCommitteeHotCert() {
        final String txHash = "498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f";
        final CertificateEvent certificateEvent = CertificateEvent.builder()
                .metadata(eventMetadata())
                .txCertificatesList(List.of(TxCertificates.builder()
                        .txHash(txHash)
                        .certificates(List.of(
                                AuthCommitteeHotCert.builder()
                                        .committeeColdCredential(Credential.builder()
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .hash("50b0f0893285e237641df982bf8cbd277a5788bd81e1014eb9e8e207")
                                                .build())
                                        .committeeHotCredential(Credential.builder()
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .hash("212f86a07149f5d19e1f841d065f9e76c6b4a76db727ae7afc2cb2e4")
                                                .build())
                                        .build()
                        ))
                        .build()))
                .build();

        committeeRegistrationProcessor.handleCommitteeRegistration(certificateEvent);

        verify(committeeRegistrationStorage).saveAll(committeeRegistrationsCaptor.capture());
        assertThat(committeeRegistrationsCaptor.getValue()).hasSize(1);
        CommitteeRegistration savedCommitteeRegistration = committeeRegistrationsCaptor.getValue().get(0);

        assertThat(savedCommitteeRegistration.getTxHash()).isEqualTo(txHash);
        assertThat(savedCommitteeRegistration.getColdKey()).isEqualTo("50b0f0893285e237641df982bf8cbd277a5788bd81e1014eb9e8e207");
        assertThat(savedCommitteeRegistration.getHotKey()).isEqualTo("212f86a07149f5d19e1f841d065f9e76c6b4a76db727ae7afc2cb2e4");
        assertThat(savedCommitteeRegistration.getCredType()).isEqualTo(StakeCredType.ADDR_KEYHASH);
        assertThat(savedCommitteeRegistration.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(savedCommitteeRegistration.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(savedCommitteeRegistration.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(savedCommitteeRegistration.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
    }

    @Test
    void testHandleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(78650212, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(78650198, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .build();

        committeeRegistrationProcessor.handleRollbackEvent(rollbackEvent);

        verify(committeeRegistrationStorage, times(1)).deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        verify(committeeDeRegistrationStorage, times(1)).deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
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
