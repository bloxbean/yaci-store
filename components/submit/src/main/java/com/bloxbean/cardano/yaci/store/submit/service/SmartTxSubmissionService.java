package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.client.transaction.util.TransactionUtil;
import com.bloxbean.cardano.yaci.store.submit.domain.SubmittedTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Smart submission service that automatically selects the best available
 * transaction submitter and tracks transaction lifecycle.
 * 
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "store.submit.lifecycle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class SmartTxSubmissionService {
    
    private final TxSubmitter txSubmitter;
    private final TxLifecycleService lifecycleService;
    
    /**
     * Submit transaction using the first available submitter.
     * Submitters are ordered by @Order annotation (lower value = higher priority).
     * 
     * @param cborTx Transaction in CBOR format
     * @return SubmittedTransaction with lifecycle tracking
     * @throws UnsupportedOperationException if no submitters are available
     */
    public SubmittedTransaction submitTransaction(byte[] cborTx) {
        try {
            String txHash = txSubmitter.submitTx(cborTx);
            return lifecycleService.createSubmittedTransaction(txHash);
        } catch (Exception e) {
            String txHash = extractTxHashOrGenerate(cborTx);
            return lifecycleService.markAsFailed(txHash, e.getMessage());
        }
    }

    /**
     * Extract txHash from CBOR or generate a placeholder.
     */
    private String extractTxHashOrGenerate(byte[] cborTx) {
        try {
            return TransactionUtil.getTxHash(cborTx);
        } catch (Exception e) {
            log.warn("Could not extract txHash from CBOR, generating a temporary one.", e);
            return "generated-failed-" + System.currentTimeMillis();
        }
    }
}

