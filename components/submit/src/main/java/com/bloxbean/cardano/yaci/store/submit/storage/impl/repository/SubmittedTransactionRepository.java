package com.bloxbean.cardano.yaci.store.submit.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.submit.domain.TxStatus;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.model.SubmittedTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface SubmittedTransactionRepository extends JpaRepository<SubmittedTransactionEntity, String> {
    
    /**
     * Find all transactions by status.
     */
    List<SubmittedTransactionEntity> findByStatus(TxStatus status);
    
    /**
     * Find all transactions by status with pagination.
     */
    Page<SubmittedTransactionEntity> findByStatus(TxStatus status, Pageable pageable);
    
    /**
     * Find SUCCESS transactions eligible for FINALIZED transition.
     * These are transactions with confirmed_block_number less than the threshold.
     */
    @Query("SELECT t FROM SubmittedTransactionEntity t " +
           "WHERE t.status = :status " +
           "AND t.confirmedBlockNumber < :maxBlockNumber")
    List<SubmittedTransactionEntity> findByStatusAndConfirmedBlockNumberLessThan(
        @Param("status") TxStatus status,
        @Param("maxBlockNumber") Long maxBlockNumber
    );
    
    /**
     * Find transactions by confirmed slot (for rollback detection).
     */
    List<SubmittedTransactionEntity> findByConfirmedSlotGreaterThan(Long slot);
    
    /**
     * Find transactions submitted after a specific time.
     */
    List<SubmittedTransactionEntity> findBySubmittedAtAfter(Timestamp after);
    
    /**
     * Find transactions by status and submitted after a specific time.
     */
    List<SubmittedTransactionEntity> findByStatusAndSubmittedAtAfter(TxStatus status, Timestamp after);
    
    /**
     * Delete transactions by confirmed slot greater than specified (for rollback).
     */
    int deleteByConfirmedSlotGreaterThan(Long slot);
    
    /**
     * Count transactions by status.
     */
    long countByStatus(TxStatus status);
}

