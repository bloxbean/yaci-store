package com.bloxbean.cardano.yaci.store.submit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for transaction lifecycle tracking.
 */
@Data
@Component
@ConfigurationProperties(prefix = "store.submit.lifecycle")
public class SubmitLifecycleProperties {
    
    /**
     * Enable transaction lifecycle tracking.
     * Default: false
     */
    private boolean enabled = false;
    
    /**
     * Block depth for SUCCESS state (configurable per application needs).
     * Default: 15 blocks
     */
    private int successBlockDepth = 15;
    
    /**
     * Block depth for FINALIZED state (Cardano security parameter - fixed).
     * Default: 2160 blocks
     */
    private int finalizedBlockDepth = 2160;
    
    /**
     * SUCCESS status check interval in milliseconds.
     * Default: 20000 ms (20 seconds)
     */
    private long successCheckIntervalMs = 20000;
    
    /**
     * FINALIZED status check interval in milliseconds.
     * Default: 3600000 ms (1 hour)
     */
    private long finalizedCheckIntervalMs = 3600000;
    
    /**
     * WebSocket notification configuration.
     */
    private WebSocketConfig websocket = new WebSocketConfig();
    
    /**
     * Webhook notification configuration.
     */
    private WebhookConfig webhook = new WebhookConfig();
    
    /**
     * Event log (audit trail) configuration.
     */
    private EventLogConfig eventlog = new EventLogConfig();
    
    @Data
    public static class WebSocketConfig {
        /**
         * Enable WebSocket notifications for status updates.
         * Default: false
         */
        private boolean enabled = false;
        
        /**
         * WebSocket endpoint path.
         * Default: /ws/tx-lifecycle
         */
        private String endpoint = "/ws/tx-lifecycle";
    }
    
    @Data
    public static class WebhookConfig {
        /**
         * Enable webhook notifications for status updates.
         * Default: false
         */
        private boolean enabled = false;
        
        /**
         * Webhook URL to send transaction status updates.
         * Example: https://your-app.com/api/webhooks/tx-lifecycle
         */
        private String url;
        
        /**
         * Optional webhook secret for authentication (Bearer token).
         * If configured, will be sent as "Authorization: Bearer {secret}" header.
         */
        private String secret;
        
        /**
         * HTTP request timeout in milliseconds.
         * Default: 5000 ms (5 seconds)
         */
        private int timeoutMs = 5000;
    }
    
    @Data
    public static class EventLogConfig {
        /**
         * Enable event log (audit trail) for transaction status updates.
         * Default: true (recommended for audit trail and debugging)
         */
        private boolean enabled = true;
        
        /**
         * Retention period for event logs in days.
         * Events older than this will be automatically deleted.
         * Default: 90 days
         * Set to 0 to disable auto-cleanup.
         */
        private int retentionDays = 90;
    }
}
