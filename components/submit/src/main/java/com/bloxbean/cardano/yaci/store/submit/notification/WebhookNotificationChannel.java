package com.bloxbean.cardano.yaci.store.submit.notification;

import com.bloxbean.cardano.yaci.store.submit.SubmitLifecycleProperties;
import com.bloxbean.cardano.yaci.store.submit.event.TxStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Webhook notification channel implementation.
 * Sends transaction status updates to configured webhook URL via HTTP POST.
 */
@Component
@ConditionalOnProperty(
        prefix = "store.submit.lifecycle.webhook",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class WebhookNotificationChannel implements TxNotificationChannel {
    
    private final RestTemplate restTemplate;
    private final SubmitLifecycleProperties properties;
    
    @Override
    @Async("txNotificationExecutor")
    public void notify(TxStatusUpdateEvent event) {
        String webhookUrl = properties.getWebhook().getUrl();
        
        log.debug("Sending webhook notification: txHash={}, status={}, url={}", 
                event.getTxHash(), event.getNewStatus(), webhookUrl);
        
        try {
            Map<String, Object> payload = buildPayload(event);
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.exchange(webhookUrl, HttpMethod.POST, request, String.class);
            
            log.info("Webhook notification sent successfully: txHash={}, url={}", 
                    event.getTxHash(), webhookUrl);
            
        } catch (Exception e) {
            log.error("Failed to send webhook notification: txHash={}, url={}, error={}", 
                    event.getTxHash(), webhookUrl, e.getMessage());
            // Don't rethrow - notification failure should not break the flow
        }
    }
    
    @Override
    public String getChannelName() {
        return "Webhook";
    }
    
    /**
     * Build webhook payload from event.
     * Follows standard webhook format with event type and data.
     */
    private Map<String, Object> buildPayload(TxStatusUpdateEvent event) {
        Map<String, Object> payload = new HashMap<>();
        
        // Webhook metadata
        payload.put("event", "tx.status.update");
        payload.put("timestamp", event.getTimestamp());
        
        // Transaction data
        Map<String, Object> data = new HashMap<>();
        data.put("txHash", event.getTxHash());
        data.put("previousStatus", event.getPreviousStatus());
        data.put("newStatus", event.getNewStatus());
        data.put("message", event.getMessage());
        
        if (event.getTransaction() != null) {
            data.put("submittedAt", event.getTransaction().getSubmittedAt());
            data.put("confirmedAt", event.getTransaction().getConfirmedAt());
            data.put("confirmedSlot", event.getTransaction().getConfirmedSlot());
            data.put("confirmedBlockNumber", event.getTransaction().getConfirmedBlockNumber());
            data.put("successAt", event.getTransaction().getSuccessAt());
            data.put("finalizedAt", event.getTransaction().getFinalizedAt());
            data.put("errorMessage", event.getTransaction().getErrorMessage());
        }
        
        payload.put("data", data);
        
        return payload;
    }
    
    /**
     * Build HTTP headers for webhook request.
     * Includes authentication if webhook secret is configured.
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Yaci-Store-TxLifecycle/1.0");
        
        // Add webhook secret as bearer token if configured
        String webhookSecret = properties.getWebhook().getSecret();
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            headers.set("Authorization", "Bearer " + webhookSecret);
        }
        
        return headers;
    }
}

