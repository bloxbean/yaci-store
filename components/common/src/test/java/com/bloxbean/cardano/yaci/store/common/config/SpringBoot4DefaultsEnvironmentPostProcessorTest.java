package com.bloxbean.cardano.yaci.store.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import static com.bloxbean.cardano.yaci.store.common.config.SpringBoot4DefaultsEnvironmentPostProcessor.BATCH_JOB_ENABLED;
import static com.bloxbean.cardano.yaci.store.common.config.SpringBoot4DefaultsEnvironmentPostProcessor.PREFERRED_JSON_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

class SpringBoot4DefaultsEnvironmentPostProcessorTest {

    @Test
    void springBoot3Defaults() {
        StandardEnvironment environment = postProcess(Map.of());

        assertThat(environment.getProperty(PREFERRED_JSON_MAPPER)).isEqualTo("jackson2");
        assertThat(environment.getProperty(BATCH_JOB_ENABLED)).isEqualTo("false");
    }

    @Test
    void keepsTheApplicationsChoice() {
        StandardEnvironment environment = postProcess(Map.of(PREFERRED_JSON_MAPPER, "jackson",
                BATCH_JOB_ENABLED, "true"));

        assertThat(environment.getProperty(PREFERRED_JSON_MAPPER)).isEqualTo("jackson");
        assertThat(environment.getProperty(BATCH_JOB_ENABLED)).isEqualTo("true");
    }

    @Test
    void copiesJacksonSettingsToTheJackson2Mapper() {
        StandardEnvironment environment = postProcess(Map.of(
                "spring.jackson.default-property-inclusion", "non_null",
                "spring.jackson.serialization.indent-output", "true",
                "spring.jackson.use-jackson2-defaults", "true", //Jackson 3 only
                "spring.jackson.time-zone", "UTC",
                "spring.jackson2.time-zone", "GMT")); //set explicitly: kept

        assertThat(environment.getProperty("spring.jackson2.default-property-inclusion")).isEqualTo("non_null");
        assertThat(environment.getProperty("spring.jackson2.serialization.indent-output")).isEqualTo("true");
        assertThat(environment.containsProperty("spring.jackson2.use-jackson2-defaults")).isFalse();
        assertThat(environment.getProperty("spring.jackson2.time-zone")).isEqualTo("GMT");
    }

    @Test
    void isRegisteredWithSpringBoot() throws Exception {
        Properties factories = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/META-INF/spring.factories")) {
            factories.load(in);
        }

        assertThat(factories.getProperty(EnvironmentPostProcessor.class.getName()))
                .contains(SpringBoot4DefaultsEnvironmentPostProcessor.class.getName());
    }

    private static StandardEnvironment postProcess(Map<String, Object> applicationProperties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("app", applicationProperties));
        new SpringBoot4DefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        return environment;
    }
}
