package com.bloxbean.cardano.yaci.store.submit.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TxSubmitControllerTest {

    private static final String SUBMIT_API_URL = "http://localhost:8090/api/submit/tx";
    private static final String TX_HASH = "1df168ca9248585c529bb52f8d97f9bd909e0de9f86545cdb10b2df05fd7d4d1";

    private TxSubmitController controller;
    private MockRestServiceServer submitApi;

    @BeforeEach
    void setup() {
        controller = new TxSubmitController(new MockEnvironment().withProperty("store.cardano.submit-api-url", SUBMIT_API_URL));
        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
        submitApi = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void cborSubmission_returns200WithTxHash() {
        submitApi.expect(requestTo(SUBMIT_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("\"" + TX_HASH + "\""));

        var response = controller.submitTx(new byte[]{(byte) 0x84});

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isEqualTo("\"" + TX_HASH + "\"");
        submitApi.verify();
    }

    @Test
    void hexSubmission_keeps202FromSubmitApi() {
        submitApi.expect(requestTo(SUBMIT_API_URL))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("\"" + TX_HASH + "\""));

        var response = controller.submitTx("84");

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).isEqualTo("\"" + TX_HASH + "\"");
    }

    @Test
    void rejectedBySubmitApi_keepsErrorStatus() {
        submitApi.expect(requestTo(SUBMIT_API_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"tag\":\"TxSubmitFail\"}"));

        var response = controller.submitTx(new byte[]{(byte) 0x84});

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isInstanceOf(TxErrorResponse.class);
        assertThat(((TxErrorResponse) response.getBody()).message()).contains("TxSubmitFail");
    }
}
