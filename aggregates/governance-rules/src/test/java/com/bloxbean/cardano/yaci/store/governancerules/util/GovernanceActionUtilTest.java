package com.bloxbean.cardano.yaci.store.governancerules.util;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil.isPrevActionAsExpected;

class GovernanceActionUtilTest {
    private final GovActionId parent = new GovActionId("11".repeat(32), 0);

    @ParameterizedTest
    @EnumSource(value = GovActionType.class, names = {"TREASURY_WITHDRAWALS_ACTION", "INFO_ACTION"},
            mode = EnumSource.Mode.EXCLUDE)
    void requiresExactOptionalParentMatch(GovActionType type) {
        assertThat(isPrevActionAsExpected(type, null, null)).isTrue();
        assertThat(isPrevActionAsExpected(type, parent, null)).isFalse();
        assertThat(isPrevActionAsExpected(type, null, parent)).isFalse();
        assertThat(isPrevActionAsExpected(type, new GovActionId("11".repeat(32), 0), parent)).isTrue();
        assertThat(isPrevActionAsExpected(type, new GovActionId("22".repeat(32), 0), parent)).isFalse();
        assertThat(isPrevActionAsExpected(type, new GovActionId("11".repeat(32), 1), parent)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = GovActionType.class, names = {"TREASURY_WITHDRAWALS_ACTION", "INFO_ACTION"})
    void actionsWithoutGovernancePurposeDoNotRequireParentMatch(GovActionType type) {
        assertThat(isPrevActionAsExpected(type, null, null)).isTrue();
        assertThat(isPrevActionAsExpected(type, parent, null)).isTrue();
        assertThat(isPrevActionAsExpected(type, null, parent)).isTrue();
        assertThat(isPrevActionAsExpected(type, new GovActionId("22".repeat(32), 1), parent)).isTrue();
    }
}
