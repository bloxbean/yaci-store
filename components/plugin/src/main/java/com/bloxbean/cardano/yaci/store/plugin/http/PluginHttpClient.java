package com.bloxbean.cardano.yaci.store.plugin.http;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.function.Predicate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PluginHttpClient {
    private final HttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Duration defaultTimeout = Duration.ofSeconds(60);

    public PluginHttpClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ===== GET Methods =====

    public HttpResponseWrapper get(String url, Map<String, String> headers) {
        return sendRequest("GET", url, null, headers, defaultTimeout);
    }

    public HttpResponseWrapper get(String url) {
        return get(url, null);
    }

    public HttpResponseWrapper getWithParams(String url, Map<String, String> params) {
        return get(buildUrlWithParams(url, params));
    }

    public HttpResponseWrapper getWithParams(String url, Map<String, String> params, Map<String, String> headers) {
        return get(buildUrlWithParams(url, params), headers);
    }

    // ===== POST Methods =====

    public HttpResponseWrapper postJson(String url, Map<String, Object> jsonBody, Map<String, String> headers) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/json");
            if (headers != null) allHeaders.putAll(headers);
            return sendRequest("POST", url, body, allHeaders, defaultTimeout);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper postJson(String url, Map<String, Object> jsonBody) {
        return postJson(url, jsonBody, null);
    }

    public HttpResponseWrapper postJson(String url, Object jsonBody) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            return sendRequest("POST", url, body, headers, defaultTimeout);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper postJson(String url, Object jsonBody, Map<String, String> headers) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/json");
            if (headers != null) allHeaders.putAll(headers);
            return sendRequest("POST", url, body, allHeaders, defaultTimeout);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper post(String url, String body, Map<String, String> headers) {
        return sendRequest("POST", url, body, headers, defaultTimeout);
    }

    public HttpResponseWrapper post(String url, String body) {
        return post(url, body, null);
    }

    // ===== PUT Methods =====

    public HttpResponseWrapper putJson(String url, Map<String, Object> jsonBody, Map<String, String> headers) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/json");
            if (headers != null) allHeaders.putAll(headers);
            return sendRequest("PUT", url, body, allHeaders, defaultTimeout);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper putJson(String url, Map<String, Object> jsonBody) {
        return putJson(url, jsonBody, null);
    }

    public HttpResponseWrapper putJson(String url, Object jsonBody) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            return sendRequest("PUT", url, body, headers, defaultTimeout);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper putJson(String url, Object jsonBody, Map<String, String> headers) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/json");
            if (headers != null) allHeaders.putAll(headers);
            return sendRequest("PUT", url, body, allHeaders, defaultTimeout);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper put(String url, String body, Map<String, String> headers) {
        return sendRequest("PUT", url, body, headers, defaultTimeout);
    }

    public HttpResponseWrapper put(String url, String body) {
        return put(url, body, null);
    }

    // ===== DELETE Methods =====

    public HttpResponseWrapper delete(String url, Map<String, String> headers) {
        return sendRequest("DELETE", url, null, headers, defaultTimeout);
    }

    public HttpResponseWrapper delete(String url) {
        return delete(url, null);
    }

    // ===== FORM Methods =====

    public HttpResponseWrapper postForm(String url, Map<String, String> formFields, Map<String, String> headers) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : formFields.entrySet()) {
                if (!sb.isEmpty()) sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/x-www-form-urlencoded");
            if (headers != null) allHeaders.putAll(headers);

            return sendRequest("POST", url, sb.toString(), allHeaders, defaultTimeout);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error encoding form: " + e.getMessage());
        }
    }

    public HttpResponseWrapper postForm(String url, Map<String, String> formFields) {
        return postForm(url, formFields, null);
    }

    // ===== Authentication Convenience Methods =====

    public HttpResponseWrapper getWithBasicAuth(String url, String username, String password) {
        return getWithBasicAuth(url, username, password, null);
    }

    public HttpResponseWrapper getWithBasicAuth(String url, String username, String password, Map<String, String> headers) {
        Map<String, String> authHeaders = addBasicAuth(headers, username, password);
        return get(url, authHeaders);
    }

    public HttpResponseWrapper getWithBearerToken(String url, String token) {
        return getWithBearerToken(url, token, null);
    }

    public HttpResponseWrapper getWithBearerToken(String url, String token, Map<String, String> headers) {
        Map<String, String> authHeaders = addBearerToken(headers, token);
        return get(url, authHeaders);
    }

    public HttpResponseWrapper getWithApiKey(String url, String apiKey) {
        return getWithApiKey(url, "X-API-Key", apiKey, null);
    }

    public HttpResponseWrapper getWithApiKey(String url, String headerName, String apiKey) {
        return getWithApiKey(url, headerName, apiKey, null);
    }

    public HttpResponseWrapper getWithApiKey(String url, String headerName, String apiKey, Map<String, String> headers) {
        Map<String, String> authHeaders = addApiKey(headers, headerName, apiKey);
        return get(url, authHeaders);
    }

    public HttpResponseWrapper postJsonWithBasicAuth(String url, Object jsonBody, String username, String password) {
        Map<String, String> authHeaders = addBasicAuth(null, username, password);
        return postJson(url, jsonBody, authHeaders);
    }

    public HttpResponseWrapper postJsonWithBearerToken(String url, Object jsonBody, String token) {
        Map<String, String> authHeaders = addBearerToken(null, token);
        return postJson(url, jsonBody, authHeaders);
    }

    public HttpResponseWrapper postJsonWithApiKey(String url, Object jsonBody, String headerName, String apiKey) {
        Map<String, String> authHeaders = addApiKey(null, headerName, apiKey);
        return postJson(url, jsonBody, authHeaders);
    }

    // ===== Timeout Methods (Script-Friendly) =====

    // Timeout in milliseconds
    public HttpResponseWrapper getWithTimeout(String url, Map<String, String> headers, long timeoutMillis) {
        return sendRequest("GET", url, null, headers, Duration.ofMillis(timeoutMillis));
    }

    public HttpResponseWrapper getWithTimeout(String url, long timeoutMillis) {
        return getWithTimeout(url, null, timeoutMillis);
    }

    // Timeout in seconds (convenience methods)
    public HttpResponseWrapper getWithTimeoutSeconds(String url, Map<String, String> headers, int timeoutSeconds) {
        return sendRequest("GET", url, null, headers, Duration.ofSeconds(timeoutSeconds));
    }

    public HttpResponseWrapper getWithTimeoutSeconds(String url, int timeoutSeconds) {
        return getWithTimeoutSeconds(url, null, timeoutSeconds);
    }

    public HttpResponseWrapper postJsonWithTimeout(String url, Object jsonBody, Map<String, String> headers, long timeoutMillis) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/json");
            if (headers != null) allHeaders.putAll(headers);
            return sendRequest("POST", url, body, allHeaders, Duration.ofMillis(timeoutMillis));
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper postJsonWithTimeout(String url, Object jsonBody, long timeoutMillis) {
        return postJsonWithTimeout(url, jsonBody, null, timeoutMillis);
    }

    public HttpResponseWrapper postJsonWithTimeoutSeconds(String url, Object jsonBody, Map<String, String> headers, int timeoutSeconds) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/json");
            if (headers != null) allHeaders.putAll(headers);
            return sendRequest("POST", url, body, allHeaders, Duration.ofSeconds(timeoutSeconds));
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper postJsonWithTimeoutSeconds(String url, Object jsonBody, int timeoutSeconds) {
        return postJsonWithTimeoutSeconds(url, jsonBody, null, timeoutSeconds);
    }

    // ===== Retry Methods (Generic Wrapper for Script Languages) =====

    /**
     * Generic retry wrapper that can retry any HTTP operation.
     * Suitable for MVEL, JavaScript, and Python using closures/functions.
     * 
     * @param operation The HTTP operation to retry (as a Supplier)
     * @param maxRetries Maximum number of retry attempts (0 = no retries)
     * @param delaySeconds Initial delay between retries in seconds
     * @return The HTTP response from the operation
     * 
     * Usage examples:
     * - MVEL: http.retry(() -> http.get("https://api.example.com/data"), 3, 1)
     * - JavaScript: http.retry(() => http.get("https://api.example.com/data"), 3, 1)
     * - Python: http.retry(lambda: http.get("https://api.example.com/data"), 3, 1)
     */
    public HttpResponseWrapper retry(Supplier<HttpResponseWrapper> operation, int maxRetries, int delaySeconds) {
        return retryWithCondition(operation, maxRetries, delaySeconds, this::defaultShouldRetry);
    }

    /**
     * Generic retry wrapper with custom retry condition.
     * 
     * @param operation The HTTP operation to retry
     * @param maxRetries Maximum number of retry attempts
     * @param delaySeconds Initial delay between retries in seconds
     * @param shouldRetry Predicate to determine if response should be retried
     * @return The HTTP response from the operation
     * 
     * Usage examples:
     * - MVEL: http.retryWithCondition(() -> http.get(url), 3, 1, (resp) -> resp.isServerError())
     * - JavaScript: http.retryWithCondition(() => http.get(url), 3, 1, (resp) => resp.isServerError())
     * - Python: http.retryWithCondition(lambda: http.get(url), 3, 1, lambda resp: resp.isServerError())
     */
    public HttpResponseWrapper retryWithCondition(Supplier<HttpResponseWrapper> operation, int maxRetries, int delaySeconds, Predicate<HttpResponseWrapper> shouldRetry) {
        HttpResponseWrapper response = null;
        int attempt = 0;
        
        while (attempt <= maxRetries) {
            try {
                response = operation.get();
                
                // If this is the last attempt or the response shouldn't be retried, return it
                if (attempt == maxRetries || !shouldRetry.test(response)) {
                    return response;
                }
                
            } catch (Exception e) {
                // If this is the last attempt, return an error response
                if (attempt == maxRetries) {
                    return new HttpResponseWrapper(500, "Request failed after " + (attempt + 1) + " attempts: " + e.getMessage());
                }
                // Otherwise, we'll retry
            }
            
            attempt++;
            
            if (attempt <= maxRetries) {
                try {
                    // Exponential backoff: delay * (2 ^ (attempt - 1))
                    long delayMs = delaySeconds * 1000L * (1L << (attempt - 1));
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new HttpResponseWrapper(500, "Request retry interrupted");
                }
            }
        }
        
        return response != null ? response : new HttpResponseWrapper(500, "Request failed after " + maxRetries + " retries");
    }

    // ===== Retry Convenience Methods =====

    /**
     * Retry operation only on server errors (5xx status codes).
     * Useful for temporary server issues.
     */
    public HttpResponseWrapper retryOnServerError(Supplier<HttpResponseWrapper> operation, int maxRetries, int delaySeconds) {
        return retryWithCondition(operation, maxRetries, delaySeconds, HttpResponseWrapper::isServerError);
    }

    /**
     * Retry operation on any error (4xx or 5xx status codes).
     * Use with caution - may retry on client errors that won't succeed.
     */
    public HttpResponseWrapper retryOnAnyError(Supplier<HttpResponseWrapper> operation, int maxRetries, int delaySeconds) {
        return retryWithCondition(operation, maxRetries, delaySeconds, HttpResponseWrapper::isError);
    }

    /**
     * Create a retry condition for specific status codes.
     * Returns a predicate that retries if the response status matches any of the provided codes.
     */
    public Predicate<HttpResponseWrapper> retryOnStatusCodes(int... statusCodes) {
        return response -> {
            int status = response.getStatus();
            for (int code : statusCodes) {
                if (status == code) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Default retry condition: retry on server errors (5xx) and request timeouts (408).
     * This provides sensible defaults for most use cases.
     */
    private boolean defaultShouldRetry(HttpResponseWrapper response) {
        int status = response.getStatus();
        return status >= 500 || status == 408; // Server errors and request timeout
    }

    // ===== Helper Methods =====

    private String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            joiner.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) +
                      "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        String paramString = joiner.toString();
        return url.contains("?") ? url + "&" + paramString : url + "?" + paramString;
    }

    private Map<String, String> addBasicAuth(Map<String, String> headers, String username, String password) {
        Map<String, String> authHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        authHeaders.put("Authorization", "Basic " + encodedCredentials);
        return authHeaders;
    }

    private Map<String, String> addBearerToken(Map<String, String> headers, String token) {
        Map<String, String> authHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
        authHeaders.put("Authorization", "Bearer " + token);
        return authHeaders;
    }

    private Map<String, String> addApiKey(Map<String, String> headers, String headerName, String apiKey) {
        Map<String, String> authHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
        authHeaders.put(headerName, apiKey);
        return authHeaders;
    }

    private HttpResponseWrapper sendRequest(String method, String url, String body, Map<String, String> headers, Duration timeout) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout);

            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            if (headers != null) {
                headers.forEach(builder::header);
            }

            HttpRequest request = builder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResponseWrapper(response.statusCode(), response.body(), response.headers().map());
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Request failed: " + e.getMessage());
        }
    }

    // Backward compatibility method
    private HttpResponseWrapper sendRequest(String method, String url, String body, Map<String, String> headers) {
        return sendRequest(method, url, body, headers, defaultTimeout);
    }
}

