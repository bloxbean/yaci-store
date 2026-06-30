package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalStatus;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.ProposalApiService;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class GovernanceAssertionHelper {
    private final GovActionProposalStorage govActionProposalStorage;
    private final GovActionProposalStatusRepository govActionProposalStatusRepository;
    private final ProposalApiService proposalApiService;

    public GovernanceAssertionHelper(GovActionProposalStorage govActionProposalStorage,
                                     GovActionProposalStatusRepository govActionProposalStatusRepository,
                                     ProposalApiService proposalApiService) {
        this.govActionProposalStorage = govActionProposalStorage;
        this.govActionProposalStatusRepository = govActionProposalStatusRepository;
        this.proposalApiService = proposalApiService;
    }

    public record StatusEpochs(int createdEpoch, int maxVotingEpoch, int expiryStatusEpoch) {
    }

    public GovActionProposalStatusEntity assertStatusAtEpoch(String txHash,
                                                             int index,
                                                             int epoch,
                                                             GovActionStatus expectedStatus) {
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(findProposalStatus(txHash, index, epoch))
                        .hasValueSatisfying(status -> assertThat(status.getStatus()).isEqualTo(expectedStatus)));

        return findProposalStatus(txHash, index, epoch).orElseThrow();
    }

    public void assertNoStatusAtEpoch(String txHash, int index, int epoch) {
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(findProposalStatus(txHash, index, epoch)).isEmpty());
    }

    public ProposalDto assertLatestApiStatus(String txHash, int index, ProposalStatus expectedStatus) {
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(proposalApiService.getProposalById(txHash, index))
                        .hasValueSatisfying(proposal -> assertThat(proposal.getStatus()).isEqualTo(expectedStatus)));

        return proposalApiService.getProposalById(txHash, index).orElseThrow();
    }

    public ProposalVotingStats assertVotingStats(String txHash,
                                                 int index,
                                                 int epoch,
                                                 Consumer<ProposalVotingStats> votingStatsAssertions) {
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(findProposalStatus(txHash, index, epoch))
                        .hasValueSatisfying(status -> assertThat(status.getVotingStats()).isNotNull()));

        var status = findProposalStatus(txHash, index, epoch).orElseThrow();
        votingStatsAssertions.accept(status.getVotingStats());
        return status.getVotingStats();
    }

    public GovActionProposal findProposal(GovActionId govActionId) {
        return findProposalOptional(govActionId).orElseThrow();
    }

    public Optional<GovActionProposal> findProposalOptional(GovActionId govActionId) {
        return govActionProposalStorage.findByGovActionIds(List.of(govActionId))
                .stream()
                .findFirst();
    }

    public GovActionProposalStatusEntity findProposalStatus(GovActionId govActionId, int epoch) {
        return findProposalStatus(govActionId.getTransactionId(), govActionId.getGov_action_index(), epoch).orElseThrow();
    }

    public Optional<GovActionProposalStatusEntity> findProposalStatus(String txHash, int index, int epoch) {
        return govActionProposalStatusRepository.findAll()
                .stream()
                .filter(status -> txHash.equals(status.getGovActionTxHash()))
                .filter(status -> index == status.getGovActionIndex())
                .filter(status -> epoch == status.getEpoch())
                .findFirst();
    }

    public StatusEpochs expectedStatusEpochs(GovActionProposal proposal, int govActionLifetime) {
        int createdEpoch = proposal.getEpoch();
        return new StatusEpochs(createdEpoch, createdEpoch + govActionLifetime, createdEpoch + govActionLifetime + 1);
    }
}
