package com.bloxbean.cardano.yaci.store.api.adapot.dto;

import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;

public enum RewardInfoType {
    pool_member,
    pool_leader,
    pool_deposit_refund,
    treasury,
    reserves,
    treasury_withdrawal,
    proposal_deposit_refund;

    public static RewardInfoType fromInstantRewardType(InstantRewardType type) {
        return switch (type) {
            case InstantRewardType.treasury -> treasury;
            case InstantRewardType.reserves -> reserves;
        };
    }

    public static RewardInfoType fromRewardRestType(RewardRestType type) {
        return switch (type) {
            case RewardRestType.treasury -> treasury_withdrawal;
            case RewardRestType.proposal_refund -> proposal_deposit_refund;
        };
    }

    public static RewardInfoType fromPoolRewardType(RewardType type) {
        return switch (type) {
            case RewardType.member -> pool_member;
            case RewardType.leader -> pool_leader;
            case RewardType.refund -> pool_deposit_refund;
        };
    }
}