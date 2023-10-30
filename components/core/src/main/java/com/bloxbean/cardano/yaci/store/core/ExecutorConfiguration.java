package com.bloxbean.cardano.yaci.store.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@ConditionalOnProperty(
        prefix = "store.core",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
@Slf4j
public class ExecutorConfiguration {
    private final StoreProperties storeProperties;

    @Bean
    @Qualifier("blockExecutor")
    public ExecutorService getBlockExecutor() {
        ExecutorService executor;
        if (storeProperties.isEnableParallelProcessing()) {
            if (storeProperties.isUseVirtualThreadForBatchProcessing()) {
                executor = Executors.newVirtualThreadPerTaskExecutor();
                log.info("Block Batch processing will be done using virtual threads");
                printExecutorConfig();
            } else {
                executor = Executors.newFixedThreadPool(storeProperties.getBlockProcessingThreads());
                log.info("Block Batch processing will be done using fixed thread pool of size {}",
                        storeProperties.getBlockProcessingThreads());
                printExecutorConfig();
            }
        } else {
            executor = Executors.newVirtualThreadPerTaskExecutor();
        }

        return executor;
    }

    @Bean
    @Qualifier("blockEventExecutor")
    public ExecutorService getEventExecutor() {
        ExecutorService eventExecutor;
        if (storeProperties.isEnableParallelProcessing()) {
            if (storeProperties.isUseVirtualThreadForEventProcessing()) {
                eventExecutor = Executors.newVirtualThreadPerTaskExecutor();
                log.info("Block Event processing will be done using virtual threads");
            } else {
                eventExecutor = Executors.newFixedThreadPool(storeProperties.getEventProcessingThreads());
                log.info("Block Event processing will be done using fixed thread pool of size {}",
                        storeProperties.getEventProcessingThreads());
                log.info("# of Event Processing Threads : " + storeProperties.getEventProcessingThreads());
            }
        } else {
            eventExecutor = Executors.newVirtualThreadPerTaskExecutor();
        }

        return eventExecutor;
    }

    private void printExecutorConfig() {
        log.info("Block Batch Size: " + storeProperties.getBlocksBatchSize());
        log.info("Block Partition Size: " + storeProperties.getBlocksPartitionSize());

        if (!storeProperties.isUseVirtualThreadForBatchProcessing())
            log.info("Block Processing Threads : " + storeProperties.getBlockProcessingThreads());
    }
}
