package com.bloxbean.cardano.yaci.store.adapot.service.model;

import lombok.Builder;
import lombok.Data;
import org.cardanofoundation.rewards.calculation.domain.Epoch;
import org.cardanofoundation.rewards.calculation.domain.MirCertificate;
import org.cardanofoundation.rewards.calculation.domain.PoolState;
import org.cardanofoundation.rewards.calculation.domain.ProtocolParameters;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;

@Data
@Builder
public class RewardsCalcInput {
    private int epoch;

    private BigInteger treasuryOfPreviousEpoch;
    private BigInteger reservesOfPreviousEpoch;

    private ProtocolParameters protocolParameters;

    private Epoch epochInfo;

    private HashSet<String> rewardAddressesOfRetiredPoolsInEpoch;
    private HashSet<String> deregisteredAccounts;
    private HashSet<String> lateDeregisteredAccounts;
    private HashSet<String> registeredAccountsSinceLastEpoch;
    private HashSet<String> registeredAccountsUntilNow;
    private HashSet<String> sharedPoolRewardAddressesWithoutReward;
    private HashSet<String> deregisteredAccountsOnEpochBoundary;

    private List<String> poolIds;
    private List<PoolState> poolStates;
    private List<MirCertificate> mirCertificates;
}
