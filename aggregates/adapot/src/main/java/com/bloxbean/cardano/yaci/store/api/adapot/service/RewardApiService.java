package com.bloxbean.cardano.yaci.store.api.adapot.service;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.*;
import com.bloxbean.cardano.yaci.store.api.adapot.mapper.AdaPotDtoMapper;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardApiService {
    private final RewardStorageReader rewardStorageReader;
    private final AdaPotDtoMapper adaPotDtoMapper;

    //Pool rewards
    public List<RewardDto> getPoolRewards(Integer epoch, int page, int count) {
        return rewardStorageReader.findRewardsBySpendableEpoch(epoch, page, count)
                .stream()
                .map(adaPotDtoMapper::toRewardDto)
                .toList();
    }

    public List<AddressPoolRewardDto> getPoolRewards(String address, int page, int count, Order order) {
        return rewardStorageReader.findRewardsByAddress(address, page, count, order)
                .stream()
                .map(reward -> AddressPoolRewardDto.toDto(reward))
                .toList();
    }

    public List<AddressPoolRewardDto> getPoolRewardsBySpendableEpoch(String address, Integer epoch) {
        return rewardStorageReader.findRewardsByAddressAndSpendableEpoch(address, epoch)
                .stream()
                .map(reward -> AddressPoolRewardDto.toDto(reward))
                .toList();
    }

    //Reward Rest

    public List<RewardRestDto> getRewardRest(Integer epoch, int page, int count) {
        return rewardStorageReader.findRewardRestBySpendableEpoch(epoch, page, count)
                .stream()
                .map(adaPotDtoMapper::toRewardRestDto)
                .toList();
    }

    public List<AddressRewardRestDto> getRewardRest(String address, int page, int count, Order order) {
        return rewardStorageReader.findRewardRestByAddress(address, page, count, order)
                .stream()
                .map(rewardRest -> AddressRewardRestDto.toDto(rewardRest))
                .toList();
    }

    public List<AddressRewardRestDto> getRewardRestBySpendableEpoch(String address, Integer epoch) {
        return rewardStorageReader.findRewardRestByAddressAndSpendableEpoch(address, epoch)
                .stream()
                .map(rewardRest -> AddressRewardRestDto.toDto(rewardRest))
                .toList();
    }

    //Unclaimed Reward Rest
    public List<UnclaimedRewardRestDto> getUnclaimedRewardRestBySpendableEpoch(Integer epoch, int page, int count, Order order) {
        return rewardStorageReader.findUnclaimedRewardRestBySpendableEpoch(epoch, page, count, order)
                .stream()
                .map(adaPotDtoMapper::unclaimedRewardRestDto)
                .toList();
    }

    //-- Rewards for Pool
    public List<PoolAddressRewardDto> getPoolRewardsByPoolHashAndSpendableEpoch(String poolHash, Integer epoch, int page, int count) {
        if (poolHash != null && poolHash.startsWith(PoolUtil.POOL_ID_PREFIX)) { //It's a Bech32 encoded pool id
            poolHash = HexUtil.encodeHexString(Bech32.decode(poolHash).data);
        }

        return rewardStorageReader.findRewardsByPoolHashAndSpendableEpoch(poolHash, epoch, page, count)
                .stream()
                .map(reward -> new PoolAddressRewardDto(reward.getAddress(), reward.getAmount(), reward.getType(), reward.getEarnedEpoch()))
                .toList();
    }

    public List<RewardInfoDto> getUnwithdrawnRewardsByAddresses(List<String> addresses, int page, int count) {
        return rewardStorageReader.findUnwithdrawnRewardsByAddresses(addresses, page, count)
                .stream().map(RewardInfoDto::toDto).toList();
    }

}
