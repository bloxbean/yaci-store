package com.bloxbean.cardano.yaci.store.submit.notification;

import com.bloxbean.cardano.yaci.store.submit.event.TxStatusUpdateEvent;
import com.bloxbean.cardano.yaci.store.submit.notification.websocket.TxLifecycleWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket notification channel implementation.
 * Broadcasts transaction status updates to connected WebSocket clients.
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
public class WebSocketNotificationChannel implements TxNotificationChannel {
    
    private final TxLifecycleWebSocketHandler webSocketHandler;
    
    @Override
    public void notify(TxStatusUpdateEvent event) {
        log.debug("Broadcasting status update via WebSocket: txHash={}, status={}", 
                event.getTxHash(), event.getNewStatus());
        
        Map<String, Object> payload = buildPayload(event);
        webSocketHandler.broadcastStatusUpdate(event.getTxHash(), payload);
    }
    
    @Override
    public String getChannelName() {
        return "WebSocket";
    }
    
    /**
     * Build WebSocket payload from event.
     */
    private Map<String, Object> buildPayload(TxStatusUpdateEvent event) {
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
        
        return payload;
    }
}

