package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.staking.service.PoolService;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.staking.enabled", "store.mcp-server.tools.staking.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpStakingService {
    private final PoolService poolService;

    @Tool(name = "pool-registrations",
            description = "Get a paginated list of stake pool registrations. Returns pool registration certificates with pool parameters like pledge, cost, margin, relay info, and metadata. Page is 0-based.")
    public List<PoolRegistration> getPoolRegistrations(int page, int count) {
        return poolService.getPoolRegistrations(page, count);
    }

    @Tool(name = "pool-retirements",
            description = "Get a paginated list of stake pool retirements. Returns pool retirement certificates showing when pools announced their retirement. Page is 0-based.")
    public List<PoolRetirement> getPoolRetirements(int page, int count) {
        return poolService.getPoolRetirements(page, count);
    }

    @Tool(name = "retiring-pools-by-epoch",
            description = "Get list of pools retiring in a specific epoch. Returns pool retirement information for pools scheduled to retire at the end of the specified epoch.")
    public List<PoolRetirement> getRetiringPoolsByEpoch(int epoch) {
        return poolService.getRetiringPoolIds(epoch);
    }

    @Tool(name = "pool-details",
            description = "Get detailed information for a specific stake pool at a given epoch. Pool ID can be in bech32 (pool1...) or hex format. Returns pool parameters, metadata, relays, owners, and rewards address.")
    public PoolDetails getPoolDetails(String poolId, int epoch) {
        return poolService.getPoolDetails(poolId, epoch)
                .orElseThrow(() -> new RuntimeException("Pool details not found for poolId: " + poolId + " at epoch: " + epoch));
    }
}
