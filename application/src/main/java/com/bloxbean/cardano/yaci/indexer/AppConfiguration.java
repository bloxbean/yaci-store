package com.bloxbean.cardano.yaci.indexer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class AppConfiguration {

    @Bean
    public WebFluxConfigurer corsMappingConfigurer() {
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS", "HEAD")
                        .maxAge(3600)
                        .allowedHeaders("Requestor-Type")
                        .exposedHeaders("X-Get-Header");
            }
        };
    }

}
