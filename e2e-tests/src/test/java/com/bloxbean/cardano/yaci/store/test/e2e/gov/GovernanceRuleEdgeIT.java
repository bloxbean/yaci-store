package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import com.bloxbean.cardano.client.spec.UnitInterval;
import com.bloxbean.cardano.client.transaction.spec.ProtocolParamUpdate;
import com.bloxbean.cardano.client.transaction.spec.cert.StakePoolId;
import com.bloxbean.cardano.client.transaction.spec.governance.DRep;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verifies ledger-state edge rules where non-votes or invalid voters are reshaped before threshold checks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = GovernanceRuleEdgeIT.DevKitInitializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GovernanceRuleEdgeIT extends BaseE2ETest {
    private static final int EPOCH_LENGTH_SECONDS = 20;
    private static final int GOV_ACTION_LIFETIME = 3;
    private static final int DREP_ACTIVITY = 6;
    private static final int HARD_FORK_TEST_MAJOR_VERSION = 10;
    private static final int HARD_FORK_TEST_MINOR_VERSION = 1;
    private static final BigDecimal THRESHOLD = new BigDecimal("0.51");
    private static final BigDecimal SPO_RESHAPE_THRESHOLD = new BigDecimal("0.67");
    private static final UnitInterval COMMITTEE_THRESHOLD = new UnitInterval(BigInteger.valueOf(51), BigInteger.valueOf(100));

    private static final long FEE_PAYER_TOP_UP_ADA = 5_000_000L;
    private static final long SETUP_POOL_TOP_UP_ADA = 3_000_000L;
    private static final long DOMINANT_DREP_TOP_UP_ADA = 900_000L;
    private static final long SMALL_DREP_TOP_UP_ADA = 200_000L;
    private static final long ABSTAIN_DREP_TOP_UP_ADA = 300_000L;
    private static final long SPO_ALWAYS_ABSTAIN_POOL_TOP_UP_ADA = 2_000_000L;
    private static final long INACTIVE_DREP_TOP_UP_ADA = 900_000L;
    private static final long ACTIVE_DREP_TOP_UP_ADA = 900_000L;

    private GovernanceTxHelper governanceTxHelper;
    private GovernanceRuleAssertionHelper governanceRuleAssertionHelper;
    private LedgerGovernanceStateReader ledgerStateReader;

    private Account setupDRepAccount;
    private Account setupPoolOwner;
    private List<Account> committeeAccounts;
    private TestStakePool setupStakePool;
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

        setupDRepAccount = accountAt(1);
        setupPoolOwner = accountAt(20);
        committeeAccounts = List.of(accountAt(10), accountAt(11), accountAt(12));

        preparePostBootstrapGovernanceActors();
    }

    static class DevKitInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            assertDevKitAdminAvailable();
            Map<String, String> config = standardGovernanceConfig(EPOCH_LENGTH_SECONDS, GOV_ACTION_LIFETIME, THRESHOLD);
            config.put("dRepActivity", String.valueOf(DREP_ACTIVITY));
            config.put("pvtHardForkInitiation", SPO_RESHAPE_THRESHOLD.toPlainString());
            config.put("pvtcommitteeNormal", SPO_RESHAPE_THRESHOLD.toPlainString());
            config.put("committeeMinSize", "1");
            createDevNet(config);
        }
    }

    /**
     * AlwaysNoConfidence stake becomes YES only for a NoConfidence action.
     */
    @Test
    @Disabled("Blocked by yaci-core GovStateQuery decoding after no-confidence ratification")
    void predefinedDRepAlwaysNoConfidence_votesYesOnlyForNoConfidence() {
        Account noConfidenceDelegator = accountAt(2);
        Account normalDRepDelegator = accountAt(3);
        Account normalDRep = accountAt(4);
        DRep normalDRepId = com.bloxbean.cardano.client.governance.GovId.toDrep(normalDRep.drepId());

        prepareAlwaysNoConfidenceDRepFixture(noConfidenceDelegator, normalDRepDelegator, normalDRep);

        BigInteger autoYesStake = ledgerDRepStake(DRep.noConfidence());
        BigInteger normalNoStake = ledgerDRepStake(normalDRepId);

        assertProposalScenario(
                "AlwaysNoConfidence DRep stake supports a no-confidence action",
                GovActionType.NO_CONFIDENCE,
                this::noConfidenceAction,
                proposal -> governanceTxHelper.castTestStakePoolVote(account0, setupStakePool, proposal.storeGovActionId(), Vote.YES),
                GovActionStatus.RATIFIED,
                stats -> {
                    assertStake("AlwaysNoConfidence raw stake", stats.getDrepNoConfidenceStake(), autoYesStake);
                    assertStake("normal non-voting DRep stake", stats.getDrepDoNotVoteStake(), normalNoStake);
                    assertStake("effective DRep YES stake", stats.getDrepTotalYesStake(), autoYesStake);
                    assertStake("effective DRep NO stake", stats.getDrepTotalNoStake(), normalNoStake);
                    assertThat(nz(stats.getDrepApprovalRatio())).isGreaterThanOrEqualTo(THRESHOLD);
                });
    }

    /**
     * AlwaysNoConfidence stake becomes NO for actions other than NoConfidence.
     */
    @Test
    void predefinedDRepAlwaysNoConfidence_votesNoForOtherActions() {
        Account noConfidenceDelegator = accountAt(2);
        Account normalDRepDelegator = accountAt(3);
        Account normalDRep = accountAt(4);
        DRep normalDRepId = com.bloxbean.cardano.client.governance.GovId.toDrep(normalDRep.drepId());

        prepareAlwaysNoConfidenceDRepFixture(noConfidenceDelegator, normalDRepDelegator, normalDRep);

        BigInteger autoNoStake = ledgerDRepStake(DRep.noConfidence());
        BigInteger normalYesStake = ledgerDRepStake(normalDRepId);

        assertProposalScenario(
                "AlwaysNoConfidence DRep stake rejects non-no-confidence actions",
                GovActionType.PARAMETER_CHANGE_ACTION,
                this::networkOnlyParameterChangeAction,
                proposal -> {
                    governanceTxHelper.castDRepVote(account0, normalDRep, proposal.storeGovActionId(), Vote.YES);
                    castCommitteeYesVotes(proposal, 2);
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertStake("normal DRep YES stake", stats.getDrepYesVoteStake(), normalYesStake);
                    assertStake("AlwaysNoConfidence raw stake", stats.getDrepNoConfidenceStake(), autoNoStake);
                    assertStake("effective DRep NO stake", stats.getDrepTotalNoStake(), autoNoStake);
                    assertThat(nz(stats.getDrepApprovalRatio())).isLessThan(THRESHOLD);
                    assertCommitteePassed(stats);
                });
    }

    /**
     * AlwaysAbstain stake is excluded from the DRep yes/(yes+no) denominator.
     */
    @Test
    void predefinedDRepAlwaysAbstain_excludedFromDenominator() {
        Account abstainDelegator = accountAt(2);
        Account yesDRepDelegator = accountAt(3);
        Account yesDRep = accountAt(4);
        DRep yesDRepId = com.bloxbean.cardano.client.governance.GovId.toDrep(yesDRep.drepId());

        topUpFund(abstainDelegator.baseAddress(), ABSTAIN_DREP_TOP_UP_ADA);
        topUpFund(yesDRepDelegator.baseAddress(), DOMINANT_DREP_TOP_UP_ADA);
        registerStakeAddresses(abstainDelegator, yesDRepDelegator);

        governanceTxHelper.registerDRep(account0, yesDRep, GovernanceTxHelper.defaultAnchor());
        governanceTxHelper.delegateVotingPowerToAlwaysAbstain(account0, abstainDelegator);
        governanceTxHelper.delegateVotingPowerToDRep(account0, yesDRepDelegator, yesDRepId);
        waitForVotingPowerSnapshot();

        BigInteger autoAbstainStake = ledgerDRepStake(DRep.abstain());
        BigInteger yesStake = ledgerDRepStake(yesDRepId);

        assertProposalScenario(
                "AlwaysAbstain DRep stake is excluded from DRep denominator",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction,
                proposal -> {
                    governanceTxHelper.castDRepVote(account0, yesDRep, proposal.storeGovActionId(), Vote.YES);
                    castCommitteeYesVotes(proposal, 2);
                },
                GovActionStatus.RATIFIED,
                stats -> {
                    assertStake("DRep YES stake", stats.getDrepYesVoteStake(), yesStake);
                    assertStake("DRep auto-abstain stake", stats.getDrepAutoAbstainStake(), autoAbstainStake);
                    assertStake("DRep total abstain stake", stats.getDrepTotalAbstainStake(), autoAbstainStake);
                    assertStake("effective DRep NO stake", stats.getDrepTotalNoStake(), BigInteger.ZERO);
                    assertThat(nz(stats.getDrepApprovalRatio())).isEqualByComparingTo(BigDecimal.ONE.setScale(4));
                    assertCommitteePassed(stats);
                });
    }

    /**
     * SPO reward-account delegation to AlwaysAbstain is action-specific:
     * hard fork treats non-voting SPO stake as NO, while UpdateCommittee treats it as ABSTAIN.
     */
    @Test
    void spoAlwaysAbstainDefaultVote_reshapedByAction() {
        Account drepAccount = accountAt(2);
        Account spoAlwaysAbstainOwner = accountAt(21);

        topUpFund(drepAccount.baseAddress(), DOMINANT_DREP_TOP_UP_ADA);
        registerStakeAddresses(drepAccount);
        governanceTxHelper.registerDRep(account0, drepAccount, GovernanceTxHelper.defaultAnchor());
        governanceTxHelper.delegateVotingPowerToDRep(account0, drepAccount, com.bloxbean.cardano.client.governance.GovId.toDrep(drepAccount.drepId()));

        topUpFund(spoAlwaysAbstainOwner.baseAddress(), SPO_ALWAYS_ABSTAIN_POOL_TOP_UP_ADA);
        TestStakePool spoAlwaysAbstainPool = governanceTxHelper.registerGeneratedStakePool(account0, spoAlwaysAbstainOwner);
        governanceTxHelper.delegateStakeToTestPool(account0, spoAlwaysAbstainOwner, spoAlwaysAbstainPool);
        governanceTxHelper.delegateVotingPowerToAlwaysAbstain(account0, spoAlwaysAbstainOwner);
        waitForVotingPowerSnapshot();

        AtomicReference<BigInteger> hardForkYesStake = new AtomicReference<>();
        BigInteger alwaysAbstainPoolStake = ledgerSPOStake(spoAlwaysAbstainPool);

        assertProposalScenario(
                "HardForkInitiation counts non-voting AlwaysAbstain SPO stake as NO",
                GovActionType.HARD_FORK_INITIATION_ACTION,
                this::hardForkInitiationAction,
                proposal -> {
                    governanceTxHelper.castDRepVote(account0, drepAccount, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castTestStakePoolVote(account0, setupStakePool, proposal.storeGovActionId(), Vote.YES);
                    castCommitteeYesVotes(proposal, 2);
                    hardForkYesStake.set(ledgerSPOStake(setupStakePool));
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertStake("hard-fork SPO YES stake", stats.getSpoYesVoteStake(), hardForkYesStake.get());
                    assertThat(nz(stats.getSpoDoNotVoteStake())).isGreaterThanOrEqualTo(alwaysAbstainPoolStake);
                    assertStake("hard-fork effective SPO abstain stake", stats.getSpoTotalAbstainStake(), BigInteger.ZERO);
                    assertThat(nz(stats.getSpoApprovalRatio())).isLessThan(SPO_RESHAPE_THRESHOLD);
                    assertCommitteePassed(stats);
                });

        AtomicReference<BigInteger> updateCommitteeYesStake = new AtomicReference<>();

        assertProposalScenario(
                "UpdateCommittee counts non-voting AlwaysAbstain SPO stake as ABSTAIN",
                GovActionType.UPDATE_COMMITTEE,
                this::committeeUpdateAction,
                proposal -> {
                    governanceTxHelper.castDRepVote(account0, drepAccount, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castTestStakePoolVote(account0, setupStakePool, proposal.storeGovActionId(), Vote.YES);
                    updateCommitteeYesStake.set(ledgerSPOStake(setupStakePool));
                },
                GovActionStatus.RATIFIED,
                stats -> {
                    assertStake("update-committee SPO YES stake", stats.getSpoYesVoteStake(), updateCommitteeYesStake.get());
                    assertThat(nz(stats.getSpoTotalAbstainStake())).isGreaterThanOrEqualTo(alwaysAbstainPoolStake);
                    assertThat(nz(stats.getSpoApprovalRatio())).isGreaterThanOrEqualTo(SPO_RESHAPE_THRESHOLD);
                });
    }

    /**
     * Inactive DRep stake must be removed from the accepted-ratio denominator.
     */
    @Test
    void drepInactive_excludedFromRatio() {
        Account inactiveDRepDelegator = accountAt(2);
        Account inactiveDRep = accountAt(3);
        Account activeDRepDelegator = accountAt(4);
        Account activeDRep = accountAt(5);
        DRep activeDRepId = com.bloxbean.cardano.client.governance.GovId.toDrep(activeDRep.drepId());

        topUpFund(inactiveDRepDelegator.baseAddress(), INACTIVE_DREP_TOP_UP_ADA);
        registerStakeAddresses(inactiveDRepDelegator);
        governanceTxHelper.registerDRep(account0, inactiveDRep, GovernanceTxHelper.defaultAnchor());
        governanceTxHelper.delegateVotingPowerToDRep(account0, inactiveDRepDelegator, com.bloxbean.cardano.client.governance.GovId.toDrep(inactiveDRep.drepId()));
        waitForVotingPowerSnapshot();

        int inactiveReadyEpoch = getCurrentEpoch() + DREP_ACTIVITY + 2;
        waitForEpoch(inactiveReadyEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, inactiveReadyEpoch);

        topUpFund(activeDRepDelegator.baseAddress(), ACTIVE_DREP_TOP_UP_ADA);
        registerStakeAddresses(activeDRepDelegator);
        governanceTxHelper.registerDRep(account0, activeDRep, GovernanceTxHelper.defaultAnchor());
        governanceTxHelper.delegateVotingPowerToDRep(account0, activeDRepDelegator, activeDRepId);
        waitForVotingPowerSnapshot();

        BigInteger activeDRepStake = ledgerDRepStake(activeDRepId);

        assertProposalScenario(
                "inactive DRep stake is excluded from DRep ratio",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction,
                proposal -> {
                    governanceTxHelper.castDRepVote(account0, activeDRep, proposal.storeGovActionId(), Vote.YES);
                    castCommitteeYesVotes(proposal, 2);
                },
                GovActionStatus.RATIFIED,
                stats -> {
                    assertStake("active DRep YES stake", stats.getDrepYesVoteStake(), activeDRepStake);
                    assertStake("effective DRep NO stake", stats.getDrepTotalNoStake(), BigInteger.ZERO);
                    assertThat(nz(stats.getDrepDoNotVoteStake())).isZero();
                    assertThat(nz(stats.getDrepApprovalRatio())).isEqualByComparingTo(BigDecimal.ONE.setScale(4));
                    assertCommitteePassed(stats);
                });
    }

    /**
     * A vote cast by a resigned committee hot key must not count at ratification.
     */
    @Test
    void committeeHotKeyResigned() {
        Account drepAccount = accountAt(2);
        topUpFund(drepAccount.baseAddress(), DOMINANT_DREP_TOP_UP_ADA);
        registerStakeAddresses(drepAccount);
        governanceTxHelper.registerDRep(account0, drepAccount, GovernanceTxHelper.defaultAnchor());
        governanceTxHelper.delegateVotingPowerToDRep(account0, drepAccount, com.bloxbean.cardano.client.governance.GovId.toDrep(drepAccount.drepId()));
        waitForVotingPowerSnapshot();

        assertProposalScenario(
                "resigned committee hot key vote is ignored",
                GovActionType.NEW_CONSTITUTION,
                GovernanceTxHelper::newConstitutionAction,
                proposal -> {
                    governanceTxHelper.castDRepVote(account0, drepAccount, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castCommitteeHotVote(account0, committeeAccounts.get(0), proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.resignCommitteeHotKey(account0, committeeAccounts.get(0), GovernanceTxHelper.defaultAnchor());
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertThat(nz(stats.getDrepYesVoteStake())).isPositive();
                    assertThat(nz(stats.getCcYes())).isZero();
                    assertThat(nz(stats.getCcAbstain())).isGreaterThanOrEqualTo(1);
                    assertThat(nz(stats.getCcApprovalRatio())).isZero();
                });
    }

    private void preparePostBootstrapGovernanceActors() {
        try {
            topUpFund(account0.baseAddress(), FEE_PAYER_TOP_UP_ADA);
            topUpFund(setupPoolOwner.baseAddress(), SETUP_POOL_TOP_UP_ADA);
            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, account0.stakeAddress()));

            setupStakePool = governanceTxHelper.registerGeneratedStakePool(account0, setupPoolOwner);
            governanceTxHelper.delegateStakeToTestPool(account0, setupPoolOwner, setupStakePool);

            governanceTxHelper.registerDRep(account0, setupDRepAccount, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.delegateVotingPowerToDRep(account0, account0, com.bloxbean.cardano.client.governance.GovId.toDrep(setupDRepAccount.drepId()));
            waitForVotingPowerSnapshot();

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
            for (Account committeeAccount : committeeAccounts) {
                governanceTxHelper.authorizeCommitteeHotKey(account0, committeeAccount, committeeAccount);
            }

            governanceTxHelper.delegateVotingPowerToAlwaysAbstain(account0, account0);
            waitForVotingPowerSnapshot();
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Governance rule-edge actor setup failed.\n"
                    + diagnostics("common setup", initialCommitteeProposal), e);
        }
    }

    private void prepareAlwaysNoConfidenceDRepFixture(Account noConfidenceDelegator,
                                                      Account normalDRepDelegator,
                                                      Account normalDRep) {
        topUpFund(noConfidenceDelegator.baseAddress(), DOMINANT_DREP_TOP_UP_ADA);
        topUpFund(normalDRepDelegator.baseAddress(), SMALL_DREP_TOP_UP_ADA);
        registerStakeAddresses(noConfidenceDelegator, normalDRepDelegator);

        governanceTxHelper.registerDRep(account0, normalDRep, GovernanceTxHelper.defaultAnchor());
        governanceTxHelper.delegateVotingPowerToAlwaysNoConfidence(account0, noConfidenceDelegator);
        governanceTxHelper.delegateVotingPowerToDRep(account0, normalDRepDelegator, com.bloxbean.cardano.client.governance.GovId.toDrep(normalDRep.drepId()));
        waitForVotingPowerSnapshot();
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

    private GovAction noConfidenceAction() {
        return GovernanceTxHelper.noConfidenceAction(currentCommitteePrevGovActionId);
    }

    private GovAction networkOnlyParameterChangeAction() {
        return GovernanceTxHelper.parameterChangeAction(
                ProtocolParamUpdate.builder()
                        .maxBlockSize(100_000)
                        .build(),
                false);
    }

    private GovAction hardForkInitiationAction() {
        return GovernanceTxHelper.hardForkInitiationAction(HARD_FORK_TEST_MAJOR_VERSION, HARD_FORK_TEST_MINOR_VERSION);
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
            throw new AssertionError("Governance rule-edge scenario failed: " + scenarioName + ".\n"
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

    private void recordRatifiedCommitteePreviousAction(GovActionType type,
                                                       GovActionStatus status,
                                                       CreatedProposal proposal) {
        if (status == GovActionStatus.RATIFIED
                && (type == GovActionType.UPDATE_COMMITTEE || type == GovActionType.NO_CONFIDENCE)) {
            currentCommitteePrevGovActionId = proposal.txGovActionId();
        }
    }

    private void castCommitteeYesVotes(CreatedProposal proposal, int count) {
        for (int i = 0; i < count; i++) {
            governanceTxHelper.castCommitteeHotVote(account0, committeeAccounts.get(i), proposal.storeGovActionId(), Vote.YES);
        }
    }

    private void registerStakeAddresses(Account... accounts) {
        for (Account account : accounts) {
            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, account.stakeAddress()));
        }
    }

    private void waitForVotingPowerSnapshot() {
        int votingPowerReadyEpoch = getCurrentEpoch() + 2;
        waitForEpoch(votingPowerReadyEpoch);
        waitTillAdaPotJobDone(adaPotJobRepository, votingPowerReadyEpoch);
    }

    private BigInteger ledgerDRepStake(DRep dRep) {
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
        assertThat(nz(stats.getCcYes())).isGreaterThanOrEqualTo(2);
        assertThat(nz(stats.getCcNo())).isZero();
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
        return "post-bootstrap protocolMajorVer=10"
                + ", epochLengthSeconds=" + EPOCH_LENGTH_SECONDS
                + ", govActionLifetime=" + GOV_ACTION_LIFETIME
                + ", dRepActivity=" + DREP_ACTIVITY
                + ", threshold=" + THRESHOLD
                + ", spoReshapeThreshold=" + SPO_RESHAPE_THRESHOLD
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
