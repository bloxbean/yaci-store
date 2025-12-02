package com.bloxbean.cardano.yaci.store.test.e2e.txs;

import com.bloxbean.cardano.yaci.store.test.e2e.common.BaseE2ETest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TxBuilderSignerRegistryIT extends BaseE2ETest {

    private static final String SUBMIT_BASE_URL = "http://localhost:9999";

    @Test
    void buildTxPlan_withRemoteSignerRef_shouldSucceed() {
        topUpFund(account0.baseAddress(), 200);
        waitForFunds(account0.baseAddress());

        String txYaml = """
            version: 1.0
            transaction:
              - tx:
                  from: %s
                  intents:
                    - type: payment
                      address: %s
                      amounts:
                        - unit: lovelace
                          quantity: 2000000
                  signers:
                    - ref: remote://ops
                      scopes: [payment]
            """.formatted(account0.baseAddress(), account1.baseAddress());

        TxPlanResponse response = RestAssured.given()
                .baseUri(SUBMIT_BASE_URL)
                .contentType(ContentType.TEXT)
                .body(txYaml)
                .post("/api/v1/tx/lifecycle/build")
                .then()
                .statusCode(200)
                .extract()
                .as(TxPlanResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.txBodyCbor).isNotBlank();
        assertThat(response.txBodyCbor).matches("^[0-9a-fA-F]+$");
    }

    private void waitForFunds(String address) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(1))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> !getUTXOSupplier().getAll(address).isEmpty());
    }

    public static class TxPlanResponse {
        public String txBodyCbor;
    }
}
