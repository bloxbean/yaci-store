package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BFQueryTimeoutPropertiesTest {

    private static Duration bind(String value) {
        return new Binder(new MapConfigurationPropertySource(Map.of("store.extensions.blockfrost.query-timeout", value)))
                .bind("store.extensions.blockfrost", BFQueryTimeoutProperties.class)
                .get()
                .getQueryTimeout();
    }

    @Test
    void numberWithoutUnit_isSeconds() {
        assertThat(bind("30")).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void explicitUnits_areKept() {
        assertThat(bind("30s")).isEqualTo(Duration.ofSeconds(30));
        assertThat(bind("500ms")).isEqualTo(Duration.ofMillis(500));
        assertThat(bind("PT1M")).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void zero_disables() {
        assertThat(bind("0")).isZero();
    }

    @Test
    void defaultIs30Seconds() {
        assertThat(new BFQueryTimeoutProperties().getQueryTimeout()).isEqualTo(Duration.ofSeconds(30));
    }
}
