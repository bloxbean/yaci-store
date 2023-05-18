package com.bloxbean.cardano.yaci.store.core.genesis;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvvmAddressConverterTest {

    @Test
    void convertAvvmToByronAddress() {
        String avvmAddr = "-0BJDi-gauylk4LptQTgjMeo7kY9lTCbZv12vwOSTZk=";
        Optional<String> byronAddr = AvvmAddressConverter.convertAvvmToByronAddress(avvmAddr);

        assertTrue(byronAddr.isPresent());
        assertThat(byronAddr.get()).isEqualTo("Ae2tdPwUPEZHFQnrr2dYB4GEQ8WVKspEyrg29pJ3f7qdjzaxjeShEEokF5f");
    }
}
