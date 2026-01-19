package com.bloxbean.cardano.yaci.store.adminui;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(AdminUiProperties.class)
@ConditionalOnProperty(name = "store.admin.ui.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.adminui")
@Slf4j
public class AdminUiAutoConfiguration {

    public AdminUiAutoConfiguration() {
        log.info("Admin UI is enabled");
    }

    @Bean
    public WebMvcConfigurer adminUiWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/admin/**")
                        .addResourceLocations("classpath:/static/");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate adminUiRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
