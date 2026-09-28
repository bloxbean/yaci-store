package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPO and mixed voting-group rows that share the generated stake-pool fixture.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AbstractGovernanceVoteTallyIT.DevKitInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GovernanceSPOVoteTallyIT extends AbstractGovernanceVoteTallyIT {

    /**
     * SPO tallies must use real pool stake snapshots, including non-voting pool stake.
     */
    @Test
    void spoVotes_shouldUseStakeFromAllKnownPools() {
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
    void mixedSecurityAndNetworkParameterChange_shouldRequireAllVotingGroups() {
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
}
