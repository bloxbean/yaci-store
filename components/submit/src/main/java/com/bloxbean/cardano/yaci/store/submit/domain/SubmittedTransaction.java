package com.bloxbean.cardano.yaci.store.submit.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Map;

/**
 * Domain model for a submitted transaction with lifecycle tracking.
 * This is the public-facing API model (DTO) that clients will work with.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SubmittedTransaction {

    private String txHash;
    private TxStatus status;

    // Transaction body CBOR (included in build-and-submit responses)
    private String txBodyCbor;

    private Timestamp submittedAt;
    
    // Confirmation info
    private Timestamp confirmedAt;
    private Long confirmedSlot;
    private Long confirmedBlockNumber;
    
    // Success info
    private Timestamp successAt;
    
    // Finalized info
    private Timestamp finalizedAt;
    
    // Error info
    private String errorMessage;
    
    /**
     * Calculate the number of confirmations based on current block number.
     * Returns null if not yet confirmed or if current block is unknown.
     */
    public Integer getConfirmations(Long currentBlockNumber) {
        if (confirmedBlockNumber == null || currentBlockNumber == null) {
            return null;
        }
        if (currentBlockNumber < confirmedBlockNumber) {
            return 0;
        }
        return (int) (currentBlockNumber - confirmedBlockNumber);
    }
    
    /**
     * Check if transaction is in a final state (cannot change anymore).
     */
    public boolean isFinalState() {
        return status == TxStatus.FINALIZED || status == TxStatus.FAILED;
    }
    
    /**
     * Check if transaction can be re-submitted (only for ROLLED_BACK or FAILED).
     */
    public boolean canResubmit() {
        return status == TxStatus.ROLLED_BACK || status == TxStatus.FAILED;
    }
}

