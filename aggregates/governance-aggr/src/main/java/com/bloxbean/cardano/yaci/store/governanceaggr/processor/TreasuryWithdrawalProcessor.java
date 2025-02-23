package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.TreasuryWithdrawalsAction;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.domain.*;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TreasuryWithdrawalProcessor {

    private final ProposalStateClient proposalStateClient;
    private final ApplicationEventPublisher publisher;
    private final StakingCertificateStorageReader stakingCertificateStorageReader;
    private final ObjectMapper objectMapper;

    public TreasuryWithdrawalProcessor(ProposalStateClient proposalStateClient, ApplicationEventPublisher publisher,
                                       StakingCertificateStorageReader stakingCertificateStorageReader, ObjectMapper objectMapper) {
        this.proposalStateClient = proposalStateClient;
        this.publisher = publisher;
        this.stakingCertificateStorageReader = stakingCertificateStorageReader;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void handleTreasuryWithdrawal(ProposalStatusCapturedEvent event) {
        int currentEpoch = event.getEpoch();
        long slot = event.getSlot();

        List<GovActionProposal> ratifiedProposalsInPrevEpoch =
                proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, currentEpoch);

        List<RewardRestAmt> rewardRestAmts = new ArrayList<>();
        List<RewardRestAmt> unclaimedRewardRestAmts = new ArrayList<>();

        for (var proposal : ratifiedProposalsInPrevEpoch) {
            if (proposal.getType() != GovActionType.TREASURY_WITHDRAWALS_ACTION) {
                continue;
            }

            try {
                TreasuryWithdrawalsAction treasuryWithdrawalsAction = objectMapper.treeToValue(proposal.getDetails(), TreasuryWithdrawalsAction.class);
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
            } catch (JsonProcessingException e) {
                log.error("Error converting to TreasuryWithdrawalsAction", e);
            }
        }

        if (!rewardRestAmts.isEmpty()) {
            var rewardRestEvent = RewardRestEvent.builder()
                    .earnedEpoch(currentEpoch)
                    .spendableEpoch(currentEpoch + 1)
                    .slot(slot)
                    .rewards(rewardRestAmts)
                    .build();
            publisher.publishEvent(rewardRestEvent);
        }

        if (!unclaimedRewardRestAmts.isEmpty()) {
            var unclaimedRewardRestEvent = UnclaimedRewardRestEvent.builder()
                    .earnedEpoch(currentEpoch)
                    .spendableEpoch(currentEpoch + 1)
                    .slot(slot)
                    .rewards(unclaimedRewardRestAmts)
                    .build();
            publisher.publishEvent(unclaimedRewardRestEvent);
        }
    }
}
