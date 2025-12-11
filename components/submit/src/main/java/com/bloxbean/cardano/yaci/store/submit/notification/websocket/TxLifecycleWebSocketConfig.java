package com.bloxbean.cardano.yaci.store.submit.notification.websocket;

import com.bloxbean.cardano.yaci.store.submit.SubmitLifecycleProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for transaction lifecycle notifications.
 * Only enabled when store.submit.lifecycle.websocket.enabled=true
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(
        prefix = "store.submit.lifecycle.websocket",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
public class TxLifecycleWebSocketConfig implements WebSocketConfigurer {
    
    private final TxLifecycleWebSocketHandler webSocketHandler;
    private final SubmitLifecycleProperties properties;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String endpoint = properties.getWebsocket().getEndpoint();
        registry.addHandler(webSocketHandler, endpoint)
                .setAllowedOrigins("*");
    }
}

