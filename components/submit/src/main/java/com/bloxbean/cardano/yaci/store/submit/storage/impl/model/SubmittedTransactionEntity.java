package com.bloxbean.cardano.yaci.store.submit.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

/**
 * Entity representing a submitted transaction with lifecycle tracking.
 * Tracks the transaction through states: SUBMITTED → CONFIRMED → SUCCESS → FINALIZED.
 * Can transition to ROLLED_BACK if chain reorganization occurs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "submitted_transaction")
@EqualsAndHashCode(callSuper = false)
public class SubmittedTransactionEntity {
    
    @Id
    @Column(name = "tx_hash", nullable = false, length = 64)
    private String txHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TxStatus status;
    
    @Column(name = "submitted_at", nullable = false)
    private Timestamp submittedAt;
    
    // Confirmation tracking
    @Column(name = "confirmed_at")
    private Timestamp confirmedAt;
    
    @Column(name = "confirmed_slot")
    private Long confirmedSlot;
    
    @Column(name = "confirmed_block_number")
    private Long confirmedBlockNumber;
    
    // Success tracking
    @Column(name = "success_at")
    private Timestamp successAt;
    
    // Finalized tracking
    @Column(name = "finalized_at")
    private Timestamp finalizedAt;
    
    // Error tracking
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Update tracking
    @Column(name = "update_datetime")
    private Timestamp updateDatetime;
}

