package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.client.spec.UnitInterval;
import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovAction;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobRepository;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;
import com.bloxbean.cardano.yaci.store.test.e2e.common.BaseE2ETest;
import com.bloxbean.cardano.yaci.store.test.e2e.common.DevKitLedgerGovernanceStateReader;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceRuleAssertionHelper;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceTxHelper;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceTxHelper.CreatedProposal;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceTxHelper.TestStakePool;
import com.bloxbean.cardano.yaci.store.test.e2e.common.LedgerGovernanceStateReader;
import com.bloxbean.cardano.yaci.store.test.e2e.common.ProposalLedgerSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

/**
 * Covers governance contexts where an enacted action changes the state used to evaluate later proposals.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = GovernanceEffectContextIT.DevKitInitializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GovernanceEffectContextIT extends BaseE2ETest {
    private static final int EPOCH_LENGTH_SECONDS = 20;
    private static final int GOV_ACTION_LIFETIME = 3;
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.51");
    private static final UnitInterval LOW_COMMITTEE_THRESHOLD = new UnitInterval(BigInteger.ONE, BigInteger.valueOf(3));
    private static final UnitInterval RAISED_COMMITTEE_THRESHOLD = new UnitInterval(BigInteger.valueOf(2), BigInteger.valueOf(3));

    private static final long FEE_PAYER_TOP_UP_ADA = 3_000_000L;
    private static final long SETUP_POOL_TOP_UP_ADA = 2_000_000L;
    private static final long DREP_YES_TOP_UP_ADA = 700_000L;
    private static final long DREP_NO_TOP_UP_ADA = 300_000L;

    private GovernanceTxHelper governanceTxHelper;
    private GovernanceRuleAssertionHelper governanceRuleAssertionHelper;
    private LedgerGovernanceStateReader ledgerStateReader;

    private Account setupDRepAccount;
    private Account drepYesAccount;
    private Account drepNoAccount;
    private List<Account> committeeAccounts;

    private TestStakePool setupStakePool;
    private com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId currentCommitteePrevGovActionId;

    @Autowired
    private Environment environment;

    @Autowired
    private ProposalStateClient proposalStateClient;

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
    private CommitteeStorageReader committeeStorageReader;

    @Autowired
    private CommitteeMemberStorageReader committeeMemberStorageReader;

    @BeforeEach
    void setup() {
        governanceTxHelper = new GovernanceTxHelper(backendService, govActionProposalStorage, GOV_ACTION_LIFETIME);
        ledgerStateReader = createLedgerStateReader();
        governanceRuleAssertionHelper = new GovernanceRuleAssertionHelper(
                proposalStateClient,
                proposalStatusRepository,
                govActionProposalStorage,
                ledgerStateReader);

        setupDRepAccount = accountAt(1);
        drepYesAccount = accountAt(2);
        drepNoAccount = accountAt(3);
        committeeAccounts = List.of(accountAt(10), accountAt(11), accountAt(12));

        preparePostBootstrapGovernanceActors();
    }

    static class DevKitInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            assertDevKitAdminAvailable();
            createDevNet(standardGovernanceConfig(EPOCH_LENGTH_SECONDS, GOV_ACTION_LIFETIME, DEFAULT_THRESHOLD));
        }
    }

    /**
     * An enacted committee threshold change must be used by later committee acceptance.
     */
    @Test
    void enactedCommitteeThreshold_shouldControlLaterNewConstitution() {
        prepareControlledDRepSplit();

        CreatedProposal committeeThresholdUpdate = createSingleProposalInFreshEpoch(
                "committee threshold update",
                GovActionType.UPDATE_COMMITTEE,
                this::committeeThresholdUpdateAction);
        castControlledDRepSplitVotes(committeeThresholdUpdate);
        governanceTxHelper.castTestStakePoolVote(account0, setupStakePool, committeeThresholdUpdate.storeGovActionId(), Vote.YES);
        waitForRatification("committee threshold update", committeeThresholdUpdate);

        int committeeReadyEpoch = waitForEnactmentContext("committee threshold update enactment", committeeThresholdUpdate);
        currentCommitteePrevGovActionId = committeeThresholdUpdate.txGovActionId();
        assertLatestCommitteeThreshold(2d / 3d);
        assertLocalCommitteeMemberCount(3);

        CreatedProposal newConstitution = createSingleProposalInFreshEpoch(
                "new constitution evaluated with raised committee threshold",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction);
        // One committee YES would pass the old 1/3 threshold, but fails after A raises it to 2/3.
        governanceTxHelper.castDRepVote(account0, drepYesAccount, newConstitution.storeGovActionId(), Vote.YES);
        castCommitteeYesVotes(newConstitution, 1);

        int expiryEpoch = newConstitution.expiryStatusEpoch();
        waitForEpoch(expiryEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, expiryEpoch,
                () -> diagnostics("new constitution evaluated with raised committee threshold", List.of(committeeThresholdUpdate, newConstitution)));

        var outcome = assertProposalStatus("new constitution evaluated with raised committee threshold", newConstitution, GovActionStatus.EXPIRED);
        governanceRuleAssertionHelper.assertVotingStats(newConstitution.storeGovActionId(), outcome.getEpoch(), stats -> {
            assertThat(nz(stats.getDrepApprovalRatio())).isGreaterThanOrEqualTo(DEFAULT_THRESHOLD);
            assertThat(nz(stats.getCcYes())).isEqualTo(1);
            assertThat(nz(stats.getCcApprovalRatio())).isLessThan(BigDecimal.valueOf(2).divide(BigDecimal.valueOf(3), java.math.MathContext.DECIMAL64));
        });
    }

    /**
     * Removing a winning sibling must prune the full descendant subtree and refund every removed proposal deposit.
     */
    @Test
    void ratifiedSibling_shouldPruneDescendantProposals() {
        prepareControlledDRepSplit();

        waitForEpoch(getCurrentEpoch() + 1);
        CreatedProposal rootA = createActiveProposal(
                "winning root constitution",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction);
        CreatedProposal rootB = createActiveProposal(
                "dropped root constitution",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction);
        CreatedProposal childOfB = createActiveProposal(
                "dropped child constitution",
                GovActionType.NEW_CONSTITUTION,
                () -> GovernanceTxHelper.newConstitutionAction(rootB.txGovActionId()));
        CreatedProposal grandchildOfB = createActiveProposal(
                "dropped grandchild constitution",
                GovActionType.NEW_CONSTITUTION,
                () -> GovernanceTxHelper.newConstitutionAction(childOfB.txGovActionId()));

        // Only the winning root needs votes. The competing root and its descendants
        // only need to be active so ledger pruning can remove and refund the subtree.
        governanceTxHelper.castDRepVote(account0, drepYesAccount, rootA.storeGovActionId(), Vote.YES);
        governanceTxHelper.castDRepVote(account0, drepNoAccount, rootA.storeGovActionId(), Vote.YES);
        castCommitteeYesVotes(rootA, 2);

        int ratifyEpoch = getCurrentEpoch() + 1;
        waitForEpoch(ratifyEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, ratifyEpoch,
                () -> diagnostics("deep sibling descendant pruning", List.of(rootA, rootB, childOfB, grandchildOfB)));

        assertProposalStatus("deep sibling descendant pruning", rootA, GovActionStatus.RATIFIED);
        assertNotRatified("dropped sibling root", rootB);
        assertNotRatified("dropped sibling child", childOfB);
        assertNotRatified("dropped sibling grandchild", grandchildOfB);
        assertLedgerRemoved("dropped sibling root", rootB);
        assertLedgerRemoved("dropped sibling child", childOfB);
        assertLedgerRemoved("dropped sibling grandchild", grandchildOfB);
        assertProposalRefund(
                "deep sibling descendant pruning refund",
                ratifyEpoch,
                account0.stakeAddress(),
                List.of(rootA, rootB, childOfB, grandchildOfB));
    }

    @Disabled("Deferred until yaci-core GovStateQuery decodes absent committee after NoConfidence")
    @Test
    void noConfidence_shouldRemoveCommitteeUntilUpdateCommitteeRestoresIt() {
    }

    private void preparePostBootstrapGovernanceActors() {
        CreatedProposal initialCommitteeProposal = null;
        try {
            Account setupPoolOwner = accountAt(20);

            topUpFund(account0.baseAddress(), FEE_PAYER_TOP_UP_ADA);
            topUpFund(setupPoolOwner.baseAddress(), SETUP_POOL_TOP_UP_ADA);
            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, account0.stakeAddress()));

            setupStakePool = governanceTxHelper.registerGeneratedStakePool(account0, setupPoolOwner);
            governanceTxHelper.delegateStakeToTestPool(account0, setupPoolOwner, setupStakePool);

            governanceTxHelper.registerDRep(account0, setupDRepAccount, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.delegateVotingPowerToDRep(account0, account0, GovId.toDrep(setupDRepAccount.drepId()));

            int votingPowerReadyEpoch = getCurrentEpoch() + 2;
            waitForEpoch(votingPowerReadyEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, votingPowerReadyEpoch);

            waitForEpoch(getCurrentEpoch() + 1);
            initialCommitteeProposal = governanceTxHelper.createProposalAndWait(
                    account0,
                    account0.stakeAddress(),
                    initialCommitteeAction());
            assertIndexedProposal(initialCommitteeProposal, GovActionType.UPDATE_COMMITTEE);
            assertLedgerActive(initialCommitteeProposal);
            governanceTxHelper.castDRepVote(account0, setupDRepAccount, initialCommitteeProposal.storeGovActionId(), Vote.YES);
            governanceTxHelper.castTestStakePoolVote(account0, setupStakePool, initialCommitteeProposal.storeGovActionId(), Vote.YES);

            int ratifyEpoch = initialCommitteeProposal.createdEpoch() + 1;
            waitForEpoch(ratifyEpoch);
            CreatedProposal proposal = initialCommitteeProposal;
            waitTillAdaPotJobDone(adaPotJobRepository, ratifyEpoch,
                    () -> diagnostics("initial committee setup", List.of(proposal)));
            governanceRuleAssertionHelper.assertMatchesLedger(initialCommitteeProposal.storeGovActionId(), GovActionStatus.RATIFIED);
            currentCommitteePrevGovActionId = initialCommitteeProposal.txGovActionId();

            waitForEnactmentContext("initial committee setup enactment", initialCommitteeProposal);
            for (Account committeeAccount : committeeAccounts) {
                governanceTxHelper.authorizeCommitteeHotKey(account0, committeeAccount, committeeAccount);
            }

            // Keep setup stake out of later DRep ratios; AlwaysAbstain is outside the accepted-ratio denominator.
            governanceTxHelper.delegateVotingPowerToAlwaysAbstain(account0, account0);
            int setupStakeNeutralizedEpoch = getCurrentEpoch() + 2;
            waitForEpoch(setupStakeNeutralizedEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, setupStakeNeutralizedEpoch);
            assertLatestCommitteeThreshold(1d / 3d);
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Governance effect-context common setup failed.\n"
                    + diagnostics("common setup", initialCommitteeProposal == null ? List.of() : List.of(initialCommitteeProposal)), e);
        }
    }

    private void prepareControlledDRepSplit() {
        try {
            topUpFund(drepYesAccount.baseAddress(), DREP_YES_TOP_UP_ADA);
            topUpFund(drepNoAccount.baseAddress(), DREP_NO_TOP_UP_ADA);

            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, drepYesAccount.stakeAddress()));
            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, drepNoAccount.stakeAddress()));

            governanceTxHelper.registerDRep(account0, drepYesAccount, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.registerDRep(account0, drepNoAccount, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.delegateVotingPowerToDRep(account0, drepYesAccount, GovId.toDrep(drepYesAccount.drepId()));
            governanceTxHelper.delegateVotingPowerToDRep(account0, drepNoAccount, GovId.toDrep(drepNoAccount.drepId()));

            int votingPowerReadyEpoch = getCurrentEpoch() + 2;
            waitForEpoch(votingPowerReadyEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, votingPowerReadyEpoch);
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Controlled DRep split setup failed.\n"
                    + diagnostics("controlled DRep split", List.of()), e);
        }
    }

    private CreatedProposal createSingleProposalInFreshEpoch(String name,
                                                             GovActionType expectedType,
                                                             Supplier<GovAction> actionSupplier) {
        waitForEpoch(getCurrentEpoch() + 1);
        int proposalEpoch = getCurrentEpoch();
        return createProposalInEpoch(name, expectedType, actionSupplier, proposalEpoch);
    }

    private CreatedProposal createActiveProposal(String name,
                                                 GovActionType expectedType,
                                                 Supplier<GovAction> actionSupplier) {
        CreatedProposal proposal = governanceTxHelper.createProposalAndWait(account0, account0.stakeAddress(), actionSupplier.get());
        assertIndexedProposal(proposal, expectedType);
        assertLedgerActive(proposal);
        return proposal;
    }

    private CreatedProposal createProposalInEpoch(String name,
                                                  GovActionType expectedType,
                                                  Supplier<GovAction> actionSupplier,
                                                  int proposalEpoch) {
        CreatedProposal proposal = governanceTxHelper.createProposalAndWait(account0, account0.stakeAddress(), actionSupplier.get());
        assertIndexedProposal(proposal, expectedType);
        assertThat(proposal.createdEpoch())
                .as("proposal '%s' must be created in epoch %s", name, proposalEpoch)
                .isEqualTo(proposalEpoch);
        assertLedgerActive(proposal);
        return proposal;
    }

    private int waitForEnactmentContext(String scenarioName, CreatedProposal proposal) {
        int enactmentReadyEpoch = proposal.createdEpoch() + 2;
        waitForEpoch(enactmentReadyEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, enactmentReadyEpoch,
                () -> diagnostics(scenarioName, List.of(proposal)));
        return enactmentReadyEpoch;
    }

    private void waitForRatification(String scenarioName, CreatedProposal proposal) {
        int ratifyEpoch = proposal.createdEpoch() + 1;
        waitForEpoch(ratifyEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, ratifyEpoch,
                () -> diagnostics(scenarioName, List.of(proposal)));
        assertProposalStatus(scenarioName, proposal, GovActionStatus.RATIFIED);
    }

    private com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity assertProposalStatus(
            String scenarioName,
            CreatedProposal proposal,
            GovActionStatus expectedStatus) {
        try {
            return governanceRuleAssertionHelper.assertMatchesLedger(proposal.storeGovActionId(), expectedStatus);
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Governance effect-context assertion failed.\n"
                    + diagnostics(scenarioName, List.of(proposal)), e);
        }
    }

    private void assertIndexedProposal(CreatedProposal proposal, GovActionType expectedType) {
        assertThat(proposal.proposal()).isNotNull();
        assertThat(proposal.proposal().getType()).isEqualTo(expectedType);

        var indexed = governanceRuleAssertionHelper.findProposal(proposal.storeGovActionId());
        assertThat(indexed.getTxHash()).isEqualTo(proposal.txHash());
        assertThat(indexed.getIndex()).isEqualTo(proposal.index());
        assertThat(indexed.getType()).isEqualTo(expectedType);
    }

    private void assertLedgerActive(CreatedProposal proposal) {
        await().atMost(Duration.ofSeconds(90))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    ProposalLedgerSnapshot snapshot = ledgerStateReader.fetchProposalState(proposal.storeGovActionId());
                    assertThat(snapshot.presentInCurrentProposals())
                            .as("ledger active proposal for %s: %s", proposal.storeGovActionId(), snapshot)
                            .isTrue();
                    assertThat(snapshot.presentInEnactedGovActions()).isFalse();
                    assertThat(snapshot.presentInExpiredGovActions()).isFalse();
                });
    }

    private void assertNotRatified(String scenarioName, CreatedProposal proposal) {
        assertThat(governanceRuleAssertionHelper.findProposalStatuses(proposal.storeGovActionId()))
                .as("%s status rows for %s", scenarioName, proposal.storeGovActionId())
                .noneSatisfy(status -> assertThat(status.getStatus()).isEqualTo(GovActionStatus.RATIFIED));
    }

    private void assertLedgerRemoved(String scenarioName, CreatedProposal proposal) {
        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    ProposalLedgerSnapshot snapshot = ledgerStateReader.fetchProposalState(proposal.storeGovActionId());
                    assertThat(snapshot.removedFromCurrentProposals())
                            .as("%s ledger removed proposal for %s: %s", scenarioName, proposal.storeGovActionId(), snapshot)
                            .isTrue();
                    assertThat(snapshot.presentInCurrentProposals()).isFalse();
                    assertThat(snapshot.presentInEnactedGovActions()).isFalse();
                });
    }

    private void assertProposalRefund(String scenarioName,
                                      int earnedEpoch,
                                      String stakeAddress,
                                      List<CreatedProposal> proposals) {
        int spendableEpoch = earnedEpoch + 1;
        waitForEpoch(spendableEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, spendableEpoch,
                () -> diagnostics(scenarioName, proposals));

        BigInteger expectedRefund = proposals.stream()
                .map(proposal -> proposal.proposal().getDeposit())
                .reduce(BigInteger.ZERO, BigInteger::add);

        BigInteger actualRefund = rewardRestRepository.findBySpendableEpochAndType(spendableEpoch, RewardRestType.proposal_refund)
                .stream()
                .filter(reward -> stakeAddress.equals(reward.getAddress()))
                .map(reward -> reward.getAmount() == null ? BigInteger.ZERO : reward.getAmount())
                .reduce(BigInteger.ZERO, BigInteger::add);

        assertThat(actualRefund)
                .as("%s proposal refund at spendable epoch %s", scenarioName, spendableEpoch)
                .isEqualTo(expectedRefund);
    }

    private void assertLatestCommitteeThreshold(double expectedThreshold) {
        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    // Committee storage records the latest committee change, not a per-epoch snapshot row.
                    var committee = committeeStorageReader.findByMaxEpoch().orElseThrow();
                    assertThat(committee.getThresholdNumerator()).isEqualTo(BigInteger.valueOf(Math.round(expectedThreshold * 3)));
                    assertThat(committee.getThresholdDenominator()).isEqualTo(BigInteger.valueOf(3));
                    assertThat(committee.getThreshold().doubleValue())
                            .as("indexed committee threshold")
                            .isCloseTo(expectedThreshold, within(0.000001));
                });
    }

    private void assertLocalCommitteeMemberCount(int expectedCount) {
        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(committeeMemberStorageReader.findCommitteeMembersWithMaxSlot())
                        .as("indexed current committee members")
                        .hasSize(expectedCount));
    }

    private void castControlledDRepSplitVotes(CreatedProposal proposal) {
        governanceTxHelper.castDRepVote(account0, drepYesAccount, proposal.storeGovActionId(), Vote.YES);
        governanceTxHelper.castDRepVote(account0, drepNoAccount, proposal.storeGovActionId(), Vote.NO);
    }

    private void castCommitteeYesVotes(CreatedProposal proposal, int count) {
        for (int i = 0; i < count; i++) {
            governanceTxHelper.castCommitteeHotVote(account0, committeeAccounts.get(i), proposal.storeGovActionId(), Vote.YES);
        }
    }

    private GovAction committeeThresholdUpdateAction() {
        UpdateCommittee updateCommittee = GovernanceTxHelper.updateCommitteeAction(RAISED_COMMITTEE_THRESHOLD);
        updateCommittee.setPrevGovActionId(currentCommitteePrevGovActionId);
        return updateCommittee;
    }

    private GovAction initialCommitteeAction() {
        Map<com.bloxbean.cardano.client.address.Credential, Integer> members = new LinkedHashMap<>();
        int term = getCurrentEpoch() + 100;
        for (Account committeeAccount : committeeAccounts) {
            members.put(committeeAccount.committeeColdCredential(), term);
        }

        return GovernanceTxHelper.updateCommitteeAction(members, LOW_COMMITTEE_THRESHOLD);
    }

    private void assertTransactionSucceeded(com.bloxbean.cardano.client.api.model.Result<String> result) {
        assertThat(result.isSuccessful())
                .as(result.toString())
                .isTrue();
        checkIfUtxoAvailable(result.getValue(), account0.baseAddress());
    }

    private Account accountAt(int index) {
        return new Account(Networks.testnet(), DEFAULT_MNEMONICS, DerivationPath.createExternalAddressDerivationPathForAccount(index));
    }

    private LedgerGovernanceStateReader createLedgerStateReader() {
        String nodeSocketPath = property("devkit.ledger-state-reader.node-socket-path", "");
        String n2cHost = property(
                "devkit.ledger-state-reader.n2c-host",
                property("store.cardano.n2c-host", property("store.cardano.host", "localhost")));
        int n2cPort = intProperty("devkit.ledger-state-reader.n2c-port", intProperty("store.cardano.n2c-port", 3333));
        long protocolMagic = longProperty("devkit.ledger-state-reader.protocol-magic", longProperty("store.cardano.protocol-magic", 42L));
        long timeoutSeconds = longProperty("devkit.ledger-state-reader.timeout-seconds", 20L);

        return new DevKitLedgerGovernanceStateReader(
                nodeSocketPath,
                n2cHost,
                n2cPort,
                protocolMagic,
                Duration.ofSeconds(timeoutSeconds));
    }

    private String diagnostics(String scenarioName, List<CreatedProposal> proposals) {
        StringBuilder message = new StringBuilder();
        message.append("scenario=").append(scenarioName);
        message.append("\ncurrentEpoch=").append(safeCurrentEpoch());
        message.append("\nlastCompletedAdaPotEpoch=").append(lastCompletedAdaPotEpoch());
        message.append("\nprofile=").append(profileSummary());
        message.append("\nlatestLocalCommittee=").append(safeLatestCommittee());

        for (CreatedProposal proposal : proposals) {
            if (proposal != null) {
                GovActionId govActionId = proposal.storeGovActionId();
                message.append("\nproposal=").append(govActionId);
                message.append("\nproposalStatusRows=").append(governanceRuleAssertionHelper.findProposalStatuses(govActionId));
                message.append("\nledgerSnapshot=").append(safeLedgerSnapshot(govActionId));
            }
        }

        return message.toString();
    }

    private String profileSummary() {
        return "post-bootstrap protocolMajorVer=10, epochLengthSeconds=" + EPOCH_LENGTH_SECONDS
                + ", govActionLifetime=" + GOV_ACTION_LIFETIME
                + ", threshold=" + DEFAULT_THRESHOLD
                + ", initialCommitteeThreshold=1/3"
                + ", raisedCommitteeThreshold=2/3"
                + ", setupStakePool=" + (setupStakePool == null ? "not-registered" : setupStakePool.poolId())
                + ", currentCommitteePrevGovActionId=" + currentCommitteePrevGovActionId;
    }

    private String safeCurrentEpoch() {
        try {
            return String.valueOf(getCurrentEpoch());
        } catch (RuntimeException e) {
            return "unavailable (" + e.getMessage() + ")";
        }
    }

    private String lastCompletedAdaPotEpoch() {
        try {
            var recentJobs = adaPotJobStorage.getRecentCompletedJobs(1);
            return recentJobs.isEmpty() ? "none" : String.valueOf(recentJobs.getFirst().getEpoch());
        } catch (RuntimeException e) {
            return "unavailable (" + e.getMessage() + ")";
        }
    }

    private String safeLedgerSnapshot(GovActionId govActionId) {
        try {
            return String.valueOf(ledgerStateReader.fetchProposalState(govActionId));
        } catch (RuntimeException e) {
            return "unavailable (" + e.getMessage() + ")";
        }
    }

    private String safeLatestCommittee() {
        try {
            return committeeStorageReader.findByMaxEpoch()
                    .map(Object::toString)
                    .orElse("none");
        } catch (RuntimeException e) {
            return "unavailable (" + e.getMessage() + ")";
        }
    }

    private String property(String key, String fallback) {
        String value = environment.getProperty(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private int intProperty(String key, int fallback) {
        String value = environment.getProperty(key);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }

    private long longProperty(String key, long fallback) {
        String value = environment.getProperty(key);
        return value == null || value.isBlank() ? fallback : Long.parseLong(value);
    }

    private BigInteger nz(BigInteger value) {
        return value == null ? BigInteger.ZERO : value;
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
