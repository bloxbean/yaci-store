package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.core.ConditionTimeoutException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;

public class DevKitAdminClient {
    public static final String DEFAULT_BASE_URL = "http://localhost:10000/";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public DevKitAdminClient() {
        this(DEFAULT_BASE_URL);
    }

    public DevKitAdminClient(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.objectMapper = new ObjectMapper();
    }

    public void assertAdminAvailable() {
        try {
            getDevNetStatus();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Yaci DevKit admin API is not available at " + baseUrl, e);
        }
    }

    public void createDevNet(Map<String, String> config) {
        String wrappedPayload;
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("genesisProperties", config);
            request.put("enableYaciStore", true);
            request.put("enableOgmios", false);
            request.put("enableKupomios", false);
            request.put("enableMultiNode", false);
            request.put("multiNodeStakeRatioFactor", 5);
            wrappedPayload = objectMapper.writeValueAsString(request);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to serialize devnet config: " + config, e);
        }

        HttpResult result = request("POST", "local-cluster/api/admin/devnet/create", wrappedPayload);
        if (isRequestBodyBindingFailure(result)) {
            String legacyPayload;
            try {
                legacyPayload = objectMapper.writeValueAsString(config);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to serialize devnet config: " + config, e);
            }

            result = request("POST", "local-cluster/api/admin/devnet/create", legacyPayload);
            requireCreateSucceeded(result, legacyPayload);
            return;
        }

        requireCreateSucceeded(result, wrappedPayload);
    }

    public void resetDevNet() {
        HttpResult result = request("POST", "local-cluster/api/admin/devnet/reset", null);
        requireOk(result, "reset devnet", null);
    }

    public void topUpFund(String address, long adaAmount) {
        String payload = String.format("{\"address\":\"%s\",\"adaAmount\":%d}", address, adaAmount);
        HttpResult result = request("POST", "local-cluster/api/addresses/topup", payload);
        requireOk(result, "top up address " + address, payload);
    }

    public int getCurrentEpoch() {
        HttpResult result = request("GET", "local-cluster/api/epochs/latest", null);
        requireOk(result, "fetch current epoch", null);

        try {
            JsonNode json = objectMapper.readTree(result.body());
            JsonNode epochNode = json.get("epoch");
            if (epochNode == null || !epochNode.canConvertToInt()) {
                throw new IllegalStateException("Current epoch response does not contain an integer epoch field: " + result.body());
            }

            return epochNode.asInt();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse current epoch response: " + result.body(), e);
        }
    }

    public String getDevNetStatus() {
        HttpResult result = request("GET", "local-cluster/api/admin/devnet/status", null);
        requireOk(result, "fetch devnet status", null);
        return result.body();
    }

    public void waitForEpoch(int epoch) {
        waitForEpoch(epoch, DEFAULT_TIMEOUT);
    }

    public void waitForEpoch(int epoch, Duration timeout) {
        try {
            await()
                    .atMost(timeout)
                    .pollInterval(POLL_INTERVAL)
                    .ignoreExceptions()
                    .until(() -> getCurrentEpoch() >= epoch);
        } catch (ConditionTimeoutException e) {
            throw new AssertionError("Timed out after " + timeout.toSeconds()
                    + "s waiting for DevKit epoch >= " + epoch
                    + ". Latest epoch: " + safeCurrentEpoch(), e);
        }
    }

    private HttpResult request(String method, String path, String payload) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");

            if (payload != null) {
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }
            }

            int statusCode = connection.getResponseCode();
            String body = readBody(connection, statusCode);
            return new HttpResult(statusCode, body);
        } catch (IOException e) {
            throw new IllegalStateException("DevKit admin request failed: " + method + " " + baseUrl + path, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void requireOk(HttpResult result, String action, String payload) {
        if (result.statusCode() == HttpURLConnection.HTTP_OK) {
            return;
        }

        String message = "Failed to " + action + ". HTTP " + result.statusCode()
                + ", response: " + result.body();
        if (payload != null) {
            message += ", request: " + payload;
        }

        throw new IllegalStateException(message);
    }

    private void requireCreateSucceeded(HttpResult result, String payload) {
        requireOk(result, "create devnet", payload);
        if ("false".equalsIgnoreCase(result.body())) {
            throw new IllegalStateException("DevKit admin accepted create devnet request but returned false. Request: " + payload);
        }
    }

    private boolean isRequestBodyBindingFailure(HttpResult result) {
        return result.statusCode() == HttpURLConnection.HTTP_BAD_REQUEST
                || result.statusCode() == HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
    }

    private String readBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream inputStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }

            return response.toString();
        }
    }

    private String safeCurrentEpoch() {
        try {
            return String.valueOf(getCurrentEpoch());
        } catch (RuntimeException e) {
            return "unavailable (" + e.getMessage() + ")";
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl;
        }

        return baseUrl + "/";
    }

    private record HttpResult(int statusCode, String body) {
    }
}
