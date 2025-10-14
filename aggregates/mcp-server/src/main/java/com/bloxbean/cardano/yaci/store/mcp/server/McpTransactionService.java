package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.transaction.service.TransactionService;
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
}
