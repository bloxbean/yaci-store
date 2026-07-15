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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Shared fixture for vote aggregation semantics that need real DevKit stake snapshots.
 */
abstract class AbstractGovernanceVoteTallyIT extends BaseE2ETest {
    protected static final int EPOCH_LENGTH_SECONDS = 20;
    protected static final int GOV_ACTION_LIFETIME = 3;
    protected static final BigDecimal THRESHOLD = new BigDecimal("0.51");
    protected static final BigDecimal DREP_UPDATE_TO_CONSTITUTION_THRESHOLD = new BigDecimal("0.75");
    protected static final UnitInterval COMMITTEE_THRESHOLD = new UnitInterval(BigInteger.valueOf(51), BigInteger.valueOf(100));
    protected static final int COMMITTEE_MIN_SIZE = 2;

    // DevKit default wallets start with 10,000 ADA; the comments in each test include that base stake.
    protected static final long FEE_PAYER_TOP_UP_ADA = 3_000_000L;
    protected static final long SETUP_POOL_TOP_UP_ADA = 3_000_000L;
    protected static final long DREP_YES_TOP_UP_ADA = 900_000L;
    protected static final long DREP_AUTO_ABSTAIN_TOP_UP_ADA = 300_000L;
    protected static final long DREP_NO_TOP_UP_ADA = 200_000L;
    protected static final long DREP_UNREGISTERED_YES_TOP_UP_ADA = 900_000L;
    protected static final long DREP_UNREGISTER_CONTROL_NO_TOP_UP_ADA = 100_000L;
    protected static final long SPO_NO_POOL_TOP_UP_ADA = 833_333L;
    protected static final long SPO_DO_NOT_VOTE_POOL_TOP_UP_ADA = 500_000L;

    protected GovernanceTxHelper governanceTxHelper;
    protected GovernanceRuleAssertionHelper governanceRuleAssertionHelper;
    protected LedgerGovernanceStateReader ledgerStateReader;

    protected Account setupDRepAccount;
    protected Account drepYesAccount;
    protected Account drepNoAccount;
    protected Account drepAlwaysAbstainDelegator;
    protected List<Account> committeeAccounts;

    protected TestStakePool setupStakePool;
    protected TestStakePool spoNoPool;
    protected TestStakePool spoDoNotVotePool;
    protected final List<TestStakePool> knownStakePools = new ArrayList<>();
    protected final Set<Integer> authorizedCommitteeHotKeyIndexes = new HashSet<>();

    protected CreatedProposal initialCommitteeProposal;
    protected com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId currentCommitteePrevGovActionId;
    protected com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId currentConstitutionPrevGovActionId;
    protected boolean commonGovernanceActorsReady;
    protected boolean drepAbstainFixtureReady;
    protected boolean spoMultiPoolFixtureReady;
    protected boolean drepUnregisterFixtureReady;

    @Autowired
    protected Environment environment;

    @Autowired
    protected ProposalStateClient proposalStateClient;

    @Autowired
    protected GovActionProposalStorage govActionProposalStorage;

    @Autowired
    protected GovActionProposalStatusRepository proposalStatusRepository;

    @Autowired
    protected AdaPotJobRepository adaPotJobRepository;

    @Autowired
    protected AdaPotJobStorage adaPotJobStorage;

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

    protected void ensureDRepAbstainFixture() {
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

    protected void ensureSPOMultiPoolFixture() {
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

    protected void ensureDRepUnregisterFixture() {
        if (drepUnregisterFixtureReady) {
            return;
        }

        try {
            Account removedYesDRep = accountAt(5);
            Account remainingNoDRep = accountAt(6);

            topUpFund(removedYesDRep.baseAddress(), DREP_UNREGISTERED_YES_TOP_UP_ADA);
            topUpFund(remainingNoDRep.baseAddress(), DREP_UNREGISTER_CONTROL_NO_TOP_UP_ADA);

            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, removedYesDRep.stakeAddress()));
            assertTransactionSucceeded(governanceTxHelper.registerStakeAddress(account0, remainingNoDRep.stakeAddress()));

            governanceTxHelper.registerDRep(account0, removedYesDRep, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.registerDRep(account0, remainingNoDRep, GovernanceTxHelper.defaultAnchor());
            governanceTxHelper.delegateVotingPowerToDRep(account0, removedYesDRep, com.bloxbean.cardano.client.governance.GovId.toDrep(removedYesDRep.drepId()));
            governanceTxHelper.delegateVotingPowerToDRep(account0, remainingNoDRep, com.bloxbean.cardano.client.governance.GovId.toDrep(remainingNoDRep.drepId()));

            int votingPowerReadyEpoch = getCurrentEpoch() + 2;
            waitForEpoch(votingPowerReadyEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, votingPowerReadyEpoch);
            drepUnregisterFixtureReady = true;
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("DRep unregister fixture setup failed.\n"
                    + diagnostics("DRep unregister fixture", null), e);
        }
    }

    protected GovActionProposalStatusEntity assertProposalScenario(String scenarioName,
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
            recordRatifiedPreviousAction(expectedType, expectedStatus, proposal);
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

    protected GovAction committeeUpdateAction() {
        UpdateCommittee updateCommittee = GovernanceTxHelper.updateCommitteeAction(COMMITTEE_THRESHOLD);
        updateCommittee.setPrevGovActionId(currentCommitteePrevGovActionId);
        return updateCommittee;
    }

    protected GovAction newConstitutionAction() {
        if (currentConstitutionPrevGovActionId == null) {
            return GovernanceTxHelper.newConstitutionAction();
        }

        return GovernanceTxHelper.newConstitutionAction(currentConstitutionPrevGovActionId);
    }

    private GovAction committeeBelowMinimumAction() {
        UpdateCommittee updateCommittee = GovernanceTxHelper.updateCommitteeAction(COMMITTEE_THRESHOLD);
        updateCommittee.setPrevGovActionId(currentCommitteePrevGovActionId);
        updateCommittee.setMembersForRemoval(Set.of(
                committeeAccounts.get(1).committeeColdCredential(),
                committeeAccounts.get(2).committeeColdCredential()));
        return updateCommittee;
    }

    protected void ratifyCommitteeReductionBelowMinimum() {
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

    protected GovAction mixedSecurityAndNetworkParameterChangeAction(int maxBlockSize) {
        return GovernanceTxHelper.parameterChangeAction(
                ProtocolParamUpdate.builder()
                        .maxBlockSize(maxBlockSize)
                        .build(),
                true);
    }

    private void recordRatifiedPreviousAction(GovActionType type,
                                              GovActionStatus status,
                                              CreatedProposal proposal) {
        if (status == GovActionStatus.RATIFIED && type == GovActionType.UPDATE_COMMITTEE) {
            currentCommitteePrevGovActionId = proposal.txGovActionId();
        } else if (status == GovActionStatus.RATIFIED && type == GovActionType.NEW_CONSTITUTION) {
            currentConstitutionPrevGovActionId = proposal.txGovActionId();
        }
    }

    protected void castCommitteeYesVotes(CreatedProposal proposal, int count) {
        for (int i = 0; i < count; i++) {
            governanceTxHelper.castCommitteeHotVote(account0, committeeAccounts.get(i), proposal.storeGovActionId(), Vote.YES);
        }
    }

    protected void castAllKnownStakePoolVotes(CreatedProposal proposal, Vote vote) {
        for (TestStakePool stakePool : knownStakePools) {
            governanceTxHelper.castTestStakePoolVote(account0, stakePool, proposal.storeGovActionId(), vote);
        }
    }

    protected BigInteger ledgerDRepStake(com.bloxbean.cardano.client.transaction.spec.governance.DRep dRep) {
        return ledgerStateReader.fetchDRepStakeDistribution(List.of(dRep))
                .values()
                .stream()
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    protected BigInteger ledgerSPOStake(TestStakePool stakePool) {
        return ledgerStateReader.fetchSPOStakeDistribution(List.of(poolKeyHashHex(stakePool)))
                .values()
                .stream()
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private String poolKeyHashHex(TestStakePool stakePool) {
        return HexUtil.encodeHexString(StakePoolId.fromBech32PoolId(stakePool.poolId()).getPoolKeyHash());
    }

    protected void assertStake(String label, BigInteger actual, BigInteger expected) {
        assertThat(nz(actual))
                .as(label)
                .isEqualTo(expected);
    }

    protected void assertCommitteePassed(ProposalVotingStats stats) {
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

    protected Account accountAt(int index) {
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
                + ", drepUnregisterFixture=" + drepUnregisterFixtureReady
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

    protected BigInteger nz(BigInteger value) {
        return value == null ? BigInteger.ZERO : value;
    }

    protected int nz(Integer value) {
        return value == null ? 0 : value;
    }

    protected BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
