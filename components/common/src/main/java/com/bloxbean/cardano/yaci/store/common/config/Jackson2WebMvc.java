package com.bloxbean.cardano.yaci.store.common.config;

import org.springframework.context.ApplicationContext;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * JSON for an {@code @EnableWebMvc} application, as with Spring Framework 6. {@code @EnableWebMvc} turns off Spring
 * Boot's MVC configuration (and with it {@code spring.http.converters.preferred-json-mapper}), and Spring Framework 7
 * then uses Jackson 3, which ignores the Jackson 2 annotations on Yaci Store's models (for example
 * {@code @JsonNaming(SnakeCaseStrategy)}). Call from {@code WebMvcConfigurer#configureMessageConverters}.
 */
public final class Jackson2WebMvc {
    private Jackson2WebMvc() {
    }

    @SuppressWarnings("removal")
    public static void useJackson2(HttpMessageConverters.ServerBuilder builder, ApplicationContext applicationContext) {
        //The mapper Spring Framework 6's WebMvcConfigurationSupport built for its default JSON converter
        builder.withJsonConverter(new MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json().applicationContext(applicationContext).build()));
    }
}
