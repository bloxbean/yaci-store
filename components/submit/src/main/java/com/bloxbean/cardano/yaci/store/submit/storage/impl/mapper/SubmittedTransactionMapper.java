package com.bloxbean.cardano.yaci.store.submit.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.submit.domain.SubmittedTransaction;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.model.SubmittedTransactionEntity;

/**
 * Mapper between SubmittedTransactionEntity and SubmittedTransaction domain model.
 */
public class SubmittedTransactionMapper {
    
    /**
     * Convert entity to domain model.
     */
    public static SubmittedTransaction toSubmittedTransaction(SubmittedTransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return SubmittedTransaction.builder()
                .txHash(entity.getTxHash())
                .status(entity.getStatus())
                .submittedAt(entity.getSubmittedAt())
                .confirmedAt(entity.getConfirmedAt())
                .confirmedSlot(entity.getConfirmedSlot())
                .confirmedBlockNumber(entity.getConfirmedBlockNumber())
                .successAt(entity.getSuccessAt())
                .finalizedAt(entity.getFinalizedAt())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
    
    /**
     * Convert domain model to entity.
     */
    public static SubmittedTransactionEntity toEntity(SubmittedTransaction domain) {
        if (domain == null) {
            return null;
        }
        
        return SubmittedTransactionEntity.builder()
                .txHash(domain.getTxHash())
                .status(domain.getStatus())
                .submittedAt(domain.getSubmittedAt())
                .confirmedAt(domain.getConfirmedAt())
                .confirmedSlot(domain.getConfirmedSlot())
                .confirmedBlockNumber(domain.getConfirmedBlockNumber())
                .successAt(domain.getSuccessAt())
                .finalizedAt(domain.getFinalizedAt())
                .errorMessage(domain.getErrorMessage())
                .build();
    }
}

