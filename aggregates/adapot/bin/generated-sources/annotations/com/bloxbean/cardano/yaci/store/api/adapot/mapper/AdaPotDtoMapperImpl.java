package com.bloxbean.cardano.yaci.store.api.adapot.mapper;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.AdaPotDto;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.RewardDto;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.RewardRestDto;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.UnclaimedRewardRestDto;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import java.math.BigInteger;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:27+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class AdaPotDtoMapperImpl extends AdaPotDtoMapper {

    @Override
    public AdaPotDto toAdaPotDto(AdaPot adaPot) {
        if ( adaPot == null ) {
            return null;
        }

        Integer epoch = null;
        BigInteger depositsStake = null;
        BigInteger fees = null;
        BigInteger treasury = null;
        BigInteger reserves = null;
        BigInteger circulation = null;
        BigInteger distributedRewards = null;
        BigInteger undistributedRewards = null;
        BigInteger rewardsPot = null;
        BigInteger poolRewardsPot = null;

        epoch = adaPot.getEpoch();
        depositsStake = adaPot.getDepositsStake();
        fees = adaPot.getFees();
        treasury = adaPot.getTreasury();
        reserves = adaPot.getReserves();
        circulation = adaPot.getCirculation();
        distributedRewards = adaPot.getDistributedRewards();
        undistributedRewards = adaPot.getUndistributedRewards();
        rewardsPot = adaPot.getRewardsPot();
        poolRewardsPot = adaPot.getPoolRewardsPot();

        AdaPotDto adaPotDto = new AdaPotDto( epoch, depositsStake, fees, treasury, reserves, circulation, distributedRewards, undistributedRewards, rewardsPot, poolRewardsPot );

        return adaPotDto;
    }

    @Override
    public RewardDto toRewardDto(Reward reward) {
        if ( reward == null ) {
            return null;
        }

        String address = null;
        Integer earnedEpoch = null;
        RewardType type = null;
        String poolId = null;
        BigInteger amount = null;
        Integer spendableEpoch = null;

        address = reward.getAddress();
        earnedEpoch = reward.getEarnedEpoch();
        type = reward.getType();
        poolId = reward.getPoolId();
        amount = reward.getAmount();
        spendableEpoch = reward.getSpendableEpoch();

        RewardDto rewardDto = new RewardDto( address, earnedEpoch, type, poolId, amount, spendableEpoch );

        return rewardDto;
    }

    @Override
    public RewardRestDto toRewardRestDto(RewardRest rewardRest) {
        if ( rewardRest == null ) {
            return null;
        }

        String address = null;
        RewardRestType type = null;
        BigInteger amount = null;
        Integer earnedEpoch = null;
        Integer spendableEpoch = null;

        address = rewardRest.getAddress();
        type = rewardRest.getType();
        amount = rewardRest.getAmount();
        earnedEpoch = rewardRest.getEarnedEpoch();
        spendableEpoch = rewardRest.getSpendableEpoch();

        RewardRestDto rewardRestDto = new RewardRestDto( address, type, amount, earnedEpoch, spendableEpoch );

        return rewardRestDto;
    }

    @Override
    public UnclaimedRewardRestDto unclaimedRewardRestDto(UnclaimedRewardRest unclaimedRewardRest) {
        if ( unclaimedRewardRest == null ) {
            return null;
        }

        String address = null;
        RewardRestType type = null;
        BigInteger amount = null;
        Integer earnedEpoch = null;
        Integer spendableEpoch = null;

        address = unclaimedRewardRest.getAddress();
        type = unclaimedRewardRest.getType();
        amount = unclaimedRewardRest.getAmount();
        earnedEpoch = unclaimedRewardRest.getEarnedEpoch();
        spendableEpoch = unclaimedRewardRest.getSpendableEpoch();

        UnclaimedRewardRestDto unclaimedRewardRestDto = new UnclaimedRewardRestDto( address, type, amount, earnedEpoch, spendableEpoch );

        return unclaimedRewardRestDto;
    }
}
