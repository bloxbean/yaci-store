package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.common.Constants;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TipFinderServiceTest {

    @Test
    @Disabled
    void findTip() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setCardanoHost(Constants.PREPROD_PUBLIC_RELAY_ADDR);
        storeProperties.setCardanoPort(Constants.PREPROD_PUBLIC_RELAY_PORT);
        storeProperties.setProtocolMagic(Constants.PREPROD_PROTOCOL_MAGIC);

        try {
            TipFinderService tipFinderService = new TipFinderService(storeProperties);
            var tip = tipFinderService.getTip().block(Duration.ofSeconds(5));
            assertThat(tip).isNotNull();
            System.out.println("Tip found: " + tip);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to find tip: " + e.getMessage());
        }
    }

}
