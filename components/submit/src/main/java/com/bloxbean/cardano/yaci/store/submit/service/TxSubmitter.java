package com.bloxbean.cardano.yaci.store.submit.service;

/**
 * Interface for transaction submission strategies.
 * Different implementations can submit transactions via different methods
 * (local node, Ogmios, remote API, etc.)
 * 
 * This interface is used by SmartTxSubmissionService to select the appropriate
 * submission method based on configuration. Implementations are ordered by @Order
 * annotation to determine priority.
 */
public interface TxSubmitter {
    
    /**
     * Submit a transaction.
     * 
     * @param cborTx The transaction in CBOR format
     * @return Transaction hash if submission was successful
     * @throws Exception if submission fails
     */
    String submitTx(byte[] cborTx) throws Exception;
    
    /**
     * Get the name of this submitter for logging/debugging purposes.
     * 
     * @return Name of the submitter (e.g., "LocalNode", "Ogmios")
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}

