package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.yaci.core.model.certs.*;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StakeRegProcessorTest {

    @Mock
    private StakingCertificateStorage stakingStorage;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private StakeRegProcessor stakeRegProcessor;

    @Captor
    ArgumentCaptor<List<StakeRegistrationDetail>> stakeRegDetailCaptor;

    @Captor
    ArgumentCaptor<List<Delegation>> delegationCaptor;

    @Test
    void processStakeRegistration_WhenCertTypeIsStakeRegistration() {
        StakeRegistration stakeRegistrationCert = StakeRegistration
                .builder()
                .stakeCredential(StakeCredential.builder()
                        .type(StakeCredType.ADDR_KEYHASH)
                        .hash("42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9")
                        .build())
                .build();

        CertificateEvent certificateEvent =
                new CertificateEvent(eventMetadata(),
                        List.of(TxCertificates.builder()
                                .txHash("08a5678d15d1a9196524a35e580b98b4e8b8dc2d57d544a6c8f9520f885d1fdb")
                                .certificates(
                                        List.of(stakeRegistrationCert))
                                .build()));

        stakeRegProcessor.processStakeRegistration(certificateEvent);

        verify(stakingStorage, times(1)).saveRegistrations(stakeRegDetailCaptor.capture());
        verify(stakingStorage, never()).saveDelegations(any());
        assertThat(stakeRegDetailCaptor.getValue()).hasSize(1);

        StakeRegistrationDetail stakeRegDeSaved = stakeRegDetailCaptor.getValue().get(0);

        assertThat(stakeRegDeSaved.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(stakeRegDeSaved.getBlockHash()).isEqualTo(eventMetadata().getBlockHash());
        assertThat(stakeRegDeSaved.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(stakeRegDeSaved.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(stakeRegDeSaved.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(stakeRegDeSaved.getAddress()).isEqualTo("stake_test1upprgdf9umls0ex79gfj3ymvjeqxuse07z4wk7jutfdxejgzkmsfe");
        assertThat(stakeRegDeSaved.getType()).isEqualTo(CertificateType.STAKE_REGISTRATION);
        assertThat(stakeRegDeSaved.getTxHash()).isEqualTo("08a5678d15d1a9196524a35e580b98b4e8b8dc2d57d544a6c8f9520f885d1fdb");
        assertThat(stakeRegDeSaved.getCredential()).isEqualTo(stakeRegistrationCert.getStakeCredential().getHash());
    }

    @Test
    void processStakeRegistration_WhenCertTypeIsStakeDeRegistration() {
        StakeDeregistration stakeDeregistrationCert = StakeDeregistration
                .builder()
                .stakeCredential(StakeCredential.builder()
                        .type(StakeCredType.ADDR_KEYHASH)
                        .hash("42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9")
                        .build())
                .build();

        CertificateEvent certificateEvent =
                new CertificateEvent(eventMetadata(),
                        List.of(TxCertificates.builder()
                                .txHash("08a5678d15d1a9196524a35e580b98b4e8b8dc2d57d544a6c8f9520f885d1fdb")
                                .certificates(
                                        List.of(stakeDeregistrationCert))
                                .build()));

        stakeRegProcessor.processStakeRegistration(certificateEvent);

        verify(stakingStorage, times(1)).saveRegistrations(stakeRegDetailCaptor.capture());
        verify(stakingStorage, never()).saveDelegations(any());
        assertThat(stakeRegDetailCaptor.getValue()).hasSize(1);

        StakeRegistrationDetail stakeRegDeSaved = stakeRegDetailCaptor.getValue().get(0);

        assertThat(stakeRegDeSaved.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(stakeRegDeSaved.getBlockHash()).isEqualTo(eventMetadata().getBlockHash());
        assertThat(stakeRegDeSaved.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(stakeRegDeSaved.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(stakeRegDeSaved.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(stakeRegDeSaved.getAddress()).isEqualTo("stake_test1upprgdf9umls0ex79gfj3ymvjeqxuse07z4wk7jutfdxejgzkmsfe");
        assertThat(stakeRegDeSaved.getType()).isEqualTo(CertificateType.STAKE_DEREGISTRATION);
        assertThat(stakeRegDeSaved.getCertIndex()).isEqualTo(0);
        assertThat(stakeRegDeSaved.getTxHash()).isEqualTo("08a5678d15d1a9196524a35e580b98b4e8b8dc2d57d544a6c8f9520f885d1fdb");
        assertThat(stakeRegDeSaved.getCredential()).isEqualTo(stakeDeregistrationCert.getStakeCredential().getHash());
    }

    @Test
    void processStakeRegistration_WhenCertTypeIsStakeDelegation() {
        StakeDelegation stakeDelegationCert = StakeDelegation
                .builder()
                .stakeCredential(StakeCredential.builder()
                        .type(StakeCredType.SCRIPTHASH)
                        .hash("e15735f549abf814616e7e30940e9de2ed6326fed12aaaf45bd6c61d")
                        .build())
                .stakePoolId(new StakePoolId("62d3773887d1c644d4b28a5743f111597e0ad5654b71d613f40f5d03"))
                .build();

        CertificateEvent certificateEvent =
                new CertificateEvent(eventMetadata(),
                        List.of(TxCertificates.builder()
                                .txHash("08a5678d15d1a9196524a35e580b98b4e8b8dc2d57d544a6c8f9520f885d1fdb")
                                .certificates(
                                        List.of(stakeDelegationCert))
                                .build()));

        stakeRegProcessor.processStakeRegistration(certificateEvent);

        verify(stakingStorage, times(1)).saveDelegations(delegationCaptor.capture());
        verify(stakingStorage, never()).saveRegistrations(any());
        assertThat(delegationCaptor.getValue()).hasSize(1);

        Delegation delegationSaved = delegationCaptor.getValue().get(0);

        assertThat(delegationSaved.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(delegationSaved.getBlockHash()).isEqualTo(eventMetadata().getBlockHash());
        assertThat(delegationSaved.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(delegationSaved.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(delegationSaved.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(delegationSaved.getAddress()).isEqualTo("stake_test17rs4wd04fx4ls9rpdelrp9qwnh3w6cexlmgj42h5t0tvv8gjrqqjc");
        assertThat(delegationSaved.getTxHash()).isEqualTo("08a5678d15d1a9196524a35e580b98b4e8b8dc2d57d544a6c8f9520f885d1fdb");
        assertThat(delegationSaved.getCertIndex()).isEqualTo(0);
        assertThat(delegationSaved.getCredential()).isEqualTo(stakeDelegationCert.getStakeCredential().getHash());
    }

    @Test
    void handleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(78650212, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(78650198, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .build();

        stakeRegProcessor.handleRollbackEvent(rollbackEvent);

        verify(stakingStorage, times(1)).deleteRegistrationsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        verify(stakingStorage, times(1)).deleteDelegationsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .mainnet(false)
                .epochNumber(77)
                .slotLeader("8ed5ab11e76094fa2a2ab29fc3a57498d07e68f6fde6f326162eea88")
                .block(1079480L)
                .blockHash("5f834500d2e4dde1bc07feb8e00cd320c53f26fa41749f2e2b2bd0a81fa833f7")
                .blockTime(1687425248L)
                .prevBlockHash("3cc1a49034fdcc2463d3a0a4d56b052429988335d6856d8f7485fbd2f2d71383")
                .slot(31742048)
                .epochSlot(119648)
                .noOfTxs(5)
                .syncMode(false)
                .remotePublish(false)
                .build();
    }
}
