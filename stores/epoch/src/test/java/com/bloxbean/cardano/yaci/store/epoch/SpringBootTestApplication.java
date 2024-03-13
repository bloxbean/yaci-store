package com.bloxbean.cardano.yaci.store.epoch;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootTestApplication {

    @Bean
    public StoreProperties storeProperties() {
        return new StoreProperties();
    }
}
