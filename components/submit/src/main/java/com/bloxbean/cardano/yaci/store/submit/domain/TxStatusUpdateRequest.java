package com.bloxbean.cardano.yaci.store.submit.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for transaction status update.
 * Encapsulates all parameters needed for status transitions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TxStatusUpdateRequest {
    
    /**
     * Transaction hash (required).
     */
    private String txHash;
    
    /**
     * Target status (required).
     */
    private TxStatus newStatus;
    
    /**
     * Confirmation slot (required for CONFIRMED status).
     */
    private Long slot;
    
    /**
     * Confirmation block number (required for CONFIRMED status).
     */
    private Long blockNumber;
    
    /**
     * Custom message for status update event (optional).
     */
    private String message;
    
    /**
     * Factory method for CONFIRMED status.
     */
    public static TxStatusUpdateRequest confirmed(String txHash, Long slot, Long blockNumber) {
        return TxStatusUpdateRequest.builder()
                .txHash(txHash)
                .newStatus(TxStatus.CONFIRMED)
                .slot(slot)
                .blockNumber(blockNumber)
                .build();
    }
    
    /**
     * Factory method for SUCCESS status.
     */
    public static TxStatusUpdateRequest success(String txHash) {
        return TxStatusUpdateRequest.builder()
                .txHash(txHash)
                .newStatus(TxStatus.SUCCESS)
                .build();
    }
    
    /**
     * Factory method for FINALIZED status.
     */
    public static TxStatusUpdateRequest finalized(String txHash) {
        return TxStatusUpdateRequest.builder()
                .txHash(txHash)
                .newStatus(TxStatus.FINALIZED)
                .build();
    }
    
    /**
     * Factory method for ROLLED_BACK status.
     */
    public static TxStatusUpdateRequest rolledBack(String txHash, String reason) {
        return TxStatusUpdateRequest.builder()
                .txHash(txHash)
                .newStatus(TxStatus.ROLLED_BACK)
                .message(reason)
                .build();
    }
    
    /**
     * Factory method for FAILED status.
     */
    public static TxStatusUpdateRequest failed(String txHash, String errorMessage) {
        return TxStatusUpdateRequest.builder()
                .txHash(txHash)
                .newStatus(TxStatus.FAILED)
                .message(errorMessage)
                .build();
    }
}

