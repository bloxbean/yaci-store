package com.bloxbean.cardano.yaci.store.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
@Slf4j
public class QuarkusExecutorConfig {

    @Produces
    @Named("blockExecutor")
    public ExecutorService produceBlockExecutor() {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        return executor;
    }

    @Produces
    @Named("blockEventExecutor")
    public ExecutorService produceEventExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

