package com.bloxbean.cardano.yaci.store.plugin.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AtomicStateTest {

    private AtomicState<String, Object> state;

    @BeforeEach
    void setUp() {
        state = new AtomicState<>();
    }

    @Test
    void testBasicOperations() {
        state.put("key1", "value1");
        assertThat(state.get("key1")).isEqualTo("value1");
        assertThat(state.containsKey("key1")).isTrue();
        assertThat(state.size()).isEqualTo(1);

        state.remove("key1");
        assertThat(state.containsKey("key1")).isFalse();
        assertThat(state.size()).isEqualTo(0);
    }

    @Test
    void testAtomicIncrement() {
        long result = state.increment("counter");
        assertThat(result).isEqualTo(1);

        result = state.increment("counter");
        assertThat(result).isEqualTo(2);

        result = state.increment("counter", 5);
        assertThat(result).isEqualTo(7);

        assertThat(state.get("counter")).isEqualTo(7L);
    }

    @Test
    void testAtomicDecrement() {
        state.put("counter", 10L);

        long result = state.decrement("counter");
        assertThat(result).isEqualTo(9);

        result = state.decrement("counter", 3);
        assertThat(result).isEqualTo(6);

        assertThat(state.get("counter")).isEqualTo(6L);
    }

    @Test
    void testAddAndGet() {
        long result = state.addAndGet("counter", 5);
        assertThat(result).isEqualTo(5);

        result = state.addAndGet("counter", 3);
        assertThat(result).isEqualTo(8);
    }

    @Test
    void testGetAndAdd() {
        long result = state.getAndAdd("counter", 5);
        assertThat(result).isEqualTo(0);

        result = state.getAndAdd("counter", 3);
        assertThat(result).isEqualTo(5);

        assertThat(state.get("counter")).isEqualTo(8L);
    }

    @Test
    void testSetOperations() {
        boolean added = state.addToSet("users", "user1");
        assertThat(added).isTrue();

        added = state.addToSet("users", "user2");
        assertThat(added).isTrue();

        added = state.addToSet("users", "user1");
        assertThat(added).isFalse(); // Already exists

        assertThat(state.containsInSet("users", "user1")).isTrue();
        assertThat(state.containsInSet("users", "user3")).isFalse();

        Set<Object> users = state.getSet("users");
        assertThat(users).containsExactlyInAnyOrder("user1", "user2");

        assertThat(state.setSize("users")).isEqualTo(2);

        boolean removed = state.removeFromSet("users", "user1");
        assertThat(removed).isTrue();
        assertThat(state.setSize("users")).isEqualTo(1);
    }

    @Test
    void testCompute() {
        state.put("value", 5);

        Object result = state.compute("value", (k, v) -> {
            if (v instanceof Number) {
                return ((Number) v).intValue() * 2;
            }
            return v;
        });

        assertThat(result).isEqualTo(10);
        assertThat(state.get("value")).isEqualTo(10);
    }

    @Test
    void testMerge() {
        state.put("sum", 10);

        Object result = state.merge("sum", 5, (oldVal, newVal) -> {
            if (oldVal instanceof Number && newVal instanceof Number) {
                return ((Number) oldVal).intValue() + ((Number) newVal).intValue();
            }
            return newVal;
        });

        assertThat(result).isEqualTo(15);
    }

    @Test
    void testCompareAndSet() {
        state.put("flag", "initial");

        boolean success = state.compareAndSet("flag", "initial", "updated");
        assertThat(success).isTrue();
        assertThat(state.get("flag")).isEqualTo("updated");

        success = state.compareAndSet("flag", "initial", "failed");
        assertThat(success).isFalse();
        assertThat(state.get("flag")).isEqualTo("updated");
    }

    @Test
    void testGetAndSet() {
        state.put("value", "old");

        Object oldValue = state.getAndSet("value", "new");
        assertThat(oldValue).isEqualTo("old");
        assertThat(state.get("value")).isEqualTo("new");
    }

    @Test
    void testConcurrentIncrement() throws InterruptedException {
        int numThreads = 10;
        int incrementsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        state.increment("counter");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(state.get("counter")).isEqualTo((long) (numThreads * incrementsPerThread));
    }

    @Test
    void testConcurrentSetOperations() throws InterruptedException {
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        String element = "thread" + threadId + "_item" + j;
                        if (state.addToSet("items", element)) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(state.setSize("items")).isEqualTo(successCount.get());
    }

    @Test
    void testMixedTypeConversion() {
        // Start with a regular number
        state.put("mixedCounter", 5);
        assertThat(state.get("mixedCounter")).isEqualTo(5);

        // Use atomic operation - should convert to AtomicLong
        long result = state.increment("mixedCounter");
        assertThat(result).isEqualTo(6);

        // Should now return as Long
        assertThat(state.get("mixedCounter")).isEqualTo(6L);

        // Continue with atomic operations
        result = state.addAndGet("mixedCounter", 4);
        assertThat(result).isEqualTo(10);
    }

    @Test
    void testStringToNumberConversion() {
        state.put("stringNumber", "42");
        
        // Should parse string and convert to number
        long result = state.increment("stringNumber");
        assertThat(result).isEqualTo(43);
        
        assertThat(state.get("stringNumber")).isEqualTo(43L);
    }

    @Test
    void testNonSetToSetConversion() {
        // Start with a single value
        state.put("tags", "tag1");
        
        // Add to set should convert single value to set
        state.addToSet("tags", "tag2");
        
        Set<Object> tags = state.getSet("tags");
        assertThat(tags).containsExactlyInAnyOrder("tag1", "tag2");
        assertThat(state.setSize("tags")).isEqualTo(2);
    }
}