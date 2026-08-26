package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.governance.Anchor;
import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.client.transaction.spec.governance.Voter;
import com.bloxbean.cardano.client.transaction.spec.governance.VoterType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.storage.DRepRegistrationStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorageReader;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceTxHelper;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceTxHelper.CreatedProposal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * DRep lifecycle rows are isolated because registration changes are irreversible within a devnet run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AbstractGovernanceVoteTallyIT.DevKitInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GovernanceDRepLifecycleVoteTallyIT extends AbstractGovernanceVoteTallyIT {

    @Autowired
    private VotingProcedureStorageReader votingProcedureStorageReader;

    @Autowired
    private DRepRegistrationStorageReader dRepRegistrationStorageReader;

    /**
     * Unregistering a DRep clears earlier and same-transaction votes; re-registration does not restore them.
     */
    @Test
    void dRepLifecycle_shouldClearInvalidatedVotesAndAcceptNewVote() {
        ensureDRepUnregisterFixture();

        Account removedYesDRep = accountAt(5);
        Account remainingNoDRep = accountAt(6);
        BigInteger removedYesStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(removedYesDRep.drepId()));
        BigInteger remainingNoStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(remainingNoDRep.drepId()));

        assertProposalScenario(
                "DRep re-registration does not revive an earlier YES vote",
                GovActionType.NEW_CONSTITUTION,
                this::newConstitutionAction,
                proposal -> {
                    // Re-registering the same credential and restoring its delegation returns its voting
                    // power, but must not restore the cleared YES vote. Otherwise, YES/(YES+NO) would be roughly
                    // 910,000 / 1,020,000 ~= 0.89 and incorrectly clear the NewConstitution threshold.
                    governanceTxHelper.castDRepVote(account0, removedYesDRep, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castDRepVote(account0, remainingNoDRep, proposal.storeGovActionId(), Vote.NO);
                    governanceTxHelper.unregisterDRep(account0, removedYesDRep);
                    registerDRepAndRestoreDelegation(removedYesDRep);
                    castCommitteeYesVotes(proposal, committeeAccounts.size());
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertThat(removedYesStake).as("removed DRep fixture stake").isPositive();
                    assertStake("deregistered DRep YES stake", stats.getDrepYesVoteStake(), BigInteger.ZERO);
                    assertStake("remaining DRep NO stake", stats.getDrepNoVoteStake(), remainingNoStake);
                    assertThat(nz(stats.getDrepApprovalRatio())).isZero();
                });

        assertProposalScenario(
                "DRep vote after re-registration replaces the cleared vote",
                GovActionType.NEW_CONSTITUTION,
                this::newConstitutionAction,
                proposal -> {
                    governanceTxHelper.castDRepVote(account0, removedYesDRep, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.unregisterDRep(account0, removedYesDRep);
                    registerDRepAndRestoreDelegation(removedYesDRep);
                    governanceTxHelper.castDRepVote(account0, removedYesDRep, proposal.storeGovActionId(), Vote.NO);
                    castCommitteeYesVotes(proposal, committeeAccounts.size());
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertStake("cleared DRep YES stake", stats.getDrepYesVoteStake(), BigInteger.ZERO);
                    assertStake("post-registration DRep NO stake", stats.getDrepNoVoteStake(), removedYesStake);
                    assertThat(nz(stats.getDrepApprovalRatio())).isZero();
                });

        assertSameTransactionVotesAreCleared(
                removedYesDRep,
                remainingNoDRep,
                removedYesStake,
                remainingNoStake);
    }

    private void assertSameTransactionVotesAreCleared(Account drepAccount,
                                                      Account remainingNoDRep,
                                                      BigInteger expectedRestoredStake,
                                                      BigInteger remainingNoStake) {
        CreatedProposal proposal = null;
        try {
            waitForEpoch(getCurrentEpoch() + 1);
            var proposals = createSameTransactionProposals();
            proposal = proposals.get(1);

            assertThat(proposal.index()).isEqualTo(1);
            assertThat(proposal.proposal().getType()).isEqualTo(GovActionType.NEW_CONSTITUTION);
            var indexed = governanceRuleAssertionHelper.findProposal(proposal.storeGovActionId());
            assertThat(indexed.getTxHash()).isEqualTo(proposal.txHash());
            assertThat(indexed.getIndex()).isEqualTo(proposal.index());
            assertThat(indexed.getType()).isEqualTo(GovActionType.NEW_CONSTITUTION);

            CreatedProposal targetProposal = proposal;
            await().atMost(Duration.ofSeconds(90))
                    .pollInterval(Duration.ofSeconds(2))
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        var snapshot = ledgerStateReader.fetchProposalState(targetProposal.storeGovActionId());
                        assertThat(snapshot.presentInCurrentProposals()).isTrue();
                        assertThat(snapshot.presentInEnactedGovActions()).isFalse();
                        assertThat(snapshot.presentInExpiredGovActions()).isFalse();
                    });

            governanceTxHelper.castDRepVote(
                    account0,
                    remainingNoDRep,
                    proposal.storeGovActionId(),
                    Vote.NO);
            castCommitteeYesVotes(proposal, committeeAccounts.size());

            String lifecycleTxHash = castVotesUnregisterAndRegister(drepAccount, proposals);
            assertSameTransactionRegressionShape(lifecycleTxHash, proposal);

            // Unregistration also clears delegations in the ledger. Restore the delegation and wait
            // for a new stake snapshot so an EXPIRED result can only be caused by the cleared vote,
            // not by the DRep being absent from the ratification distribution.
            governanceTxHelper.delegateVotingPowerToDRep(
                    account0,
                    drepAccount,
                    com.bloxbean.cardano.client.governance.GovId.toDrep(drepAccount.drepId()));
            int restoredStakeEpoch = getCurrentEpoch() + 2;
            waitForEpoch(restoredStakeEpoch);
            waitTillAdaPotJobDone(
                    adaPotJobRepository,
                    restoredStakeEpoch,
                    () -> sameTransactionDiagnostics(targetProposal));

            BigInteger restoredStake = ledgerDRepStake(
                    com.bloxbean.cardano.client.governance.GovId.toDrep(drepAccount.drepId()));
            assertStake("re-registered DRep stake", restoredStake, expectedRestoredStake);

            int statusProcessingEpoch = proposal.expiryStatusEpoch();
            waitForEpoch(statusProcessingEpoch);
            waitTillAdaPotJobDone(
                    adaPotJobRepository,
                    statusProcessingEpoch,
                    () -> sameTransactionDiagnostics(targetProposal));

            var outcomeRow = governanceRuleAssertionHelper.assertLatestDbStatus(
                    proposal.storeGovActionId(),
                    GovActionStatus.EXPIRED);
            governanceRuleAssertionHelper.assertVotingStats(
                    proposal.storeGovActionId(),
                    outcomeRow.getEpoch(),
                    stats -> {
                        assertStake(
                                "same-transaction DRep YES stake",
                                stats.getDrepYesVoteStake(),
                                BigInteger.ZERO);
                        assertStake(
                                "remaining DRep NO stake",
                                stats.getDrepNoVoteStake(),
                                remainingNoStake);
                        assertThat(nz(stats.getDrepDoNotVoteStake()))
                                .as("restored DRep stake remains in the ratification distribution")
                                .isGreaterThanOrEqualTo(restoredStake);
                        assertCommitteePassed(stats);

                        BigInteger actualYes = nz(stats.getDrepTotalYesStake());
                        BigInteger actualNo = nz(stats.getDrepTotalNoStake());
                        assertThat(actualNo).isGreaterThanOrEqualTo(restoredStake);

                        // A same-transaction unregistration invalidates this vote even though the DRep
                        // is active again at the snapshot. Counting the invalidated stake as YES instead
                        // of do-not-vote/NO would cross the constitution ratification threshold.
                        BigInteger counterfactualYes = actualYes.add(restoredStake);
                        BigInteger counterfactualNo = actualNo.subtract(restoredStake);
                        BigDecimal counterfactualRatio = new BigDecimal(counterfactualYes)
                                .divide(
                                        new BigDecimal(counterfactualYes.add(counterfactualNo)),
                                        8,
                                        RoundingMode.HALF_UP);
                        assertThat(counterfactualRatio)
                                .as("DRep ratio if the same-transaction vote were not cleared")
                                .isGreaterThanOrEqualTo(DREP_UPDATE_TO_CONSTITUTION_THRESHOLD);
                    });
            governanceRuleAssertionHelper.assertDbStatusMatchesLedgerSnapshot(
                    proposal.storeGovActionId(),
                    GovActionStatus.EXPIRED);
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError(
                    "Governance vote tally scenario failed: DRep unregistration clears every vote in the same transaction.\n"
                            + sameTransactionDiagnostics(proposal),
                    e);
        }
    }

    private List<CreatedProposal> createSameTransactionProposals() {
        Anchor firstAnchor = GovernanceTxHelper.defaultAnchor();
        Anchor secondAnchor = new Anchor("https://xyz.com/constitution/1", firstAnchor.getAnchorDataHash());
        var tx = new Tx()
                .createProposal(GovernanceTxHelper.infoAction(), account0.stakeAddress(), firstAnchor)
                .createProposal(newConstitutionAction(), account0.stakeAddress(), secondAnchor)
                .from(account0.baseAddress());

        var result = new QuickTxBuilder(backendService).compose(tx)
                .withSigner(SignerProviders.drepKeySignerFrom(account0))
                .withSigner(SignerProviders.signerFrom(account0))
                .completeAndWait(System.out::println);

        assertThat(result.isSuccessful()).as(result.toString()).isTrue();
        checkIfUtxoAvailable(result.getValue(), account0.baseAddress());
        return List.of(
                governanceTxHelper.waitForCreatedProposal(result.getValue(), 0),
                governanceTxHelper.waitForCreatedProposal(result.getValue(), 1));
    }

    private String castVotesUnregisterAndRegister(Account drepAccount, List<CreatedProposal> proposals) {
        var voter = new Voter(VoterType.DREP_KEY_HASH, drepAccount.drepCredential());
        var tx = new Tx();
        for (CreatedProposal proposal : proposals) {
            tx.createVote(
                    voter,
                    GovernanceTxHelper.clientGovActionId(proposal.storeGovActionId()),
                    Vote.YES,
                    GovernanceTxHelper.defaultAnchor());
        }
        tx.unregisterDRep(drepAccount.drepCredential())
                .registerDRep(drepAccount, GovernanceTxHelper.defaultAnchor())
                .from(account0.baseAddress());

        var result = new QuickTxBuilder(backendService).compose(tx)
                .withSigner(SignerProviders.signerFrom(account0))
                .withSigner(SignerProviders.drepKeySignerFrom(drepAccount))
                .completeAndWait(System.out::println);

        assertThat(result.isSuccessful()).as(result.toString()).isTrue();
        checkIfUtxoAvailable(result.getValue(), account0.baseAddress());
        return result.getValue();
    }

    private void assertSameTransactionRegressionShape(String txHash, CreatedProposal targetProposal) {
        await().atMost(Duration.ofSeconds(90))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var targetVotes = votingProcedureStorageReader.findByTxHash(txHash).stream()
                            .filter(vote -> vote.getGovActionTxHash().equals(targetProposal.txHash()))
                            .filter(vote -> vote.getGovActionIndex().equals(targetProposal.index()))
                            .toList();
                    var unregistrations = dRepRegistrationStorageReader
                            .findDeRegistrations(0, 500, Order.desc)
                            .stream()
                            .filter(registration -> registration.getTxHash().equals(txHash))
                            .toList();

                    assertThat(targetVotes).singleElement();
                    assertThat(unregistrations).singleElement();

                    var targetVote = targetVotes.getFirst();
                    var unregistration = unregistrations.getFirst();
                    assertThat(targetVote.getTxIndex()).isEqualTo(unregistration.getTxIndex());
                    assertThat(targetVote.getIndex()).isEqualTo(1L);
                    assertThat(unregistration.getCertIndex()).isZero();

                    // Regression fixture for https://github.com/bloxbean/yaci-store/issues/1108.
                    // Voting-procedure and certificate indexes belong to unrelated collections;
                    // their inequality only confirms that this transaction exercises the reported case.
                    assertThat(targetVote.getIndex()).isGreaterThan(unregistration.getCertIndex());
                });
    }

    private String sameTransactionDiagnostics(CreatedProposal proposal) {
        return "currentEpoch=" + getCurrentEpoch()
                + "\nproposal=" + (proposal == null ? "not-created" : proposal.storeGovActionId());
    }

    private void registerDRepAndRestoreDelegation(Account drepAccount) {
        governanceTxHelper.registerDRep(account0, drepAccount, GovernanceTxHelper.defaultAnchor());
        governanceTxHelper.delegateVotingPowerToDRep(
                account0,
                drepAccount,
                com.bloxbean.cardano.client.governance.GovId.toDrep(drepAccount.drepId()));
    }
}
