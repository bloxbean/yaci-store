package com.bloxbean.cardano.yaci.store.submit.domain;

/**
 * Transaction lifecycle status enum.
 * 
 * State transitions:
 * - SUBMITTED → CONFIRMED: Via TransactionEvent or periodic check
 * - CONFIRMED → SUCCESS: After N blocks without rollback (configurable)
 * - SUCCESS → FINALIZED: After 2,160 blocks (security parameter)
 * - CONFIRMED/SUCCESS → ROLLED_BACK: Via RollbackEvent
 * - ROLLED_BACK → SUBMITTED → CONFIRMED: After re-submission
 * - SUBMITTED → FAILED: If submission rejected
 */
public enum TxStatus {
    /**
     * Transaction submitted to the network but not yet confirmed in a block.
     */
    SUBMITTED,
    
    /**
     * Transaction appeared in a block (may still be rolled back).
     * This is the target state for re-submission after rollback.
     */
    CONFIRMED,
    
    /**
     * Transaction has N confirmations (configurable, e.g., 15 blocks).
     * Practically safe for most use cases.
     */
    SUCCESS,
    
    /**
     * Transaction reached mathematical finality (2,160 blocks - security parameter k).
     * Practically impossible to rollback.
     */
    FINALIZED,
    
    /**
     * Transaction submission failed (rejected by network).
     */
    FAILED,
    
    /**
     * Transaction was rolled back due to chain reorganization.
     * Can be re-submitted to return to SUBMITTED → CONFIRMED.
     */
    ROLLED_BACK
}

