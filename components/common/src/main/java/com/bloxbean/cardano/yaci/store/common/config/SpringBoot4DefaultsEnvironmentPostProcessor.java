package com.bloxbean.cardano.yaci.store.common.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps Spring Boot 3 behaviour where Spring Boot 4 changed a default. Every value is added with the lowest
 * precedence, so an application setting (properties, environment, command line) always wins.
 * <ul>
 *     <li>JSON over HTTP: Spring Boot 4 uses Jackson 3, which ignores the Jackson 2 annotations (for example
 *     {@code @JsonSerialize}, {@code @JsonNaming}) on Yaci Store, Yaci and CCL models, so the REST output would
 *     change. {@code spring.http.converters.preferred-json-mapper=jackson2} keeps Jackson 2.</li>
 *     <li>{@code spring.jackson.*} settings now configure Jackson 3; the Jackson 2 mapper reads
 *     {@code spring.jackson2.*}. Each {@code spring.jackson.*} setting that also exists for Jackson 2 is copied
 *     there, so it still applies to the REST output.</li>
 *     <li>Spring Batch: with {@code @EnableBatchProcessing}, Spring Boot 3 never ran jobs at startup. Spring Boot 4
 *     does unless {@code spring.batch.job.enabled=false}. Yaci Store starts its jobs itself.</li>
 * </ul>
 */
public class SpringBoot4DefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {
    static final String PREFERRED_JSON_MAPPER = "spring.http.converters.preferred-json-mapper";
    static final String BATCH_JOB_ENABLED = "spring.batch.job.enabled";
    private static final String PROPERTY_SOURCE_NAME = "yaciStoreSpringBoot4Defaults";

    private static final String JACKSON = "spring.jackson.";
    private static final String JACKSON2 = "spring.jackson2.";
    //Properties of spring.jackson2 (Spring Boot 4.1), the same names Spring Boot 3 used under spring.jackson
    private static final List<String> JACKSON2_PROPERTIES = List.of("constructor-detector", "datatype.",
            "date-format", "default-leniency", "default-property-inclusion", "deserialization.", "generator.",
            "locale", "mapper.", "parser.", "property-naming-strategy", "serialization.", "time-zone", "visibility.");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        if (!environment.containsProperty(PREFERRED_JSON_MAPPER))
            defaults.put(PREFERRED_JSON_MAPPER, "jackson2");
        if (!environment.containsProperty(BATCH_JOB_ENABLED))
            defaults.put(BATCH_JOB_ENABLED, "false");

        for (PropertySource<?> source : environment.getPropertySources()) {
            if (!(source instanceof EnumerablePropertySource<?> enumerable))
                continue;
            for (String name : enumerable.getPropertyNames()) {
                if (!name.startsWith(JACKSON))
                    continue;
                String suffix = name.substring(JACKSON.length());
                String jackson2Name = JACKSON2 + suffix;
                if (JACKSON2_PROPERTIES.stream().anyMatch(p -> p.endsWith(".") ? suffix.startsWith(p) : suffix.equals(p))
                        && !environment.containsProperty(jackson2Name) && !defaults.containsKey(jackson2Name))
                    defaults.put(jackson2Name, environment.getProperty(name));
            }
        }

        if (!defaults.isEmpty())
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }
}
