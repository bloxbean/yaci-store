package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Jackson2RestTemplateTest {

    @Test
    void readsAndWritesSnakeCaseJsonAsBeforeSpringBoot4() {
        RestTemplate restTemplate = Jackson2RestTemplate.useJackson2(new RestTemplate(), new ObjectMapper());
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("/utxos"))
                .andExpect(content().json("[{\"tx_hash\":\"abc\",\"output_index\":1}]", true))
                .andRespond(withSuccess("[{\"tx_hash\":\"abc\",\"output_index\":1,\"owner_addr\":\"addr1\"}]",
                        MediaType.APPLICATION_JSON));

        AddressUtxo[] utxos = restTemplate.postForObject("/utxos", List.of(new UtxoKey("abc", 1)), AddressUtxo[].class);

        server.verify();
        assertThat(utxos).hasSize(1);
        assertThat(utxos[0].getTxHash()).isEqualTo("abc");
        assertThat(utxos[0].getOutputIndex()).isEqualTo(1);
        assertThat(utxos[0].getOwnerAddr()).isEqualTo("addr1");
    }
}
