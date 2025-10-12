package com.bloxbean.cardano.yaci.store.submit.websocket;

import com.bloxbean.cardano.yaci.store.submit.event.TxStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Event listener that broadcasts transaction status updates to WebSocket clients.
 * Only enabled when WebSocket is configured.
 */
@Component
@ConditionalOnProperty(
        prefix = "store.submit.lifecycle.websocket",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class TxStatusUpdateEventListener {
    
    private final TxLifecycleWebSocketHandler webSocketHandler;
    
    @EventListener
    public void handleStatusUpdateEvent(TxStatusUpdateEvent event) {
        log.debug("Broadcasting status update via WebSocket: txHash={}, status={}", 
                event.getTxHash(), event.getNewStatus());
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("previousStatus", event.getPreviousStatus());
        payload.put("newStatus", event.getNewStatus());
        payload.put("timestamp", event.getTimestamp());
        payload.put("message", event.getMessage());
        
        if (event.getTransaction() != null) {
            payload.put("submittedAt", event.getTransaction().getSubmittedAt());
            payload.put("confirmedAt", event.getTransaction().getConfirmedAt());
            payload.put("confirmedSlot", event.getTransaction().getConfirmedSlot());
            payload.put("confirmedBlockNumber", event.getTransaction().getConfirmedBlockNumber());
            payload.put("successAt", event.getTransaction().getSuccessAt());
            payload.put("finalizedAt", event.getTransaction().getFinalizedAt());
            payload.put("errorMessage", event.getTransaction().getErrorMessage());
        }
        
        webSocketHandler.broadcastStatusUpdate(event.getTxHash(), payload);
    }
}

