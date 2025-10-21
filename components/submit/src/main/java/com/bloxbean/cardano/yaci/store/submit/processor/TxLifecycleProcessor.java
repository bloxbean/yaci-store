package com.bloxbean.cardano.yaci.store.submit.processor;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.submit.SubmitLifecycleProperties;
import com.bloxbean.cardano.yaci.store.submit.SubmitStoreConfiguration;
import com.bloxbean.cardano.yaci.store.submit.domain.SubmittedTransaction;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatus;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatusUpdateRequest;
import com.bloxbean.cardano.yaci.store.submit.service.TxLifecycleService;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.model.SubmittedTransactionEntity;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.repository.SubmittedTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Unified processor for transaction lifecycle management.
 * 
 * Responsibilities:
 * 1. Listen to TransactionEvent to mark transactions as CONFIRMED
 * 2. Listen to RollbackEvent to mark transactions as ROLLED_BACK
 * 3. Scheduled job to transition CONFIRMED → SUCCESS
 * 4. Scheduled job to transition SUCCESS → FINALIZED
 */
@Component
@RequiredArgsConstructor
@Transactional
@EnableIf(SubmitStoreConfiguration.STORE_SUBMIT_ENABLED)
@ConditionalOnProperty(
        prefix = "store.submit.lifecycle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@Slf4j
public class TxLifecycleProcessor {
    
    private final TxLifecycleService lifecycleService;
    private final CursorService cursorService;
    private final SubmitLifecycleProperties properties;
    
    // ========================================================================
    // EVENT LISTENERS
    // ========================================================================
    
    /**
     * Listen to TransactionEvent to mark submitted transactions as CONFIRMED.
     * Triggered when a transaction appears in a block.
     */
    @EventListener
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        if (event.getMetadata() == null) {
            return;
        }
        
        Long slot = event.getMetadata().getSlot();
        Long blockNumber = event.getMetadata().getBlock();
        
        if (slot == null || blockNumber == null) {
            log.warn("TransactionEvent missing slot or block number, skipping confirmation");
            return;
        }

        Set<String> existingSubmittedTxs = lifecycleService.findExistingTxs(
                event.getTransactions().stream()
                        .map(tx -> tx.getBody().getTxHash())
                        .toList()
        );
        
        event.getTransactions()
                .stream().filter(transaction -> existingSubmittedTxs.contains(transaction.getBody().getTxHash()))
                .forEach(transaction -> {
            String txHash = transaction.getBody().getTxHash();
            
            // Try to mark as confirmed
            TxStatusUpdateRequest request = TxStatusUpdateRequest.confirmed(txHash, slot, blockNumber);
            lifecycleService.updateStatus(request)
                    .ifPresent(submittedTx -> 
                        log.debug("Transaction confirmed: txHash={}, slot={}, block={}", txHash, slot, blockNumber)
                    );
        });
    }
    
    /**
     * Listen to RollbackEvent to mark confirmed transactions as ROLLED_BACK.
     * Triggered when a chain reorganization occurs.
     */
    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        Long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        
        log.info("Processing rollback for submitted transactions, rollback to slot: {}", rollbackSlot);
        
        // Find all transactions confirmed after the rollback slot
        Set<String> affectedTxs = lifecycleService.findTxConfirmedAfterSlot(rollbackSlot);

        log.info("Found {} submitted transactions to rollback", affectedTxs.size());
        
        affectedTxs.forEach(txHash -> {
            String reason = String.format("Chain reorganization: rollback to slot %d", rollbackSlot);
            TxStatusUpdateRequest request = TxStatusUpdateRequest.rolledBack(txHash, reason);
            lifecycleService.updateStatus(request);
        });
        
        log.info("Completed rollback processing for {} submitted transactions", affectedTxs.size());
    }
    
    // ========================================================================
    // SCHEDULED JOBS
    // ========================================================================
    
    /**
     * Scheduled job: Transition CONFIRMED → SUCCESS.
     * Runs every 20 seconds by default.
     * Marks transactions as SUCCESS after they have N confirmations without rollback.
     */
    @Scheduled(fixedDelayString = "${store.submit.lifecycle.success-check-interval-ms:20000}")
    @Transactional
    public void updateSuccessStatus() {
        Optional<Cursor> cursorOpt = cursorService.getCursor();
        if (cursorOpt.isEmpty()) {
            return;
        }
        
        Long currentBlock = cursorOpt.get().getBlock();
        if (currentBlock == null) {
            return;
        }
        
        // Find CONFIRMED transactions where confirmed_block_number <= currentBlock - successBlockDepth
        Long maxBlockNumber = currentBlock - properties.getSuccessBlockDepth();
        Set<String> eligibleTxs =
                lifecycleService.findByStatusAndConfirmedBlockNumberLessThan(TxStatus.CONFIRMED, maxBlockNumber);
        
        if (!eligibleTxs.isEmpty()) {
            log.info("Found {} CONFIRMED transactions eligible for SUCCESS state (current block: {}, threshold: {})", 
                    eligibleTxs.size(), currentBlock, maxBlockNumber);
            
            eligibleTxs.forEach(txHash -> {
                TxStatusUpdateRequest request = TxStatusUpdateRequest.success(txHash);
                lifecycleService.updateStatus(request);
            });
            
            log.info("Updated {} transactions to SUCCESS state", eligibleTxs.size());
        }
    }
    
    /**
     * Scheduled job: Transition SUCCESS → FINALIZED.
     * Runs every hour by default (3600000 ms = 1 hour).
     * Marks transactions as FINALIZED after they reach mathematical finality (2,160 blocks).
     */
    @Scheduled(fixedDelayString = "${store.submit.lifecycle.finalized-check-interval-ms:3600000}")
    @Transactional
    public void updateFinalizedStatus() {
        Optional<Cursor> cursorOpt = cursorService.getCursor();
        if (cursorOpt.isEmpty()) {
            return;
        }
        
        Long currentBlock = cursorOpt.get().getBlock();
        if (currentBlock == null) {
            return;
        }
        
        // Find SUCCESS transactions where confirmed_block_number <= currentBlock - finalizedBlockDepth
        Long maxBlockNumber = currentBlock - properties.getFinalizedBlockDepth();
        
        Set<String> eligibleTxs =
                lifecycleService.findByStatusAndConfirmedBlockNumberLessThan(TxStatus.SUCCESS, maxBlockNumber);
        
        if (!eligibleTxs.isEmpty()) {
            log.info("Found {} SUCCESS transactions eligible for FINALIZED state (current block: {}, threshold: {})", 
                    eligibleTxs.size(), currentBlock, maxBlockNumber);
            
            eligibleTxs.forEach(txHash -> {
                TxStatusUpdateRequest request = TxStatusUpdateRequest.finalized(txHash);
                lifecycleService.updateStatus(request);
            });
            
            log.info("Updated {} transactions to FINALIZED state", eligibleTxs.size());
        }
    }
}

