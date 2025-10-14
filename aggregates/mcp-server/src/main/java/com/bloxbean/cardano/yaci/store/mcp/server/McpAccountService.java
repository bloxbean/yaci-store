package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountRewardInfo;
import com.bloxbean.cardano.yaci.store.api.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.account.enabled", "store.mcp-server.tools.accounts.enabled"},
    havingValue = "true"
)
public class McpAccountService {
    private final AccountService accountService;

    @Tool(name = "stake-account-info",
            description = "Get stake account information including delegation and rewards for a stake address (stake1...). Returns current pool delegation and available rewards. Requires node-to-client (n2c) connection to be configured.")
    public StakeAccountRewardInfo getStakeAccountInfo(String stakeAddress) {
        return accountService.getAccountInfo(stakeAddress)
                .orElseThrow(() -> new RuntimeException("Stake account info not available for: " + stakeAddress + ". Check if n2c connection is configured."));
    }
}
