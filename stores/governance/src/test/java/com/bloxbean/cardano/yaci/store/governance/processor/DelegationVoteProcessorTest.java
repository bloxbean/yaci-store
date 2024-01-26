package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.certs.*;
import com.bloxbean.cardano.yaci.core.model.governance.Drep;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.storage.DelegationVoteStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DelegationVoteProcessorTest {

    @Mock
    private DelegationVoteStorage delegationVoteStorage;
    @InjectMocks
    private DelegationVoteProcessor delegationVoteProcessor;
    @Captor
    private ArgumentCaptor<List<DelegationVote>> delegationVotesCaptor;

    @Test
    void testHandleDelegationVote_WhenCertIsVoteDelegCert() {
        final String txHash = "498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f";
        final CertificateEvent certificateEvent = CertificateEvent.builder()
                .metadata(eventMetadata())
                .txCertificatesList(List.of(TxCertificates.builder()
                        .txHash(txHash)
                        .certificates(List.of(
                                VoteDelegCert.builder()
                                        .stakeCredential(StakeCredential.builder()
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .hash("42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9")
                                                .build())
                                        .drep(Drep.addrKeyHash("d1e89233461ca210819328b840ec79ec825b93d9b0501749dea1ecb8"))
                                        .build()
                        ))
                        .build()))
                .build();

        delegationVoteProcessor.handleDelegationVote(certificateEvent);

        verify(delegationVoteStorage).saveAll(delegationVotesCaptor.capture());
        assertThat(delegationVotesCaptor.getValue()).hasSize(1);

        DelegationVote savedDelegationVote = delegationVotesCaptor.getValue().get(0);

        assertThat(savedDelegationVote.getTxHash()).isEqualTo(txHash);
        assertThat(savedDelegationVote.getAddress()).isEqualTo("stake_test1upprgdf9umls0ex79gfj3ymvjeqxuse07z4wk7jutfdxejgzkmsfe");
        assertThat(savedDelegationVote.getDrepHash()).isEqualTo("d1e89233461ca210819328b840ec79ec825b93d9b0501749dea1ecb8");
        assertThat(savedDelegationVote.getDrepId()).isEqualTo("drep1685fyv6xrj3ppqvn9zuypmreajp9hy7ekpgpwjw758ktstncpw9");
        assertThat(savedDelegationVote.getCertIndex()).isEqualTo(0);
        assertThat(savedDelegationVote.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(savedDelegationVote.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(savedDelegationVote.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(savedDelegationVote.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
    }

    @Test
    void testHandleDelegationVote_WhenCertIsVoteRegDelegCert() {
        final String txHash = "498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f";
        final CertificateEvent certificateEvent = CertificateEvent.builder()
                .metadata(eventMetadata())
                .txCertificatesList(List.of(TxCertificates.builder()
                        .txHash(txHash)
                        .certificates(List.of(
                                VoteRegDelegCert.builder()
                                        .stakeCredential(StakeCredential.builder()
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .hash("42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9")
                                                .build())
                                        .drep(Drep.addrKeyHash("d1e89233461ca210819328b840ec79ec825b93d9b0501749dea1ecb8"))
                                        .build()
                        ))
                        .build()))
                .build();

        delegationVoteProcessor.handleDelegationVote(certificateEvent);

        verify(delegationVoteStorage).saveAll(delegationVotesCaptor.capture());
        assertThat(delegationVotesCaptor.getValue()).hasSize(1);

        DelegationVote savedDelegationVote = delegationVotesCaptor.getValue().get(0);

        assertThat(savedDelegationVote.getTxHash()).isEqualTo(txHash);
        assertThat(savedDelegationVote.getAddress()).isEqualTo("stake_test1upprgdf9umls0ex79gfj3ymvjeqxuse07z4wk7jutfdxejgzkmsfe");
        assertThat(savedDelegationVote.getDrepHash()).isEqualTo("d1e89233461ca210819328b840ec79ec825b93d9b0501749dea1ecb8");
        assertThat(savedDelegationVote.getDrepId()).isEqualTo("drep1685fyv6xrj3ppqvn9zuypmreajp9hy7ekpgpwjw758ktstncpw9");
        assertThat(savedDelegationVote.getCertIndex()).isEqualTo(0);
        assertThat(savedDelegationVote.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(savedDelegationVote.getCredential()).isEqualTo("42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9");
        assertThat(savedDelegationVote.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(savedDelegationVote.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(savedDelegationVote.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
    }

    @Test
    void testHandleDelegationVote_WhenCertIsStakeVoteDelegCert() {
        final String txHash = "498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f";
        final CertificateEvent certificateEvent = CertificateEvent.builder()
                .metadata(eventMetadata())
                .txCertificatesList(List.of(TxCertificates.builder()
                        .txHash(txHash)
                        .certificates(List.of(
                                StakeVoteDelegCert.builder()
                                        .stakeCredential(StakeCredential.builder()
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .hash("42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9")
                                                .build())
                                        .drep(Drep.addrKeyHash("d1e89233461ca210819328b840ec79ec825b93d9b0501749dea1ecb8"))
                                        .build()
                        ))
                        .build()))
                .build();

        delegationVoteProcessor.handleDelegationVote(certificateEvent);

        verify(delegationVoteStorage).saveAll(delegationVotesCaptor.capture());
        assertThat(delegationVotesCaptor.getValue()).hasSize(1);

        DelegationVote savedDelegationVote = delegationVotesCaptor.getValue().get(0);

        assertThat(savedDelegationVote.getTxHash()).isEqualTo(txHash);
        assertThat(savedDelegationVote.getAddress()).isEqualTo("stake_test1upprgdf9umls0ex79gfj3ymvjeqxuse07z4wk7jutfdxejgzkmsfe");
        assertThat(savedDelegationVote.getDrepHash()).isEqualTo("d1e89233461ca210819328b840ec79ec825b93d9b0501749dea1ecb8");
        assertThat(savedDelegationVote.getDrepId()).isEqualTo("drep1685fyv6xrj3ppqvn9zuypmreajp9hy7ekpgpwjw758ktstncpw9");
        assertThat(savedDelegationVote.getCertIndex()).isEqualTo(0);
        assertThat(savedDelegationVote.getCredential()).isEqualTo("42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9");
        assertThat(savedDelegationVote.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(savedDelegationVote.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(savedDelegationVote.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(savedDelegationVote.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
    }

    @Test
    void testHandleDelegationVote_WhenCertIsStakeVoteRegDelegCert() {
        final String txHash = "498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f";
        final CertificateEvent certificateEvent = CertificateEvent.builder()
                .metadata(eventMetadata())
                .txCertificatesList(List.of(TxCertificates.builder()
                        .txHash(txHash)
                        .certificates(List.of(
                                StakeVoteRegDelegCert.builder()
                                        .stakeCredential(StakeCredential.builder()
                                                .type(StakeCredType.ADDR_KEYHASH)
                                                .hash("42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9")
                                                .build())
                                        .drep(Drep.addrKeyHash("d1e89233461ca210819328b840ec79ec825b93d9b0501749dea1ecb8"))
                                        .build()
                        ))
                        .build()))
                .build();

        delegationVoteProcessor.handleDelegationVote(certificateEvent);

        verify(delegationVoteStorage).saveAll(delegationVotesCaptor.capture());
        assertThat(delegationVotesCaptor.getValue()).hasSize(1);

        DelegationVote savedDelegationVote = delegationVotesCaptor.getValue().get(0);

        assertThat(savedDelegationVote.getTxHash()).isEqualTo(txHash);
        assertThat(savedDelegationVote.getAddress()).isEqualTo("stake_test1upprgdf9umls0ex79gfj3ymvjeqxuse07z4wk7jutfdxejgzkmsfe");
        assertThat(savedDelegationVote.getDrepHash()).isEqualTo("d1e89233461ca210819328b840ec79ec825b93d9b0501749dea1ecb8");
        assertThat(savedDelegationVote.getDrepId()).isEqualTo("drep1685fyv6xrj3ppqvn9zuypmreajp9hy7ekpgpwjw758ktstncpw9");
        assertThat(savedDelegationVote.getCertIndex()).isEqualTo(0);
        assertThat(savedDelegationVote.getCredential()).isEqualTo("42343525e6ff07e4de2a1328936c96406e432ff0aaeb7a5c5a5a6cc9");
        assertThat(savedDelegationVote.getEpoch()).isEqualTo(eventMetadata().getEpochNumber());
        assertThat(savedDelegationVote.getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(savedDelegationVote.getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(savedDelegationVote.getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
    }

    @Test
    void testHandleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(78650212, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(78650198, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .build();

        delegationVoteProcessor.handleRollbackEvent(rollbackEvent);
        verify(delegationVoteStorage, times(1)).deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .epochNumber(50)
                .block(100L)
                .blockTime(99999L)
                .slot(10000L)
                .mainnet(false)
                .build();
    }

}
