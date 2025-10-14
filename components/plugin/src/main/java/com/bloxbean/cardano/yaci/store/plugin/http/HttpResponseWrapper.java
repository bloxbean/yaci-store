package com.bloxbean.cardano.yaci.store.plugin.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HttpResponseWrapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final int status;
    private final String body;
    private final Map<String, List<String>> headers;

    public HttpResponseWrapper(int status, String body) {
        this(status, body, Collections.emptyMap());
    }

    public HttpResponseWrapper(int status, String body, Map<String, List<String>> headers) {
        this.status = status;
        this.body = body;
        this.headers = headers != null ? headers : Collections.emptyMap();
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        List<String> values = headers.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    public List<String> getHeaderValues(String name) {
        return headers.getOrDefault(name, Collections.emptyList());
    }

    // Status checking convenience methods
    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }

    public boolean isClientError() {
        return status >= 400 && status < 500;
    }

    public boolean isServerError() {
        return status >= 500 && status < 600;
    }

    public boolean isError() {
        return status >= 400;
    }

    // JSON parsing convenience methods - designed for scripting languages
    public Map<String, Object> asJsonMap() {
        if (body == null || body.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response body as JSON: " + e.getMessage(), e);
        }
    }

    // Alias for asJsonMap() - more intuitive for script developers
    public Map<String, Object> asJson() {
        return asJsonMap();
    }

    // Parse JSON array responses
    public List<Object> asJsonList() {
        if (body == null || body.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(body, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response body as JSON array: " + e.getMessage(), e);
        }
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    public boolean isJson() {
        String contentType = getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    public boolean isXml() {
        String contentType = getContentType();
        return contentType != null && (contentType.toLowerCase().contains("application/xml") ||
                                     contentType.toLowerCase().contains("text/xml"));
    }

    public boolean isHtml() {
        String contentType = getContentType();
        return contentType != null && contentType.toLowerCase().contains("text/html");
    }

    @Override
    public String toString() {
        return "HttpResponse(status=" + status + ", body=" + body + ", headers=" + headers.size() + ")";
    }
}

