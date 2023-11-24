package com.bloxbean.cardano.yaci.store.api.utxo.controller;

import com.bloxbean.cardano.yaci.store.api.utxo.service.AssetService;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
public class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${apiPrefix}")
    private String apiPrefix;

    @MockBean
    private AssetService assetService;

    @Test
    void testGetUtxosByAddress_shouldReturn200() throws Exception {
        when(assetService.getUtxosByAsset("9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145", 0, 10, Order.asc))
                .thenReturn(utxos());

        mockMvc.perform(get(apiPrefix + "/assets/utxos/unit/{unit}", "9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145")
                        .param("page", "1")
                        .param("count", "10")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(utxos())));
    }

    private List<Utxo> utxos() {
        return List.of(Utxo.builder()
                        .txHash("4328bf6a85ac2193b37843b236e66ec9f05f957ae3c47b46d752b2eba076bd65")
                        .address("addr_test1qqxnp3khzm7kcj9t23hskehat7428ghsenk0pfew4rqy5v9frnmht7uwrl073q4jvq20z82kh4rksyns540azhndqexqpvhgqr")
                        .outputIndex(0)
                        .amount(List.of(Utxo.Amount.builder().unit("lovelace").quantity(BigInteger.valueOf(2000000)).build(),
                                Utxo.Amount.builder().unit("9a9693a9a37912a5097918f97918d15240c92ab729a0b7c4aa144d7753554e444145").quantity(new BigInteger("71271811461339")).build()))
                        .dataHash(null)
                        .inlineDatum(null)
                        .referenceScriptHash(null)
                        .build()
        );
    }
}
