package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.test.e2e.common.GovernanceTxHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DRep lifecycle rows are isolated because registration changes are irreversible within a devnet run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AbstractGovernanceVoteTallyIT.DevKitInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GovernanceDRepLifecycleVoteTallyIT extends AbstractGovernanceVoteTallyIT {

    /**
     * Re-registering a DRep does not restore votes cleared by its earlier deregistration.
     */
    @Test
    void dRepReregistration_shouldNotReviveStaleVoteAndShouldAcceptNewVote() {
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
                    // Re-registering the same credential restores its voting power, but must not restore
                    // the cleared YES vote. Otherwise, YES/(YES+NO) would be roughly
                    // 910,000 / 1,020,000 ~= 0.89 and incorrectly clear the NewConstitution threshold.
                    governanceTxHelper.castDRepVote(account0, removedYesDRep, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castDRepVote(account0, remainingNoDRep, proposal.storeGovActionId(), Vote.NO);
                    governanceTxHelper.unregisterDRep(account0, removedYesDRep);
                    governanceTxHelper.registerDRep(account0, removedYesDRep, GovernanceTxHelper.defaultAnchor());
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
                    governanceTxHelper.registerDRep(account0, removedYesDRep, GovernanceTxHelper.defaultAnchor());
                    governanceTxHelper.castDRepVote(account0, removedYesDRep, proposal.storeGovActionId(), Vote.NO);
                    castCommitteeYesVotes(proposal, committeeAccounts.size());
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertStake("cleared DRep YES stake", stats.getDrepYesVoteStake(), BigInteger.ZERO);
                    assertStake("post-registration DRep NO stake", stats.getDrepNoVoteStake(), removedYesStake);
                    assertThat(nz(stats.getDrepApprovalRatio())).isZero();
                });
    }
}
