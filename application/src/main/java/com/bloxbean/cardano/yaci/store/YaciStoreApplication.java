package com.bloxbean.cardano.yaci.store;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class YaciStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaciStoreApplication.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info().title("Customer accounts API").version("1.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }

}
