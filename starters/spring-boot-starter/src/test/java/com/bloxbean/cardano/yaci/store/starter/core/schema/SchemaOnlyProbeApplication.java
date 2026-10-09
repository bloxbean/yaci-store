package com.bloxbean.cardano.yaci.store.starter.core.schema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
@EnableAutoConfiguration
public class SchemaOnlyProbeApplication {
    @Bean
    Object mustNeverInitialize() {
        throw new IllegalStateException("CONSUMING_APPLICATION_WAS_STARTED");
    }
    public static void main(String[] args) {
        SpringApplication.run(SchemaOnlyProbeApplication.class, args);
        throw new IllegalStateException("SCHEMA_ONLY_DID_NOT_EXIT");
    }
}
