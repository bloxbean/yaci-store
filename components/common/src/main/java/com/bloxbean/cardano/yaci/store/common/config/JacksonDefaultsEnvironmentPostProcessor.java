package com.bloxbean.cardano.yaci.store.common.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Spring Boot 4 serves JSON with Jackson 3 by default. Yaci Store models (and Yaci / CCL types) use Jackson 2
 * annotations such as {@code @JsonSerialize} and {@code @JsonNaming}, which Jackson 3 ignores, so the REST API output
 * would change. This keeps Jackson 2 as the HTTP JSON mapper unless the application sets
 * {@code spring.http.converters.preferred-json-mapper} itself.
 */
public class JacksonDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {
    static final String PREFERRED_JSON_MAPPER = "spring.http.converters.preferred-json-mapper";
    private static final String PROPERTY_SOURCE_NAME = "yaciStoreJacksonDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty(PREFERRED_JSON_MAPPER))
            return;

        //Lowest precedence: any user setting (properties, env, command line) wins
        environment.getPropertySources()
                .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(PREFERRED_JSON_MAPPER, "jackson2")));
    }
}
