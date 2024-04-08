package com.bloxbean.cardano.yaci.store.governancerules.util;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ProtocolParamGroup;

import java.util.ArrayList;
import java.util.List;

public class ProtocolParamUtil {
    private ProtocolParamUtil() {
    }

    public static List<ProtocolParamGroup> getGroupsWithNonNullField(ProtocolParamUpdate params) {
        List<ProtocolParamGroup> groups = new ArrayList<>();

        if (isNonNull(params.getMinFeeA(), params.getMinFeeB(), params.getKeyDeposit(), params.getPoolDeposit(),
                params.getMinPoolCost(), params.getPriceMem(), params.getPriceMem())) {
            groups.add(ProtocolParamGroup.ECONOMIC);
        }

        if (isNonNull(params.getMaxBlockSize(), params.getMaxTxSize(), params.getMaxBlockHeaderSize(),
                params.getMaxValSize(), params.getMaxTxExMem(), params.getMaxTxExSteps(),
                params.getMaxBlockExMem(), params.getMaxBlockExSteps(), params.getMaxCollateralInputs())) {
            groups.add(ProtocolParamGroup.NETWORK);
        }

        if (isNonNull(params.getNOpt(), params.getPoolPledgeInfluence(), params.getExpansionRate(),
                params.getDecentralisationParam(), params.getCostModels(),
                params.getCollateralPercent())) {
            groups.add(ProtocolParamGroup.TECHNICAL);
        }

        if (isNonNull(params.getPoolVotingThresholds(), params.getDrepVotingThresholds(), params.getCommitteeMinSize(),
                params.getCommitteeMaxTermLength(), params.getGovActionLifetime(), params.getGovActionDeposit(),
                params.getDrepDeposit(), params.getDrepActivity())) {
            groups.add(ProtocolParamGroup.GOVERNANCE);
        }

        if (isNonNull(params.getMaxBlockSize(), params.getMaxTxSize(), params.getMaxBlockHeaderSize(),
                params.getMaxValSize(), params.getMaxBlockExMem(), params.getMaxBlockExSteps(), params.getMinFeeA(),
                params.getMinFeeB(), params.getGovActionDeposit(), params.getMinFeeRefScriptCostPerByte())) {
            groups.add(ProtocolParamGroup.SECURITY);
        }

        return groups;
    }

    private static boolean isNonNull(Object... objects) {
        for (Object obj : objects) {
            if (obj != null) {
                return true;
            }
        }
        return false;
    }
}
