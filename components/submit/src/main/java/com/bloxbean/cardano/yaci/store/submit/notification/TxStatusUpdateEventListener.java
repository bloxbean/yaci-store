package com.bloxbean.cardano.yaci.store.submit.notification;

import com.bloxbean.cardano.yaci.store.submit.notification.event.TxStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Orchestrator that dispatches transaction status updates to all enabled notification channels.
 * 
 * Supports multiple notification mechanisms:
 * - WebSocket (real-time push to connected clients)
 * - Webhook (HTTP POST to configured endpoints)
 * - Future: Email, SMS, Slack, Discord, etc.
 */
@Component
@ConditionalOnProperty(
        prefix = "store.submit.lifecycle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class TxStatusUpdateEventListener {
    
    private final List<TxNotificationChannel> notificationChannels;
    
    @EventListener
    public void handleStatusUpdateEvent(TxStatusUpdateEvent event) {
        log.debug("Processing status update event: txHash={}, previousStatus={}, newStatus={}", 
                event.getTxHash(), event.getPreviousStatus(), event.getNewStatus());
        
        if (notificationChannels.isEmpty()) {
            log.debug("No notification channels configured");
            return;
        }
        
        // Dispatch to all enabled notification channels
        for (TxNotificationChannel channel : notificationChannels) {
            if (!channel.isEnabled()) {
                continue;
            }
            
            try {
                log.debug("Dispatching notification to channel: {}", channel.getChannelName());
                channel.notify(event);
            } catch (Exception e) {
                log.error("Failed to send notification via {}: txHash={}, error={}", 
                        channel.getChannelName(), event.getTxHash(), e.getMessage(), e);
                // Continue to next channel - one failure should not affect others
            }
        }
        
        log.debug("Notification dispatched to {} channel(s)", notificationChannels.size());
    }
}
