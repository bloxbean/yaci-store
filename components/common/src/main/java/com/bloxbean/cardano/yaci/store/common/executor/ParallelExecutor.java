package com.bloxbean.cardano.yaci.store.common.executor;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Data
public class ParallelExecutor {
    private Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public Executor getVirtualThreadExecutor() {
        return virtualThreadExecutor;
    }

    public void executesInParallel(Runnable... runnable) {
        List<CompletableFuture> futures = new ArrayList<>();
        for (Runnable r: runnable) {
            var future = CompletableFuture.runAsync(r, virtualThreadExecutor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
