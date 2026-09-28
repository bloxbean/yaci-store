package com.bloxbean.cardano.yaci.store.api.core.controller;

import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.core.service.SyncStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock
    private SyncStatusService syncStatusService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SyncController(syncStatusService))
                .addPlaceholderValue("apiPrefix", "/api/v1")
                .build();
    }

    @Test
    void getSyncStatus_returnsLagBlocks() throws Exception {
        when(syncStatusService.getSyncStatus()).thenReturn(SyncStatus.builder()
                .block(11499000)
                .slot(146000000)
                .epoch(560)
                .era("Conway")
                .blockHash("d9e7e2c1a5b3f4e6d8c9b0a1f2e3d4c5b6a7f8e9d0c1b2a3f4e5d6c7b8a9f0e1")
                .syncPercentage(99.99)
                .networkBlock(11499100)
                .networkSlot(146000200)
                .synced(false)
                .protocolMagic(764824073L)
                .build());

        mockMvc.perform(get("/api/v1/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.block").value(11499000))
                .andExpect(jsonPath("$.slot").value(146000000))
                .andExpect(jsonPath("$.epoch").value(560))
                .andExpect(jsonPath("$.era").value("Conway"))
                .andExpect(jsonPath("$.block_hash").value("d9e7e2c1a5b3f4e6d8c9b0a1f2e3d4c5b6a7f8e9d0c1b2a3f4e5d6c7b8a9f0e1"))
                .andExpect(jsonPath("$.sync_percentage").value(99.99))
                .andExpect(jsonPath("$.network_block").value(11499100))
                .andExpect(jsonPath("$.network_slot").value(146000200))
                .andExpect(jsonPath("$.lag_blocks").value(100))
                .andExpect(jsonPath("$.network_tip_available").value(true))
                .andExpect(jsonPath("$.synced").value(false))
                .andExpect(jsonPath("$.protocol_magic").value(764824073L));
    }

    @Test
    void getSyncStatus_lagBlocksClampedToZeroWhenStoreAhead() throws Exception {
        when(syncStatusService.getSyncStatus()).thenReturn(SyncStatus.builder()
                .block(11499100)
                .networkBlock(11499000)
                .synced(true)
                .protocolMagic(2L)
                .build());

        mockMvc.perform(get("/api/v1/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lag_blocks").value(0))
                .andExpect(jsonPath("$.synced").value(true))
                .andExpect(jsonPath("$.protocol_magic").value(2));
    }

    @Test
    void getSyncStatus_exposesUnknownTipWithoutClaimingSynced() throws Exception {
        when(syncStatusService.getSyncStatus()).thenReturn(SyncStatus.builder()
                .block(11499100)
                .slot(146000000)
                .networkBlock(SyncStatus.UNKNOWN_NETWORK_TIP)
                .networkSlot(SyncStatus.UNKNOWN_NETWORK_TIP)
                .synced(false)
                .protocolMagic(2L)
                .build());

        mockMvc.perform(get("/api/v1/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.network_tip_available").value(false))
                .andExpect(jsonPath("$.network_block").doesNotExist())
                .andExpect(jsonPath("$.network_slot").doesNotExist())
                .andExpect(jsonPath("$.lag_blocks").doesNotExist())
                .andExpect(jsonPath("$.sync_percentage").doesNotExist())
                .andExpect(jsonPath("$.synced").value(false));
    }
}
