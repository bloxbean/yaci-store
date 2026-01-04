package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobRepository;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

public class BaseE2ETest {
    public static final String DEVKIT_ADMIN_BASE_URL = "http://localhost:10000/";
    public static final String SUBMIT_BASE_URL = "http://localhost:9999";
    public static final String DEFAULT_MNEMONICS = "test test test test test test test test test test test test test test test test test test test test test test test sauce";

    public static final BackendService backendService = new BFBackendService("http://localhost:8080/api/v1/", "Dummy");

    protected Account account0 = new Account(Networks.testnet(), DEFAULT_MNEMONICS);
    protected Account account1 = new Account(Networks.testnet(), DEFAULT_MNEMONICS, DerivationPath.createExternalAddressDerivationPathForAccount(1));
    protected Account account2 = new Account(Networks.testnet(), DEFAULT_MNEMONICS, DerivationPath.createExternalAddressDerivationPathForAccount(2));

    // Static flag to ensure devnet is reset only once per test suite run
    private static volatile boolean devnetInitialized = false;
    private static final Object LOCK = new Object();

    public UtxoSupplier getUTXOSupplier() {
        return new DefaultUtxoSupplier(backendService.getUtxoService());
    }

    protected static void topUpFund(String address, long adaAmount) {
        try {
            // URL to the top-up API
            String url = DEVKIT_ADMIN_BASE_URL + "local-cluster/api/addresses/topup";
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

            // Set request method to POST
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Create JSON payload
            String jsonInputString = String.format("{\"address\": \"%s\", \"adaAmount\": %d}", address, adaAmount);

            // Send the request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Check the response code
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Funds topped up successfully.");
            } else {
                System.out.println("Failed to top up funds. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void resetDevNet() {
        try {
            // URL to reset the network
            String url = DEVKIT_ADMIN_BASE_URL + "local-cluster/api/admin/devnet/reset";
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

            // Set request method to POST
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Check the response code
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Devnet reset successful");
            } else {
                System.out.println("Failed to reset the network. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize devnet once for the entire test suite.
     * This method is thread-safe and ensures devnet is reset only once.
     */
    protected static void initializeDevnetOnce() {
        synchronized (LOCK) {
            if (!devnetInitialized) {
                System.out.println("=== Initializing Devnet for Test Suite ===");
                resetDevNet();
                waitForDevnetSync();
                devnetInitialized = true;
                System.out.println("=== Devnet Initialization Complete ===");
            }
        }
    }

    /**
     * Wait for yaci-store to sync with the devnet after reset.
     * Uses Awaitility to poll until the blockchain tip is reachable.
     */
    protected static void waitForDevnetSync() {
        System.out.println("Waiting for devnet to sync...");
        try {
            // Initial wait for nodes to start
            Thread.sleep(10000);

            // Then poll until yaci-store can reach the blockchain
            Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(3))
                .until(BaseE2ETest::isDevnetReachable);

            System.out.println("Devnet sync complete.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for devnet sync", e);
        }
    }

    /**
     * Check if the devnet is reachable and yaci-store can query it.
     */
    private static boolean isDevnetReachable() {
        try {
            String url = DEVKIT_ADMIN_BASE_URL + "local-cluster/api/epochs/latest";
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        }
    }

    protected static void createDevNet(Map<String, String> config) {
        try {
            // URL to create the network with the provided configuration
            String url = DEVKIT_ADMIN_BASE_URL + "local-cluster/api/admin/devnet/create";
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

            // Set request method to POST
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Convert the config Map to JSON string using Gson
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(config);

            // Send the JSON payload in the POST request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Check the response code
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Create devnet is successful");
            } else {
                System.out.println("Failed to create the network. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected int getCurrentEpoch() {
        String url = DEVKIT_ADMIN_BASE_URL + "local-cluster/api/epochs/latest";
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response;
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line.trim());
                    }
                }

                var jsonObject = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.toString());
                return jsonObject.get("epoch").asInt();
            } else {
                System.out.println("Failed to fetch current epoch. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;

    }

    protected void waitForTransaction(Result<String> result) {
        try {
            if (result.isSuccessful()) { //Wait for transaction to be mined
                int count = 0;
                while (count < 60) {
                    Result<TransactionContent> txnResult = backendService.getTransactionService().getTransaction(result.getValue());
                    if (txnResult.isSuccessful()) {
                        System.out.println(JsonUtil.getPrettyJson(txnResult.getValue()));
                        break;
                    } else {
                        System.out.println("Waiting for transaction to be mined ....");
                    }

                    count++;
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void checkIfUtxoAvailable(String txHash, String address) {
        Optional<Utxo> utxo = Optional.empty();
        int count = 0;
        while (utxo.isEmpty()) {
            if (count++ >= 20)
                break;
            List<Utxo> utxos = new DefaultUtxoSupplier(backendService.getUtxoService()).getAll(address);
            utxo = utxos.stream().filter(u -> u.getTxHash().equals(txHash))
                    .findFirst();
            System.out.println("Try to get new output... txhash: " + txHash);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }
    }

    protected void waitTillAdaPotJobDone(AdaPotJobRepository adaPotJobRepository, long epoch) {
        await().atMost(Duration.ofSeconds(100))
                .until(() -> adaPotJobRepository.findById(epoch).filter(adaPotJobEntity -> adaPotJobEntity.getStatus() == AdaPotJobStatus.COMPLETED).isPresent());
    }

    // ========== Transaction Building and Submission Methods ==========

    /**
     * Build transaction from YAML without submitting to blockchain
     * @param yaml Transaction plan YAML
     * @return TxPlanResponse with txBodyCbor (txHash will be null)
     */
    protected TxPlanResponse buildTxFromYaml(String yaml) {
        io.restassured.response.Response response = RestAssured.given()
                .baseUri(SUBMIT_BASE_URL)
                .contentType(ContentType.TEXT)
                .body(yaml)
                .post("/api/v1/tx/lifecycle/build");

        if (response.statusCode() != 200) {
            System.out.println("=== HTTP " + response.statusCode() + " ERROR ===");
            System.out.println(response.getBody().asString());
            System.out.println("========================");
        }

        TxPlanResponse txPlan = response.then()
                .statusCode(200)
                .extract()
                .as(TxPlanResponse.class);

        return txPlan;
    }

    /**
     * Build transaction from YAML, submit to blockchain, and wait for confirmation.
     * Uses the /build-and-submit endpoint that does both operations in a single API call.
     * @param yaml Transaction plan YAML
     * @return TxPlanResponse with txBodyCbor and txHash
     */
    protected TxPlanResponse buildAndSubmitTxFromYaml(String yaml) {
        io.restassured.response.Response response = RestAssured.given()
                .baseUri(SUBMIT_BASE_URL)
                .contentType(ContentType.TEXT)
                .body(yaml)
                .post("/api/v1/tx/lifecycle/build-and-submit");

        if (response.statusCode() != 202 && response.statusCode() != 400) {
            System.out.println("=== HTTP " + response.statusCode() + " ERROR ===");
            System.out.println(response.getBody().asString());
            System.out.println("========================");
        }

        SubmittedTxResponse submittedTx = response.then()
                .statusCode(anyOf(is(202), is(400)))
                .extract()
                .as(SubmittedTxResponse.class);

        System.out.println("Parsed response - txHash: " + submittedTx.txHash + ", status: " + submittedTx.status);
        if (submittedTx.errorMessage != null) {
            System.out.println("Error message: " + submittedTx.errorMessage);
        }

        if (TxStatus.FAILED == submittedTx.status) {
            System.out.println("Transaction submission failed: " + submittedTx.txHash);
            System.out.println("  Error: " + submittedTx.errorMessage);
            throw new RuntimeException("Transaction submission failed - " + (submittedTx.errorMessage != null ? submittedTx.errorMessage : "UTXO already spent or invalid"));
        }

        System.out.println("Transaction submitted successfully: " + submittedTx.txHash);

        TxPlanResponse txPlan = new TxPlanResponse();
        txPlan.txBodyCbor = submittedTx.txBodyCbor;
        txPlan.txHash = submittedTx.txHash;

        return txPlan;
    }

    /**
     * Submit a signed transaction to the blockchain
     * @param txBodyCbor Transaction CBOR hex string
     * @return Transaction hash
     */
    protected String submitTransaction(String txBodyCbor) {
        System.out.println("=== SUBMITTING TRANSACTION ===");
        System.out.println("CBOR length: " + txBodyCbor.length());

        io.restassured.response.Response response = RestAssured.given()
                .baseUri(SUBMIT_BASE_URL)
                .contentType(ContentType.TEXT)
                .body(txBodyCbor)
                .post("/api/v1/tx/lifecycle/submit");

        System.out.println("Response status: " + response.statusCode());
        System.out.println("Response body: " + response.getBody().asString());

        if (response.statusCode() != 202 && response.statusCode() != 400) {
            System.out.println("=== HTTP " + response.statusCode() + " SUBMIT ERROR ===");
            System.out.println(response.getBody().asString());
            System.out.println("========================");
            throw new RuntimeException("Transaction submission failed with status: " + response.statusCode());
        }

        SubmittedTxResponse submittedTx = response.then()
                .statusCode(anyOf(is(202), is(400)))
                .extract()
                .as(SubmittedTxResponse.class);

        System.out.println("Parsed response - txHash: " + submittedTx.txHash + ", status: " + submittedTx.status);
        if (submittedTx.errorMessage != null) {
            System.out.println("Error message: " + submittedTx.errorMessage);
        }

        if (TxStatus.FAILED == submittedTx.status) {
            System.out.println("Transaction submission failed: " + submittedTx.txHash);
            System.out.println("  Error: " + submittedTx.errorMessage);
            System.out.println("  This usually means UTXOs were already spent or invalid");
            throw new RuntimeException("Transaction submission failed - " + (submittedTx.errorMessage != null ? submittedTx.errorMessage : "UTXO already spent or invalid"));
        }

        System.out.println("Transaction submitted successfully: " + submittedTx.txHash);
        return submittedTx.txHash;
    }


    /**
     * Assert that transaction plan response is valid
     */
    protected void assertTxPlanSuccess(TxPlanResponse response) {
        assertThat(response).isNotNull();
        assertThat(response.txBodyCbor).isNotBlank();
        assertThat(response.txBodyCbor).matches("^[0-9a-fA-F]+$");
        assertThat(response.txBodyCbor.length()).isGreaterThan(10);
    }

    /**
     * Wait for funds to be available at an address
     */
    protected void waitForFunds(String address) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(1))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> !getUTXOSupplier().getAll(address).isEmpty());
    }

    // ========== Response Classes ==========

    public static class TxPlanResponse {
        public String txBodyCbor;
        public String txHash;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmittedTxResponse {
        @JsonProperty("tx_hash")
        public String txHash;

        @JsonProperty("status")
        public TxStatus status;

        @JsonProperty("tx_body_cbor")
        public String txBodyCbor;

        @JsonProperty("error_message")
        public String errorMessage;
    }
}
