package com.bloxbean.cardano.yaci.store.test.e2e.txs;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.store.core.service.BlockFetchService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import com.bloxbean.cardano.yaci.store.test.e2e.common.BaseE2ETest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TxPlanBuilderIT extends BaseE2ETest {

    private static final String SUBMIT_BASE_URL = "http://localhost:9999";

    @MockBean
    private BlockFetchService blockFetchService;

    @MockBean
    private StartService startService;

    @BeforeAll
    static void fundAccounts() {
        Account fundAccount0 = new Account(Networks.testnet(), DEFAULT_MNEMONICS);
        topUpFund(fundAccount0.baseAddress(), 50);
    }

    @Test
    void buildUnsignedTxFromPlan_shouldReturnTxBodyCbor() {
        waitForFunds(account0.baseAddress());

        String txPlanYaml = """
                version: 1.0
                context:
                  fee_payer: %s
                transaction:
                  - tx:
                      from: %s
                      intents:
                        - type: payment
                          to: %s
                          amount:
                            unit: lovelace
                            quantity: 1000000
                """.formatted(account0.baseAddress(), account0.baseAddress(), account1.baseAddress());

        TxPlanResponse response = RestAssured.given()
                .baseUri(SUBMIT_BASE_URL)
                .contentType(ContentType.TEXT)
                .body(txPlanYaml)
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
