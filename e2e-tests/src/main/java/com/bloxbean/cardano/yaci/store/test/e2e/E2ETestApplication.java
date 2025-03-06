package com.bloxbean.cardano.yaci.store.test.e2e;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableBatchProcessing
@Slf4j
public class E2ETestApplication {
    public static void main(String[] args) {
        SpringApplication.run(E2ETestApplication.class, args);
    }
}
