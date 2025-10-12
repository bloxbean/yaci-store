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
     * WebSocket configuration.
     */
    private WebSocketConfig websocket = new WebSocketConfig();
    
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
}

