package com.bloxbean.cardano.yaci.store.utxo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootTestApplication.class, args);
    }

    @Bean
    public UtxoStoreProperties utxoStoreProperties() {
        var utxoStoreProperties = new UtxoStoreProperties();
        utxoStoreProperties.setSaveAddress(true);

        return utxoStoreProperties;
    }
}
