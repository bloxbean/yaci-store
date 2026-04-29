package com.bloxbean.cardano.yaci.store.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StoreConfiguration")
class StoreConfigurationTest {

    @Test
    @DisplayName("systemClock() returns a system-default-zone Clock")
    void systemClockReturnsSystemDefaultZoneClock() {
        // Bytecode-equivalence with LocalDateTime.now() depends on the bean
        // returning Clock.systemDefaultZone(), so call sites that migrate from
        // `LocalDateTime.now()` to `LocalDateTime.now(clock)` see no behaviour
        // change. If a future contributor switches the default to systemUTC()
        // (or anything zone-fixed), this test breaks — fix the configuration,
        // not the assertion.
        StoreConfiguration config = new StoreConfiguration();

        Clock clock = config.systemClock();

        assertThat(clock).isEqualTo(Clock.systemDefaultZone());
    }
}
