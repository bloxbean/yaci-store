package com.bloxbean.cardano.yaci.store.common.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyncStatusTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesNetworkTipAvailabilityWithoutChangingRecordComponents() throws Exception {
        SyncStatus syncStatus = SyncStatus.builder()
                .networkBlock(SyncStatus.UNKNOWN_NETWORK_TIP)
                .networkSlot(SyncStatus.UNKNOWN_NETWORK_TIP)
                .build();

        String json = objectMapper.writeValueAsString(syncStatus);

        assertThat(json).contains("\"networkTipAvailable\":false");
        assertThat(syncStatus.networkTipAvailable()).isFalse();
    }
}
