package com.bloxbean.cardano.yaci.store.submit.notification.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time transaction lifecycle updates.
 * Clients can connect to receive status update notifications.
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
public class TxLifecycleWebSocketHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    
    // Store active sessions: sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // Store subscriptions: txHash -> Set<sessionId>
    private final Map<String, ConcurrentHashMap<String, Boolean>> subscriptions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket connection established: {}", session.getId());
        
        // Send welcome message
        Map<String, Object> welcome = Map.of(
                "type", "connected",
                "message", "Transaction lifecycle WebSocket connected",
                "sessionId", session.getId()
        );
        sendMessage(session, welcome);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        
        // Remove all subscriptions for this session
        subscriptions.values().forEach(subs -> subs.remove(sessionId));
        
        log.info("WebSocket connection closed: {}, status: {}", sessionId, status);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    message.getPayload(), 
                    new TypeReference<Map<String, Object>>() {}
            );
            String action = (String) payload.get("action");
            
            if ("subscribe".equals(action)) {
                String txHash = (String) payload.get("txHash");
                subscribe(session.getId(), txHash);
                
                Map<String, Object> response = Map.of(
                        "type", "subscribed",
                        "txHash", txHash
                );
                sendMessage(session, response);
                
            } else if ("unsubscribe".equals(action)) {
                String txHash = (String) payload.get("txHash");
                unsubscribe(session.getId(), txHash);
                
                Map<String, Object> response = Map.of(
                        "type", "unsubscribed",
                        "txHash", txHash
                );
                sendMessage(session, response);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            Map<String, Object> error = Map.of(
                    "type", "error",
                    "message", e.getMessage()
            );
            sendMessage(session, error);
        }
    }
    
    /**
     * Subscribe a session to a specific transaction.
     */
    private void subscribe(String sessionId, String txHash) {
        subscriptions.computeIfAbsent(txHash, k -> new ConcurrentHashMap<>())
                .put(sessionId, true);
        log.debug("Session {} subscribed to txHash: {}", sessionId, txHash);
    }
    
    /**
     * Unsubscribe a session from a specific transaction.
     */
    private void unsubscribe(String sessionId, String txHash) {
        ConcurrentHashMap<String, Boolean> subs = subscriptions.get(txHash);
        if (subs != null) {
            subs.remove(sessionId);
            if (subs.isEmpty()) {
                subscriptions.remove(txHash);
            }
        }
        log.debug("Session {} unsubscribed from txHash: {}", sessionId, txHash);
    }
    
    /**
     * Broadcast a status update to all subscribed sessions.
     */
    public void broadcastStatusUpdate(String txHash, Map<String, Object> statusUpdate) {
        ConcurrentHashMap<String, Boolean> subs = subscriptions.get(txHash);
        if (subs == null || subs.isEmpty()) {
            return;
        }
        
        statusUpdate.put("type", "status_update");
        statusUpdate.put("txHash", txHash);
        
        subs.keySet().forEach(sessionId -> {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    sendMessage(session, statusUpdate);
                } catch (Exception e) {
                    log.error("Error sending status update to session: {}", sessionId, e);
                }
            }
        });
    }
    
    /**
     * Send a message to a specific session.
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }
}

