package com.bloxbean.cardano.yaci.store.submit.notification;

import com.bloxbean.cardano.yaci.store.submit.domain.SubmittedTransaction;
import com.bloxbean.cardano.yaci.store.submit.event.TxStatusUpdateEvent;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.model.TxEventLogEntity;
import com.bloxbean.cardano.yaci.store.submit.storage.impl.repository.TxEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

/**
 * Event log notification channel implementation.
 * Records all transaction status updates to database for audit trail and analytics.
 * 
 * Use cases:
 * - Compliance and audit trail
 * - Debugging transaction issues
 * - Analytics and reporting
 * - Historical event replay
 */
@Component
@ConditionalOnProperty(
        prefix = "store.submit.lifecycle.eventlog",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true  // Enabled by default - critical for audit
)
@RequiredArgsConstructor
@Slf4j
public class EventLogNotificationChannel implements TxNotificationChannel {
    
    private final TxEventLogRepository eventLogRepository;
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(TxStatusUpdateEvent event) {
        log.debug("Recording event to audit log: txHash={}, status={} -> {}", 
                event.getTxHash(), event.getPreviousStatus(), event.getNewStatus());
        
        try {
            TxEventLogEntity logEntity = buildEventLog(event);
            eventLogRepository.save(logEntity);
            
            log.debug("Event recorded successfully: txHash={}, eventId={}", 
                    event.getTxHash(), logEntity.getId());
            
        } catch (Exception e) {
            log.error("Failed to record event to audit log: txHash={}, error={}", 
                    event.getTxHash(), e.getMessage(), e);
            // Don't rethrow - audit log failure should not break transaction flow
        }
    }
    
    @Override
    public String getChannelName() {
        return "EventLog";
    }
    
    /**
     * Build event log entity from status update event.
     */
    private TxEventLogEntity buildEventLog(TxStatusUpdateEvent event) {
        TxEventLogEntity.TxEventLogEntityBuilder builder = TxEventLogEntity.builder()
                .txHash(event.getTxHash())
                .previousStatus(event.getPreviousStatus())
                .newStatus(event.getNewStatus())
                .message(event.getMessage())
                .eventTimestamp(new Timestamp(event.getTimestamp()));
        
        // Add additional context if transaction data is available
        SubmittedTransaction tx = event.getTransaction();
        if (tx != null) {
            builder.confirmedSlot(tx.getConfirmedSlot())
                   .confirmedBlockNumber(tx.getConfirmedBlockNumber());
        }
        
        return builder.build();
    }
}

