package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class GovernanceRuleAssertionHelper {
    private final ProposalStateClient proposalStateClient;
    private final GovActionProposalStatusRepository govActionProposalStatusRepository;
    private final GovActionProposalStorage govActionProposalStorage;
    private final LedgerGovernanceStateReader ledgerGovernanceStateReader;

    public GovernanceRuleAssertionHelper(ProposalStateClient proposalStateClient,
                                         GovActionProposalStatusRepository govActionProposalStatusRepository,
                                         GovActionProposalStorage govActionProposalStorage,
                                         LedgerGovernanceStateReader ledgerGovernanceStateReader) {
        this.proposalStateClient = proposalStateClient;
        this.govActionProposalStatusRepository = govActionProposalStatusRepository;
        this.govActionProposalStorage = govActionProposalStorage;
        this.ledgerGovernanceStateReader = ledgerGovernanceStateReader;
    }

    public GovActionProposalStatusEntity assertDbStatusAtEpoch(GovActionId govActionId,
                                                               int epoch,
                                                               GovActionStatus expectedStatus) {
        AtomicReference<GovActionProposalStatusEntity> statusRef = new AtomicReference<>();

        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    Optional<GovActionProposalStatusEntity> status = findProposalStatus(govActionId, epoch);
                    assertThat(status)
                            .as("DB status row for %s at epoch %s", govActionId, epoch)
                            .hasValueSatisfying(row -> assertThat(row.getStatus()).isEqualTo(expectedStatus));
                    assertProposalStateClientContains(govActionId, epoch, expectedStatus);
                    statusRef.set(status.orElseThrow());
                });

        return statusRef.get();
    }

    public GovActionProposalStatusEntity assertLatestDbStatus(GovActionId govActionId,
                                                              GovActionStatus expectedStatus) {
        AtomicReference<GovActionProposalStatusEntity> statusRef = new AtomicReference<>();

        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    Optional<GovActionProposalStatusEntity> status = findProposalStatuses(govActionId).stream()
                            .filter(row -> row.getStatus() == expectedStatus)
                            .max(Comparator.comparing(GovActionProposalStatusEntity::getEpoch));
                    assertThat(status)
                            .as("latest %s DB status for %s", expectedStatus, govActionId)
                            .isPresent();
                    GovActionProposalStatusEntity row = status.orElseThrow();
                    assertProposalStateClientContains(govActionId, row.getEpoch(), expectedStatus);
                    statusRef.set(row);
                });

        return statusRef.get();
    }

    public ProposalVotingStats assertVotingStats(GovActionId govActionId,
                                                 int epoch,
                                                 Consumer<ProposalVotingStats> assertions) {
        AtomicReference<ProposalVotingStats> votingStatsRef = new AtomicReference<>();

        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    ProposalVotingStats votingStats = findProposalStatus(govActionId, epoch)
                            .map(GovActionProposalStatusEntity::getVotingStats)
                            .orElse(null);
                    assertThat(votingStats)
                            .as("voting stats for %s at epoch %s", govActionId, epoch)
                            .isNotNull();
                    assertions.accept(votingStats);
                    votingStatsRef.set(votingStats);
                });

        return votingStatsRef.get();
    }

    public ProposalLedgerSnapshot assertLedgerSnapshotMatchesDb(GovActionId govActionId, GovActionStatus dbStatus) {
        AtomicReference<ProposalLedgerSnapshot> snapshotRef = new AtomicReference<>();

        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    ProposalLedgerSnapshot snapshot = ledgerGovernanceStateReader.fetchProposalState(govActionId);
                    snapshotRef.set(snapshot);
                    assertLedgerSnapshotMatchesStatus(snapshot, dbStatus);
                });

        return snapshotRef.get();
    }

    public GovActionProposalStatusEntity assertMatchesLedger(GovActionId govActionId, GovActionStatus expectedStatus) {
        GovActionProposalStatusEntity status = assertLatestDbStatus(govActionId, expectedStatus);
        assertLedgerSnapshotMatchesDb(govActionId, expectedStatus);
        return status;
    }

    public GovActionProposal findProposal(GovActionId govActionId) {
        return findProposalOptional(govActionId).orElseThrow();
    }

    public Optional<GovActionProposal> findProposalOptional(GovActionId govActionId) {
        return govActionProposalStorage.findByGovActionIds(List.of(govActionId))
                .stream()
                .findFirst();
    }

    public Optional<GovActionProposalStatusEntity> findProposalStatus(GovActionId govActionId, int epoch) {
        return govActionProposalStatusRepository.findAll()
                .stream()
                .filter(status -> govActionId.getTransactionId().equals(status.getGovActionTxHash()))
                .filter(status -> govActionId.getGov_action_index() == status.getGovActionIndex())
                .filter(status -> epoch == status.getEpoch())
                .findFirst();
    }

    public List<GovActionProposalStatusEntity> findProposalStatuses(GovActionId govActionId) {
        return govActionProposalStatusRepository.findAll()
                .stream()
                .filter(status -> govActionId.getTransactionId().equals(status.getGovActionTxHash()))
                .filter(status -> govActionId.getGov_action_index() == status.getGovActionIndex())
                .sorted(Comparator.comparing(GovActionProposalStatusEntity::getEpoch))
                .toList();
    }

    private void assertProposalStateClientContains(GovActionId govActionId,
                                                   int epoch,
                                                   GovActionStatus expectedStatus) {
        assertThat(proposalStateClient.getProposalsByStatusAndEpoch(expectedStatus, epoch))
                .as("ProposalStateClient %s proposals at epoch %s", expectedStatus, epoch)
                .anySatisfy(proposal -> {
                    assertThat(proposal.getTxHash()).isEqualTo(govActionId.getTransactionId());
                    assertThat(proposal.getIndex()).isEqualTo(govActionId.getGov_action_index());
                });
    }

    private void assertLedgerSnapshotMatchesStatus(ProposalLedgerSnapshot snapshot, GovActionStatus dbStatus) {
        if (dbStatus == GovActionStatus.RATIFIED) {
            assertThat(snapshot.presentInEnactedGovActions() || snapshot.removedFromCurrentProposals())
                    .as("ledger ratified/enacted fact for %s: %s", snapshot.govActionId(), snapshot)
                    .isTrue();
            assertThat(snapshot.presentInExpiredGovActions())
                    .as("ratified proposal must not be in expired ledger facts: %s", snapshot)
                    .isFalse();
        } else if (dbStatus == GovActionStatus.EXPIRED) {
            assertThat(snapshot.presentInExpiredGovActions() || snapshot.removedFromCurrentProposals())
                    .as("ledger expired fact for %s: %s", snapshot.govActionId(), snapshot)
                    .isTrue();
            assertThat(snapshot.presentInEnactedGovActions())
                    .as("expired proposal must not be in enacted ledger facts: %s", snapshot)
                    .isFalse();
        } else if (dbStatus == GovActionStatus.ACTIVE) {
            assertThat(snapshot.presentInCurrentProposals())
                    .as("ledger active fact for %s: %s", snapshot.govActionId(), snapshot)
                    .isTrue();
        }
    }
}
