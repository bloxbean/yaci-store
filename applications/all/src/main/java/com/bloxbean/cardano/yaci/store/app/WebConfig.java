package com.bloxbean.cardano.yaci.store.app;

import com.bloxbean.cardano.yaci.store.analytics.query.controller.AnalyticsQueryController;
import com.bloxbean.cardano.yaci.store.common.config.Jackson2WebMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    private final ApplicationContext applicationContext;

    public WebConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        //JSON with Jackson 2 as before Spring Boot 4 (@EnableWebMvc: Spring Boot's converter settings do not apply)
        Jackson2WebMvc.useJackson2(builder, applicationContext);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                // Non-safelisted response headers are invisible to cross-origin browser clients
                // unless exposed explicitly (a wildcard is ignored with allowCredentials=true).
                .exposedHeaders(AnalyticsQueryController.ROW_LIMIT_HEADER, AnalyticsQueryController.TRUNCATED_HEADER)
                .allowCredentials(true).maxAge(3600);
    }

}
