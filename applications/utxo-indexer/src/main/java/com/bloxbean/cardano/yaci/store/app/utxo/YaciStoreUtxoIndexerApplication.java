package com.bloxbean.cardano.yaci.store.app.utxo;

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
public class YaciStoreUtxoIndexerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YaciStoreUtxoIndexerApplication.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info().title("Yaci Store Utxo Indexer API").version("1.0")
                        .license(new License().name("MIT").url("https://github.com/bloxbean/yaci-store")));
    }

}
