package com.bloxbean.cardano.yaci.store.api.assets.controller;

import com.bloxbean.cardano.yaci.store.api.assets.service.AssetService;
import com.bloxbean.cardano.yaci.store.assets.domain.MintType;
import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssetController assetController;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${apiPrefix}")
    private String apiPrefix;

    @MockBean
    private AssetService assetService;

    @Test
    void contextLoads() {
        assertThat(assetController).isNotNull();
    }

    @Test
    void testGetAssetTxsByTx_shouldReturn200() throws Exception {
        when(assetService.getAssetsByTx("fd960815810b788da1f1d8719e3fdb47c5e4a82b9527f9c337a49512d255d545"))
                .thenReturn(List.of(txAsset()));

        mockMvc.perform(get(apiPrefix + "/assets/txs/{txHash}", "fd960815810b788da1f1d8719e3fdb47c5e4a82b9527f9c337a49512d255d545"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(txAsset()))));
    }

    @Test
    void testGetAssetTxsByFingerprint_shouldReturn200() throws Exception {
        when(assetService.getAssetTxsByFingerprint("asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs", 0, 10))
                .thenReturn(List.of(txAsset()));

        mockMvc.perform(get(apiPrefix + "/assets/fingerprint/{fingerprint}/history",
                        "asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs")
                        .param("page", "0")
                        .param("count", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(txAsset()))));
    }

    @Test
    void testGetAssetTxsByPolicyId_shouldReturn200() throws Exception {
        when(assetService.getAssetTxsByPolicyId("34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518", 0, 10))
                .thenReturn(List.of(txAsset()));

        mockMvc.perform(get(apiPrefix + "/assets/policy/{policyId}/history",
                        "34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518")
                        .param("page", "0")
                        .param("count", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(txAsset()))));
    }

    @Test
    void testGetAssetTxsByUnit_shouldReturn200() throws Exception {
        when(assetService.getAssetTxsByUnit("34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e", 0, 10)).thenReturn(List.of(txAsset()));

        mockMvc.perform(get(apiPrefix + "/assets/{unit}/history",
                        "34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e")
                        .param("page", "0")
                        .param("count", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(txAsset()))));
    }

    @Test
    void testGetSupplyByFingerprint_WhenAssetFound_ShouldReturn200() throws Exception {
        when(assetService.getSupplyByFingerprint("asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs")).thenReturn(Optional.of(BigInteger.valueOf(1000L)));

        mockMvc.perform(get(apiPrefix + "/assets/fingerprint/{fingerprint}/supply",
                        "asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(
                        new AssetController.FingerprintSupply("asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs", BigInteger.valueOf(1000)))));
    }

    @Test
    void testGetSupplyByFingerprint_WhenAssetNotFound_ShouldReturnNotFound() throws Exception {
        when(assetService.getSupplyByFingerprint("asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs")).thenReturn(Optional.empty());

        mockMvc.perform(get(apiPrefix + "/assets/fingerprint/{fingerprint}/supply",
                        "asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetSupplyByUnit_WhenAssetFound_ShouldReturn200() throws Exception {
        when(assetService.getSupplyByUnit("34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e"))
                .thenReturn(Optional.of(BigInteger.valueOf(1000L)));

        mockMvc.perform(get(apiPrefix + "/assets/{unit}/supply",
                        "34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(
                        new AssetController.UnitSupply("34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e", BigInteger.valueOf(1000L)))));
    }

    @Test
    void testGetSupplyByUnit_WhenAssetNotFound_ShouldReturnNotFound() throws Exception {
        when(assetService.getSupplyByUnit("34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(apiPrefix + "/assets/{unit}/supply",
                        "34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetSupplyByPolicy_WhenAssetFound_ShouldReturn200() throws Exception {
        when(assetService.getSupplyByPolicy("34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518"))
                .thenReturn(Optional.of(BigInteger.valueOf(1000L)));

        mockMvc.perform(get(apiPrefix + "/assets/policy/{policy}/supply",
                        "34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(
                        new AssetController.PolicySupply("34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518", BigInteger.valueOf(1000L)))));
    }

    @Test
    void testGetSupplyByPolicy_WhenAssetNotFound_ShouldReturnNotFound() throws Exception {
        when(assetService.getSupplyByPolicy("34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(apiPrefix + "/assets/policy/{policy}/supply",
                        "34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518"))
                .andExpect(status().isNotFound());
    }

    private TxAsset txAsset() {
        return TxAsset.builder()
                .assetName("ATADAcoin")
                .txHash("fd960815810b788da1f1d8719e3fdb47c5e4a82b9527f9c337a49512d255d545")
                .unit("34250edd1e9836f5378702fbf9416b709bc140e04f668cc3552085184154414441636f696e")
                .blockNumber(204450L)
                .blockTime(1666901639L)
                .slot(11218439L)
                .mintType(MintType.MINT)
                .fingerprint("asset1ee0u29k4xwauf0r7w8g30klgraxw0y4rz2t7xs")
                .quantity(BigInteger.ONE)
                .policy("34250edd1e9836f5378702fbf9416b709bc140e04f668cc355208518")
                .build();
    }
}
