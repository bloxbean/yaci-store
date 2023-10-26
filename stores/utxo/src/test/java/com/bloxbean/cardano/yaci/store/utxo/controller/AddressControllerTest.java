package com.bloxbean.cardano.yaci.store.utxo.controller;

import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.service.AddressService;
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

@WebMvcTest(AddressController.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${apiPrefix}")
    private String apiPrefix;

    @MockBean
    private AddressService addressService;

    @Test
    void testGetUtxosByAddressVerificationKeyHash_shouldReturn200() throws Exception {
        when(addressService.getUtxoByPaymentCredential("addr_vkh1p5cvd4ckl4ky3265du9kdl2l42369uxvanc2wt4gcp9rqzc60ky", 0, 10, Order.asc))
                .thenReturn(utxos());

        mockMvc.perform(get(apiPrefix + "/addresses/{address}/utxos", "addr_vkh1p5cvd4ckl4ky3265du9kdl2l42369uxvanc2wt4gcp9rqzc60ky")
                        .param("page", "1")
                        .param("count", "10")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(utxos())));
    }

    @Test
    void testGetUtxosByStakeAddress_shouldReturn200() throws Exception {
        when(addressService.getUtxoByStakeAddress("stake_test1uz53eam4lw8plhlgs2exq983r4tt63mgzfc22h73teksvnq5hwnfs", 0, 10, Order.asc))
                .thenReturn(utxos());

        mockMvc.perform(get(apiPrefix + "/addresses/{address}/utxos", "stake_test1uz53eam4lw8plhlgs2exq983r4tt63mgzfc22h73teksvnq5hwnfs")
                        .param("page", "1")
                        .param("count", "10")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(utxos())));
    }

    @Test
    void testGetUtxosByAddress_shouldReturn200() throws Exception {
        when(addressService.getUtxoByAddress("addr_test1qqxnp3khzm7kcj9t23hskehat7428ghsenk0pfew4rqy5v9frnmht7uwrl073q4jvq20z82kh4rksyns540azhndqexqpvhgqr", 0, 10, Order.asc))
                .thenReturn(utxos());

        mockMvc.perform(get(apiPrefix + "/addresses/{address}/utxos", "addr_test1qqxnp3khzm7kcj9t23hskehat7428ghsenk0pfew4rqy5v9frnmht7uwrl073q4jvq20z82kh4rksyns540azhndqexqpvhgqr")
                        .param("page", "1")
                        .param("count", "10")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(utxos())));
    }

    @Test
    void testGetUtxosForAssetByAddressVerificationKeyHash_shouldReturn200() throws Exception {
        when(addressService.getUtxoByPaymentCredentialAndAsset("addr_vkh1p5cvd4ckl4ky3265du9kdl2l42369uxvanc2wt4gcp9rqzc60ky",
                "749482b2fe4ac715bdeadc67db1f42600483ebb1913fa80a26411a63506c757475734d696e74546f6b656e2d320", 0, 10, Order.asc))
                .thenReturn(utxos());

        mockMvc.perform(get(apiPrefix + "/addresses/{address}/utxos/{asset}",
                        "addr_vkh1p5cvd4ckl4ky3265du9kdl2l42369uxvanc2wt4gcp9rqzc60ky",
                        "749482b2fe4ac715bdeadc67db1f42600483ebb1913fa80a26411a63506c757475734d696e74546f6b656e2d320")
                        .param("page", "1")
                        .param("count", "10")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(utxos())));
    }

    @Test
    void testGetUtxosForAssetByStakeAddress_shouldReturn200() throws Exception {
        when(addressService.getUtxoByStakeAddressAndAsset("stake_test1uz53eam4lw8plhlgs2exq983r4tt63mgzfc22h73teksvnq5hwnfs",
                "749482b2fe4ac715bdeadc67db1f42600483ebb1913fa80a26411a63506c757475734d696e74546f6b656e2d3200", 0, 10, Order.asc))
                .thenReturn(utxos());

        mockMvc.perform(get(apiPrefix + "/addresses/{address}/utxos/{asset}",
                        "stake_test1uz53eam4lw8plhlgs2exq983r4tt63mgzfc22h73teksvnq5hwnfs",
                        "749482b2fe4ac715bdeadc67db1f42600483ebb1913fa80a26411a63506c757475734d696e74546f6b656e2d3200")
                        .param("page", "1")
                        .param("count", "10")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(utxos())));
    }

    @Test
    void testGetUtxosForAssetByAddress_shouldReturn200() throws Exception {
        when(addressService.getUtxoByAddressAndAsset("addr_test1qqxnp3khzm7kcj9t23hskehat7428ghsenk0pfew4rqy5v9frnmht7uwrl073q4jvq20z82kh4rksyns540azhndqexqpvhgqr",
                "749482b2fe4ac715bdeadc67db1f42600483ebb1913fa80a26411a63506c757475734d696e74546f6b656e2d3200", 0, 10, Order.asc))
                .thenReturn(utxos());

        mockMvc.perform(get(apiPrefix + "/addresses/{address}/utxos/{asset}",
                        "addr_test1qqxnp3khzm7kcj9t23hskehat7428ghsenk0pfew4rqy5v9frnmht7uwrl073q4jvq20z82kh4rksyns540azhndqexqpvhgqr",
                        "749482b2fe4ac715bdeadc67db1f42600483ebb1913fa80a26411a63506c757475734d696e74546f6b656e2d3200")
                        .param("page", "1")
                        .param("count", "10")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(utxos())));
    }
    private List<Utxo> utxos() {
        return List.of(
                Utxo.builder()
                        .txHash("4328bf6a85ac2193b37843b236e66ec9f05f957ae3c47b46d752b2eba076bd65")
                        .address("addr_test1qqxnp3khzm7kcj9t23hskehat7428ghsenk0pfew4rqy5v9frnmht7uwrl073q4jvq20z82kh4rksyns540azhndqexqpvhgqr")
                        .outputIndex(0)
                        .amount(List.of(Utxo.Amount.builder().unit("lovelace").quantity(BigInteger.valueOf(3690683)).build()))
                        .dataHash(null)
                        .inlineDatum(null)
                        .referenceScriptHash(null)
                        .build()
        );
    }
}
