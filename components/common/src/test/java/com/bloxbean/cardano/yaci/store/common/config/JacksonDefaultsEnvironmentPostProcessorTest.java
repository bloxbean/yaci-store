package com.bloxbean.cardano.yaci.store.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import static com.bloxbean.cardano.yaci.store.common.config.JacksonDefaultsEnvironmentPostProcessor.PREFERRED_JSON_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

class JacksonDefaultsEnvironmentPostProcessorTest {

    @Test
    void defaultsHttpJsonToJackson2() {
        StandardEnvironment environment = new StandardEnvironment();

        new JacksonDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(PREFERRED_JSON_MAPPER)).isEqualTo("jackson2");
    }

    @Test
    void keepsTheApplicationsChoice() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("app", Map.of(PREFERRED_JSON_MAPPER, "jackson")));

        new JacksonDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(PREFERRED_JSON_MAPPER)).isEqualTo("jackson");
    }

    @Test
    void isRegisteredWithSpringBoot() throws Exception {
        Properties factories = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/META-INF/spring.factories")) {
            factories.load(in);
        }

        assertThat(factories.getProperty(EnvironmentPostProcessor.class.getName()))
                .contains(JacksonDefaultsEnvironmentPostProcessor.class.getName());
    }
}
