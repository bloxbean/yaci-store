package com.bloxbean.cardano.yaci.store.submit.notification;

import com.bloxbean.cardano.yaci.store.submit.event.TxStatusUpdateEvent;

/**
 * Interface for transaction status notification channels.
 * Implementations can provide different notification mechanisms (WebSocket, Webhook, Email, etc.).
 */
public interface TxNotificationChannel {
    
    /**
     * Send notification about transaction status update.
     * 
     * @param event The status update event containing transaction details
     */
    void notify(TxStatusUpdateEvent event);
    
    /**
     * Get the name of this notification channel (for logging).
     * 
     * @return Channel name (e.g., "WebSocket", "Webhook", "Email")
     */
    String getChannelName();
    
    /**
     * Check if this channel is enabled/ready to send notifications.
     * 
     * @return true if channel is enabled and operational
     */
    default boolean isEnabled() {
        return true;
    }
}

