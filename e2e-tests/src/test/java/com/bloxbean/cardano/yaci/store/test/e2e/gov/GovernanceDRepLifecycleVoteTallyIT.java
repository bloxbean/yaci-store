package com.bloxbean.cardano.yaci.store.test.e2e.gov;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
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
     * A DRep deregistration removes that DRep's effective votes from active proposals.
     */
    @Test
    void dRepDeregistration_shouldClearEffectiveVoteForProposal() {
        ensureDRepUnregisterFixture();

        Account removedYesDRep = accountAt(5);
        Account remainingNoDRep = accountAt(6);
        BigInteger removedYesStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(removedYesDRep.drepId()));
        BigInteger remainingNoStake = ledgerDRepStake(com.bloxbean.cardano.client.governance.GovId.toDrep(remainingNoDRep.drepId()));

        assertProposalScenario(
                "DRep deregistration clears an earlier YES vote before ratification",
                GovActionType.NEW_CONSTITUTION,
                this::newConstitutionAction,
                proposal -> {
                    // If the YES DRep stayed registered, YES/(YES+NO) would be roughly
                    // 910,000 / 1,020,000 ~= 0.89 and clear the NewConstitution DRep threshold.
                    // The unregister transaction must remove that YES vote from effective ledger state.
                    governanceTxHelper.castDRepVote(account0, removedYesDRep, proposal.storeGovActionId(), Vote.YES);
                    governanceTxHelper.castDRepVote(account0, remainingNoDRep, proposal.storeGovActionId(), Vote.NO);
                    governanceTxHelper.unregisterDRep(account0, removedYesDRep);
                    castCommitteeYesVotes(proposal, committeeAccounts.size());
                },
                GovActionStatus.EXPIRED,
                stats -> {
                    assertThat(removedYesStake).as("removed DRep fixture stake").isPositive();
                    assertStake("deregistered DRep YES stake", stats.getDrepYesVoteStake(), BigInteger.ZERO);
                    assertStake("remaining DRep NO stake", stats.getDrepNoVoteStake(), remainingNoStake);
                    assertThat(nz(stats.getDrepApprovalRatio())).isZero();
                });
    }
}
