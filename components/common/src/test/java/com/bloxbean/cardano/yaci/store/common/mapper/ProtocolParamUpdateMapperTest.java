package com.bloxbean.cardano.yaci.store.common.mapper;

import com.bloxbean.cardano.yaci.core.model.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.core.model.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.util.PlutusKeys;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolParamUpdateMapperTest {
    @Test
    void mapsUpdatesForRatificationAndEpochParameters() {
        var threshold = UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.9"));
        var update = ProtocolParamUpdate.builder()
                .nOpt(500).committeeMinSize(3).committeeMaxTermLength(20)
                .drepVotingThresholds(DrepVoteThresholds.builder().dvtTreasuryWithdrawal(threshold).build())
                .poolVotingThresholds(PoolVotingThresholds.builder().pvtPPSecurityGroup(threshold).build())
                .costModels(Map.of(0, "820102", 2, "822003"))
                .costModelsHash("hash").extraEntropy(new Tuple<>(0, "entropy")).build();
        var params = ProtocolParamUpdateMapper.INSTANCE.toProtocolParams(update);
        assertThat(params.getNOpt()).isEqualTo(500);
        assertThat(params.getCommitteeMinSize()).isEqualTo(3);
        assertThat(params.getCommitteeMaxTermLength()).isEqualTo(20);
        assertThat(params.getDrepVotingThresholds().getDvtTreasuryWithdrawal()).isEqualTo(threshold);
        assertThat(params.getPoolVotingThresholds().getPvtPPSecurityGroup()).isEqualTo(threshold);
        assertThat(params.getCostModels().get(PlutusKeys.PLUTUS_V1)).containsExactly(1, 2);
        assertThat(params.getCostModels().get(PlutusKeys.PLUTUS_V3)).containsExactly(-1, 3);
        assertThat(params.getCostModelsHash()).isEqualTo("hash");
        assertThat(params.getExtraEntropy()).isEqualTo("0,entropy");
    }

    @Test
    void mergingAnUpdateIntoWorkingParametersLeavesSnapshotCostModelsUnchanged() {
        var snapshot = ProtocolParams.builder().minFeeA(44)
                .costModels(Map.of(PlutusKeys.PLUTUS_V1, new long[]{1, 2})).build();
        var working = new ProtocolParams();
        working.merge(snapshot);
        working.merge(ProtocolParamUpdateMapper.INSTANCE.toProtocolParams(ProtocolParamUpdate.builder()
                .costModels(Map.of(2, "820304")).build()));
        assertThat(working.getMinFeeA()).isEqualTo(44);
        assertThat(working.getCostModels()).containsOnlyKeys(PlutusKeys.PLUTUS_V1, PlutusKeys.PLUTUS_V3);
        assertThat(snapshot.getCostModels()).containsOnlyKeys(PlutusKeys.PLUTUS_V1);
    }
}
