package com.bloxbean.cardano.yaci.store.app;

import com.bloxbean.cardano.yaci.store.analytics.query.controller.AnalyticsQueryController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

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
