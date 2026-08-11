package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
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
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises indexer lifecycle behavior across real DevKit epoch boundaries.
 *
 * <p>This class submits real governance transactions and verifies the derived database status
 * history, duplicate-row protection, and proposal deposit refund. Ledger-rule parity is covered by
 * the rule-focused governance integration tests, while API status translation is covered by
 * {@link GovernanceProposalApiStatusIT}.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = GovernanceProposalLifecycleIT.DevKitInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GovernanceProposalLifecycleIT extends BaseE2ETest {
    private static final int EPOCH_LENGTH_SECONDS = 20;
    private static final int GOV_ACTION_LIFETIME = 3;

    private GovernanceTxHelper governanceTxHelper;
    private GovernanceAssertionHelper governanceAssertionHelper;

    @Autowired
    private GovActionProposalStorage govActionProposalStorage;

    @Autowired
    private GovActionProposalStatusRepository proposalStatusRepository;

    @Autowired
    private AdaPotJobRepository adaPotJobRepository;

    @Autowired
    private RewardRestRepository rewardRestRepository;

    @BeforeAll
    void setup() {
        // The helper lifetime must match the devnet protocol parameter used below.
        governanceTxHelper = new GovernanceTxHelper(backendService, govActionProposalStorage, GOV_ACTION_LIFETIME);
        governanceAssertionHelper = new GovernanceAssertionHelper(govActionProposalStorage, proposalStatusRepository);
    }

    static class DevKitInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            // Create the devnet before Spring downloads genesis files and starts chain sync.
            assertDevKitAdminAvailable();
            createDevNet(votedGovernanceConfig(EPOCH_LENGTH_SECONDS, GOV_ACTION_LIFETIME));
        }
    }

    /**
     * Uses real governance transactions to verify proposal lifecycle rows remain correct as later
     * AdaPot jobs and epoch-boundary evaluations continue to arrive.
     */
    @Test
    @Order(1)
    void devnetProposals_shouldExpireRefundAndRemainStableAcrossEpochDrift() {
        // The return address must be registered before proposal deposits can be refunded.
        governanceTxHelper.registerStakeAddress(account0, account0.stakeAddress());

        // Info actions never ratify, so they are the simplest real-chain expiry baseline.
        CreatedProposal infoProposal = governanceTxHelper.createInfoProposalAndWait(account0, account0.stakeAddress());
        assertIndexedProposal(infoProposal, GovActionType.INFO_ACTION);

        // Create the second proposal in a later epoch to exercise drift across staggered lifecycles.
        waitForEpoch(infoProposal.createdEpoch() + 1);
        CreatedProposal noConfidenceProposal = governanceTxHelper.createProposalAndWait(
                account0,
                account0.stakeAddress(),
                GovernanceTxHelper.noConfidenceAction());
        assertIndexedProposal(noConfidenceProposal, GovActionType.NO_CONFIDENCE);
        assertThat(noConfidenceProposal.createdEpoch()).isGreaterThan(infoProposal.createdEpoch());

        // The polling window covers every status epoch for both proposals plus the refund epoch.
        int firstStatusEpoch = infoProposal.createdEpoch() + 1;
        int lastStatusEpoch = Math.max(infoProposal.expiryStatusEpoch(), noConfidenceProposal.expiryStatusEpoch()) + 1;
        List<CreatedProposal> proposals = List.of(infoProposal, noConfidenceProposal);

        // Check the status history after each completed AdaPot job, not only at the final epoch.
        for (int epoch = firstStatusEpoch; epoch <= lastStatusEpoch; epoch++) {
            waitTillAdaPotJobDone(adaPotJobRepository, epoch, statusDiagnostics(proposals));

            for (CreatedProposal proposal : proposals) {
                // A proposal only has derived status rows after entering the next epoch.
                if (epoch > proposal.createdEpoch()) {
                    assertLatestStatusAfterCompletedJob(proposal, epoch);
                }
            }

            assertNoDuplicateStatusRows(proposals);
        }

        // Final assertions are explicit so a failure points to lifecycle status rows or refund.
        assertActiveUntilLastOpportunity(infoProposal);
        assertExpiredAtLifecycleBoundary(infoProposal);
        assertProposalRefund(infoProposal, account0.stakeAddress());

        assertActiveUntilLastOpportunity(noConfidenceProposal);
        assertExpiredAtLifecycleBoundary(noConfidenceProposal);
    }

    private void assertIndexedProposal(CreatedProposal proposal, GovActionType expectedType) {
        // First validate the helper's cached proposal, then re-read through storage.
        assertThat(proposal.proposal()).isNotNull();
        assertThat(proposal.proposal().getType()).isEqualTo(expectedType);

        var indexed = governanceAssertionHelper.findProposal(proposal.storeGovActionId());
        assertThat(indexed.getTxHash()).isEqualTo(proposal.txHash());
        assertThat(indexed.getIndex()).isEqualTo(proposal.index());
        assertThat(indexed.getType()).isEqualTo(expectedType);
    }

    private void assertActiveUntilLastOpportunity(CreatedProposal proposal) {
        // Status evaluation starts in the epoch after proposal submission.
        for (int epoch = proposal.createdEpoch() + 1; epoch <= proposal.maxVotingEpoch(); epoch++) {
            governanceAssertionHelper.assertStatusAtEpoch(proposal.txHash(), proposal.index(), epoch, GovActionStatus.ACTIVE);
        }
    }

    private void assertExpiredAtLifecycleBoundary(CreatedProposal proposal) {
        governanceAssertionHelper.assertStatusAtEpoch(proposal.txHash(), proposal.index(), proposal.expiryStatusEpoch(), GovActionStatus.EXPIRED);
    }

    private void assertProposalRefund(CreatedProposal proposal, String stakeAddress) {
        // Proposal refunds become spendable one epoch after the EXPIRED status row.
        waitTillAdaPotJobDone(adaPotJobRepository, proposal.expiryStatusEpoch() + 1, statusDiagnostics(List.of(proposal)));

        var proposalRefund = rewardRestRepository.findBySpendableEpochAndType(proposal.expiryStatusEpoch() + 1, RewardRestType.proposal_refund)
                .stream()
                .filter(rewardRestEntity -> rewardRestEntity.getAddress().equals(stakeAddress))
                .findFirst();

        assertThat(proposalRefund).isPresent();
    }

    private void assertLatestStatusAfterCompletedJob(CreatedProposal proposal, int completedEpoch) {
        // This detects drift where an older or missing row becomes the latest API-visible state.
        Optional<GovActionProposalStatusEntity> latestStatus = latestStatus(proposal, completedEpoch);
        assertThat(latestStatus)
                .as("latest status for %s#%s after AdaPot epoch %s", proposal.txHash(), proposal.index(), completedEpoch)
                .isPresent();

        // After expiry, the latest row should remain the expiry row even as later epochs complete.
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
        // Limit by completed epoch so the assertion matches what the chain has made observable.
        return proposalStatusRepository.findAll()
                .stream()
                .filter(status -> status.getGovActionTxHash().equals(proposal.txHash()))
                .filter(status -> status.getGovActionIndex() == proposal.index())
                .filter(status -> status.getEpoch() <= upToEpoch)
                .max(Comparator.comparing(GovActionProposalStatusEntity::getEpoch));
    }

    private void assertNoDuplicateStatusRows(List<CreatedProposal> proposals) {
        // Re-processing an epoch should replace rows for that epoch, not append duplicates.
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
        // Diagnostics are evaluated only on timeout, keeping normal test output small.
        return () -> proposals.stream()
                .map(proposal -> proposalStatusDiagnostics(proposalStatusRepository, proposal.storeGovActionId()).get())
                .collect(Collectors.joining("\n"));
    }

}
