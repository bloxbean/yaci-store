package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import com.bloxbean.cardano.client.spec.UnitInterval;
import com.bloxbean.cardano.client.transaction.spec.ProtocolParamUpdate;
import com.bloxbean.cardano.client.transaction.spec.cert.StakePoolId;
import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovAction;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.client.util.HexUtil;
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
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verifies vote aggregation semantics that need real DevKit stake snapshots.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = GovernanceVoteTallyIT.DevKitInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GovernanceVoteTallyIT extends BaseE2ETest {
    private static final int EPOCH_LENGTH_SECONDS = 20;
    private static final int GOV_ACTION_LIFETIME = 3;
    private static final BigDecimal THRESHOLD = new BigDecimal("0.51");
    private static final BigDecimal DREP_UPDATE_TO_CONSTITUTION_THRESHOLD = new BigDecimal("0.75");
    private static final UnitInterval COMMITTEE_THRESHOLD = new UnitInterval(BigInteger.valueOf(51), BigInteger.valueOf(100));
    private static final int COMMITTEE_MIN_SIZE = 2;

    // DevKit default wallets start with 10,000 ADA; the comments in each test include that base stake.
    private static final long FEE_PAYER_TOP_UP_ADA = 3_000_000L;
    private static final long SETUP_POOL_TOP_UP_ADA = 3_000_000L;
    private static final long DREP_YES_TOP_UP_ADA = 900_000L;
    private static final long DREP_AUTO_ABSTAIN_TOP_UP_ADA = 300_000L;
    private static final long DREP_NO_TOP_UP_ADA = 200_000L;
    private static final long SPO_NO_POOL_TOP_UP_ADA = 833_333L;
    private static final long SPO_DO_NOT_VOTE_POOL_TOP_UP_ADA = 500_000L;

    private GovernanceTxHelper governanceTxHelper;
    private GovernanceRuleAssertionHelper governanceRuleAssertionHelper;
    private LedgerGovernanceStateReader ledgerStateReader;

    private Account setupDRepAccount;
    private Account drepYesAccount;
    private Account drepNoAccount;
    private Account drepAlwaysAbstainDelegator;
    private List<Account> committeeAccounts;

    private TestStakePool setupStakePool;
    private TestStakePool spoNoPool;
    private TestStakePool spoDoNotVotePool;
    private final List<TestStakePool> knownStakePools = new ArrayList<>();
    private final Set<Integer> authorizedCommitteeHotKeyIndexes = new HashSet<>();

    private CreatedProposal initialCommitteeProposal;
    private com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId currentCommitteePrevGovActionId;
    private boolean commonGovernanceActorsReady;
    private boolean drepAbstainFixtureReady;
    private boolean spoMultiPoolFixtureReady;

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

    @BeforeAll
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
        drepAlwaysAbstainDelegator = accountAt(4);
        committeeAccounts = List.of(accountAt(10), accountAt(11), accountAt(12));

        preparePostBootstrapGovernanceActors();
    }

    static class DevKitInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            assertDevKitAdminAvailable();
            Map<String, String> config = standardGovernanceConfig(EPOCH_LENGTH_SECONDS, GOV_ACTION_LIFETIME, THRESHOLD);
            config.put("committeeMinSize", String.valueOf(COMMITTEE_MIN_SIZE));
            createDevNet(config);
        }
    }

    /**
     * DRep abstain stake must be excluded from the yes/(yes+no) denominator.
     * The yes/no split intentionally clears the node's default NewConstitution DRep threshold.
     */
    @Test
    @Order(1)
    void dRepAbstainDenominator() {
        ensureDRepAbstainFixture();

        BigInteger yesStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(drepYesAccount.drepId()));
        BigInteger noStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(drepNoAccount.drepId()));
        AtomicReference<BigInteger> autoAbstainStake = new AtomicReference<>();

        assertProposalScenario(
                "DRep abstain stake is excluded from accepted-ratio denominator",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction,
                proposal -> {
                    // Fixture math: YES = 900,000 + 10,000, NO = 200,000 + 10,000.
                    // yes/(yes+no) = 910,000 / 1,120,000 = 0.8125, above the ledger's 0.75 threshold.
                    // AlwaysAbstain has 300,000 + 10,000 ADA and must stay outside that denominator.
                    governanceTxHelper.castDRepVote(account0, drepYesAccount, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castDRepVote(account0, drepNoAccount, proposal.storeGovActionId(), Vote.NO);
                    castCommitteeYesVotes(proposal, COMMITTEE_MIN_SIZE);
                },
                GovActionStatus.RATIFIED,
                stats -> {
                    // account0 is delegated to AlwaysAbstain and pays the scenario tx fees. Read this after the
                    // status epoch so the expected value uses the same ratification stake snapshot as yaci-store.
                    if (autoAbstainStake.get() == null) {
                        autoAbstainStake.set(ledgerDRepStake(com.bloxbean.cardano.client.transaction.spec.governance.DRep.abstain()));
                    }
                    assertStake("DRep YES stake", stats.getDrepYesVoteStake(), yesStake);
                    assertStake("DRep NO stake", stats.getDrepNoVoteStake(), noStake);
                    assertStake("DRep auto-abstain stake", stats.getDrepAutoAbstainStake(), autoAbstainStake.get());
                    assertStake("DRep total abstain stake", stats.getDrepTotalAbstainStake(), autoAbstainStake.get());
                    assertThat(nz(stats.getDrepAbstainVoteStake())).isZero();
                    assertThat(nz(stats.getDrepApprovalRatio())).isGreaterThan(DREP_UPDATE_TO_CONSTITUTION_THRESHOLD);
                    assertCommitteePassed(stats);
                });
    }

    /**
     * A later vote by the same voter/proposal pair must replace the earlier vote.
     */
    @Test
    @Order(2)
    void latestVoteWins() {
        ensureDRepAbstainFixture();

        BigInteger drepStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(drepYesAccount.drepId()));

        assertProposalScenario(
                "latest DRep vote replaces the earlier vote",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction,
                proposal -> {
                    // The same 910,000 ADA DRep bucket votes YES then NO.
                    // Replacement leaves YES = 0 and NO = 910,000, so yes/(yes+no) = 0.
                    governanceTxHelper.castDRepVote(account0, drepYesAccount, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castDRepVote(account0, drepYesAccount, proposal.storeGovActionId(), Vote.NO);
                    castCommitteeYesVotes(proposal, COMMITTEE_MIN_SIZE);
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertStake("replaced DRep YES stake", stats.getDrepYesVoteStake(), BigInteger.ZERO);
                    assertStake("latest DRep NO stake", stats.getDrepNoVoteStake(), drepStake);
                    assertThat(nz(stats.getDrepApprovalRatio())).isZero();
                    assertCommitteePassed(stats);
                });
    }

    /**
     * SPO tallies must use real pool stake snapshots, including non-voting pool stake.
     */
    @Test
    @Order(3)
    void spoMultiPoolStake() {
        ensureDRepAbstainFixture();
        ensureSPOMultiPoolFixture();

        AtomicReference<BigInteger> yesPoolStake = new AtomicReference<>();
        BigInteger noPoolStake = ledgerSPOStake(spoNoPool);
        BigInteger doNotVotePoolStake = ledgerSPOStake(spoDoNotVotePool);

        assertProposalScenario(
                "SPO multi-pool stake distribution drives accepted ratio",
                GovActionType.UPDATE_COMMITTEE,
                this::committeeUpdateAction,
                proposal -> {
                    // Controlled SPO buckets are roughly:
                    // YES = 3,000,000 + 10,000, NO = 833,333 + 10,000, do-not-vote = 500,000 + 10,000.
                    // 3,010,000 / (3,010,000 + 843,333 + 510,000) ~= 0.69, above the 0.51 threshold.
                    governanceTxHelper.castDRepVote(account0, drepYesAccount, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castTestStakePoolVote(account0, setupStakePool, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castTestStakePoolVote(account0, spoNoPool, proposal.storeGovActionId(), Vote.NO);
                    yesPoolStake.set(ledgerSPOStake(setupStakePool));
                },
                GovActionStatus.RATIFIED,
                stats -> {
                    assertStake("SPO YES stake", stats.getSpoYesVoteStake(), yesPoolStake.get());
                    assertStake("SPO NO stake", stats.getSpoNoVoteStake(), noPoolStake);
                    assertThat(nz(stats.getSpoDoNotVoteStake())).isGreaterThanOrEqualTo(doNotVotePoolStake);
                    assertThat(nz(stats.getSpoApprovalRatio())).isGreaterThan(THRESHOLD);
                });
    }

    /**
     * A parameter change that touches SECURITY plus another group must require SPO, DRep, and committee support.
     */
    @Test
    @Order(4)
    void mixedProtocolParameterGroups() {
        ensureDRepAbstainFixture();
        ensureSPOMultiPoolFixture();

        BigInteger drepStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(drepYesAccount.drepId()));
        BigInteger spoYesStake = knownStakePools.stream()
                .map(this::ledgerSPOStake)
                .reduce(BigInteger.ZERO, BigInteger::add);

        assertProposalScenario(
                "mixed security and network parameter change fails when DRep rejects",
                GovActionType.PARAMETER_CHANGE_ACTION,
                () -> mixedSecurityAndNetworkParameterChangeAction(100_000),
                proposal -> {
                    // DRep rejection uses the 910,000 ADA DRep bucket as NO, so the DRep ratio is 0.
                    // This keeps the proposal expired even though all known pools and two committee members vote YES.
                    governanceTxHelper.castDRepVote(account0, drepYesAccount, proposal.storeGovActionId(), Vote.NO);
                    castAllKnownStakePoolVotes(proposal, Vote.YES);
                    castCommitteeYesVotes(proposal, COMMITTEE_MIN_SIZE);
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertStake("DRep rejecting stake", stats.getDrepNoVoteStake(), drepStake);
                    assertStake("DRep YES stake", stats.getDrepYesVoteStake(), BigInteger.ZERO);
                    assertStake("SPO YES stake", stats.getSpoYesVoteStake(), spoYesStake);
                    assertThat(nz(stats.getDrepApprovalRatio())).isZero();
                    assertThat(nz(stats.getSpoApprovalRatio())).isGreaterThanOrEqualTo(THRESHOLD);
                    assertCommitteePassed(stats);
                });

        assertProposalScenario(
                "mixed security and network parameter change ratifies when all groups pass",
                GovActionType.PARAMETER_CHANGE_ACTION,
                () -> mixedSecurityAndNetworkParameterChangeAction(100_001),
                proposal -> {
                    // DRep YES uses the 910,000 ADA bucket with no explicit DRep NO vote, so the DRep ratio passes.
                    // All generated pools vote YES, and committee approval is 2 / 3 = 0.666..., above 0.51.
                    governanceTxHelper.castDRepVote(account0, drepYesAccount, proposal.storeGovActionId(), Vote.YES);
                    castAllKnownStakePoolVotes(proposal, Vote.YES);
                    castCommitteeYesVotes(proposal, COMMITTEE_MIN_SIZE);
                },
                GovActionStatus.RATIFIED,
                stats -> {
                    assertStake("DRep YES stake", stats.getDrepYesVoteStake(), drepStake);
                    assertStake("DRep NO stake", stats.getDrepNoVoteStake(), BigInteger.ZERO);
                    assertStake("SPO YES stake", stats.getSpoYesVoteStake(), spoYesStake);
                    assertThat(nz(stats.getDrepApprovalRatio())).isGreaterThanOrEqualTo(THRESHOLD);
                    assertThat(nz(stats.getSpoApprovalRatio())).isGreaterThanOrEqualTo(THRESHOLD);
                    assertCommitteePassed(stats);
                });
    }

    /**
     * Outside bootstrap, a committee smaller than committeeMinSize is treated as if there is no committee.
     */
    @Test
    @Order(5)
    void committeeQuorumMinSize() {
        ensureDRepAbstainFixture();
        ratifyCommitteeReductionBelowMinimum();

        assertProposalScenario(
                "new constitution expires when active committee size is below committeeMinSize",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction,
                proposal -> {
                    // The prior committee update leaves 1 active committee member while committeeMinSize = 2.
                    // The ledger treats that as no committee, so the committee ratio is 0 despite DRep YES stake.
                    governanceTxHelper.castDRepVote(account0, drepYesAccount, proposal.storeGovActionId(), Vote.YES);
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertThat(nz(stats.getDrepYesVoteStake())).isPositive();
                    assertThat(nz(stats.getDrepNoVoteStake())).isZero();
                    assertThat(nz(stats.getCcYes())).isZero();
                    assertThat(nz(stats.getCcNo())).isZero();
                    assertThat(nz(stats.getCcAbstain())).isZero();
                    assertThat(nz(stats.getCcDoNotVote())).isLessThan(COMMITTEE_MIN_SIZE);
                    assertThat(nz(stats.getCcApprovalRatio())).isZero();
                });
    }

    private void preparePostBootstrapGovernanceActors() {
        if (commonGovernanceActorsReady) {
            return;
        }

        try {
            Account setupPoolOwner = accountAt(20);

            topUpFund(account0.baseAddress(), FEE_PAYER_TOP_UP_ADA);
            topUpFund(setupPoolOwner.baseAddress(), SETUP_POOL_TOP_UP_ADA);
            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, account0.stakeAddress()));

            setupStakePool = governanceTxHelper.registerGeneratedStakePool(account0, setupPoolOwner);
            knownStakePools.add(setupStakePool);
            governanceTxHelper.delegateStakeToTestPool(account0, setupPoolOwner, setupStakePool);

            governanceTxHelper.registerDRep(account0, setupDRepAccount, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.delegateVotingPowerToDRep(account0, account0, com.bloxbean.cardano.client.governance.GovId.toDrep(setupDRepAccount.drepId()));

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
            waitTillAdaPotJobDone(adaPotJobRepository, ratifyEpoch,
                    () -> diagnostics("initial committee setup", initialCommitteeProposal));
            governanceRuleAssertionHelper.assertMatchesLedger(
                    initialCommitteeProposal.storeGovActionId(),
                    GovActionStatus.RATIFIED);
            currentCommitteePrevGovActionId = initialCommitteeProposal.txGovActionId();

            waitForEpoch(initialCommitteeProposal.createdEpoch() + 2);
            for (int i = 0; i < committeeAccounts.size(); i++) {
                Account committeeAccount = committeeAccounts.get(i);
                governanceTxHelper.authorizeCommitteeHotKey(account0, committeeAccount, committeeAccount);
                authorizedCommitteeHotKeyIndexes.add(i);
            }

            // Ledger counts an ordinary registered DRep that does not vote as No.
            // Move setup stake to AlwaysAbstain after the bootstrap committee proposal so it does not mask 04b rows.
            governanceTxHelper.delegateVotingPowerToAlwaysAbstain(account0, account0);
            int setupStakeNeutralizedEpoch = getCurrentEpoch() + 2;
            waitForEpoch(setupStakeNeutralizedEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, setupStakeNeutralizedEpoch);

            commonGovernanceActorsReady = true;
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Governance vote tally common actor setup failed.\n"
                    + diagnostics("common setup", initialCommitteeProposal), e);
        }
    }

    private void ensureDRepAbstainFixture() {
        if (drepAbstainFixtureReady) {
            return;
        }

        try {
            topUpFund(drepYesAccount.baseAddress(), DREP_YES_TOP_UP_ADA);
            topUpFund(drepAlwaysAbstainDelegator.baseAddress(), DREP_AUTO_ABSTAIN_TOP_UP_ADA);
            topUpFund(drepNoAccount.baseAddress(), DREP_NO_TOP_UP_ADA);

            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, drepYesAccount.stakeAddress()));
            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, drepAlwaysAbstainDelegator.stakeAddress()));
            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, drepNoAccount.stakeAddress()));

            governanceTxHelper.registerDRep(account0, drepYesAccount, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.registerDRep(account0, drepNoAccount, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.delegateVotingPowerToDRep(account0, drepYesAccount, com.bloxbean.cardano.client.governance.GovId.toDrep(drepYesAccount.drepId()));
            governanceTxHelper.delegateVotingPowerToAlwaysAbstain(account0, drepAlwaysAbstainDelegator);
            governanceTxHelper.delegateVotingPowerToDRep(account0, drepNoAccount, com.bloxbean.cardano.client.governance.GovId.toDrep(drepNoAccount.drepId()));

            int votingPowerReadyEpoch = getCurrentEpoch() + 2;
            waitForEpoch(votingPowerReadyEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, votingPowerReadyEpoch);
            drepAbstainFixtureReady = true;
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("DRep abstain fixture setup failed.\n"
                    + diagnostics("DRep abstain fixture", null), e);
        }
    }

    private void ensureSPOMultiPoolFixture() {
        if (spoMultiPoolFixtureReady) {
            return;
        }

        try {
            Account spoNoPoolOwner = accountAt(21);
            Account spoDoNotVotePoolOwner = accountAt(22);

            spoNoPool = governanceTxHelper.registerGeneratedStakePool(account0, spoNoPoolOwner);
            spoDoNotVotePool = governanceTxHelper.registerGeneratedStakePool(account0, spoDoNotVotePoolOwner);
            knownStakePools.add(spoNoPool);
            knownStakePools.add(spoDoNotVotePool);

            topUpFund(spoNoPoolOwner.baseAddress(), SPO_NO_POOL_TOP_UP_ADA);
            topUpFund(spoDoNotVotePoolOwner.baseAddress(), SPO_DO_NOT_VOTE_POOL_TOP_UP_ADA);
            governanceTxHelper.delegateStakeToTestPool(account0, spoNoPoolOwner, spoNoPool);
            governanceTxHelper.delegateStakeToTestPool(account0, spoDoNotVotePoolOwner, spoDoNotVotePool);

            int stakeReadyEpoch = getCurrentEpoch() + 2;
            waitForEpoch(stakeReadyEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, stakeReadyEpoch);
            spoMultiPoolFixtureReady = true;
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("SPO multi-pool fixture setup failed.\n"
                    + diagnostics("SPO multi-pool fixture", null), e);
        }
    }

    private GovActionProposalStatusEntity assertProposalScenario(String scenarioName,
                                                                 GovActionType expectedType,
                                                                 Supplier<GovAction> actionSupplier,
                                                                 Consumer<CreatedProposal> voteCaster,
                                                                 GovActionStatus expectedStatus,
                                                                 Consumer<ProposalVotingStats> statsAssertions) {
        CreatedProposal proposal = null;
        try {
            waitForEpoch(getCurrentEpoch() + 1);
            proposal = governanceTxHelper.createProposalAndWait(account0, account0.stakeAddress(), actionSupplier.get());
            assertIndexedProposal(proposal, expectedType);
            assertLedgerActive(proposal);

            voteCaster.accept(proposal);

            int statusProcessingEpoch = statusProcessingEpoch(proposal, expectedStatus);
            CreatedProposal scenarioProposal = proposal;
            waitForEpoch(statusProcessingEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, statusProcessingEpoch, () -> diagnostics(scenarioName, scenarioProposal));

            GovActionProposalStatusEntity outcomeRow = governanceRuleAssertionHelper.assertLatestDbStatus(
                    proposal.storeGovActionId(),
                    expectedStatus);
            governanceRuleAssertionHelper.assertVotingStats(
                    proposal.storeGovActionId(),
                    outcomeRow.getEpoch(),
                    statsAssertions);
            governanceRuleAssertionHelper.assertDbStatusMatchesLedgerSnapshot(proposal.storeGovActionId(), expectedStatus);
            recordRatifiedCommitteePreviousAction(expectedType, expectedStatus, proposal);
            return outcomeRow;
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Governance vote tally scenario failed: " + scenarioName + ".\n"
                    + diagnostics(scenarioName, proposal), e);
        }
    }

    private int statusProcessingEpoch(CreatedProposal proposal, GovActionStatus expectedStatus) {
        if (expectedStatus == GovActionStatus.RATIFIED) {
            return proposal.createdEpoch() + 1;
        }

        return proposal.expiryStatusEpoch();
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

    private GovAction initialCommitteeAction() {
        Map<com.bloxbean.cardano.client.address.Credential, Integer> members = new LinkedHashMap<>();
        int term = getCurrentEpoch() + 100;
        for (Account committeeAccount : committeeAccounts) {
            members.put(committeeAccount.committeeColdCredential(), term);
        }

        return GovernanceTxHelper.updateCommitteeAction(members, COMMITTEE_THRESHOLD);
    }

    private GovAction committeeUpdateAction() {
        UpdateCommittee updateCommittee = GovernanceTxHelper.updateCommitteeAction(COMMITTEE_THRESHOLD);
        updateCommittee.setPrevGovActionId(currentCommitteePrevGovActionId);
        return updateCommittee;
    }

    private GovAction committeeBelowMinimumAction() {
        UpdateCommittee updateCommittee = GovernanceTxHelper.updateCommitteeAction(COMMITTEE_THRESHOLD);
        updateCommittee.setPrevGovActionId(currentCommitteePrevGovActionId);
        updateCommittee.setMembersForRemoval(Set.of(
                committeeAccounts.get(1).committeeColdCredential(),
                committeeAccounts.get(2).committeeColdCredential()));
        return updateCommittee;
    }

    private void ratifyCommitteeReductionBelowMinimum() {
        CreatedProposal proposal = null;
        try {
            waitForEpoch(getCurrentEpoch() + 1);
            proposal = governanceTxHelper.createProposalAndWait(account0, account0.stakeAddress(), committeeBelowMinimumAction());
            assertIndexedProposal(proposal, GovActionType.UPDATE_COMMITTEE);
            assertLedgerActive(proposal);

            governanceTxHelper.castDRepVote(account0, drepYesAccount, proposal.storeGovActionId(), Vote.YES);
            governanceTxHelper.castTestStakePoolVote(account0, setupStakePool, proposal.storeGovActionId(), Vote.YES);

            int ratifyEpoch = proposal.createdEpoch() + 1;
            waitForEpoch(ratifyEpoch);
            CreatedProposal committeeReductionProposal = proposal;
            waitTillAdaPotJobDone(adaPotJobRepository, ratifyEpoch,
                    () -> diagnostics("committee reduction below minimum", committeeReductionProposal));
            governanceRuleAssertionHelper.assertMatchesLedger(proposal.storeGovActionId(), GovActionStatus.RATIFIED);
            currentCommitteePrevGovActionId = proposal.txGovActionId();

            int enactmentReadyEpoch = proposal.createdEpoch() + 2;
            waitForEpoch(enactmentReadyEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, enactmentReadyEpoch,
                    () -> diagnostics("committee reduction enactment", committeeReductionProposal));
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Committee reduction below minimum setup failed.\n"
                    + diagnostics("committee reduction below minimum", proposal), e);
        }
    }

    private GovAction mixedSecurityAndNetworkParameterChangeAction(int maxBlockSize) {
        return GovernanceTxHelper.parameterChangeAction(
                ProtocolParamUpdate.builder()
                        .maxBlockSize(maxBlockSize)
                        .build(),
                true);
    }

    private void recordRatifiedCommitteePreviousAction(GovActionType type,
                                                       GovActionStatus status,
                                                       CreatedProposal proposal) {
        if (status == GovActionStatus.RATIFIED && type == GovActionType.UPDATE_COMMITTEE) {
            currentCommitteePrevGovActionId = proposal.txGovActionId();
        }
    }

    private void castCommitteeYesVotes(CreatedProposal proposal, int count) {
        for (int i = 0; i < count; i++) {
            governanceTxHelper.castCommitteeHotVote(account0, committeeAccounts.get(i), proposal.storeGovActionId(), Vote.YES);
        }
    }

    private void castAllKnownStakePoolVotes(CreatedProposal proposal, Vote vote) {
        for (TestStakePool stakePool : knownStakePools) {
            governanceTxHelper.castTestStakePoolVote(account0, stakePool, proposal.storeGovActionId(), vote);
        }
    }

    private BigInteger ledgerDRepStake(com.bloxbean.cardano.client.transaction.spec.governance.DRep dRep) {
        return ledgerStateReader.fetchDRepStakeDistribution(List.of(dRep))
                .values()
                .stream()
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private BigInteger ledgerSPOStake(TestStakePool stakePool) {
        return ledgerStateReader.fetchSPOStakeDistribution(List.of(poolKeyHashHex(stakePool)))
                .values()
                .stream()
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private String poolKeyHashHex(TestStakePool stakePool) {
        return HexUtil.encodeHexString(StakePoolId.fromBech32PoolId(stakePool.poolId()).getPoolKeyHash());
    }

    private void assertStake(String label, BigInteger actual, BigInteger expected) {
        assertThat(nz(actual))
                .as(label)
                .isEqualTo(expected);
    }

    private void assertCommitteePassed(ProposalVotingStats stats) {
        assertThat(nz(stats.getCcYes())).isGreaterThanOrEqualTo(COMMITTEE_MIN_SIZE);
        assertThat(nz(stats.getCcNo())).isZero();
        assertThat(nz(stats.getCcAbstain())).isZero();
        assertThat(nz(stats.getCcApprovalRatio())).isGreaterThanOrEqualTo(THRESHOLD);
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

    private String diagnostics(String scenarioName, CreatedProposal proposal) {
        StringBuilder message = new StringBuilder();
        message.append("scenario=").append(scenarioName);
        message.append("\ncurrentEpoch=").append(safeCurrentEpoch());
        message.append("\nlastCompletedAdaPotEpoch=").append(lastCompletedAdaPotEpoch());
        message.append("\nprofile=").append(profileSummary());

        if (proposal != null) {
            GovActionId govActionId = proposal.storeGovActionId();
            message.append("\nproposal=").append(govActionId);
            message.append("\nproposalStatusRows=").append(governanceRuleAssertionHelper.findProposalStatuses(govActionId));
            message.append("\nledgerSnapshot=").append(safeLedgerSnapshot(govActionId));
        }

        return message.toString();
    }

    private String profileSummary() {
        return "post-bootstrap protocolMajorVer=10, epochLengthSeconds=" + EPOCH_LENGTH_SECONDS
                + ", govActionLifetime=" + GOV_ACTION_LIFETIME
                + ", threshold=" + THRESHOLD
                + ", committeeThreshold=51/100, committeeMinSize=" + COMMITTEE_MIN_SIZE
                + ", setupStakePool=" + (setupStakePool == null ? "not-registered" : setupStakePool.poolId())
                + ", spoNoPool=" + (spoNoPool == null ? "not-registered" : spoNoPool.poolId())
                + ", spoDoNotVotePool=" + (spoDoNotVotePool == null ? "not-registered" : spoDoNotVotePool.poolId())
                + ", setupFlags={commonActors=" + commonGovernanceActorsReady
                + ", drepAbstainFixture=" + drepAbstainFixtureReady
                + ", spoMultiPoolFixture=" + spoMultiPoolFixtureReady
                + ", authorizedCommitteeHotKeys=" + authorizedCommitteeHotKeyIndexes
                + "}";
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
