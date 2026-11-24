package com.bloxbean.cardano.yaci.store.governancerules.util;

import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitteeTermWidthTest {
    @Test
    void comparesUnsignedTermsWithoutOverflowingTheEpochLimit() {
        Credential member = Credential.builder().hash("01".repeat(28)).build();
        UpdateCommittee committee = UpdateCommittee.builder()
                .newMembersAndTerms(Map.of(member, (long) Integer.MAX_VALUE + 1))
                .build();
        assertTrue(GovernanceActionUtil.isValidCommitteeTerm(committee, 1, Integer.MAX_VALUE));
        assertFalse(GovernanceActionUtil.isValidCommitteeTerm(committee, 0, Integer.MAX_VALUE));

        UpdateCommittee unsignedMax = UpdateCommittee.builder().newMembersAndTerms(Map.of(member, -1L)).build();
        assertFalse(GovernanceActionUtil.isValidCommitteeTerm(unsignedMax, 10, 100));
    }
}
