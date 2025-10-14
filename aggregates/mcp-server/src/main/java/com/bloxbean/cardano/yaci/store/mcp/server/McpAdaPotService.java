package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.adapot.dto.*;
import com.bloxbean.cardano.yaci.store.api.adapot.service.AdaPotApiService;
import com.bloxbean.cardano.yaci.store.api.adapot.service.NetworkInfoApiService;
import com.bloxbean.cardano.yaci.store.api.adapot.service.RewardApiService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.adapot.enabled", "store.mcp-server.tools.adapot.enabled"},
    havingValue = "true"
)
public class McpAdaPotService {
    private final AdaPotApiService adaPotApiService;
    private final NetworkInfoApiService networkInfoApiService;
    private final RewardApiService rewardApiService;

    @Tool(name = "ada-pot-current",
            description = "Get the current Ada pot showing reserves, treasury, fees, deposits, and total supply. Requires adapot module to be enabled.")
    public AdaPotDto getCurrentAdaPot() {
        return adaPotApiService.getAdaPot()
                .orElseThrow(() -> new RuntimeException("Ada pot data not available. Check if adapot module is enabled and synced."));
    }

    @Tool(name = "ada-pot-by-epoch",
            description = "Get Ada pot data for a specific epoch. Shows reserves, treasury, fees, deposits for that epoch. Requires adapot module to be enabled.")
    public AdaPotDto getAdaPotByEpoch(int epoch) {
        return adaPotApiService.getAdaPot(epoch)
                .orElseThrow(() -> new RuntimeException("Ada pot data not available for epoch " + epoch));
    }

    @Tool(name = "ada-pots-list",
            description = "Get a paginated list of Ada pot data across epochs. Returns historical Ada pot information. Page is 0-based. Requires adapot module to be enabled.")
    public List<AdaPotDto> getAdaPots(int page, int count) {
        return adaPotApiService.getAdaPots(page, count);
    }

    @Tool(name = "network-info-current",
            description = "Get current network information including total supply, circulating supply, treasury, reserves, and total active stake. Requires adapot module to be enabled.")
    public NetworkInfoDto getCurrentNetworkInfo() {
        return networkInfoApiService.getNetworkInfo()
                .orElseThrow(() -> new RuntimeException("Network info not available. Check if adapot module is enabled and synced."));
    }

    @Tool(name = "network-info-by-epoch",
            description = "Get network information for a specific epoch including supply and stake statistics. Requires adapot module to be enabled.")
    public NetworkInfoDto getNetworkInfoByEpoch(int epoch) {
        return networkInfoApiService.getNetworkInfo(epoch)
                .orElseThrow(() -> new RuntimeException("Network info not available for epoch " + epoch));
    }

    @Tool(name = "pool-rewards-by-epoch",
            description = "Get pool rewards for a specific spendable epoch. Returns list of rewards earned by stake pools. Page is 0-based. Requires adapot module to be enabled.")
    public List<RewardDto> getPoolRewardsByEpoch(int epoch, int page, int count) {
        return rewardApiService.getPoolRewards(epoch, page, count);
    }

    @Tool(name = "pool-rewards-by-address",
            description = "Get pool rewards for a specific stake address. Returns rewards earned by the address across epochs. Page is 0-based. Requires adapot module to be enabled.")
    public List<AddressPoolRewardDto> getPoolRewardsByAddress(String address, int page, int count, String order) {
        Order orderEnum = "asc".equalsIgnoreCase(order) ? Order.asc : Order.desc;
        return rewardApiService.getPoolRewards(address, page, count, orderEnum);
    }

    @Tool(name = "pool-rewards-by-address-and-epoch",
            description = "Get pool rewards for a specific stake address in a specific spendable epoch. Requires adapot module to be enabled.")
    public List<AddressPoolRewardDto> getPoolRewardsByAddressAndEpoch(String address, int epoch) {
        return rewardApiService.getPoolRewardsBySpendableEpoch(address, epoch);
    }

    @Tool(name = "pool-rewards-by-pool-and-epoch",
            description = "Get rewards for a specific stake pool in a specific spendable epoch. Pool ID can be in bech32 (pool1...) or hex format. Returns list of addresses and their rewards from this pool. Page is 0-based. Requires adapot module to be enabled.")
    public List<PoolAddressRewardDto> getPoolRewardsByPoolAndEpoch(String poolHash, int epoch, int page, int count) {
        return rewardApiService.getPoolRewardsByPoolHashAndSpendableEpoch(poolHash, epoch, page, count);
    }

    @Tool(name = "reward-rest-by-epoch",
            description = "Get reward rest (treasury and reserves rewards) for a specific spendable epoch. Page is 0-based. Requires adapot module to be enabled.")
    public List<RewardRestDto> getRewardRestByEpoch(int epoch, int page, int count) {
        return rewardApiService.getRewardRest(epoch, page, count);
    }

    @Tool(name = "reward-rest-by-address",
            description = "Get reward rest (treasury and reserves rewards) for a specific stake address. Page is 0-based. Requires adapot module to be enabled.")
    public List<AddressRewardRestDto> getRewardRestByAddress(String address, int page, int count, String order) {
        Order orderEnum = "asc".equalsIgnoreCase(order) ? Order.asc : Order.desc;
        return rewardApiService.getRewardRest(address, page, count, orderEnum);
    }

    @Tool(name = "unclaimed-reward-rest",
            description = "Get unclaimed reward rest for a specific spendable epoch. Shows rewards that haven't been claimed yet. Page is 0-based. Requires adapot module to be enabled.")
    public List<UnclaimedRewardRestDto> getUnclaimedRewardRest(int epoch, int page, int count, String order) {
        Order orderEnum = "asc".equalsIgnoreCase(order) ? Order.asc : Order.desc;
        return rewardApiService.getUnclaimedRewardRestBySpendableEpoch(epoch, page, count, orderEnum);
    }
}
