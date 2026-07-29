package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Committee-size and quorum rows that mutate the active committee fixture.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AbstractGovernanceVoteTallyIT.DevKitInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GovernanceCommitteeVoteTallyIT extends AbstractGovernanceVoteTallyIT {

    /**
     * Outside bootstrap, a committee smaller than committeeMinSize is treated as if there is no committee.
     */
    @Test
    void committeeBelowMinimumSize_shouldBlockNewConstitutionRatification() {
        ensureDRepAbstainFixture();
        ratifyCommitteeReductionBelowMinimum();

        assertProposalScenario(
                "new constitution expires when active committee size is below committeeMinSize",
                GovActionType.NEW_CONSTITUTION,
                this::newConstitutionAction,
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
}
