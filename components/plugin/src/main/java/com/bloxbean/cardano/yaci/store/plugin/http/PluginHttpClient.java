package com.bloxbean.cardano.yaci.store.plugin.http;

import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PluginHttpClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpResponseWrapper get(String url, Map<String, String> headers) {
        return sendRequest("GET", url, null, headers);
    }

    public HttpResponseWrapper postJson(String url, Map<String, Object> jsonBody, Map<String, String> headers) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/json");
            if (headers != null) allHeaders.putAll(headers);
            return sendRequest("POST", url, body, allHeaders);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper putJson(String url, Map<String, Object> jsonBody, Map<String, String> headers) {
        try {
            String body = objectMapper.writeValueAsString(jsonBody);
            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/json");
            if (headers != null) allHeaders.putAll(headers);
            return sendRequest("PUT", url, body, allHeaders);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error serializing JSON: " + e.getMessage());
        }
    }

    public HttpResponseWrapper delete(String url, Map<String, String> headers) {
        return sendRequest("DELETE", url, null, headers);
    }

    public HttpResponseWrapper postForm(String url, Map<String, String> formFields, Map<String, String> headers) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : formFields.entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(URIEncoder.encode(entry.getKey()))
                        .append("=")
                        .append(URIEncoder.encode(entry.getValue()));
            }

            Map<String, String> allHeaders = new HashMap<>();
            allHeaders.put("Content-Type", "application/x-www-form-urlencoded");
            if (headers != null) allHeaders.putAll(headers);

            return sendRequest("POST", url, sb.toString(), allHeaders);
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Error encoding form: " + e.getMessage());
        }
    }

    private HttpResponseWrapper sendRequest(String method, String url, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url));

            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            if (headers != null) {
                headers.forEach(builder::header);
            }

            HttpRequest request = builder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResponseWrapper(response.statusCode(), response.body());
        } catch (Exception e) {
            return new HttpResponseWrapper(500, "Request failed: " + e.getMessage());
        }
    }
}

