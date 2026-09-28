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
 * DRep vote tally rows that share the same controlled DRep stake fixture.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AbstractGovernanceVoteTallyIT.DevKitInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GovernanceDRepVoteTallyIT extends AbstractGovernanceVoteTallyIT {

    /**
     * DRep abstain stake must be excluded from the yes/(yes+no) denominator.
     * The yes/no split intentionally clears the node's default NewConstitution DRep threshold.
     */
    @Test
    void dRepAbstainStake_shouldStayOutOfAcceptedRatioDenominator() {
        ensureDRepAbstainFixture();

        BigInteger yesStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(drepYesAccount.drepId()));
        BigInteger noStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(drepNoAccount.drepId()));
        AtomicReference<BigInteger> autoAbstainStake = new AtomicReference<>();

        assertProposalScenario(
                "DRep abstain stake is excluded from accepted-ratio denominator",
                GovActionType.NEW_CONSTITUTION,
                this::newConstitutionAction,
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
    void latestDRepVote_shouldReplaceEarlierVote() {
        ensureDRepAbstainFixture();

        BigInteger drepStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(drepYesAccount.drepId()));

        assertProposalScenario(
                "latest DRep vote replaces the earlier vote",
                GovActionType.NEW_CONSTITUTION,
                this::newConstitutionAction,
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
}
