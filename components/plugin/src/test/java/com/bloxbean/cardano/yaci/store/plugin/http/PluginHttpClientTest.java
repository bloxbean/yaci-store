package com.bloxbean.cardano.yaci.store.plugin.http;

import static org.junit.jupiter.api.Assertions.*;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginHttpClientTest {

    private static MockWebServer server;
    private PluginHttpClient httpClient;

    @BeforeEach
    void setup() throws Exception {
        server = new MockWebServer();
        server.start();
        httpClient = new PluginHttpClient();
    }

    @AfterEach
    void teardown() throws Exception {
        server.shutdown();
    }

    @Test
    void testGet() {
        server.enqueue(new MockResponse()
                .setBody("Hello World")
                .setResponseCode(200));

        String url = server.url("/hello").toString();
        HttpResponseWrapper response = httpClient.get(url, null);

        assertEquals(200, response.getStatus());
        assertEquals("Hello World", response.getBody());
    }

    @Test
    void testPostJson() {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"result\": \"ok\"}"));

        String url = server.url("/submit").toString();
        HttpResponseWrapper response = httpClient.postJson(url, Map.of("key", "value"), null);

        assertEquals(201, response.getStatus());
        assertTrue(response.getBody().contains("ok"));
    }

    @Test
    void testPutJsonWithHeaders() {
        server.enqueue(new MockResponse().setResponseCode(204));

        String url = server.url("/update").toString();
        HttpResponseWrapper response = httpClient.putJson(
                url,
                Map.of("id", "123"),
                Map.of("Authorization", "Bearer test-token")
        );

        assertEquals(204, response.getStatus());
    }

    @Test
    void testDelete() {
        server.enqueue(new MockResponse().setResponseCode(204));

        String url = server.url("/remove").toString();
        HttpResponseWrapper response = httpClient.delete(url, Map.of("X-Test", "true"));

        assertEquals(204, response.getStatus());
    }

    @Test
    void testPostForm() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("form received"));

        String url = server.url("/form").toString();
        HttpResponseWrapper response = httpClient.postForm(
                url,
                Map.of("name", "Satya", "age", "30"),
                Map.of("X-Custom", "test")
        );

        assertEquals(200, response.getStatus());
        assertEquals("form received", response.getBody());
    }

    // ===== Enhanced Feature Tests =====

    @Test
    void testSimpleGet() {
        server.enqueue(new MockResponse()
                .setBody("Simple response")
                .setResponseCode(200));

        String url = server.url("/simple").toString();
        HttpResponseWrapper response = httpClient.get(url);

        assertEquals(200, response.getStatus());
        assertEquals("Simple response", response.getBody());
        assertTrue(response.isSuccess());
        assertFalse(response.isError());
    }

    @Test
    void testGetWithParams() {
        server.enqueue(new MockResponse()
                .setBody("Param response")
                .setResponseCode(200));

        String baseUrl = server.url("/search").toString();
        Map<String, String> params = new HashMap<>();
        params.put("query", "cardano");
        params.put("limit", "10");

        HttpResponseWrapper response = httpClient.getWithParams(baseUrl, params);

        assertEquals(200, response.getStatus());
        assertEquals("Param response", response.getBody());
    }

    @Test
    void testPostJsonSimple() {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"id\": 123, \"status\": \"created\"}"));

        String url = server.url("/create").toString();
        Map<String, Object> data = Map.of("name", "Test", "value", 42);

        HttpResponseWrapper response = httpClient.postJson(url, data);

        assertEquals(201, response.getStatus());
        assertTrue(response.isSuccess());
    }

    @Test
    void testJsonResponseParsing() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"message\": \"hello\", \"count\": 5}")
                .addHeader("Content-Type", "application/json"));

        String url = server.url("/json").toString();
        HttpResponseWrapper response = httpClient.get(url);

        assertTrue(response.isJson());

        // Test both asJsonMap() and asJson() methods
        Map<String, Object> data = response.asJsonMap();
        assertEquals("hello", data.get("message"));
        assertEquals(5, ((Number) data.get("count")).intValue());

        // Test the alias method
        Map<String, Object> data2 = response.asJson();
        assertEquals("hello", data2.get("message"));
        assertEquals(5, ((Number) data2.get("count")).intValue());
    }

    @Test
    void testStatusChecking() {
        // Test client error
        server.enqueue(new MockResponse().setResponseCode(404));
        String url = server.url("/notfound").toString();
        HttpResponseWrapper response = httpClient.get(url);

        assertTrue(response.isClientError());
        assertTrue(response.isError());
        assertFalse(response.isSuccess());
        assertFalse(response.isServerError());

        // Test server error
        server.enqueue(new MockResponse().setResponseCode(500));
        response = httpClient.get(url);

        assertTrue(response.isServerError());
        assertTrue(response.isError());
        assertFalse(response.isSuccess());
        assertFalse(response.isClientError());
    }

    @Test
    void testResponseHeaders() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("test")
                .addHeader("Content-Type", "text/plain")
                .addHeader("X-Custom-Header", "custom-value"));

        String url = server.url("/headers").toString();
        HttpResponseWrapper response = httpClient.get(url);

        assertEquals("text/plain", response.getContentType());
        assertEquals("custom-value", response.getHeader("X-Custom-Header"));
        assertFalse(response.isJson());
        assertFalse(response.isXml());
        assertFalse(response.isHtml());
    }

    @Test
    void testBasicAuthentication() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("authenticated"));

        String url = server.url("/protected").toString();
        HttpResponseWrapper response = httpClient.getWithBasicAuth(url, "user", "pass");

        assertEquals(200, response.getStatus());

        RecordedRequest request = server.takeRequest();
        String authHeader = request.getHeader("Authorization");
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Basic "));
    }

    @Test
    void testBearerTokenAuthentication() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("token authenticated"));

        String url = server.url("/api").toString();
        HttpResponseWrapper response = httpClient.getWithBearerToken(url, "test-token");

        assertEquals(200, response.getStatus());

        RecordedRequest request = server.takeRequest();
        String authHeader = request.getHeader("Authorization");
        assertEquals("Bearer test-token", authHeader);
    }

    @Test
    void testApiKeyAuthentication() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("api key authenticated"));

        String url = server.url("/api").toString();
        HttpResponseWrapper response = httpClient.getWithApiKey(url, "X-API-Key", "secret-key");

        assertEquals(200, response.getStatus());

        RecordedRequest request = server.takeRequest();
        String apiKeyHeader = request.getHeader("X-API-Key");
        assertEquals("secret-key", apiKeyHeader);
    }

    @Test
    void testPutJsonMethods() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"updated\": true}"));

        String url = server.url("/update").toString();
        Map<String, Object> data = Map.of("field", "new-value");

        HttpResponseWrapper response = httpClient.putJson(url, data);

        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());
    }

    @Test
    void testDeleteMethods() {
        server.enqueue(new MockResponse().setResponseCode(204));

        String url = server.url("/delete").toString();
        HttpResponseWrapper response = httpClient.delete(url);

        assertEquals(204, response.getStatus());
        assertTrue(response.isSuccess());
    }

    @Test
    void testPostFormSimple() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("form processed"));

        String url = server.url("/simple-form").toString();
        Map<String, String> formData = Map.of("field1", "value1", "field2", "value2");

        HttpResponseWrapper response = httpClient.postForm(url, formData);

        assertEquals(200, response.getStatus());
        assertEquals("form processed", response.getBody());
    }

    @Test
    void testAuthenticatedPostJson() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"created\": true}"));

        String url = server.url("/secure-create").toString();
        Map<String, Object> data = Map.of("name", "secure-item");

        HttpResponseWrapper response = httpClient.postJsonWithBearerToken(url, data, "secure-token");

        assertEquals(201, response.getStatus());

        RecordedRequest request = server.takeRequest();
        assertEquals("Bearer secure-token", request.getHeader("Authorization"));
        assertEquals("application/json", request.getHeader("Content-Type"));
    }

    @Test
    void testCustomTimeout() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("timeout test"));

        String url = server.url("/timeout").toString();

        // Test milliseconds timeout
        HttpResponseWrapper response = httpClient.getWithTimeout(url, 5000L);
        assertEquals(200, response.getStatus());
        assertEquals("timeout test", response.getBody());
    }

    @Test
    void testCustomTimeoutSeconds() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("timeout seconds test"));

        String url = server.url("/timeout-seconds").toString();

        // Test seconds timeout
        HttpResponseWrapper response = httpClient.getWithTimeoutSeconds(url, 5);
        assertEquals(200, response.getStatus());
        assertEquals("timeout seconds test", response.getBody());
    }

    @Test
    void testPostJsonWithTimeout() {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"created\": true}"));

        String url = server.url("/create-timeout").toString();
        Map<String, Object> data = Map.of("name", "test");

        // Test POST with milliseconds timeout
        HttpResponseWrapper response = httpClient.postJsonWithTimeout(url, data, 3000L);
        assertEquals(201, response.getStatus());
        assertTrue(response.isSuccess());
    }

    @Test
    void testPostJsonWithTimeoutSeconds() {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"created\": true}"));

        String url = server.url("/create-timeout-sec").toString();
        Map<String, Object> data = Map.of("name", "test");

        // Test POST with seconds timeout
        HttpResponseWrapper response = httpClient.postJsonWithTimeoutSeconds(url, data, 3);
        assertEquals(201, response.getStatus());
        assertTrue(response.isSuccess());
    }

    @Test
    void testPostWithObject() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"processed\": true}"));

        String url = server.url("/object").toString();
        TestObject obj = new TestObject("test-name", 42);

        HttpResponseWrapper response = httpClient.postJson(url, obj);

        assertEquals(200, response.getStatus());
        assertTrue(response.isSuccess());
    }

    @Test
    void testJsonListParsing() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[{\"id\": 1}, {\"id\": 2}, {\"id\": 3}]")
                .addHeader("Content-Type", "application/json"));

        String url = server.url("/list").toString();
        HttpResponseWrapper response = httpClient.get(url);

        assertTrue(response.isJson());
        List<Object> data = response.asJsonList();
        assertEquals(3, data.size());
    }

    @Test
    void testErrorJsonParsing() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("invalid json {"));

        String url = server.url("/invalid-json").toString();
        HttpResponseWrapper response = httpClient.get(url);

        assertEquals(200, response.getStatus());

        assertThrows(RuntimeException.class, () -> {
            response.asJsonMap();
        });
    }

    @Test
    void testEmptyJsonResponse() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(""));

        String url = server.url("/empty").toString();
        HttpResponseWrapper response = httpClient.get(url);

        assertEquals(200, response.getStatus());
        Map<String, Object> data = response.asJsonMap();
        assertTrue(data.isEmpty());
    }

    // ===== Retry Tests =====

    @Test
    void testRetrySuccess() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("success"));

        String url = server.url("/retry-success").toString();

        // Should succeed on first attempt
        HttpResponseWrapper response = httpClient.retry(() -> httpClient.get(url), 3, 1);

        assertEquals(200, response.getStatus());
        assertEquals("success", response.getBody());
    }

    @Test
    void testRetryOnServerError() {
        // First two requests fail with 500, third succeeds
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("success after retry"));

        String url = server.url("/retry-server-error").toString();

        long startTime = System.currentTimeMillis();
        HttpResponseWrapper response = httpClient.retry(() -> httpClient.get(url), 3, 1);
        long endTime = System.currentTimeMillis();

        assertEquals(200, response.getStatus());
        assertEquals("success after retry", response.getBody());

        // Should take at least 3 seconds (1s + 2s delays between retries)
        assertTrue(endTime - startTime >= 3000, "Should have delayed for retries");
    }

    @Test
    void testRetryExhaustsAttempts() {
        // All requests fail with 500
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));

        String url = server.url("/retry-exhaust").toString();

        HttpResponseWrapper response = httpClient.retry(() -> httpClient.get(url), 3, 1);

        assertEquals(500, response.getStatus()); // Should return the last failed response
    }

    @Test
    void testRetryWithCondition() {
        // First request fails with 429 (rate limit), second succeeds
        server.enqueue(new MockResponse().setResponseCode(429));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("success"));

        String url = server.url("/retry-condition").toString();

        // Retry only on 429 status
        HttpResponseWrapper response = httpClient.retryWithCondition(
                () -> httpClient.get(url),
                3, 1,
                resp -> resp.getStatus() == 429
        );

        assertEquals(200, response.getStatus());
        assertEquals("success", response.getBody());
    }

    @Test
    void testRetryOnServerErrorConvenience() {
        // First request fails with 500, second succeeds
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("server error retry success"));

        String url = server.url("/retry-server-conv").toString();

        HttpResponseWrapper response = httpClient.retryOnServerError(() -> httpClient.get(url), 3, 1);

        assertEquals(200, response.getStatus());
        assertEquals("server error retry success", response.getBody());
    }

    @Test
    void testRetryOnAnyErrorConvenience() {
        // First request fails with 404, second succeeds
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("any error retry success"));

        String url = server.url("/retry-any-error").toString();

        HttpResponseWrapper response = httpClient.retryOnAnyError(() -> httpClient.get(url), 3, 1);

        assertEquals(200, response.getStatus());
        assertEquals("any error retry success", response.getBody());
    }

    @Test
    void testRetryOnStatusCodes() {
        // First request fails with 503, second succeeds
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("specific status retry success"));

        String url = server.url("/retry-status").toString();

        HttpResponseWrapper response = httpClient.retryWithCondition(
                () -> httpClient.get(url),
                3, 1,
                httpClient.retryOnStatusCodes(503, 429)
        );

        assertEquals(200, response.getStatus());
        assertEquals("specific status retry success", response.getBody());
    }

    @Test
    void testRetryDoesNotRetryClientError() {
        // Request fails with 404 (client error) - should not retry by default
        server.enqueue(new MockResponse().setResponseCode(404));

        String url = server.url("/retry-no-client-error").toString();

        HttpResponseWrapper response = httpClient.retry(() -> httpClient.get(url), 3, 1);

        assertEquals(404, response.getStatus());
        // Should have only made one request (no retries for 4xx by default)
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void testRetryWithPostJson() {
        // First request fails with 500, second succeeds
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"created\": true}"));

        String url = server.url("/retry-post").toString();
        Map<String, Object> data = Map.of("name", "test", "value", 123);

        HttpResponseWrapper response = httpClient.retry(
                () -> httpClient.postJson(url, data),
                3, 1
        );

        assertEquals(201, response.getStatus());
        assertTrue(response.getBody().contains("created"));
    }

    @Test
    void testRetryExponentialBackoff() {
        // All requests fail to test backoff timing
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));

        String url = server.url("/retry-backoff").toString();

        long startTime = System.currentTimeMillis();
        HttpResponseWrapper response = httpClient.retry(() -> httpClient.get(url), 2, 1);
        long endTime = System.currentTimeMillis();

        assertEquals(500, response.getStatus());

        // Should take at least 3 seconds (1s + 2s exponential delays)
        // Being generous with timing in tests to avoid flakiness
        assertTrue(endTime - startTime >= 2900,
                "Should have exponential backoff delays, took: " + (endTime - startTime) + "ms");
    }

    @Test
    void testRetryWithException() {
        String invalidUrl = "http://invalid-host-that-does-not-exist:9999/test";

        HttpResponseWrapper response = httpClient.retry(() -> httpClient.get(invalidUrl), 2, 1);

        assertEquals(500, response.getStatus());
        assertTrue(response.getBody().contains("Request failed"));
    }

    // Helper class for testing object serialization
    private static class TestObject {
        private String name;
        private int value;

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}

