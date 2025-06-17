package com.bloxbean.cardano.yaci.store.plugin.util;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class Locker {
    private final Object defaultLock = new Object();

    public <T> T syncAndGet(Supplier<T> supplier) {
        return syncAndReturn(supplier, defaultLock);
    }

    public <T> T syncAndReturn(Supplier<T> supplier, Object lock) {
        synchronized (lock) {
            return supplier.get();
        }
    }

    public void sync(Runnable runnable) {
        sync(runnable, defaultLock);
    }

    public void sync(Runnable runnable, Object lock) {
        synchronized (lock) {
            runnable.run();
        }
    }


}

