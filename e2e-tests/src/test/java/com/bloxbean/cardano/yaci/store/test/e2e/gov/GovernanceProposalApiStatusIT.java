package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobRepository;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalStatus;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.ProposalApiService;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;
import com.bloxbean.cardano.yaci.store.test.e2e.common.BaseE2ETest;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceApiAssertionHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies API status translation over controlled governance-aggr status rows.
 *
 * <p>The rows are synthetic by design: this keeps API mapping isolated from transaction
 * construction, ledger ratification, and proposal lifecycle indexing. Rule parity remains in the
 * DevKit ledger-state tests.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = GovernanceProposalApiStatusIT.DevKitInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GovernanceProposalApiStatusIT extends BaseE2ETest {
    private static final int EPOCH_LENGTH_SECONDS = 20;
    private static final int GOV_ACTION_LIFETIME = 3;
    private static final String DEFAULT_ANCHOR_HASH = "cafef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937";
    private static final long RUN_ID = System.currentTimeMillis();
    private static final AtomicInteger SYNTHETIC_ID = new AtomicInteger(1);

    private GovernanceApiAssertionHelper governanceApiAssertionHelper;

    @Autowired
    private GovActionProposalStorage govActionProposalStorage;

    @Autowired
    private GovActionProposalStatusRepository proposalStatusRepository;

    @Autowired
    private AdaPotJobRepository adaPotJobRepository;

    @Autowired
    private AdaPotJobStorage adaPotJobStorage;

    @Autowired
    private ProposalApiService proposalApiService;

    @Autowired
    private BlockStorage blockStorage;

    @BeforeAll
    void setup() {
        governanceApiAssertionHelper = new GovernanceApiAssertionHelper(proposalApiService);
    }

    static class DevKitInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            assertDevKitAdminAvailable();
            createDevNet(votedGovernanceConfig(EPOCH_LENGTH_SECONDS, GOV_ACTION_LIFETIME));
        }
    }

    /**
     * Persists controlled proposal-status rows to isolate get-by-id API status translation.
     */
    @Test
    @Order(1)
    void getProposalById_shouldTranslateCurrentAndStaleStorageStatuses() {
        // ProposalApiService treats the latest completed AdaPot epoch as the current status boundary.
        int latestCompletedEpoch = latestCompletedAdaPotEpoch();
        assertThat(latestCompletedEpoch).isGreaterThan(0);
        int staleEpoch = latestCompletedEpoch - 1;

        // ACTIVE at the current boundary is still a live proposal.
        GovActionProposal currentActive = saveSyntheticProposal("api-current-active", latestCompletedEpoch, GovActionType.INFO_ACTION);
        saveStatus(currentActive, latestCompletedEpoch, GovActionStatus.ACTIVE);
        governanceApiAssertionHelper.assertLatestApiStatus(currentActive.getTxHash(), (int) currentActive.getIndex(), ProposalStatus.LIVE);

        // EXPIRED is terminal and should not be remapped by later AdaPot jobs.
        GovActionProposal expired = saveSyntheticProposal("api-expired", staleEpoch, GovActionType.INFO_ACTION);
        saveStatus(expired, staleEpoch, GovActionStatus.EXPIRED);
        governanceApiAssertionHelper.assertLatestApiStatus(expired.getTxHash(), (int) expired.getIndex(), ProposalStatus.EXPIRED);

        // A stale ACTIVE row means the proposal disappeared from current state, so the API reports DROPPED.
        GovActionProposal staleActive = saveSyntheticProposal("api-stale-active", staleEpoch, GovActionType.PARAMETER_CHANGE_ACTION);
        saveStatus(staleActive, staleEpoch, GovActionStatus.ACTIVE);
        governanceApiAssertionHelper.assertLatestApiStatus(staleActive.getTxHash(), (int) staleActive.getIndex(), ProposalStatus.DROPPED);

        // RATIFIED at the current boundary is visible as ratified until a later completed job advances.
        GovActionProposal currentRatified = saveSyntheticProposal("api-current-ratified", latestCompletedEpoch, GovActionType.PARAMETER_CHANGE_ACTION);
        saveStatus(currentRatified, latestCompletedEpoch, GovActionStatus.RATIFIED);
        governanceApiAssertionHelper.assertLatestApiStatus(currentRatified.getTxHash(), (int) currentRatified.getIndex(), ProposalStatus.RATIFIED);

        // Once RATIFIED is stale, the API exposes it as enacted.
        GovActionProposal staleRatified = saveSyntheticProposal("api-stale-ratified", staleEpoch, GovActionType.PARAMETER_CHANGE_ACTION);
        saveStatus(staleRatified, staleEpoch, GovActionStatus.RATIFIED);
        governanceApiAssertionHelper.assertLatestApiStatus(staleRatified.getTxHash(), (int) staleRatified.getIndex(), ProposalStatus.ENACTED);
    }

    /**
     * Verifies current-epoch proposal queries return the expected rows for each storage-status
     * filter and expose the public API status names.
     */
    @Test
    @Order(2)
    void getCurrentProposals_shouldRespectStatusFilter() {
        // getCurrentProposals reads rows at the latest indexed block epoch.
        int currentEpoch = latestIndexedEpoch();

        // Use one synthetic row per storage status so each filter can be checked independently.
        GovActionProposal active = saveSyntheticProposal("current-active", currentEpoch, GovActionType.INFO_ACTION);
        GovActionProposal ratified = saveSyntheticProposal("current-ratified", currentEpoch, GovActionType.PARAMETER_CHANGE_ACTION);
        GovActionProposal expired = saveSyntheticProposal("current-expired", currentEpoch, GovActionType.INFO_ACTION);

        saveStatus(active, currentEpoch, GovActionStatus.ACTIVE);
        saveStatus(ratified, currentEpoch, GovActionStatus.RATIFIED);
        saveStatus(expired, currentEpoch, GovActionStatus.EXPIRED);

        // The unfiltered view should include all current rows; filtered views should be exact.
        assertCurrentProposals(null, List.of(active, ratified, expired), List.of());
        assertCurrentProposals(GovActionStatus.ACTIVE, List.of(active), List.of(ratified, expired));
        assertCurrentProposals(GovActionStatus.RATIFIED, List.of(ratified), List.of(active, expired));
        assertCurrentProposals(GovActionStatus.EXPIRED, List.of(expired), List.of(active, ratified));

        // Current proposal listings use public API status names, not storage enum names.
        assertCurrentProposalApiStatus(active, ProposalStatus.LIVE);
        assertCurrentProposalApiStatus(ratified, ProposalStatus.RATIFIED);
        assertCurrentProposalApiStatus(expired, ProposalStatus.EXPIRED);
    }

    private int latestCompletedAdaPotEpoch() {
        // API status translation depends on completed AdaPot jobs, not merely the tip epoch.
        var recentJobs = adaPotJobStorage.getRecentCompletedJobs(1);
        if (recentJobs.isEmpty()) {
            waitForEpoch(1);
            waitTillAdaPotJobDone(adaPotJobRepository, 1);
            recentJobs = adaPotJobStorage.getRecentCompletedJobs(1);
        }
        assertThat(recentJobs).isNotEmpty();
        return recentJobs.getFirst().getEpoch();
    }

    private int latestIndexedEpoch() {
        // Current-proposal queries use the latest indexed block epoch as their target epoch.
        return blockStorage.findRecentBlock()
                .orElseThrow(() -> new AssertionError("No recent block found"))
                .getEpochNumber();
    }

    private GovActionProposal saveSyntheticProposal(String seed, int epoch, GovActionType type) {
        // Synthetic proposal rows keep API-mapping assertions deterministic while using real services.
        int syntheticId = SYNTHETIC_ID.getAndIncrement();
        GovActionProposal proposal = GovActionProposal.builder()
                .txHash(syntheticTxHash(syntheticId))
                .index(0)
                .txIndex(0)
                .slot(1_000_000L + syntheticId)
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
        // The API reader consumes the same persisted status rows produced by governance-aggr.
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
        // API DTOs expect voting stats to be present, even when a synthetic row has no votes.
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
        // Compare by tx hash because every synthetic proposal uses action index zero.
        List<String> txHashes = proposalApiService.getCurrentProposals(filter)
                .stream()
                .map(ProposalDto::getTxHash)
                .toList();

        assertThat(txHashes).containsAll(expectedPresent.stream().map(GovActionProposal::getTxHash).toList());
        var absentTxHashes = expectedAbsent.stream().map(GovActionProposal::getTxHash).toList();
        if (!absentTxHashes.isEmpty()) {
            assertThat(txHashes).doesNotContainAnyElementsOf(absentTxHashes);
        }
    }

    private void assertCurrentProposalApiStatus(GovActionProposal proposal, ProposalStatus expectedStatus) {
        assertThat(proposalApiService.getCurrentProposals(null))
                .filteredOn(dto -> dto.getTxHash().equals(proposal.getTxHash()) && dto.getIndex() == proposal.getIndex())
                .singleElement()
                .extracting(ProposalDto::getStatus)
                .isEqualTo(expectedStatus);
    }

    private String syntheticTxHash(int syntheticId) {
        // Keep the value hash-shaped so storage/API formatting paths stay realistic.
        return "fe" + String.format("%046x", RUN_ID) + String.format("%016x", syntheticId);
    }
}
