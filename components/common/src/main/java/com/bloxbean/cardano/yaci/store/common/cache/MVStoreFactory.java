package com.bloxbean.cardano.yaci.store.common.cache;

import org.h2.mvstore.MVStore;

public class MVStoreFactory {
    private boolean isInitialized = false;
    private MVStore store;

    private MVStoreFactory() {
    }

    public synchronized void init(String dbPath) {
        if (isInitialized)
            throw new IllegalStateException("MVStore is already initialized");

        store = new MVStore.Builder().fileName(dbPath).compress().open();
        isInitialized = true;
    }

    public MVStore getStore() {
        return store;
    }

    public synchronized void close() {
        if(store != null && !store.isClosed()) {
            store.close();
            store = null;
        }
    }

    private static class SingletonHelper {
        private static final MVStoreFactory INSTANCE = new MVStoreFactory();
    }

    public static MVStoreFactory getInstance() {
        return SingletonHelper.INSTANCE;
    }
}
