package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.epoch.service.EpochParamService;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.epoch.enabled", "store.mcp-server.tools.epochs.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpEpochParamService {
    private final EpochParamService epochParamService;

    @Tool(name = "latest-protocol-params",
            description = "Get the latest epoch's protocol parameters. Returns all Cardano protocol parameters including min fee, max block size, max tx size, key deposits, pool deposits, expansion rate, treasury cut, min pool cost, cost models, and governance thresholds.")
    public ProtocolParamsDto getLatestProtocolParams() {
        return epochParamService.getLatestProtocolParams()
                .orElseThrow(() -> new RuntimeException("Protocol parameters not available"));
    }

    @Tool(name = "protocol-params-by-epoch",
            description = "Get protocol parameters for a specific epoch number. Returns all Cardano protocol parameters that were active during the specified epoch including fees, deposits, limits, and governance parameters.")
    public ProtocolParamsDto getProtocolParamsByEpoch(int epochNumber) {
        return epochParamService.getProtocolParams(epochNumber)
                .orElseThrow(() -> new RuntimeException("Protocol parameters not found for epoch: " + epochNumber));
    }
}
