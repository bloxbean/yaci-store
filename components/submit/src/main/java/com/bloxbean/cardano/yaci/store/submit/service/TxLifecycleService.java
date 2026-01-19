package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.client.transaction.util.TransactionUtil;
import com.bloxbean.cardano.yaci.store.submit.domain.SubmittedTransaction;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatus;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatusUpdateRequest;
import com.bloxbean.cardano.yaci.store.submit.notification.event.TxStatusUpdateEvent;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.mapper.SubmittedTransactionMapper;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.model.SubmittedTransactionEntity;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.repository.SubmittedTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing transaction lifecycle.
 * Handles creation, updates, and state transitions of submitted transactions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TxLifecycleService {
    
    private final SubmittedTransactionRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final TxSubmitter txSubmitter;

    @Transactional(readOnly = true)
    public Set<String> findExistingTxs(List<String> txHashes) {
        return repository.findByTxHashIn(txHashes).stream()
                .map(SubmittedTransactionEntity::getTxHash)
                .collect(Collectors.toSet());

    }

    @Transactional(readOnly = true)
    public Set<String> findTxConfirmedAfterSlot(Long slot) {
        return repository.findByConfirmedSlotGreaterThan(slot).stream()
                .map(SubmittedTransactionEntity::getTxHash)
                .collect(Collectors.toSet());
    }

    public Set<String> findByStatusAndConfirmedBlockNumberLessThan(
            TxStatus status,
            Long maxBlockNumber
    ) {
        return repository.findByStatusAndConfirmedBlockNumberLessThan(status, maxBlockNumber).stream()
                .map(SubmittedTransactionEntity::getTxHash)
                .collect(Collectors.toSet());
    }
    /**
     * Create a new submitted transaction record.
     */
    @Transactional
    public SubmittedTransaction createSubmittedTransaction(String txHash) {
        log.info("Creating submitted transaction record for txHash: {}", txHash);
        
        SubmittedTransactionEntity entity = SubmittedTransactionEntity.builder()
                .txHash(txHash)
                .status(TxStatus.SUBMITTED)
                .submittedAt(new Timestamp(System.currentTimeMillis()))
                .build();
        
        entity = repository.save(entity);
        SubmittedTransaction transaction = SubmittedTransactionMapper.toSubmittedTransaction(entity);
        
        // Publish status update event
        publishStatusUpdateEvent(txHash, null, TxStatus.SUBMITTED, transaction, "Transaction submitted");
        
        return transaction;
    }

    /**
     * Update transaction status with validation.
     * 
     * @param request Status update request containing all parameters
     * @return Updated transaction or empty if not found/invalid transition
     */
    @Transactional
    public Optional<SubmittedTransaction> updateStatus(TxStatusUpdateRequest request) {
        log.info("Updating transaction status: txHash={}, newStatus={}", request.getTxHash(), request.getNewStatus());
        
        Optional<SubmittedTransactionEntity> entityOpt = repository.findById(request.getTxHash());
        
        // Special handling for FAILED status - can create new record if not exists
        if (entityOpt.isEmpty() && request.getNewStatus() == TxStatus.FAILED) {
            return Optional.of(createFailedTransaction(request.getTxHash(), request.getMessage()));
        }
        
        if (entityOpt.isEmpty()) {
            log.warn("Transaction not found: {}", request.getTxHash());
            return Optional.empty();
        }
        
        SubmittedTransactionEntity entity = entityOpt.get();
        TxStatus previousStatus = entity.getStatus();
        TxStatus newStatus = request.getNewStatus();
        
        // Validate state transition
        if (!isValidTransition(previousStatus, newStatus)) {
            log.warn("Invalid state transition: {} -> {} for txHash={}", previousStatus, newStatus, request.getTxHash());
            return Optional.of(SubmittedTransactionMapper.toSubmittedTransaction(entity));
        }
        
        // Update status and related fields
        entity.setStatus(newStatus);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        
        switch (newStatus) {
            case CONFIRMED:
                entity.setConfirmedAt(now);
                entity.setConfirmedSlot(request.getSlot());
                entity.setConfirmedBlockNumber(request.getBlockNumber());
                break;
            case SUCCESS:
                entity.setSuccessAt(now);
                break;
            case FINALIZED:
                entity.setFinalizedAt(now);
                break;
            case ROLLED_BACK:
                entity.setErrorMessage(request.getMessage());
                // Keep confirmed_at and confirmed_slot for history
                break;
            case FAILED:
                entity.setErrorMessage(request.getMessage());
                break;
            default:
                break;
        }
        
        entity = repository.save(entity);
        SubmittedTransaction transaction = SubmittedTransactionMapper.toSubmittedTransaction(entity);
        
        // Publish status update event
        String eventMessage = request.getMessage() != null ? request.getMessage() 
                : generateStatusMessage(newStatus, request.getBlockNumber());
        publishStatusUpdateEvent(request.getTxHash(), previousStatus, newStatus, transaction, eventMessage);
        
        return Optional.of(transaction);
    }
    
    /**
     * Create a new FAILED transaction record.
     */
    private SubmittedTransaction createFailedTransaction(String txHash, String errorMessage) {
        log.info("Creating failed transaction record: txHash={}", txHash);
        
        SubmittedTransactionEntity entity = SubmittedTransactionEntity.builder()
                .txHash(txHash)
                .status(TxStatus.FAILED)
                .submittedAt(new Timestamp(System.currentTimeMillis()))
                .errorMessage(errorMessage)
                .build();
        
        entity = repository.save(entity);
        SubmittedTransaction transaction = SubmittedTransactionMapper.toSubmittedTransaction(entity);
        
        publishStatusUpdateEvent(txHash, null, TxStatus.FAILED, transaction, errorMessage);
        
        return transaction;
    }
    
    /**
     * Validate if state transition is allowed.
     */
    private boolean isValidTransition(TxStatus from, TxStatus to) {
        if (from == to) {
            return false; // No transition needed
        }
        
        switch (to) {
            case CONFIRMED:
                return from == TxStatus.SUBMITTED || from == TxStatus.ROLLED_BACK;
            case SUCCESS:
                return from == TxStatus.CONFIRMED;
            case FINALIZED:
                return from == TxStatus.SUCCESS;
            case ROLLED_BACK:
                return from == TxStatus.CONFIRMED || from == TxStatus.SUCCESS;
            case FAILED:
                return true; // Can fail from any state
            default:
                return false;
        }
    }
    
    /**
     * Generate default status message.
     */
    private String generateStatusMessage(TxStatus status, Long blockNumber) {
        switch (status) {
            case CONFIRMED:
                return "Transaction confirmed in block " + blockNumber;
            case SUCCESS:
                return "Transaction reached SUCCESS state";
            case FINALIZED:
                return "Transaction reached FINALIZED state (2160+ blocks)";
            case ROLLED_BACK:
                return "Transaction rolled back due to chain reorganization";
            case FAILED:
                return "Transaction submission failed";
            default:
                return "Transaction status updated to " + status;
        }
    }
    
    /**
     * Mark transaction as FAILED.
     * Wrapper method for backwards compatibility and convenience.
     */
    @Transactional
    public SubmittedTransaction markAsFailed(String txHash, String errorMessage) {
        TxStatusUpdateRequest request = TxStatusUpdateRequest.failed(txHash, errorMessage);
        return updateStatus(request)
                .orElseThrow(() -> new IllegalStateException("Failed to create FAILED transaction record"));
    }
    
    /**
     * Get transaction by txHash.
     */
    public Optional<SubmittedTransaction> getTransaction(String txHash) {
        return repository.findById(txHash)
                .map(SubmittedTransactionMapper::toSubmittedTransaction);
    }
    
    /**
     * Submit transaction and track its lifecycle.
     * This method combines transaction submission with automatic lifecycle tracking.
     * 
     * @param cborTx Transaction in CBOR format
     * @return SubmittedTransaction with lifecycle tracking
     * @throws UnsupportedOperationException if no transaction submitter is configured
     */
    @Transactional
    public SubmittedTransaction submitTransaction(byte[] cborTx) {
        try {
            String txHash = txSubmitter.submitTx(cborTx);
            log.info("Transaction accepted via {}: {}", txSubmitter.getName(), txHash);
            return createSubmittedTransaction(txHash);
        } catch (Exception e) {
            log.error("Error submitting transaction via {}: {}", txSubmitter.getName(), e.getMessage());
            String txHash = TransactionUtil.getTxHash(cborTx);
            return markAsFailed(txHash, e.getMessage());
        }
    }
    
    /**
     * Publish status update event.
     */
    private void publishStatusUpdateEvent(String txHash, TxStatus previousStatus, TxStatus newStatus, 
                                           SubmittedTransaction transaction, String message) {
        TxStatusUpdateEvent event = TxStatusUpdateEvent.of(txHash, previousStatus, newStatus, transaction, message);
        eventPublisher.publishEvent(event);
        log.debug("Published TxStatusUpdateEvent: {}", event);
    }
}

