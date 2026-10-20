package com.bloxbean.cardano.yaci.store.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Keeps Spring Boot 3 behaviour where Spring Boot 4 changed a default. Every value is added with the lowest
 * precedence, so an application setting (properties, environment, command line) always wins.
 * <ul>
 *     <li>JSON over HTTP: Spring Boot 4 uses Jackson 3, which ignores the Jackson 2 annotations (for example
 *     {@code @JsonSerialize}, {@code @JsonNaming}) on Yaci Store, Yaci and CCL models, so the REST output would
 *     change. {@code spring.http.converters.preferred-json-mapper=jackson2} keeps Jackson 2.</li>
 *     <li>{@code spring.jackson.*} settings now configure Jackson 3; the Jackson 2 mapper reads
 *     {@code spring.jackson2.*}. A {@code spring.jackson.*} setting (from a properties or YAML file, not an
 *     environment variable) that Jackson 2 also has is copied there, so it still applies to the Jackson 2 mapper.
 *     Features only Jackson 3 has are not copied, as they would fail Jackson 2 binding.</li>
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
    private static final List<String> JACKSON2_PROPERTIES = List.of("constructor-detector", "date-format",
            "default-leniency", "default-property-inclusion", "locale", "property-naming-strategy", "time-zone",
            "visibility.");
    //Feature maps of spring.jackson2: copied only when the feature exists in Jackson 2
    private static final Map<String, Class<? extends Enum<?>>> JACKSON2_FEATURES = Map.of(
            "serialization.", SerializationFeature.class,
            "deserialization.", DeserializationFeature.class,
            "mapper.", MapperFeature.class,
            "parser.", JsonParser.Feature.class,
            "generator.", JsonGenerator.Feature.class,
            "datatype.enum.", EnumFeature.class,
            "datatype.json-node.", JsonNodeFeature.class);

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
                if (isJackson2Property(suffix) && !environment.containsProperty(jackson2Name)
                        && !defaults.containsKey(jackson2Name))
                    defaults.put(jackson2Name, environment.getProperty(name));
            }
        }

        if (!defaults.isEmpty())
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    static boolean isJackson2Property(String suffix) {
        if (JACKSON2_PROPERTIES.stream().anyMatch(p -> p.endsWith(".") ? suffix.startsWith(p) : suffix.equals(p)))
            return true;
        for (Map.Entry<String, Class<? extends Enum<?>>> feature : JACKSON2_FEATURES.entrySet()) {
            if (!suffix.startsWith(feature.getKey()))
                continue;
            //write-dates-as-timestamps or WRITE_DATES_AS_TIMESTAMPS -> WRITE_DATES_AS_TIMESTAMPS
            String name = suffix.substring(feature.getKey().length()).replace('-', '_').toUpperCase(Locale.ROOT);
            return Arrays.stream(feature.getValue().getEnumConstants()).anyMatch(c -> c.name().equals(name));
        }
        return false;
    }
}
