package com.bloxbean.cardano.yaci.store.common.executor;

import lombok.Data;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Data
public class ParallelExecutor {
    private Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public Executor getVirtualThreadExecutor() {
        return virtualThreadExecutor;
    }
}
