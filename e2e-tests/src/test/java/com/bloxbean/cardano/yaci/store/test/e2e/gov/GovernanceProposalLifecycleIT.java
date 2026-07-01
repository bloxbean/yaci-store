package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalStatus;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.ProposalApiService;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;
import com.bloxbean.cardano.yaci.store.test.e2e.common.BaseE2ETest;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceAssertionHelper;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceTxHelper;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceTxHelper.CreatedProposal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises proposal status behavior across real DevKit epoch boundaries and the API layer's
 * status translation rules.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GovernanceProposalLifecycleIT extends BaseE2ETest {
    private static final int EPOCH_LENGTH_SECONDS = 20;
    private static final int GOV_ACTION_LIFETIME = 3;
    private static final String DEFAULT_ANCHOR_HASH = "cafef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937";
    private static final AtomicInteger SYNTHETIC_ID = new AtomicInteger(1);

    private GovernanceTxHelper governanceTxHelper;
    private GovernanceAssertionHelper governanceAssertionHelper;

    @Autowired
    private GovActionProposalStorage govActionProposalStorage;

    @Autowired
    private GovActionProposalStatusRepository proposalStatusRepository;

    @Autowired
    private AdaPotJobRepository adaPotJobRepository;

    @Autowired
    private AdaPotJobStorage adaPotJobStorage;

    @Autowired
    private RewardRestRepository rewardRestRepository;

    @Autowired
    private ProposalApiService proposalApiService;

    @Autowired
    private BlockStorage blockStorage;

    @BeforeAll
    void setup() {
        governanceTxHelper = new GovernanceTxHelper(backendService, govActionProposalStorage, GOV_ACTION_LIFETIME);
        governanceAssertionHelper = new GovernanceAssertionHelper(govActionProposalStorage, proposalStatusRepository, proposalApiService);

        assertDevKitAdminAvailable();
        createDevNet(votedGovernanceConfig(EPOCH_LENGTH_SECONDS, GOV_ACTION_LIFETIME));
    }

    /**
     * Uses real governance transactions to verify proposal lifecycle rows remain correct as later
     * AdaPot jobs and epoch-boundary evaluations continue to arrive.
     */
    @Test
    @Order(1)
    void devnetProposals_shouldExpireRefundAndRemainStableAcrossEpochDrift() {
        governanceTxHelper.registerStakeAddress(account0, account0.stakeAddress());

        CreatedProposal infoProposal = governanceTxHelper.createInfoProposalAndWait(account0, account0.stakeAddress());
        assertIndexedProposal(infoProposal, GovActionType.INFO_ACTION);

        waitForEpoch(infoProposal.createdEpoch() + 1);
        CreatedProposal parameterChangeProposal = governanceTxHelper.createProposalAndWait(
                account0,
                account0.stakeAddress(),
                GovernanceTxHelper.parameterChangeAction());
        assertIndexedProposal(parameterChangeProposal, GovActionType.PARAMETER_CHANGE_ACTION);
        assertThat(parameterChangeProposal.createdEpoch()).isGreaterThan(infoProposal.createdEpoch());

        int firstStatusEpoch = infoProposal.createdEpoch() + 1;
        int lastStatusEpoch = Math.max(infoProposal.expiryStatusEpoch(), parameterChangeProposal.expiryStatusEpoch()) + 1;
        List<CreatedProposal> proposals = List.of(infoProposal, parameterChangeProposal);

        // Check the status history after each completed AdaPot job, not only at the final epoch.
        for (int epoch = firstStatusEpoch; epoch <= lastStatusEpoch; epoch++) {
            waitTillAdaPotJobDone(adaPotJobRepository, epoch, statusDiagnostics(proposals));

            for (CreatedProposal proposal : proposals) {
                if (epoch > proposal.createdEpoch()) {
                    assertLatestStatusAfterCompletedJob(proposal, epoch);
                }
            }

            assertNoDuplicateStatusRows(proposals);
        }

        assertActiveUntilLastOpportunity(infoProposal);
        assertExpiredAtLifecycleBoundary(infoProposal);
        assertProposalRefund(infoProposal, account0.stakeAddress());
        governanceAssertionHelper.assertLatestApiStatus(infoProposal.txHash(), infoProposal.index(), ProposalStatus.EXPIRED);

        assertActiveUntilLastOpportunity(parameterChangeProposal);
        assertExpiredAtLifecycleBoundary(parameterChangeProposal);
        governanceAssertionHelper.assertLatestApiStatus(parameterChangeProposal.txHash(), parameterChangeProposal.index(), ProposalStatus.EXPIRED);
    }

    /**
     * Persists controlled proposal-status rows to isolate API status translation from transaction
     * construction and ledger ratification rules.
     */
    @Test
    @Order(2)
    void getProposalById_shouldTranslateCurrentAndStaleStorageStatuses() {
        int latestCompletedEpoch = latestCompletedAdaPotEpoch();
        assertThat(latestCompletedEpoch).isGreaterThan(0);
        int staleEpoch = latestCompletedEpoch - 1;

        GovActionProposal currentActive = saveSyntheticProposal("api-current-active", latestCompletedEpoch, GovActionType.INFO_ACTION);
        saveStatus(currentActive, latestCompletedEpoch, GovActionStatus.ACTIVE);
        governanceAssertionHelper.assertLatestApiStatus(currentActive.getTxHash(), (int) currentActive.getIndex(), ProposalStatus.LIVE);

        GovActionProposal expired = saveSyntheticProposal("api-expired", staleEpoch, GovActionType.INFO_ACTION);
        saveStatus(expired, staleEpoch, GovActionStatus.EXPIRED);
        governanceAssertionHelper.assertLatestApiStatus(expired.getTxHash(), (int) expired.getIndex(), ProposalStatus.EXPIRED);

        GovActionProposal staleActive = saveSyntheticProposal("api-stale-active", staleEpoch, GovActionType.PARAMETER_CHANGE_ACTION);
        saveStatus(staleActive, staleEpoch, GovActionStatus.ACTIVE);
        governanceAssertionHelper.assertLatestApiStatus(staleActive.getTxHash(), (int) staleActive.getIndex(), ProposalStatus.DROPPED);

        GovActionProposal currentRatified = saveSyntheticProposal("api-current-ratified", latestCompletedEpoch, GovActionType.PARAMETER_CHANGE_ACTION);
        saveStatus(currentRatified, latestCompletedEpoch, GovActionStatus.RATIFIED);
        governanceAssertionHelper.assertLatestApiStatus(currentRatified.getTxHash(), (int) currentRatified.getIndex(), ProposalStatus.RATIFIED);

        GovActionProposal staleRatified = saveSyntheticProposal("api-stale-ratified", staleEpoch, GovActionType.PARAMETER_CHANGE_ACTION);
        saveStatus(staleRatified, staleEpoch, GovActionStatus.RATIFIED);
        governanceAssertionHelper.assertLatestApiStatus(staleRatified.getTxHash(), (int) staleRatified.getIndex(), ProposalStatus.ENACTED);
    }

    /**
     * Verifies current-epoch proposal queries return the expected proposals for each storage status
     * filter and expose the public API status names.
     */
    @Test
    @Order(3)
    void getCurrentProposals_shouldRespectStatusFilter() {
        int currentEpoch = latestIndexedEpoch();

        GovActionProposal active = saveSyntheticProposal("current-active", currentEpoch, GovActionType.INFO_ACTION);
        GovActionProposal ratified = saveSyntheticProposal("current-ratified", currentEpoch, GovActionType.PARAMETER_CHANGE_ACTION);
        GovActionProposal expired = saveSyntheticProposal("current-expired", currentEpoch, GovActionType.INFO_ACTION);

        saveStatus(active, currentEpoch, GovActionStatus.ACTIVE);
        saveStatus(ratified, currentEpoch, GovActionStatus.RATIFIED);
        saveStatus(expired, currentEpoch, GovActionStatus.EXPIRED);

        assertCurrentProposals(null, List.of(active, ratified, expired), List.of());
        assertCurrentProposals(GovActionStatus.ACTIVE, List.of(active), List.of(ratified, expired));
        assertCurrentProposals(GovActionStatus.RATIFIED, List.of(ratified), List.of(active, expired));
        assertCurrentProposals(GovActionStatus.EXPIRED, List.of(expired), List.of(active, ratified));

        assertCurrentProposalApiStatus(active, ProposalStatus.LIVE);
        assertCurrentProposalApiStatus(ratified, ProposalStatus.RATIFIED);
        assertCurrentProposalApiStatus(expired, ProposalStatus.EXPIRED);
    }

    private void assertIndexedProposal(CreatedProposal proposal, GovActionType expectedType) {
        assertThat(proposal.proposal()).isNotNull();
        assertThat(proposal.proposal().getType()).isEqualTo(expectedType);

        var indexed = governanceAssertionHelper.findProposal(proposal.storeGovActionId());
        assertThat(indexed.getTxHash()).isEqualTo(proposal.txHash());
        assertThat(indexed.getIndex()).isEqualTo(proposal.index());
        assertThat(indexed.getType()).isEqualTo(expectedType);
    }

    private void assertActiveUntilLastOpportunity(CreatedProposal proposal) {
        for (int epoch = proposal.createdEpoch() + 1; epoch <= proposal.maxVotingEpoch(); epoch++) {
            governanceAssertionHelper.assertStatusAtEpoch(proposal.txHash(), proposal.index(), epoch, GovActionStatus.ACTIVE);
        }
    }

    private void assertExpiredAtLifecycleBoundary(CreatedProposal proposal) {
        governanceAssertionHelper.assertStatusAtEpoch(proposal.txHash(), proposal.index(), proposal.expiryStatusEpoch(), GovActionStatus.EXPIRED);
    }

    private void assertProposalRefund(CreatedProposal proposal, String stakeAddress) {
        waitTillAdaPotJobDone(adaPotJobRepository, proposal.expiryStatusEpoch() + 1, statusDiagnostics(List.of(proposal)));

        var proposalRefund = rewardRestRepository.findBySpendableEpochAndType(proposal.expiryStatusEpoch() + 1, RewardRestType.proposal_refund)
                .stream()
                .filter(rewardRestEntity -> rewardRestEntity.getAddress().equals(stakeAddress))
                .findFirst();

        assertThat(proposalRefund).isPresent();
    }

    private void assertLatestStatusAfterCompletedJob(CreatedProposal proposal, int completedEpoch) {
        Optional<GovActionProposalStatusEntity> latestStatus = latestStatus(proposal, completedEpoch);
        assertThat(latestStatus)
                .as("latest status for %s#%s after AdaPot epoch %s", proposal.txHash(), proposal.index(), completedEpoch)
                .isPresent();

        var expectedStatus = completedEpoch < proposal.expiryStatusEpoch()
                ? GovActionStatus.ACTIVE
                : GovActionStatus.EXPIRED;

        assertThat(latestStatus.get().getStatus()).isEqualTo(expectedStatus);
        if (expectedStatus == GovActionStatus.ACTIVE) {
            assertThat(latestStatus.get().getEpoch()).isEqualTo(completedEpoch);
        } else {
            assertThat(latestStatus.get().getEpoch()).isEqualTo(proposal.expiryStatusEpoch());
        }
    }

    private Optional<GovActionProposalStatusEntity> latestStatus(CreatedProposal proposal, int upToEpoch) {
        return proposalStatusRepository.findAll()
                .stream()
                .filter(status -> status.getGovActionTxHash().equals(proposal.txHash()))
                .filter(status -> status.getGovActionIndex() == proposal.index())
                .filter(status -> status.getEpoch() <= upToEpoch)
                .max(Comparator.comparing(GovActionProposalStatusEntity::getEpoch));
    }

    private void assertNoDuplicateStatusRows(List<CreatedProposal> proposals) {
        var proposalKeys = proposals.stream()
                .map(proposal -> proposal.txHash() + "#" + proposal.index())
                .collect(Collectors.toSet());

        Map<String, Long> duplicates = proposalStatusRepository.findAll()
                .stream()
                .filter(status -> proposalKeys.contains(status.getGovActionTxHash() + "#" + status.getGovActionIndex()))
                .collect(Collectors.groupingBy(
                        status -> status.getGovActionTxHash() + "#" + status.getGovActionIndex() + "#" + status.getEpoch(),
                        Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicates).isEmpty();
    }

    private Supplier<String> statusDiagnostics(List<CreatedProposal> proposals) {
        return () -> proposals.stream()
                .map(proposal -> proposalStatusDiagnostics(proposalStatusRepository, proposal.storeGovActionId()).get())
                .collect(Collectors.joining("\n"));
    }

    private int latestCompletedAdaPotEpoch() {
        var recentJobs = adaPotJobStorage.getRecentCompletedJobs(1);
        assertThat(recentJobs).isNotEmpty();
        return recentJobs.getFirst().getEpoch();
    }

    private int latestIndexedEpoch() {
        return blockStorage.findRecentBlock()
                .orElseThrow(() -> new AssertionError("No recent block found"))
                .getEpochNumber();
    }

    private GovActionProposal saveSyntheticProposal(String seed, int epoch, GovActionType type) {
        // Synthetic proposal rows keep API-mapping assertions deterministic while using real services.
        GovActionProposal proposal = GovActionProposal.builder()
                .txHash(syntheticTxHash())
                .index(0)
                .txIndex(0)
                .slot(1_000_000L + SYNTHETIC_ID.get())
                .deposit(BigInteger.valueOf(100_000_000L))
                .returnAddress(account0.stakeAddress())
                .type(type)
                .anchorUrl("https://example.com/governance/" + seed)
                .anchorHash(DEFAULT_ANCHOR_HASH)
                .epoch(epoch)
                .blockNumber(0L)
                .blockTime(0L)
                .build();

        govActionProposalStorage.saveAll(List.of(proposal));
        return proposal;
    }

    private void saveStatus(GovActionProposal proposal, int epoch, GovActionStatus status) {
        proposalStatusRepository.save(GovActionProposalStatusEntity.builder()
                .govActionTxHash(proposal.getTxHash())
                .govActionIndex((int) proposal.getIndex())
                .type(proposal.getType())
                .status(status)
                .votingStats(emptyVotingStats())
                .epoch(epoch)
                .build());
    }

    private ProposalVotingStats emptyVotingStats() {
        return ProposalVotingStats.builder()
                .spoTotalYesStake(BigInteger.ZERO)
                .spoTotalNoStake(BigInteger.ZERO)
                .spoTotalAbstainStake(BigInteger.ZERO)
                .spoYesVoteStake(BigInteger.ZERO)
                .spoNoVoteStake(BigInteger.ZERO)
                .spoAbstainVoteStake(BigInteger.ZERO)
                .spoDoNotVoteStake(BigInteger.ZERO)
                .drepTotalYesStake(BigInteger.ZERO)
                .drepTotalNoStake(BigInteger.ZERO)
                .drepTotalAbstainStake(BigInteger.ZERO)
                .drepYesVoteStake(BigInteger.ZERO)
                .drepNoVoteStake(BigInteger.ZERO)
                .drepAbstainVoteStake(BigInteger.ZERO)
                .drepNoConfidenceStake(BigInteger.ZERO)
                .drepAutoAbstainStake(BigInteger.ZERO)
                .drepDoNotVoteStake(BigInteger.ZERO)
                .ccYes(0)
                .ccNo(0)
                .ccDoNotVote(0)
                .ccAbstain(0)
                .build();
    }

    private void assertCurrentProposals(GovActionStatus filter,
                                        List<GovActionProposal> expectedPresent,
                                        List<GovActionProposal> expectedAbsent) {
        List<String> txHashes = proposalApiService.getCurrentProposals(filter)
                .stream()
                .map(ProposalDto::getTxHash)
                .toList();

        assertThat(txHashes).containsAll(expectedPresent.stream().map(GovActionProposal::getTxHash).toList());
        assertThat(txHashes).doesNotContainAnyElementsOf(expectedAbsent.stream().map(GovActionProposal::getTxHash).toList());
    }

    private void assertCurrentProposalApiStatus(GovActionProposal proposal, ProposalStatus expectedStatus) {
        assertThat(proposalApiService.getCurrentProposals(null))
                .filteredOn(dto -> dto.getTxHash().equals(proposal.getTxHash()) && dto.getIndex() == proposal.getIndex())
                .singleElement()
                .extracting(ProposalDto::getStatus)
                .isEqualTo(expectedStatus);
    }

    private String syntheticTxHash() {
        return "ff" + String.format("%062x", SYNTHETIC_ID.getAndIncrement());
    }
}
