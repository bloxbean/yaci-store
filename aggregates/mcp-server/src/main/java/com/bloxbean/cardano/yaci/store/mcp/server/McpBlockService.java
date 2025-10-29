package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.blocks.service.BlockService;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.blocks.enabled", "store.mcp-server.tools.blocks.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpBlockService {
    private final BlockService blockService;

    @Tool(name = "block-by-number",
            description = "Get block details by block number (height). Returns complete block information including hash, epoch, slot, producer, transactions count, and size. " +
                         "⏰ Note: block_time is Unix timestamp in SECONDS. Use 'cardano-blockchain-time-info' tool for timezone conversions.")
    public Block getBlockByNumber(long blockNumber) {
        return blockService.getBlockByNumber(blockNumber)
                .orElseThrow(() -> new RuntimeException("Block not found with number: " + blockNumber));
    }

    @Tool(name = "block-by-hash",
            description = "Get block details by block hash. Returns complete block information including block number, epoch, slot, producer, transactions count, and size. " +
                         "⏰ Note: block_time is Unix timestamp in SECONDS. Use 'cardano-blockchain-time-info' tool for timezone conversions.")
    public Block getBlockByHash(String blockHash) {
        return blockService.getBlockByHash(blockHash)
                .orElseThrow(() -> new RuntimeException("Block not found with hash: " + blockHash));
    }

    @Tool(name = "latest-block",
            description = "Get the most recent block on the blockchain. Returns the latest block with all details including current blockchain height, latest epoch, and slot information. " +
                         "⏰ Note: block_time is Unix timestamp in SECONDS. Use 'cardano-blockchain-time-info' tool for timezone conversions.")
    public Block getLatestBlock() {
        return blockService.getLatestBlock()
                .orElseThrow(() -> new RuntimeException("No blocks found in the blockchain"));
    }

    @Tool(name = "blocks-list",
            description = "Get a paginated list of blocks in descending order (most recent first). Returns blocks with basic information. Page is 0-based. Useful for browsing recent blockchain activity. " +
                         "⏰ Note: block_time is Unix timestamp in SECONDS. Use 'cardano-blockchain-time-info' tool for timezone conversions.")
    public BlocksPage getBlocks(int page, int count) {
        return blockService.getBlocks(page, count);
    }

    @Tool(name = "blocks-by-epoch",
            description = "Get all blocks produced in a specific epoch. Returns paginated list of blocks for the given epoch. Page is 0-based. Useful for analyzing epoch statistics and producer performance. " +
                         "⏰ Note: block_time is Unix timestamp in SECONDS. Use 'cardano-blockchain-time-info' tool for timezone conversions.")
    public BlocksPage getBlocksByEpoch(int epoch, int page, int count) {
        return blockService.getBlocksByEpoch(epoch, page, count);
    }

    @Tool(name = "blocks-by-pool-epoch",
            description = "Get blocks produced by a specific stake pool in a given epoch. Stake pool is identified by pool_id (bech32 or hex). Returns list of blocks with producer information. Useful for analyzing pool performance in an epoch. " +
                         "⏰ Note: block_time is Unix timestamp in SECONDS. Use 'cardano-blockchain-time-info' tool for timezone conversions.")
    public List<PoolBlock> getBlocksByPoolAndEpoch(String poolId, int epoch) {
        return blockService.getBlocksBySlotLeaderEpoch(poolId, epoch);
    }
}
