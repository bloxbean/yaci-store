package com.bloxbean.cardano.yaci.store.client.utxo;

import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

class UtxoClientImplTest {
    private static final String BASE_URL = "http://utxo-store/api/v1";

    private MockRestServiceServer server;
    private UtxoClientImpl utxoClient;

    @BeforeEach
    void setUp() {
        var restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        utxoClient = new UtxoClientImpl(restTemplate, BASE_URL);
    }

    @Test
    void getUtxosByIdsReturnsEmptyListForNoContent() {
        server.expect(requestTo(BASE_URL + "/utxos"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        var utxos = utxoClient.getUtxosByIds(List.of(new UtxoKey("tx", 0)));

        assertThat(utxos).isEmpty();
        server.verify();
    }

    @Test
    void getUtxoByAddressReturnsEmptyListForNoContent() {
        server.expect(requestTo(BASE_URL + "/addresses/addr1/utxos?page=0&count=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withNoContent());

        var utxos = utxoClient.getUtxoByAddress("addr1", 0, 10);

        assertThat(utxos).isEmpty();
        server.verify();
    }
}
