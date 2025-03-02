package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NewConstitution;
import com.bloxbean.cardano.yaci.core.model.governance.actions.TreasuryWithdrawalsAction;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.domain.*;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreasuryWithdrawalProcessorTest {

    @Mock
    private ProposalStateClient proposalStateClient;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private StakingCertificateStorageReader stakingCertificateStorageReader;

    private TreasuryWithdrawalProcessor treasuryWithdrawalProcessor;

    @BeforeEach
    void setUp() {
        treasuryWithdrawalProcessor = new TreasuryWithdrawalProcessor(proposalStateClient, publisher, stakingCertificateStorageReader);
    }

    @Test
    void testHandleTreasuryWithdrawal_noEnactedProposals_found() {
        ProposalStatusCapturedEvent event = ProposalStatusCapturedEvent.builder()
                .epoch(100)
                .slot(1234L)
                .build();

        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, 100))
                .thenReturn(Collections.emptyList());

        treasuryWithdrawalProcessor.handleTreasuryWithdrawal(event);

        verify(publisher, never()).publishEvent(any(RewardRestEvent.class));
        verify(publisher, never()).publishEvent(any(UnclaimedRewardRestEvent.class));
    }

    @Test
    void testHandleTreasuryWithdrawal_proposalsButNoTreasuryType_noEventsPublished() {
        ProposalStatusCapturedEvent event = ProposalStatusCapturedEvent.builder()
                .epoch(100)
                .slot(1234L)
                .build();

        GovActionProposal ratifiedProposalInPrevEpoch = new GovActionProposal();

        ratifiedProposalInPrevEpoch.setEpoch(100);
        ratifiedProposalInPrevEpoch.setGovAction(NewConstitution.builder().build());

        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, 100))
                .thenReturn(Collections.singletonList(ratifiedProposalInPrevEpoch));

        treasuryWithdrawalProcessor.handleTreasuryWithdrawal(event);

        verify(publisher, never()).publishEvent(any(RewardRestEvent.class));
        verify(publisher, never()).publishEvent(any(UnclaimedRewardRestEvent.class));
    }

    @Test
    void testHandleTreasuryWithdrawal_treasuryWithdrawalProposal_someAddresses() throws Exception {
        int epoch = 100;
        long slot = 2000;

        ProposalStatusCapturedEvent event = ProposalStatusCapturedEvent.builder()
                .epoch(epoch)
                .slot(slot)
                .build();

        Map<String, BigInteger> withdrawalsMap = new HashMap<>();

        String addressA = "e0720bce084cdd9ca5c9ee13a69052a24b53031aba67258e777e45cf03"; // stake_test1upeqhnsgfnweefwfacf6dyzj5f94xqc6hfnjtrnh0ezu7qclg7p6m
        String addressB = "e003a6388659a93f8d463fbd1537b9039c72e76c623158b4c8c061dea8"; // stake_test1uqp6vwyxtx5nlr2x87732daeqww89emvvgc43dxgcpsaa2q6klkvm
        withdrawalsMap.put(addressA, BigInteger.valueOf(1000));
        withdrawalsMap.put(addressB, BigInteger.valueOf(2000));

        TreasuryWithdrawalsAction treasuryWithdrawalsAction = TreasuryWithdrawalsAction.builder()
                .withdrawals(withdrawalsMap)
                .policyHash("186e32faa80a26810392fda6d559c7ed4721a65ce1c9d4ef3e1c87b4")
                .build();

        GovActionProposal proposal = new GovActionProposal();

        proposal.setEpoch(epoch);
        proposal.setGovAction(treasuryWithdrawalsAction);

        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch))
                .thenReturn(Collections.singletonList(proposal));
        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch))
                .thenReturn(Collections.singletonList(proposal));

        StakeRegistrationDetail stakingCertA = new StakeRegistrationDetail();
        stakingCertA.setType(CertificateType.STAKE_REGISTRATION);

        when(stakingCertificateStorageReader.getRegistrationByStakeAddress(anyString(), eq(slot)))
                .thenAnswer(invocation -> {
                    String stakeAddr = invocation.getArgument(0, String.class);
                    if (stakeAddr.equals("stake_test1upeqhnsgfnweefwfacf6dyzj5f94xqc6hfnjtrnh0ezu7qclg7p6m")) {
                        return Optional.of(stakingCertA);
                    } else {
                        return Optional.empty();
                    }
                });

        treasuryWithdrawalProcessor.handleTreasuryWithdrawal(event);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(2)).publishEvent(eventCaptor.capture());

        List<Object> publishedEvents = eventCaptor.getAllValues();
        assertEquals(2, publishedEvents.size());

        RewardRestEvent rewardEvent = null;
        UnclaimedRewardRestEvent unclaimedEvent = null;

        for (Object e : publishedEvents) {
            if (e instanceof RewardRestEvent) {
                rewardEvent = (RewardRestEvent) e;
            } else if (e instanceof UnclaimedRewardRestEvent) {
                unclaimedEvent = (UnclaimedRewardRestEvent) e;
            }
        }

        assertNotNull(rewardEvent);
        assertEquals(epoch, rewardEvent.getEarnedEpoch());
        assertEquals(epoch + 1, rewardEvent.getSpendableEpoch());
        assertEquals(slot, rewardEvent.getSlot());
        assertEquals(1, rewardEvent.getRewards().size());
        RewardRestAmt rrA = rewardEvent.getRewards().get(0);
        assertEquals("stake_test1upeqhnsgfnweefwfacf6dyzj5f94xqc6hfnjtrnh0ezu7qclg7p6m", rrA.getAddress());
        assertEquals(BigInteger.valueOf(1000), rrA.getAmount());
        assertEquals(RewardRestType.treasury, rrA.getType());

        assertNotNull(unclaimedEvent);
        assertEquals(epoch, unclaimedEvent.getEarnedEpoch());
        assertEquals(epoch + 1, unclaimedEvent.getSpendableEpoch());
        assertEquals(slot, unclaimedEvent.getSlot());
        assertEquals(1, unclaimedEvent.getRewards().size());
        RewardRestAmt rrB = unclaimedEvent.getRewards().get(0);
        assertEquals("stake_test1uqp6vwyxtx5nlr2x87732daeqww89emvvgc43dxgcpsaa2q6klkvm", rrB.getAddress());
        assertEquals(BigInteger.valueOf(2000), rrB.getAmount());
        assertEquals(RewardRestType.treasury, rrB.getType());
    }
}
