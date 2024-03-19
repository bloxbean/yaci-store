package com.bloxbean.cardano.yaci.store.redis.app;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.extensions.redis")
@EntityScan(basePackages = "com.bloxbean.cardano.yaci.store.extensions.redis")
public class YaciStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaciStoreApplication.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info().title("Yaci Store API").version("1.0")
                        .license(new License().name("MIT").url("https://github.com/bloxbean/yaci-store")));
    }

}
