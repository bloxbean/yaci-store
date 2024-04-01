package com.bloxbean.cardano.yaci.store.aggregation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableBatchProcessing
@Slf4j
public class AggregationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AggregationApplication.class, args);
    }
}
