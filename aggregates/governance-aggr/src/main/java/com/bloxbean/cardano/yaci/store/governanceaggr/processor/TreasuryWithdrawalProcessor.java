package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction;
import com.bloxbean.cardano.yaci.core.model.governance.actions.TreasuryWithdrawalsAction;
import com.bloxbean.cardano.yaci.store.adapot.event.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.domain.*;
import com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrProperties;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TreasuryWithdrawalProcessor {
    private final GovernanceAggrProperties governanceAggrProperties;
    private final ProposalStateClient proposalStateClient;
    private final ApplicationEventPublisher publisher;
    private final StakingCertificateStorageReader stakingCertificateStorageReader;
    //This is here only for cleanup purpose. Ideally, according to domain, it should be in RewardProcessor of adapot
    //Refactor it later to remove direct dependency with RewardStorage here.
    private final RewardStorage rewardStorage;

    public TreasuryWithdrawalProcessor(GovernanceAggrProperties governanceAggrProperties, ProposalStateClient proposalStateClient, ApplicationEventPublisher publisher,
                                       StakingCertificateStorageReader stakingCertificateStorageReader, RewardStorage rewardStorage) {
        this.governanceAggrProperties = governanceAggrProperties;
        this.proposalStateClient = proposalStateClient;
        this.publisher = publisher;
        this.stakingCertificateStorageReader = stakingCertificateStorageReader;
        this.rewardStorage = rewardStorage;
    }

    @EventListener
    @Transactional
    public void handleTreasuryWithdrawal(PreAdaPotJobProcessingEvent event) {
        if (!governanceAggrProperties.isEnabled())
            return;

        //Find status of proposals in previous epoch to process ratified treasury withdrawals.
        int epoch = event.getEpoch() - 1;
        long slot = event.getSlot();

        //Delete if any existing treasury withdrawals both in reward_rest and unclaimed_reward_rest
        //earned epoch = epoch
        rewardStorage.deleteRewardRest(epoch, RewardRestType.treasury);
        rewardStorage.deleteUnclaimedRewardRest(epoch, RewardRestType.treasury);

        List<GovActionProposal> ratifiedProposalsInPrevEpoch =
                proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch);

        List<RewardRestAmt> rewardRestAmts = new ArrayList<>();
        List<RewardRestAmt> unclaimedRewardRestAmts = new ArrayList<>();

        for (var proposal : ratifiedProposalsInPrevEpoch) {
            GovAction govAction = proposal.getGovAction();

            if (govAction.getType() != GovActionType.TREASURY_WITHDRAWALS_ACTION) {
                continue;
            }

            TreasuryWithdrawalsAction treasuryWithdrawalsAction = (TreasuryWithdrawalsAction) govAction;
            var withdrawals = treasuryWithdrawalsAction.getWithdrawals();

            for (var withdrawal : withdrawals.entrySet()) {
                String addressHash = withdrawal.getKey();
                BigInteger amount = withdrawal.getValue();

                Address address = new Address(HexUtil.decodeHexString(addressHash));

                var regCert = stakingCertificateStorageReader
                        .getRegistrationByStakeAddress(address.getAddress(), slot)
                        .orElse(null);

                if (regCert == null || regCert.getType() == CertificateType.STAKE_DEREGISTRATION
                        || regCert.getType() == CertificateType.UNREG_CERT) { //account not found or deregistered
                    // TODO: verify this case
                    log.info("Address is not registered. or deregistered :{}", address.getAddress());
                    unclaimedRewardRestAmts.add(RewardRestAmt.builder()
                            .address(address.getAddress())
                            .type(RewardRestType.treasury)
                            .amount(amount)
                            .build());
                } else {
                    rewardRestAmts.add(RewardRestAmt.builder()
                            .address(address.getAddress())
                            .type(RewardRestType.treasury)
                            .amount(amount)
                            .build());
                }
            }
        }

        if (!rewardRestAmts.isEmpty()) {
            var rewardRestEvent = RewardRestEvent.builder()
                    .earnedEpoch(epoch)
                    .spendableEpoch(epoch + 1)
                    .slot(slot)
                    .rewards(rewardRestAmts)
                    .build();
            publisher.publishEvent(rewardRestEvent);
        }

        if (!unclaimedRewardRestAmts.isEmpty()) {
            var unclaimedRewardRestEvent = UnclaimedRewardRestEvent.builder()
                    .earnedEpoch(epoch)
                    .spendableEpoch(epoch + 1)
                    .slot(slot)
                    .rewards(unclaimedRewardRestAmts)
                    .build();
            publisher.publishEvent(unclaimedRewardRestEvent);
        }
    }
}
