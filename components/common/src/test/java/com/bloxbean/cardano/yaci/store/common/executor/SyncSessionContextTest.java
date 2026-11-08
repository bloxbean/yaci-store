package com.bloxbean.cardano.yaci.store.common.executor;

import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SyncSessionContextTest {

    @AfterEach
    void tearDown() {
        SyncSessionContext.exit(null);
    }

    @Test
    void workerThreadsOfAStoreRunAsTheCallersSession() throws Exception {
        SyncSessionContext.enter(7);
        Set<Long> seen = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture.runAsync(() -> seen.add(SyncSessionContext.current()),
                    SyncSessionContext.propagating(pool)).get(5, TimeUnit.SECONDS);
            CompletableFuture.supplyAsync(SyncSessionContext.propagating(() -> seen.add(SyncSessionContext.current())))
                    .get(5, TimeUnit.SECONDS);
            IntStream.range(0, 100).boxed().parallel()
                    .forEach(SyncSessionContext.propagating((Integer i) -> seen.add(SyncSessionContext.current())));
            new ParallelExecutor().executesInParallel(() -> seen.add(SyncSessionContext.current()));
            ListUtil.partitionAndApplyInParallel(IntStream.range(0, 10).boxed().toList(), 2,
                    (List<Integer> part) -> seen.add(SyncSessionContext.current()), pool);

            assertThat(seen).containsExactly(7L);
            //the pooled threads do not keep the mark after their task
            assertThat(CompletableFuture.supplyAsync(SyncSessionContext::current, pool).get(5, TimeUnit.SECONDS)).isNull();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void withoutASessionNothingIsMarked() throws Exception {
        assertThat(CompletableFuture.supplyAsync(SyncSessionContext.propagating(SyncSessionContext::current))
                .get(5, TimeUnit.SECONDS)).isNull();
    }
}
