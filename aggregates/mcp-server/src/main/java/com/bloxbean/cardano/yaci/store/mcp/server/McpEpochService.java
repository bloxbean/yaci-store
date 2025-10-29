package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.epoch.dto.EpochDto;
import com.bloxbean.cardano.yaci.store.api.epoch.service.EpochParamService;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.EpochStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.epoch-aggr.enabled", "store.mcp-server.tools.epochs.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpEpochService {
    private final EpochStorageReader epochStorageReader;
    private final EpochParamService epochParamService;

    @Tool(name = "current-epoch",
            description = "Get the current epoch information. Returns epoch number, start/end time, block count, transaction count, total output, total fees, and max slot. " +
                         "⏰ Note: start_time and end_time are Unix timestamps in SECONDS. Use 'cardano-blockchain-time-info' tool for timezone conversions.")
    public Epoch getCurrentEpoch() {
        return epochStorageReader.findRecentEpoch()
                .orElseThrow(() -> new RuntimeException("No epoch data available"));
    }

    @Tool(name = "epoch-by-number",
            description = "Get epoch details by epoch number. Returns comprehensive epoch statistics including time boundaries, blocks, transactions, fees, and output amounts. " +
                         "⏰ Note: start_time and end_time are Unix timestamps in SECONDS. Use 'cardano-blockchain-time-info' tool for timezone conversions.")
    public Epoch getEpochByNumber(int epochNumber) {
        return epochStorageReader.findByNumber(epochNumber)
                .orElseThrow(() -> new RuntimeException("Epoch not found: " + epochNumber));
    }

    @Tool(name = "epochs-list",
            description = "Get a paginated list of epochs in descending order (most recent first). Returns epoch summaries with statistics. Page is 0-based. Useful for analyzing epoch history. " +
                         "⏰ Note: start_time and end_time are Unix timestamps in SECONDS. Use 'cardano-blockchain-time-info' tool for timezone conversions.")
    public EpochsPage getEpochs(int page, int count) {
        return epochStorageReader.findEpochs(page, count);
    }

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

    @Tool(name = "latest-epoch-details",
            description = "Get the latest epoch details including epoch number, start time, end time. Useful for getting basic epoch information without full statistics.")
    public EpochDto getLatestEpochDetails() {
        int latestEpoch = epochParamService.getLatestEpoch();
        return epochParamService.getEpochDetails(latestEpoch);
    }

    @Tool(name = "epoch-details",
            description = "Get epoch details for a specific epoch number including start time, end time, and basic epoch information.")
    public EpochDto getEpochDetails(int epochNumber) {
        return epochParamService.getEpochDetails(epochNumber);
    }
}
