package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.adapot.event.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestAmt;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.ProposalMapper;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalRefundProcessorTest {
    @Mock
    private GovActionProposalStorage govActionProposalStorage;

    @Mock
    private ProposalStateClient proposalStateClient;

    @Mock
    private ProposalMapper proposalMapper;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private StakingCertificateStorageReader stakingCertificateStorageReader;

    @Mock
    private RewardStorage rewardStorage;

    @InjectMocks
    private ProposalRefundProcessor proposalRefundProcessor;

    @Test
    void testHandleProposalStatusCapturedEvent() {
        PreAdaPotJobProcessingEvent event = new PreAdaPotJobProcessingEvent(2, 100);

        Proposal proposal1 = new Proposal(GovActionId.builder()
                .gov_action_index(0)
                .transactionId("aaaaaaaaaade062ade1e70d0040abde47a6626c7d8e98816a9d87e6bd6228b45")
                .build(),
                null, GovActionType.PARAMETER_CHANGE_ACTION);

        Proposal proposal2 = new Proposal(GovActionId.builder()
                .gov_action_index(0)
                .transactionId("bbbbbbbbbbde062ade1e70d0040abde47a6626c7d8e98816a9d87e6bd6228b45")
                .build(), null, GovActionType.PARAMETER_CHANGE_ACTION);

        Proposal proposal3 = new Proposal(GovActionId.builder()
                .gov_action_index(0)
                .transactionId("ccccccccccde062ade1e70d0040abde47a6626c7d8e98816a9d87e6bd6228b45")
                .build(), null, GovActionType.UPDATE_COMMITTEE);

        Proposal proposal4 = new Proposal(GovActionId.builder()
                .gov_action_index(0)
                .transactionId("ddddddddddde062ade1e70d0040abde47a6626c7d8e98816a9d87e6bd6228b45")
                .build(),
                GovActionId.builder()
                .gov_action_index(0)
                .transactionId("ccccccccccde062ade1e70d0040abde47a6626c7d8e98816a9d87e6bd6228b45")
                .build(), GovActionType.UPDATE_COMMITTEE);

        Proposal proposal5 = new Proposal(GovActionId.builder()
                .gov_action_index(0)
                .transactionId("eeeeeeeeeee062ade1e70d0040abde47a6626c7d8e98816a9d87e6bd6228b45")
                .build(), null, GovActionType.NEW_CONSTITUTION);

        GovActionProposal govProposal1 = new GovActionProposal();
        govProposal1.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9aaaaa");
        govProposal1.setDeposit(BigInteger.valueOf(1000));

        GovActionProposal govProposal2 = new GovActionProposal();
        govProposal2.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9bbbbb");
        govProposal2.setDeposit(BigInteger.valueOf(2000));

        GovActionProposal govProposal3 = new GovActionProposal();
        govProposal3.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9ccccc");
        govProposal3.setDeposit(BigInteger.valueOf(3000));

        GovActionProposal govProposal4 = new GovActionProposal();
        govProposal4.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9ddddd");
        govProposal4.setDeposit(BigInteger.valueOf(4000));

        GovActionProposal govProposal5 = new GovActionProposal();
        govProposal5.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9eeeee");
        govProposal5.setDeposit(BigInteger.valueOf(5000));

        com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal domainGovProposal1 =
                new com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal();
        domainGovProposal1.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9aaaaa");
        domainGovProposal1.setDeposit(BigInteger.valueOf(1000));

        com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal domainGovProposal2 =
                new com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal();
        domainGovProposal2.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9bbbbb");
        domainGovProposal2.setDeposit(BigInteger.valueOf(2000));

        com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal domainGovProposal3 =
                new com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal();
        domainGovProposal3.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9ccccc");
        domainGovProposal3.setDeposit(BigInteger.valueOf(3000));

        com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal domainGovProposal4 =
                new com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal();
        domainGovProposal4.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9ddddd");
        domainGovProposal4.setDeposit(BigInteger.valueOf(4000));

        com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal domainGovProposal5 =
                new com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal();
        domainGovProposal5.setReturnAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9eeeee");
        domainGovProposal5.setDeposit(BigInteger.valueOf(5000));

        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.EXPIRED, 1))
                .thenReturn(List.of(govProposal1));
        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, 1))
                .thenReturn(List.of(govProposal3));
        when(proposalStateClient.getProposalsByStatusListAndEpoch(anyList(), eq(1)))
                .thenReturn(List.of(govProposal1, govProposal2, govProposal3, govProposal4, govProposal5));

        when(proposalMapper.toProposal(govProposal1)).thenReturn(proposal1);
        when(proposalMapper.toProposal(govProposal2)).thenReturn(proposal2);
        when(proposalMapper.toProposal(govProposal3)).thenReturn(proposal3);
        when(proposalMapper.toProposal(govProposal4)).thenReturn(proposal4);
        when(proposalMapper.toProposal(govProposal5)).thenReturn(proposal5);

        when(govActionProposalStorage.findByGovActionIds(anyList())).thenReturn(
                List.of(domainGovProposal1, domainGovProposal3, domainGovProposal4));

        StakeRegistrationDetail stakeRegistrationDetail1 = Mockito.mock(StakeRegistrationDetail.class);

        StakeRegistrationDetail stakeRegistrationDetail4 = Mockito.mock(StakeRegistrationDetail.class);

        when(stakingCertificateStorageReader
                .getRegistrationByStakeAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9aaaaa", 100L))
                .thenReturn(Optional.of(stakeRegistrationDetail1));
        when(stakingCertificateStorageReader
                .getRegistrationByStakeAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9ccccc",100L))
                .thenReturn(Optional.empty());
        when(stakingCertificateStorageReader
                .getRegistrationByStakeAddress("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9ddddd",100L))
                .thenReturn(Optional.of(stakeRegistrationDetail4));

        proposalRefundProcessor.handleProposalRefund(event);

        ArgumentCaptor<List<GovActionId>> govActionIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(govActionProposalStorage).findByGovActionIds(govActionIdsCaptor.capture());
        List<GovActionId> capturedGovActionIds = govActionIdsCaptor.getValue();

        assertEquals(3, capturedGovActionIds.size());
        assertThat(capturedGovActionIds).containsExactlyInAnyOrder(proposal1.getGovActionId(),
                proposal3.getGovActionId(), proposal4.getGovActionId());

        ArgumentCaptor<RewardRestEvent> eventCaptor = ArgumentCaptor.forClass(RewardRestEvent.class);
        verify(publisher, times(1)).publishEvent(eventCaptor.capture());

        RewardRestEvent capturedEvent = eventCaptor.getValue();
        List<RewardRestAmt> rewards = capturedEvent.getRewards();

        assertEquals(2, rewards.size());
        assertThat(rewards).extracting(RewardRestAmt::getAddress).containsExactlyInAnyOrder("stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9aaaaa",
                "stake_test1upnakjguet3zc7qzrw54p3nc3j8c7pd5v4w8x5evdzseygs9ddddd");
        assertThat(rewards).extracting(RewardRestAmt::getAmount).containsExactlyInAnyOrder(BigInteger.valueOf(1000), BigInteger.valueOf(4000));
    }

}
