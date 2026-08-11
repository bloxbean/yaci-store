package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import com.bloxbean.cardano.client.plutus.spec.ExUnits;
import com.bloxbean.cardano.client.spec.UnitInterval;
import com.bloxbean.cardano.client.transaction.spec.ProtocolParamUpdate;
import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovAction;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobRepository;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verifies inter-proposal ledger rules where one proposal changes another proposal's outcome.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = GovernanceInterProposalIT.DevKitInitializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GovernanceInterProposalIT extends BaseE2ETest {
    private static final int EPOCH_LENGTH_SECONDS = 20;
    private static final int GOV_ACTION_LIFETIME = 3;
    private static final int HARD_FORK_TEST_MAJOR_VERSION = 10;
    private static final int HARD_FORK_TEST_MINOR_VERSION = 1;
    private static final long CONTROLLED_STAKE_TOP_UP_ADA = 2_000_000L;
    private static final BigInteger TREASURY_CONTEXT_SEED_LOVELACE = BigInteger.valueOf(100_000_000_000L);
    private static final BigInteger UNWITHDRAWABLE_TREASURY_AMOUNT_LOVELACE = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigDecimal THRESHOLD = new BigDecimal("0.51");
    private static final UnitInterval COMMITTEE_THRESHOLD = new UnitInterval(BigInteger.valueOf(51), BigInteger.valueOf(100));

    private GovernanceTxHelper governanceTxHelper;
    private GovernanceRuleAssertionHelper governanceRuleAssertionHelper;
    private LedgerGovernanceStateReader ledgerStateReader;

    private Account drepAccount;
    private List<Account> committeeAccounts;
    private TestStakePool spoTestPool;
    private CreatedProposal initialCommitteeProposal;
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

    @BeforeEach
    void setup() {
        governanceTxHelper = new GovernanceTxHelper(backendService, govActionProposalStorage, GOV_ACTION_LIFETIME);
        ledgerStateReader = createLedgerStateReader();
        governanceRuleAssertionHelper = new GovernanceRuleAssertionHelper(
                proposalStateClient,
                proposalStatusRepository,
                govActionProposalStorage,
                ledgerStateReader);

        drepAccount = accountAt(1);
        committeeAccounts = List.of(accountAt(2), accountAt(3), accountAt(4));

        preparePostBootstrapGovernanceActors();
    }

    static class DevKitInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            assertDevKitAdminAvailable();
            createDevNet(standardGovernanceConfig(EPOCH_LENGTH_SECONDS, GOV_ACTION_LIFETIME, THRESHOLD));
        }
    }

    /**
     * A higher-priority delaying action ratifies first and leaves the lower-priority action active.
     */
    @Test
    void higherPriorityDelayingAction_shouldHoldLowerPriorityProposal() {
        // Both proposals are created in the same epoch so the ledger must order them in one
        // RATIFY pass.
        List<CreatedProposal> proposals = createProposalsInSameEpoch(
                proposalSpec("priority new constitution", GovActionType.NEW_CONSTITUTION, GovernanceTxHelper::newConstitutionAction),
                proposalSpec("lower priority parameter change", GovActionType.PARAMETER_CHANGE_ACTION, () -> nonSecurityParameterChangeAction(100_000)));
        CreatedProposal newConstitution = proposals.get(0);
        CreatedProposal parameterChange = proposals.get(1);

        // NewConstitution has higher priority and is a delaying action; the lower-priority
        // ParameterChange also has passing votes.
        castPassingVotes(GovActionType.NEW_CONSTITUTION, newConstitution);
        castPassingVotes(GovActionType.PARAMETER_CHANGE_ACTION, parameterChange);

        int firstRatifyEpoch = newConstitution.createdEpoch() + 1;
        waitForEpoch(firstRatifyEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, firstRatifyEpoch,
                () -> diagnostics("action priority ordering", List.of(newConstitution, parameterChange)));

        assertProposalStatus("action priority ordering", newConstitution, GovActionStatus.RATIFIED);
        // The lower-priority proposal must stay active because rsDelayed short-circuits later
        // actions in this boundary.
        assertDbStatusAtEpoch("action priority ordering", parameterChange, firstRatifyEpoch, GovActionStatus.ACTIVE);
        assertLedgerActiveAndDelayed("action priority ordering", parameterChange);
    }

    /**
     * A child proposal can ratify only after its current enacted parent is in place.
     */
    @Test
    void childProposal_shouldRatifyAfterReferencedParentIsEnacted() {
        // First enact a root constitution so the child can reference the current Constitution purpose root.
        CreatedProposal parent = createSingleProposalInFreshEpoch(
                "parent constitution",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction);
        castPassingVotes(GovActionType.NEW_CONSTITUTION, parent);
        waitForRatification("parent constitution", parent);

        // The child uses the parent's GovActionId as prevGovActionId; this should satisfy
        // prevActionAsExpected.
        CreatedProposal child = createSingleProposalInFreshEpoch(
                "child constitution",
                GovActionType.NEW_CONSTITUTION,
                () -> GovernanceTxHelper.newConstitutionAction(parent.txGovActionId()));
        castPassingVotes(GovActionType.NEW_CONSTITUTION, child);
        waitForRatification("child constitution", child);

        assertProposalStatus("parent-child chain", parent, GovActionStatus.RATIFIED);
        assertProposalStatus("parent-child chain", child, GovActionStatus.RATIFIED);
    }

    /**
     * When one root proposal ratifies, competing siblings in the same purpose group are removed.
     */
    @Test
    void ratifiedProposal_shouldDropCompetingSiblingInSamePurpose() {
        // Two root Constitution proposals compete in the same purpose group.
        List<CreatedProposal> proposals = createProposalsInSameEpoch(
                proposalSpec("winning constitution sibling", GovActionType.NEW_CONSTITUTION, GovernanceTxHelper::newConstitutionAction),
                proposalSpec("dropped constitution sibling", GovActionType.NEW_CONSTITUTION, GovernanceTxHelper::newConstitutionAction));
        CreatedProposal winner = proposals.get(0);
        CreatedProposal sibling = proposals.get(1);

        castPassingVotes(GovActionType.NEW_CONSTITUTION, winner);
        castPassingVotes(GovActionType.NEW_CONSTITUTION, sibling);

        int ratifyEpoch = winner.createdEpoch() + 1;
        waitForEpoch(ratifyEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, ratifyEpoch,
                () -> diagnostics("sibling drop", List.of(winner, sibling)));

        assertProposalStatus("sibling drop", winner, GovActionStatus.RATIFIED);
        // Ledger removes the sibling from current proposals instead of ratifying both passing actions.
        assertNotRatified("dropped sibling", sibling);
        assertLedgerRemoved("dropped sibling", sibling);
    }

    /**
     * A delaying HardForkInitiation holds a lower-priority non-delaying proposal until the next boundary.
     */
    @Test
    void delayingHardFork_shouldDeferParameterChangeUntilNextBoundary() {
        // HardForkInitiation and ParameterChange are different purposes; any hold on
        // ParameterChange must come from rsDelayed.
        List<CreatedProposal> proposals = createProposalsInSameEpoch(
                proposalSpec("delaying hard fork", GovActionType.HARD_FORK_INITIATION_ACTION, this::hardForkInitiationAction),
                proposalSpec("delayed parameter change", GovActionType.PARAMETER_CHANGE_ACTION, () -> nonSecurityParameterChangeAction(100_001)));
        CreatedProposal hardFork = proposals.get(0);
        CreatedProposal parameterChange = proposals.get(1);

        // The ParameterChange deliberately changes a non-security field, so this row does not
        // depend on SPO threshold arithmetic.
        castPassingVotes(GovActionType.HARD_FORK_INITIATION_ACTION, hardFork);
        castPassingVotes(GovActionType.PARAMETER_CHANGE_ACTION, parameterChange);

        int delayedEpoch = hardFork.createdEpoch() + 1;
        waitForEpoch(delayedEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, delayedEpoch,
                () -> diagnostics("delaying action propagation - delayed boundary", List.of(hardFork, parameterChange)));

        assertProposalStatus("delaying action propagation - delayed boundary", hardFork, GovActionStatus.RATIFIED);
        assertDbStatusAtEpoch("delaying action propagation - delayed boundary", parameterChange, delayedEpoch, GovActionStatus.ACTIVE);
        assertLedgerActiveAndDelayed("delaying action propagation - delayed boundary", parameterChange);

        // At the next boundary rsDelayed is reset, so the still-active ParameterChange can
        // ratify with the same votes.
        int followUpEpoch = hardFork.createdEpoch() + 2;
        waitForEpoch(followUpEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, followUpEpoch,
                () -> diagnostics("delaying action propagation - follow-up boundary", List.of(hardFork, parameterChange)));

        assertProposalStatus("delaying action propagation - follow-up boundary", parameterChange, GovActionStatus.RATIFIED);
    }

    /**
     * A later withdrawal uses the treasury left after an earlier enacted withdrawal.
     */
    @Test
    void laterTreasuryWithdrawal_shouldUseTreasuryAfterPriorWithdrawal() {
        BigInteger treasuryBeforeFirstWithdrawal = seedTreasuryForContextTest("treasury effect context");
        BigInteger firstWithdrawalAmount = treasuryBeforeFirstWithdrawal
                .multiply(BigInteger.valueOf(2))
                .divide(BigInteger.valueOf(3));

        // Withdraw a large portion first so the second proposal is evaluated after A has
        // updated the ledger's treasury context.
        CreatedProposal firstWithdrawal = createSingleProposalInFreshEpoch(
                "treasury withdrawal that updates treasury context",
                GovActionType.TREASURY_WITHDRAWALS_ACTION,
                () -> GovernanceTxHelper.treasuryWithdrawalsAction(account0.stakeAddress(), firstWithdrawalAmount));
        castTreasuryWithdrawalPassingVotes(firstWithdrawal);
        waitForRatification("treasury withdrawal that updates treasury context", firstWithdrawal);

        BigInteger treasuryAfterFirstWithdrawal = ledgerTreasury("treasury effect context after first withdrawal");
        BigInteger secondWithdrawalAmount = UNWITHDRAWABLE_TREASURY_AMOUNT_LOVELACE;
        assertThat(secondWithdrawalAmount)
                .as("second withdrawal should stay above treasury even if the treasury grows before ratification")
                .isGreaterThan(treasuryAfterFirstWithdrawal);

        CreatedProposal secondWithdrawal = createSingleProposalInFreshEpoch(
                "treasury withdrawal above remaining treasury",
                GovActionType.TREASURY_WITHDRAWALS_ACTION,
                () -> GovernanceTxHelper.treasuryWithdrawalsAction(account0.stakeAddress(), secondWithdrawalAmount));
        castTreasuryWithdrawalPassingVotes(secondWithdrawal);

        int expiryEpoch = secondWithdrawal.expiryStatusEpoch();
        waitForEpoch(expiryEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, expiryEpoch,
                () -> diagnostics("treasury withdrawal above remaining treasury", List.of(firstWithdrawal, secondWithdrawal)));

        assertProposalStatus("treasury withdrawal above remaining treasury", secondWithdrawal, GovActionStatus.EXPIRED);
    }

    private void preparePostBootstrapGovernanceActors() {
        try {
            topUpFund(account0.baseAddress(), CONTROLLED_STAKE_TOP_UP_ADA);
            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, account0.stakeAddress()));

            spoTestPool = governanceTxHelper.registerTestStakePool(account0);
            governanceTxHelper.delegateStakeToTestPool(account0, account0, spoTestPool);

            governanceTxHelper.registerDRep(account0, drepAccount, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.delegateVotingPowerToDRep(account0, account0, com.bloxbean.cardano.client.governance.GovId.toDrep(drepAccount.drepId()));

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
            governanceTxHelper.castDRepVote(account0, drepAccount, initialCommitteeProposal.storeGovActionId(), Vote.YES);
            governanceTxHelper.castTestStakePoolVote(account0, spoTestPool, initialCommitteeProposal.storeGovActionId(), Vote.YES);

            int ratifyEpoch = initialCommitteeProposal.createdEpoch() + 1;
            waitForEpoch(ratifyEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, ratifyEpoch,
                    () -> diagnostics("initial committee setup", List.of(initialCommitteeProposal)));
            assertProposalStatus("initial committee setup", initialCommitteeProposal, GovActionStatus.RATIFIED);
            currentCommitteePrevGovActionId = initialCommitteeProposal.txGovActionId();

            waitForEpoch(initialCommitteeProposal.createdEpoch() + 2);
            for (Account committeeAccount : committeeAccounts) {
                governanceTxHelper.authorizeCommitteeHotKey(account0, committeeAccount, committeeAccount);
            }
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Governance inter-proposal actor setup failed.\n"
                    + diagnostics("common setup", initialCommitteeProposal == null ? List.of() : List.of(initialCommitteeProposal)), e);
        }
    }

    private CreatedProposal createSingleProposalInFreshEpoch(String name,
                                                             GovActionType expectedType,
                                                             Supplier<GovAction> actionSupplier) {
        return createProposalsInSameEpoch(proposalSpec(name, expectedType, actionSupplier)).getFirst();
    }

    private List<CreatedProposal> createProposalsInSameEpoch(ProposalSpec... specs) {
        waitForEpoch(getCurrentEpoch() + 1);
        int proposalEpoch = getCurrentEpoch();

        List<CreatedProposal> proposals = Arrays.stream(specs)
                .map(spec -> {
                    CreatedProposal proposal = governanceTxHelper.createProposalAndWait(account0, account0.stakeAddress(), spec.actionSupplier().get());
                    assertIndexedProposal(proposal, spec.expectedType());
                    assertThat(proposal.createdEpoch())
                            .as("proposal '%s' must be created in epoch %s", spec.name(), proposalEpoch)
                            .isEqualTo(proposalEpoch);
                    assertLedgerActive(proposal);
                    return proposal;
                })
                .toList();

        assertThat(proposals)
                .as("all proposals must be created in the same epoch")
                .extracting(CreatedProposal::createdEpoch)
                .containsOnly(proposalEpoch);
        return proposals;
    }

    private void castPassingVotes(GovActionType actionType, CreatedProposal proposal) {
        governanceTxHelper.castDRepVote(account0, drepAccount, proposal.storeGovActionId(), Vote.YES);

        if (actionType == GovActionType.HARD_FORK_INITIATION_ACTION || actionType == GovActionType.UPDATE_COMMITTEE) {
            governanceTxHelper.castTestStakePoolVote(account0, spoTestPool, proposal.storeGovActionId(), Vote.YES);
        }

        if (actionType == GovActionType.NEW_CONSTITUTION
                || actionType == GovActionType.PARAMETER_CHANGE_ACTION
                || actionType == GovActionType.HARD_FORK_INITIATION_ACTION) {
            castCommitteeYesVotes(proposal, 2);
        }
    }

    private void castTreasuryWithdrawalPassingVotes(CreatedProposal proposal) {
        governanceTxHelper.castDRepVote(account0, drepAccount, proposal.storeGovActionId(), Vote.YES);
        castCommitteeYesVotes(proposal, 2);
    }

    private void waitForRatification(String scenarioName, CreatedProposal proposal) {
        int ratifyEpoch = proposal.createdEpoch() + 1;
        waitForEpoch(ratifyEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, ratifyEpoch,
                () -> diagnostics(scenarioName, List.of(proposal)));
        assertProposalStatus(scenarioName, proposal, GovActionStatus.RATIFIED);
    }

    private void assertProposalStatus(String scenarioName, CreatedProposal proposal, GovActionStatus expectedStatus) {
        try {
            governanceRuleAssertionHelper.assertMatchesLedger(proposal.storeGovActionId(), expectedStatus);
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Governance inter-proposal assertion failed.\n"
                    + diagnostics(scenarioName, List.of(proposal)), e);
        }
    }

    private void assertDbStatusAtEpoch(String scenarioName,
                                       CreatedProposal proposal,
                                       int epoch,
                                       GovActionStatus expectedStatus) {
        try {
            governanceRuleAssertionHelper.assertDbStatusAtEpoch(proposal.storeGovActionId(), epoch, expectedStatus);
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Governance inter-proposal DB assertion failed at epoch " + epoch + ".\n"
                    + diagnostics(scenarioName, List.of(proposal)), e);
        }
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

    private void assertLedgerActiveAndDelayed(String scenarioName, CreatedProposal proposal) {
        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    ProposalLedgerSnapshot snapshot = ledgerStateReader.fetchProposalState(proposal.storeGovActionId());
                    assertThat(snapshot.presentInCurrentProposals())
                            .as("%s ledger active delayed proposal for %s: %s", scenarioName, proposal.storeGovActionId(), snapshot)
                            .isTrue();
                    assertThat(snapshot.ratificationDelayed())
                            .as("%s ledger ratificationDelayed flag for %s: %s", scenarioName, proposal.storeGovActionId(), snapshot)
                            .isTrue();
                });
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

    private BigInteger seedTreasuryForContextTest(String scenarioName) {
        waitForEpoch(getCurrentEpoch() + 1);
        BigInteger treasuryBeforeDonation = ledgerTreasury(scenarioName + " before treasury seed");
        int donationEpoch = getCurrentEpoch();

        governanceTxHelper.donateToTreasury(account0, treasuryBeforeDonation, TREASURY_CONTEXT_SEED_LOVELACE);

        int treasuryReadyEpoch = donationEpoch + 1;
        waitForEpoch(treasuryReadyEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, treasuryReadyEpoch,
                () -> diagnostics(scenarioName + " treasury seed", List.of()));

        BigInteger treasuryAfterDonation = ledgerTreasury(scenarioName + " after treasury seed");
        assertThat(treasuryAfterDonation)
                .as("%s treasury after seed donation", scenarioName)
                .isGreaterThan(treasuryBeforeDonation);
        return treasuryAfterDonation;
    }

    private BigInteger ledgerTreasury(String scenarioName) {
        try {
            return ledgerStateReader.fetchTreasury();
        } catch (RuntimeException e) {
            throw new AssertionError("Ledger treasury is not available.\n"
                    + diagnostics(scenarioName, List.of()), e);
        }
    }

    private GovAction initialCommitteeAction() {
        Map<com.bloxbean.cardano.client.address.Credential, Integer> members = new LinkedHashMap<>();
        int term = getCurrentEpoch() + 100;
        for (Account committeeAccount : committeeAccounts) {
            members.put(committeeAccount.committeeColdCredential(), term);
        }

        return GovernanceTxHelper.updateCommitteeAction(members, COMMITTEE_THRESHOLD);
    }

    private GovAction hardForkInitiationAction() {
        return GovernanceTxHelper.hardForkInitiationAction(HARD_FORK_TEST_MAJOR_VERSION, HARD_FORK_TEST_MINOR_VERSION);
    }

    private GovAction nonSecurityParameterChangeAction(int maxExecutionUnitsMemOffset) {
        return GovernanceTxHelper.parameterChangeAction(
                ProtocolParamUpdate.builder()
                        .maxTxExUnits(new ExUnits(
                                BigInteger.valueOf(30_000_000L + maxExecutionUnitsMemOffset),
                                BigInteger.valueOf(12_000_000_000L)))
                        .build(),
                false);
    }

    private GovAction committeeUpdateAction() {
        UpdateCommittee updateCommittee = GovernanceTxHelper.updateCommitteeAction(COMMITTEE_THRESHOLD);
        updateCommittee.setPrevGovActionId(currentCommitteePrevGovActionId);
        return updateCommittee;
    }

    private void castCommitteeYesVotes(CreatedProposal proposal, int count) {
        for (int i = 0; i < count; i++) {
            governanceTxHelper.castCommitteeHotVote(account0, committeeAccounts.get(i), proposal.storeGovActionId(), Vote.YES);
        }
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

    private ProposalSpec proposalSpec(String name, GovActionType expectedType, Supplier<GovAction> actionSupplier) {
        return new ProposalSpec(name, expectedType, actionSupplier);
    }

    private String diagnostics(String scenarioName, CreatedProposal proposal) {
        return diagnostics(scenarioName, proposal == null ? List.of() : List.of(proposal));
    }

    private String diagnostics(String scenarioName, List<CreatedProposal> proposals) {
        StringBuilder message = new StringBuilder();
        message.append("scenario=").append(scenarioName);
        message.append("\ncurrentEpoch=").append(safeCurrentEpoch());
        message.append("\nlastCompletedAdaPotEpoch=").append(lastCompletedAdaPotEpoch());
        message.append("\nprofile=").append(profileSummary());

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
        return "post-bootstrap protocolMajorVer=10"
                + ", epochLengthSeconds=" + EPOCH_LENGTH_SECONDS
                + ", govActionLifetime=" + GOV_ACTION_LIFETIME
                + ", threshold=" + THRESHOLD
                + ", committeeThreshold=51/100"
                + ", controlledStakeTopUpAda=" + CONTROLLED_STAKE_TOP_UP_ADA
                + ", spoTestPool=" + (spoTestPool == null ? "not-registered" : spoTestPool.poolId())
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

    private record ProposalSpec(String name,
                                GovActionType expectedType,
                                Supplier<GovAction> actionSupplier) {
    }
}
