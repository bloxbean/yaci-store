package com.bloxbean.cardano.yaci.store.plugin.http;

import static org.junit.jupiter.api.Assertions.*;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.util.Map;

public class PluginHttpClientTest {

    private static MockWebServer server;
    private PluginHttpClient httpClient;

    @BeforeAll
    static void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void shutdownServer() throws Exception {
        server.shutdown();
    }

    @BeforeEach
    void setup() {
        httpClient = new PluginHttpClient();
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
}

