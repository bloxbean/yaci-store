package com.bloxbean.cardano.yaci.store.api.core.controller;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GenesisControllerTest {

    @Mock
    private GenesisConfig genesisConfig;

    @Mock
    private StoreProperties storeProperties;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GenesisController(genesisConfig, storeProperties))
                .addPlaceholderValue("apiPrefix", "/api/v1")
                .build();
    }

    private void mockMainnetGenesis() {
        when(storeProperties.getProtocolMagic()).thenReturn(764824073L);
        when(genesisConfig.getActiveSlotsCoeff()).thenReturn(0.05);
        when(genesisConfig.getUpdateQuorum()).thenReturn(5);
        when(genesisConfig.getMaxLovelaceSupply()).thenReturn(new BigInteger("45000000000000000"));
        when(genesisConfig.getEpochLength()).thenReturn(432000L);
        when(genesisConfig.getStartTime(764824073L)).thenReturn(1506203091L);
        when(genesisConfig.getSlotsPerKesPeriod()).thenReturn(129600L);
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
        when(genesisConfig.getMaxKesEvolutions()).thenReturn(62);
        when(genesisConfig.getSecurityParam()).thenReturn(2160);
    }

    @Test
    void getGenesisInfo_returnsBlockfrostCompatibleShape() throws Exception {
        mockMainnetGenesis();

        mockMvc.perform(get("/api/v1/genesis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.network_magic").value(764824073L))
                .andExpect(jsonPath("$.system_start").value(1506203091L))
                .andExpect(jsonPath("$.epoch_length").value(432000))
                .andExpect(jsonPath("$.security_param").value(2160))
                .andExpect(jsonPath("$.active_slots_coefficient").value(0.05))
                .andExpect(jsonPath("$.update_quorum").value(5))
                .andExpect(jsonPath("$.slots_per_kes_period").value(129600))
                .andExpect(jsonPath("$.max_kes_evolutions").value(62))
                .andExpect(jsonPath("$.max_lovelace_supply").value("45000000000000000"));
    }

    @Test
    void getGenesisInfo_integralSlotLengthSerializedAsInteger() throws Exception {
        mockMainnetGenesis();

        String content = mockMvc.perform(get("/api/v1/genesis"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        //Blockfrost returns slot_length as integer, and max_lovelace_supply as string
        assertThat(content).contains("\"slot_length\":1,");
        assertThat(content).contains("\"max_lovelace_supply\":\"45000000000000000\"");
    }

    @Test
    void getGenesisInfo_fractionalSlotLengthRoundedUpToBlockfrostInteger() throws Exception {
        when(storeProperties.getProtocolMagic()).thenReturn(42L);
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(0.1);
        when(genesisConfig.getMaxLovelaceSupply()).thenReturn(new BigInteger("20000000000000000"));

        mockMvc.perform(get("/api/v1/genesis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.network_magic").value(42))
                .andExpect(jsonPath("$.slot_length").value(1));
    }
}
