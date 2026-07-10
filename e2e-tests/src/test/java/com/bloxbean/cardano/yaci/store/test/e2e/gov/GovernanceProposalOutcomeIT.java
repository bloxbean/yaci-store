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
 * Cross-checks post-bootstrap governance rule outcomes against cardano-node ledger state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = GovernanceProposalOutcomeIT.DevKitInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GovernanceProposalOutcomeIT extends BaseE2ETest {
    private static final int EPOCH_LENGTH_SECONDS = 20;
    private static final int GOV_ACTION_LIFETIME = 3;
    private static final int HARD_FORK_TEST_MAJOR_VERSION = 10;
    private static final int HARD_FORK_TEST_MINOR_VERSION = 1;
    private static final long CONTROLLED_STAKE_TOP_UP_ADA = 2_000_000L;
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
    private final Set<Integer> authorizedCommitteeHotKeyIndexes = new HashSet<>();
    private boolean accountFundingReady;
    private boolean accountStakeAddressRegistered;
    private boolean testStakePoolRegistered;
    private boolean stakeDelegatedToTestPool;
    private boolean drepRegistered;
    private boolean votingPowerDelegatedToDRep;
    private boolean votingPowerReady;
    private boolean initialCommitteeVotesCast;
    private boolean initialCommitteeRatified;
    private boolean committeeHotKeysAuthorized;

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
        drepAccount = accountAt(1);
        committeeAccounts = List.of(accountAt(2), accountAt(3), accountAt(4));
    }

    static class DevKitInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            assertDevKitAdminAvailable();
            createDevNet(standardGovernanceConfig(EPOCH_LENGTH_SECONDS, GOV_ACTION_LIFETIME, THRESHOLD));
        }
    }

    /**
     * Verifies action-type dispatch in post-bootstrap governance with one accepted and one rejected scenario
     * for each Conway governance action family.
     */
    @Test
    @Order(2)
    void postBootstrapActionTypes_shouldMatchLedgerOutcomes() {
        preparePostBootstrapGovernanceActors();

        List<ProposalOutcomeScenario> scenarios = List.of(
                scenario("info action with votes still expires", GovActionType.INFO_ACTION, GovernanceTxHelper::infoAction,
                        votes(Vote.YES, Vote.YES, 3), GovActionStatus.EXPIRED),
                scenario("info action without votes expires", GovActionType.INFO_ACTION, GovernanceTxHelper::infoAction,
                        votes(null, null, 0), GovActionStatus.EXPIRED),
                scenario("hard fork initiation without committee support expires", GovActionType.HARD_FORK_INITIATION_ACTION, this::hardForkInitiationAction,
                        votes(Vote.YES, Vote.YES, 0), GovActionStatus.EXPIRED),
                scenario("hard fork initiation with DRep, SPO, and committee support ratifies", GovActionType.HARD_FORK_INITIATION_ACTION, this::hardForkInitiationAction,
                        votes(Vote.YES, Vote.YES, 2), GovActionStatus.RATIFIED),
                scenario("new constitution without committee support expires", GovActionType.NEW_CONSTITUTION, GovernanceTxHelper::newConstitutionAction,
                        votes(Vote.YES, null, 0), GovActionStatus.EXPIRED),
                scenario("new constitution with DRep and committee support ratifies", GovActionType.NEW_CONSTITUTION, GovernanceTxHelper::newConstitutionAction,
                        votes(Vote.YES, null, 2), GovActionStatus.RATIFIED),
                scenario("treasury withdrawal without committee support expires", GovActionType.TREASURY_WITHDRAWALS_ACTION, () -> GovernanceTxHelper.treasuryWithdrawalsAction(account0.stakeAddress(), BigInteger.ONE),
                        votes(Vote.YES, null, 0), GovActionStatus.EXPIRED),
                scenario("treasury withdrawal with DRep and committee support ratifies", GovActionType.TREASURY_WITHDRAWALS_ACTION, () -> GovernanceTxHelper.treasuryWithdrawalsAction(account0.stakeAddress(), BigInteger.ONE),
                        votes(Vote.YES, null, 2), GovActionStatus.RATIFIED),
                scenario("security parameter change without SPO support expires", GovActionType.PARAMETER_CHANGE_ACTION, this::securityParameterChangeAction,
                        votes(Vote.YES, null, 2), GovActionStatus.EXPIRED),
                scenario("network parameter change with DRep and committee support ratifies", GovActionType.PARAMETER_CHANGE_ACTION, this::networkOnlyParameterChangeAction,
                        votes(Vote.YES, null, 2), GovActionStatus.RATIFIED),
                scenario("committee update rejected by DRep expires", GovActionType.UPDATE_COMMITTEE, this::committeeUpdateAction,
                        votes(Vote.NO, Vote.YES, 0), GovActionStatus.EXPIRED),
                scenario("committee update with DRep and SPO support ratifies", GovActionType.UPDATE_COMMITTEE, this::committeeUpdateAction,
                        votes(Vote.YES, Vote.YES, 0), GovActionStatus.RATIFIED),
                scenario("no-confidence action rejected by DRep expires", GovActionType.NO_CONFIDENCE, this::noConfidenceAction,
                        votes(Vote.NO, Vote.YES, 0), GovActionStatus.EXPIRED)
                // TODO: Re-enable the no-confidence ratification row after yaci-core GovStateQuery can decode
                // the no-confidence ledger state. The node accepts and yaci-store marks the row RATIFIED, but
                // GovStateQuery.deserializeResult currently throws IndexOutOfBoundsException while decoding
                // the committee/no-confidence state, so the ledger snapshot assertion cannot complete.
                // scenario("no-confidence action with DRep and SPO support ratifies", GovActionType.NO_CONFIDENCE, this::noConfidenceAction,
                //         votes(Vote.YES, Vote.YES, 0), GovActionStatus.RATIFIED)
        );

        assertThat(scenarios).hasSize(13);
        scenarios.forEach(this::assertProposalOutcome);
    }

    /**
     * Verifies post-bootstrap ratification qualifier gates when the vote thresholds are otherwise satisfied.
     *
     * `prevActionAsExpected=false` is not a valid single-proposal scenario: a nonexistent previous action is
     * rejected by the ledger GOV rule before it can enter the proposal set, while a stale-but-valid previous
     * action needs another proposal to enact first. That belongs with the multi-proposal ordering coverage.
     */
    @Test
    @Order(1)
    void postBootstrapRatificationGates_shouldBlockOtherwiseAcceptedProposals() {
        preparePostBootstrapGovernanceActors();

        List<ProposalOutcomeScenario> scenarios = List.of(
                scenario("treasury withdrawal above available treasury expires", GovActionType.TREASURY_WITHDRAWALS_ACTION,
                        () -> GovernanceTxHelper.treasuryWithdrawalsAction(account0.stakeAddress(), BigInteger.valueOf(Long.MAX_VALUE)),
                        votes(Vote.YES, null, 2), GovActionStatus.EXPIRED),
                scenario("committee update with invalid member term expires", GovActionType.UPDATE_COMMITTEE, this::committeeUpdateWithInvalidTermAction,
                        votes(Vote.YES, Vote.YES, 0), GovActionStatus.EXPIRED)
        );

        assertThat(scenarios).hasSize(2);
        scenarios.forEach(this::assertProposalOutcome);
    }

    private void preparePostBootstrapGovernanceActors() {
        if (committeeHotKeysAuthorized) {
            return;
        }

        try {
            if (!accountFundingReady) {
                // The default DevKit pool keeps genesis stake, so the test pool needs controlled stake to cross 0.51.
                topUpFund(account0.baseAddress(), CONTROLLED_STAKE_TOP_UP_ADA);
                accountFundingReady = true;
            }

            if (!accountStakeAddressRegistered) {
                governanceTxHelper.registerStakeAddress(account0, account0.stakeAddress());
                accountStakeAddressRegistered = true;
            }

            if (!testStakePoolRegistered) {
                spoTestPool = governanceTxHelper.registerTestStakePool(account0);
                testStakePoolRegistered = true;
            }

            if (!stakeDelegatedToTestPool) {
                governanceTxHelper.delegateStakeToTestPool(account0, account0, spoTestPool);
                stakeDelegatedToTestPool = true;
            }

            if (!drepRegistered) {
                governanceTxHelper.registerDRep(account0, drepAccount, GovernanceTxHelper.defaultAnchor());
                drepRegistered = true;
            }

            if (!votingPowerDelegatedToDRep) {
                governanceTxHelper.delegateVotingPowerToDRep(account0, account0, com.bloxbean.cardano.client.governance.GovId.toDrep(drepAccount.drepId()));
                votingPowerDelegatedToDRep = true;
            }

            if (!votingPowerReady) {
                // SPO and DRep voting power use epoch snapshots, so wait until AdaPot sees the new delegation.
                int votingPowerReadyEpoch = getCurrentEpoch() + 2;
                waitForEpoch(votingPowerReadyEpoch);
                waitTillAdaPotJobDone(adaPotJobRepository, votingPowerReadyEpoch);
                votingPowerReady = true;
            }

            if (!initialCommitteeRatified) {
                if (initialCommitteeProposal == null) {
                    // Start from a fresh epoch so the setup proposal and its votes have time to land
                    // before the first ratification boundary.
                    waitForEpoch(getCurrentEpoch() + 1);
                    initialCommitteeProposal = governanceTxHelper.createProposalAndWait(
                            account0,
                            account0.stakeAddress(),
                            initialCommitteeAction());
                    assertIndexedProposal(initialCommitteeProposal, GovActionType.UPDATE_COMMITTEE);
                    assertLedgerActive(initialCommitteeProposal);
                }

                if (!initialCommitteeVotesCast) {
                    VotePlan setupVotes = votes(Vote.YES, Vote.YES, 0);
                    setupVotes.cast(initialCommitteeProposal);
                    initialCommitteeVotesCast = true;
                }

                int ratifyEpoch = initialCommitteeProposal.createdEpoch() + 1;
                waitForEpoch(ratifyEpoch);
                waitTillAdaPotJobDone(adaPotJobRepository, ratifyEpoch,
                        () -> diagnostics("initial-committee", initialCommitteeProposal));
                // Actor setup only needs the committee to become available before scenario rows start.
                // SPO distribution/votes can become visible one ratification epoch later on a fast devnet.
                governanceRuleAssertionHelper.assertMatchesLedger(
                        initialCommitteeProposal.storeGovActionId(),
                        GovActionStatus.RATIFIED);
                currentCommitteePrevGovActionId = initialCommitteeProposal.txGovActionId();
                initialCommitteeRatified = true;
            }

            waitForEpoch(initialCommitteeProposal.createdEpoch() + 2);
            for (int i = 0; i < committeeAccounts.size(); i++) {
                if (!authorizedCommitteeHotKeyIndexes.contains(i)) {
                    Account committeeAccount = committeeAccounts.get(i);
                    governanceTxHelper.authorizeCommitteeHotKey(account0, committeeAccount, committeeAccount);
                    authorizedCommitteeHotKeyIndexes.add(i);
                }
            }
            committeeHotKeysAuthorized = true;
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Post-bootstrap governance actor setup failed.\n"
                    + diagnostics("post-bootstrap-setup", initialCommitteeProposal), e);
        }
    }

    private void assertProposalOutcome(ProposalOutcomeScenario scenario) {
        CreatedProposal proposal = null;
        try {
            waitForEpoch(getCurrentEpoch() + 1);
            proposal = governanceTxHelper.createProposalAndWait(account0, account0.stakeAddress(), scenario.govActionSupplier().get());
            assertIndexedProposal(proposal, scenario.expectedType());
            assertLedgerActive(proposal);

            scenario.votePlan().cast(proposal);

            int statusProcessingEpoch = statusProcessingEpoch(proposal, scenario.expectedStatus());
            CreatedProposal scenarioProposal = proposal;
            waitForEpoch(statusProcessingEpoch);
            waitTillAdaPotJobDone(adaPotJobRepository, statusProcessingEpoch, () -> diagnostics(scenario.name(), scenarioProposal));

            GovActionProposalStatusEntity outcomeRow = governanceRuleAssertionHelper.assertLatestDbStatus(
                    proposal.storeGovActionId(),
                    scenario.expectedStatus());
            governanceRuleAssertionHelper.assertVotingStats(
                    proposal.storeGovActionId(),
                    outcomeRow.getEpoch(),
                    scenario.votePlan().statsAssertions());
            governanceRuleAssertionHelper.assertDbStatusMatchesLedgerSnapshot(proposal.storeGovActionId(), scenario.expectedStatus());
            recordRatifiedCommitteePreviousAction(scenario, proposal);
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Governance proposal outcome scenario failed: " + scenario.name() + ".\n" + diagnostics(scenario.name(), proposal), e);
        }
    }

    private int statusProcessingEpoch(CreatedProposal proposal, GovActionStatus expectedStatus) {
        if (expectedStatus == GovActionStatus.RATIFIED) {
            // A proposal created in epoch x can first be evaluated at the x -> x+1 boundary.
            // The final outcome row may still be later if votes are included near that boundary.
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

    private GovAction committeeUpdateWithInvalidTermAction() {
        Map<com.bloxbean.cardano.client.address.Credential, Integer> members = new LinkedHashMap<>();
        members.put(accountAt(5).committeeColdCredential(), getCurrentEpoch() + 500);

        UpdateCommittee updateCommittee = GovernanceTxHelper.updateCommitteeAction(members, COMMITTEE_THRESHOLD);
        updateCommittee.setPrevGovActionId(currentCommitteePrevGovActionId);
        return updateCommittee;
    }

    private GovAction networkOnlyParameterChangeAction() {
        return GovernanceTxHelper.parameterChangeAction(
                ProtocolParamUpdate.builder()
                        .maxTxExUnits(new ExUnits(BigInteger.valueOf(30_000_000L), BigInteger.valueOf(12_000_000_000L)))
                        .build(),
                false);
    }

    private GovAction hardForkInitiationAction() {
        // A minor bump still exercises HardForkInitiation rules without forcing the devnet to an unsupported major PV.
        return GovernanceTxHelper.hardForkInitiationAction(HARD_FORK_TEST_MAJOR_VERSION, HARD_FORK_TEST_MINOR_VERSION);
    }

    private GovAction securityParameterChangeAction() {
        return GovernanceTxHelper.parameterChangeAction(
                ProtocolParamUpdate.builder()
                        .minFeeA(BigInteger.valueOf(45))
                        .build(),
                true);
    }

    private GovAction noConfidenceAction() {
        return GovernanceTxHelper.noConfidenceAction(currentCommitteePrevGovActionId);
    }

    private void recordRatifiedCommitteePreviousAction(ProposalOutcomeScenario scenario, CreatedProposal proposal) {
        if (scenario.expectedStatus() != GovActionStatus.RATIFIED) {
            return;
        }

        if (scenario.expectedType() == GovActionType.UPDATE_COMMITTEE
                || scenario.expectedType() == GovActionType.NO_CONFIDENCE) {
            currentCommitteePrevGovActionId = proposal.txGovActionId();
        }
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

    private ProposalOutcomeScenario scenario(String name,
                                             GovActionType expectedType,
                                             Supplier<GovAction> govActionSupplier,
                                             VotePlan votePlan,
                                             GovActionStatus expectedStatus) {
        return new ProposalOutcomeScenario(name, expectedType, govActionSupplier, votePlan, expectedStatus);
    }

    private VotePlan votes(Vote drepVote, Vote spoVote, int committeeYesVotes) {
        return new VotePlan(drepVote, spoVote, committeeYesVotes);
    }

    private Account accountAt(int index) {
        return new Account(Networks.testnet(), DEFAULT_MNEMONICS, DerivationPath.createExternalAddressDerivationPathForAccount(index));
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
                + ", committeeThreshold=51/100, committeeMinSize=1"
                + ", controlledStakeTopUpAda=" + CONTROLLED_STAKE_TOP_UP_ADA
                + ", spoTestPool=" + (spoTestPool == null ? "not-registered" : spoTestPool.poolId())
                + ", setupFlags={funding=" + accountFundingReady
                + ", stakeAddress=" + accountStakeAddressRegistered
                + ", pool=" + testStakePoolRegistered
                + ", poolDelegation=" + stakeDelegatedToTestPool
                + ", drep=" + drepRegistered
                + ", voteDelegation=" + votingPowerDelegatedToDRep
                + ", votingPower=" + votingPowerReady
                + ", initialCommitteeVotes=" + initialCommitteeVotesCast
                + ", initialCommittee=" + initialCommitteeRatified
                + ", committeeHotKeys=" + committeeHotKeysAuthorized
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

    private record ProposalOutcomeScenario(String name,
                                           GovActionType expectedType,
                                           Supplier<GovAction> govActionSupplier,
                                           VotePlan votePlan,
                                           GovActionStatus expectedStatus) {
    }

    private class VotePlan {
        private final Vote drepVote;
        private final Vote spoVote;
        private final int committeeYesVotes;

        private VotePlan(Vote drepVote, Vote spoVote, int committeeYesVotes) {
            this.drepVote = drepVote;
            this.spoVote = spoVote;
            this.committeeYesVotes = committeeYesVotes;
        }

        private void cast(CreatedProposal proposal) {
            if (drepVote != null) {
                governanceTxHelper.castDRepVote(account0, drepAccount, proposal.storeGovActionId(), drepVote);
            }

            if (spoVote != null) {
                governanceTxHelper.castTestStakePoolVote(account0, spoTestPool, proposal.storeGovActionId(), spoVote);
            }

            for (int i = 0; i < committeeYesVotes; i++) {
                governanceTxHelper.castCommitteeHotVote(account0, committeeAccounts.get(i), proposal.storeGovActionId(), Vote.YES);
            }
        }

        private Consumer<ProposalVotingStats> statsAssertions() {
            return stats -> {
                assertDRepStats(stats);
                assertSPOStats(stats);
                assertThat(nullToZero(stats.getCcYes())).isEqualTo(committeeYesVotes);
                assertThat(nullToZero(stats.getCcNo())).isZero();
                assertThat(nullToZero(stats.getCcAbstain())).isZero();
                assertThat(nullToZero(stats.getCcDoNotVote())).isGreaterThanOrEqualTo(3 - committeeYesVotes);
            };
        }

        private void assertDRepStats(ProposalVotingStats stats) {
            if (drepVote == Vote.YES) {
                assertThat(nullToZero(stats.getDrepYesVoteStake())).isPositive();
                assertThat(nullToZero(stats.getDrepNoVoteStake())).isZero();
            } else if (drepVote == Vote.NO) {
                assertThat(nullToZero(stats.getDrepNoVoteStake())).isPositive();
                assertThat(nullToZero(stats.getDrepYesVoteStake())).isZero();
            } else {
                assertThat(nullToZero(stats.getDrepYesVoteStake())).isZero();
                assertThat(nullToZero(stats.getDrepNoVoteStake())).isZero();
            }
        }

        private void assertSPOStats(ProposalVotingStats stats) {
            if (spoVote == Vote.YES) {
                assertThat(nullToZero(stats.getSpoYesVoteStake())).isPositive();
                assertThat(nullToZero(stats.getSpoNoVoteStake())).isZero();
            } else if (spoVote == Vote.NO) {
                assertThat(nullToZero(stats.getSpoNoVoteStake())).isPositive();
                assertThat(nullToZero(stats.getSpoYesVoteStake())).isZero();
            } else {
                assertThat(nullToZero(stats.getSpoYesVoteStake())).isZero();
                assertThat(nullToZero(stats.getSpoNoVoteStake())).isZero();
            }
        }

        private BigInteger nullToZero(BigInteger value) {
            return value == null ? BigInteger.ZERO : value;
        }

        private int nullToZero(Integer value) {
            return value == null ? 0 : value;
        }
    }
}
