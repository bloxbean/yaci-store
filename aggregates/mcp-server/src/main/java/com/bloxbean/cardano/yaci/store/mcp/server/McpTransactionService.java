package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.transaction.service.TransactionService;
import com.bloxbean.cardano.yaci.store.mcp.server.model.AddressActivityPage;
import com.bloxbean.cardano.yaci.store.mcp.server.model.PagedResult;
import com.bloxbean.cardano.yaci.store.mcp.server.model.PaginationCursor;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TransactionNetTransfer;
import com.bloxbean.cardano.yaci.store.mcp.server.service.AddressNetActivityService;
import com.bloxbean.cardano.yaci.store.mcp.server.service.TransactionNetTransferBatchService;
import com.bloxbean.cardano.yaci.store.mcp.server.util.TransactionNetTransferCalculator;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionDetails;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionPage;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionSummary;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.transaction.enabled", "store.mcp-server.tools.transactions.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpTransactionService {
    private final TransactionService transactionService;
    private final TransactionNetTransferBatchService batchService;
    private final AddressNetActivityService addressActivityService;

    @Tool(name = "transaction-by-hash",
            description = "Get complete transaction details by transaction hash. Returns comprehensive information including inputs, outputs, fees, UTXOs, collateral, reference inputs, metadata, and script information.")
    public TransactionDetails getTransactionByHash(String txHash) {
        return transactionService.getTransaction(txHash)
                .orElseThrow(() -> new RuntimeException("Transaction not found with hash: " + txHash));
    }

    @Tool(name = "transactions-list",
            description = "Get a paginated list of recent transactions in descending order (most recent first). Returns transaction summaries including hash, block, slot, output addresses, total output, and fees. Page is 0-based.")
    public TransactionPage getTransactions(int page, int count) {
        return transactionService.getTransactions(page, count);
    }

    @Tool(name = "transactions-by-block-number",
            description = "Get all transactions in a specific block by block number (height). Returns list of transaction summaries for all transactions included in the block.")
    public List<TransactionSummary> getTransactionsByBlockNumber(long blockNumber) {
        return transactionService.getTransactionsByBlockNumber(blockNumber);
    }

    @Tool(name = "transactions-by-block-hash",
            description = "Get all transactions in a specific block by block hash. Returns list of transaction summaries for all transactions included in the block.")
    public List<TransactionSummary> getTransactionsByBlockHash(String blockHash) {
        return transactionService.getTransactionsByBlockHash(blockHash);
    }

    @Tool(name = "transaction-witnesses",
            description = "Get transaction witnesses (signatures and scripts) for a specific transaction hash. Returns all witnesses including verification keys, native scripts, Plutus scripts, redeemers, and datums used in the transaction.")
    public List<TxnWitness> getTransactionWitnesses(String txHash) {
        List<TxnWitness> witnesses = transactionService.getTransactionWitnesses(txHash);
        if (witnesses == null || witnesses.isEmpty()) {
            throw new RuntimeException("No witnesses found for transaction: " + txHash);
        }
        return witnesses;
    }

    @Tool(name = "transaction-net-transfers",
            description = "Calculate net ADA and token transfers for a transaction by comparing inputs vs outputs per address. " +
                    "IMPORTANT: This solves the UTXO chain problem where most amounts appear in both inputs and outputs. " +
                    "Returns: " +
                    "- Net lovelace change per address (positive = received, negative = sent) " +
                    "- Net asset changes per address for all native tokens " +
                    "- List of sender addresses (those with negative net) " +
                    "- List of receiver addresses (those with positive net) " +
                    "- Summary statistics including total ADA moved, address counts, and transfer type " +
                    "Use this to understand ACTUAL value flow instead of raw inputs/outputs. " +
                    "Example: If address A has 100 ADA input and 95 ADA output, net = -5 ADA (sent 5 ADA). " +
                    "Perfect for: payment analysis, wallet activity tracking, token distribution analysis, DApp interaction analysis.")
    public TransactionNetTransfer getTransactionNetTransfers(String txHash) {
        TransactionDetails txDetails = transactionService.getTransaction(txHash)
                .orElseThrow(() -> new RuntimeException("Transaction not found with hash: " + txHash));

        return TransactionNetTransferCalculator.calculate(txDetails);
    }

    @Tool(name = "transaction-net-transfers-batch",
            description = "Calculate net ADA and token transfers for multiple transactions in a time range. " +
                    "OPTIMIZED: Now supports filtering and sorting to handle large datasets within token limits. " +
                    "IMPORTANT: This is the BATCH version for analyzing multiple transactions efficiently. " +
                    "Use this when you need to analyze activity over a time period (e.g., last 30 minutes, last hour). " +
                    "Parameters: " +
                    "- startSlot: Start slot number (inclusive) " +
                    "- endSlot: End slot number (inclusive) " +
                    "- limit: Maximum number of transactions to process (default: 50, max: 100) " +
                    "- minNetLovelace: Minimum absolute net ADA transfer to include (default: 10000000 = 10 ADA). " +
                    "  Use this to filter out noise (change outputs, collateral returns). " +
                    "  Set to 0 to include all transfers, or higher (e.g., 100000000 = 100 ADA) for major movements only. " +
                    "- sortBy: Sort order - 'net_amount_desc' (default, largest movers first), 'net_amount_asc' (smallest first), " +
                    "  'slot_desc' (most recent first), 'slot_asc' (oldest first) " +
                    "- includeTokens: false for ADA-only analysis (FAST), true to include all native tokens (SLOWER) " +
                    "Returns list of TransactionNetTransfer objects, filtered and sorted as specified. " +
                    "Performance: ADA-only mode uses optimized SQL CTEs (very fast). Token mode fetches full tx details (slower). " +
                    "IMPORTANT: For large time ranges (>1 hour), use higher minNetLovelace (50-100 ADA) and lower limit (25-50) to stay within token limits. " +
                    "Use cases: " +
                    "- Analyze address activity in last N minutes " +
                    "- Find large transfers in time range (use high minNetLovelace) " +
                    "- Track whale movements (e.g., minNetLovelace=1000000000 for 1000+ ADA) " +
                    "- Monitor wallet activity patterns " +
                    "Example: Last 30 min major transfers: minNetLovelace=50000000, limit=50, sortBy='net_amount_desc'")
    public List<TransactionNetTransfer> getTransactionNetTransfersBatch(
            Long startSlot,
            Long endSlot,
            Integer limit,
            Long minNetLovelace,
            String sortBy,
            Boolean includeTokens) {

        // Validate and set defaults
        if (limit == null || limit <= 0) {
            limit = 50;  // Reduced from 100
        }
        if (limit > 100) {
            throw new IllegalArgumentException("Limit cannot exceed 100 transactions");
        }
        if (minNetLovelace == null) {
            minNetLovelace = 10_000_000L;  // 10 ADA default
        }
        if (minNetLovelace < 0) {
            throw new IllegalArgumentException("minNetLovelace cannot be negative");
        }
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "net_amount_desc";
        }
        if (!sortBy.matches("^(net_amount_desc|net_amount_asc|slot_desc|slot_asc)$")) {
            throw new IllegalArgumentException(
                    "Invalid sortBy. Must be: net_amount_desc, net_amount_asc, slot_desc, or slot_asc");
        }
        if (includeTokens == null) {
            includeTokens = false;
        }

        return batchService.getNetTransfersBatch(startSlot, endSlot, limit, minNetLovelace, sortBy, includeTokens);
    }

    @Tool(name = "address-net-activity-summary",
            description = "Aggregate net ADA activity by ADDRESS across multiple transactions in a time range. " +
                    "SCALABLE: Returns address-level summary instead of per-transaction details, perfect for analyzing large time ranges. " +
                    "This tool groups net transfers by address, showing total activity across all transactions. " +
                    "Much more efficient than transaction-net-transfers-batch for large datasets because it returns " +
                    "O(unique addresses) instead of O(transactions × addresses). " +
                    "Parameters: " +
                    "- startSlot: Start slot number (inclusive) " +
                    "- endSlot: End slot number (inclusive) " +
                    "- maxTransactions: Maximum number of transactions to analyze (default: 1000, max: 10000). " +
                    "  This limits the transaction scan but returns aggregated addresses. " +
                    "- minTotalNetLovelace: Minimum absolute net ADA per address to include (default: 10000000 = 10 ADA). " +
                    "  Filters out addresses with insignificant net movements. " +
                    "- sortBy: Sort order - 'total_net_desc' (default, largest net movers first), 'total_net_asc', " +
                    "  'tx_count_desc' (most active addresses), 'total_sent_desc' (largest senders), 'total_received_desc' (largest receivers) " +
                    "- page: Page number (0-based, default: 0) " +
                    "- pageSize: Results per page (default: 50, max: 100) " +
                    "Returns: AddressActivityPage with top addresses showing total_net_lovelace, total_sent, total_received, " +
                    "transaction_count, and list of involved_transactions. " +
                    "Perfect for: " +
                    "- Whale watching: Find addresses with largest net movements (e.g., minTotalNetLovelace=1000000000 for 1000+ ADA) " +
                    "- Identifying most active addresses: sortBy='tx_count_desc' " +
                    "- Daily/weekly summaries: Analyze full day (10000 tx) in single call " +
                    "- Finding major senders/receivers: sortBy='total_sent_desc' or 'total_received_desc' " +
                    "Example: Last 2 hours whale movements: maxTransactions=1000, minTotalNetLovelace=100000000, sortBy='total_net_desc'")
    public AddressActivityPage getAddressNetActivitySummary(
            Long startSlot,
            Long endSlot,
            Integer maxTransactions,
            Long minTotalNetLovelace,
            String sortBy,
            Integer page,
            Integer pageSize) {

        // Validate and set defaults
        if (maxTransactions == null || maxTransactions <= 0) {
            maxTransactions = 1000;
        }
        if (maxTransactions > 10000) {
            throw new IllegalArgumentException("maxTransactions cannot exceed 10000");
        }
        if (minTotalNetLovelace == null) {
            minTotalNetLovelace = 10_000_000L;  // 10 ADA default
        }
        if (minTotalNetLovelace < 0) {
            throw new IllegalArgumentException("minTotalNetLovelace cannot be negative");
        }
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "total_net_desc";
        }
        if (!sortBy.matches("^(total_net_desc|total_net_asc|tx_count_desc|total_sent_desc|total_received_desc)$")) {
            throw new IllegalArgumentException(
                    "Invalid sortBy. Must be: total_net_desc, total_net_asc, tx_count_desc, total_sent_desc, or total_received_desc");
        }
        if (page == null || page < 0) {
            page = 0;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 50;
        }
        if (pageSize > 100) {
            throw new IllegalArgumentException("pageSize cannot exceed 100");
        }

        return addressActivityService.getAddressNetActivity(
                startSlot, endSlot, maxTransactions, minTotalNetLovelace, sortBy, page, pageSize);
    }

    @Tool(name = "transaction-net-transfers-batch-paged",
            description = "Calculate net ADA transfers for large datasets using cursor-based pagination. " +
                    "PRODUCTION-GRADE: Stable, efficient pagination for processing thousands of transactions. " +
                    "Unlike OFFSET-based pagination, this uses keyset pagination for O(log N) performance and stable results. " +
                    "Perfect for: " +
                    "- Enterprise bulk analysis (weeks/months of data) " +
                    "- Export/archival workflows " +
                    "- Processing large time ranges incrementally " +
                    "- Batch jobs that need to process all data without missing records " +
                    "How to use: " +
                    "1. First call: Set cursor=null to get first page " +
                    "2. Response contains 'next_cursor' (Base64 encoded) and 'has_more' flag " +
                    "3. Subsequent calls: Pass the 'next_cursor' value to get next page " +
                    "4. Continue until 'has_more' = false " +
                    "Parameters: " +
                    "- startSlot: Start slot number (inclusive) " +
                    "- endSlot: End slot number (inclusive) " +
                    "- pageSize: Results per page (default: 100, max: 500) " +
                    "- cursor: Base64 encoded cursor from previous page (null for first page) " +
                    "- minNetLovelace: Minimum absolute net ADA transfer (default: 10000000 = 10 ADA) " +
                    "- sortBy: Sort order - 'net_amount_desc' (default), 'net_amount_asc', 'slot_desc', 'slot_asc' " +
                    "Returns: PagedResult with: " +
                    "- results: List of TransactionNetTransfer for this page " +
                    "- next_cursor: Pass this to get next page (null if no more pages) " +
                    "- has_more: true if more pages available " +
                    "- page_size: Actual number of results in this page " +
                    "- total_processed: Total records processed across all pages so far " +
                    "Benefits: " +
                    "- Stable: New transactions during pagination don't cause duplicates/gaps " +
                    "- Fast: Page 1000 performs same as page 1 (O(log N)) " +
                    "- Stateless: Cursor encodes all state, no server-side session needed " +
                    "Example workflow: " +
                    "Page 1: cursor=null → returns next_cursor='eyJsYXN0U2xvdCI6MTIzNDU2LCJ0b3RhbFByb2Nlc3NlZCI6MTAwfQ==' " +
                    "Page 2: cursor='eyJ...' → returns next_cursor='eyJsYXN0U2xvdCI6MTIzNDAwLCJ0b3RhbFByb2Nlc3NlZCI6MjAwfQ==' " +
                    "Continue until has_more=false")
    public PagedResult<TransactionNetTransfer> getTransactionNetTransfersBatchPaged(
            Long startSlot,
            Long endSlot,
            Integer pageSize,
            String cursor,
            Long minNetLovelace,
            String sortBy) {

        // Validate and set defaults
        if (pageSize == null || pageSize <= 0) {
            pageSize = 100;
        }
        if (pageSize > 500) {
            throw new IllegalArgumentException("pageSize cannot exceed 500");
        }
        if (minNetLovelace == null) {
            minNetLovelace = 10_000_000L;  // 10 ADA default
        }
        if (minNetLovelace < 0) {
            throw new IllegalArgumentException("minNetLovelace cannot be negative");
        }
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "net_amount_desc";
        }
        if (!sortBy.matches("^(net_amount_desc|net_amount_asc|slot_desc|slot_asc)$")) {
            throw new IllegalArgumentException(
                    "Invalid sortBy. Must be: net_amount_desc, net_amount_asc, slot_desc, or slot_asc");
        }

        // Decode cursor if provided
        PaginationCursor paginationCursor = null;
        if (cursor != null && !cursor.isEmpty()) {
            try {
                paginationCursor = PaginationCursor.decode(cursor);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid cursor format: " + e.getMessage(), e);
            }
        }

        return batchService.getNetTransfersBatchPaged(
                startSlot, endSlot, pageSize, paginationCursor, minNetLovelace, sortBy);
    }
}
