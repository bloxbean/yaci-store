package com.bloxbean.cardano.yaci.store.submit.notification.websocket;

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
 * Broadcasts ALL transaction status updates to all connected clients.
 * No subscription needed - just connect and receive all events.
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
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket connection established: sessionId={}, total_sessions={}", 
                session.getId(), sessions.size());
        
        // Send welcome message
        Map<String, Object> welcome = Map.of(
                "type", "connected",
                "message", "Connected to Transaction Lifecycle WebSocket. You will receive all transaction status updates.",
                "sessionId", session.getId(),
                "connectedClients", sessions.size()
        );
        sendMessage(session, welcome);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        
        log.info("WebSocket connection closed: sessionId={}, status={}, remaining_sessions={}", 
                sessionId, status, sessions.size());
    }
    
    /**
     * Broadcast a transaction status update to ALL connected sessions.
     * 
     * @param txHash the transaction hash
     * @param statusUpdate the status update payload
     */
    public void broadcastToAll(String txHash, Map<String, Object> statusUpdate) {
        if (sessions.isEmpty()) {
            log.debug("No active WebSocket sessions to broadcast to");
            return;
        }
        
        statusUpdate.put("type", "status_update");
        statusUpdate.put("txHash", txHash);
        
        int successCount = 0;
        int failureCount = 0;
        
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = entry.getValue();
            
            if (session != null && session.isOpen()) {
                try {
                    sendMessage(session, statusUpdate);
                    successCount++;
                    log.debug("Broadcasted status update to session: sessionId={}, txHash={}, status={}", 
                            sessionId, txHash, statusUpdate.get("newStatus"));
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to send status update to session: sessionId={}, txHash={}", 
                            sessionId, txHash, e);
                }
            } else {
                failureCount++;
                log.warn("Session not open, removing: sessionId={}", sessionId);
                sessions.remove(sessionId);
            }
        }
        
        log.info("Broadcasted transaction update: txHash={}, status={}, success={}, failed={}, total_sessions={}", 
                txHash, statusUpdate.get("newStatus"), successCount, failureCount, sessions.size());
    }
    
    /**
     * Send a message to a specific session.
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }
    
    /**
     * Get the number of active WebSocket connections.
     */
    public int getActiveConnectionCount() {
        return sessions.size();
    }
}

