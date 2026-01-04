package com.bloxbean.cardano.yaci.store.submit.notification.event;

import com.bloxbean.cardano.yaci.store.submit.domain.SubmittedTransaction;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatus;
import lombok.*;

/**
 * Event published when a transaction status changes.
 * This event can be listened to for notifications, logging, or other actions.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
public class TxStatusUpdateEvent {
    
    /**
     * The transaction hash.
     */
    private String txHash;
    
    /**
     * The previous status (null if this is the initial submission).
     */
    private TxStatus previousStatus;
    
    /**
     * The new status.
     */
    private TxStatus newStatus;
    
    /**
     * The complete transaction details.
     */
    private SubmittedTransaction transaction;
    
    /**
     * Timestamp when the status changed.
     */
    private Long timestamp;
    
    /**
     * Additional metadata or context about the status change.
     */
    private String message;
    
    /**
     * Create an event for a status transition.
     */
    public static TxStatusUpdateEvent of(String txHash, TxStatus previousStatus, TxStatus newStatus, 
                                          SubmittedTransaction transaction) {
        return TxStatusUpdateEvent.builder()
                .txHash(txHash)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .transaction(transaction)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * Create an event with a custom message.
     */
    public static TxStatusUpdateEvent of(String txHash, TxStatus previousStatus, TxStatus newStatus, 
                                          SubmittedTransaction transaction, String message) {
        return TxStatusUpdateEvent.builder()
                .txHash(txHash)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .transaction(transaction)
                .timestamp(System.currentTimeMillis())
                .message(message)
                .build();
    }
}

