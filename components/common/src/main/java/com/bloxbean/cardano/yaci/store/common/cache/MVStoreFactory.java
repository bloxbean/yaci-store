package com.bloxbean.cardano.yaci.store.common.cache;

import org.h2.mvstore.MVStore;

public class MVStoreFactory {
    private boolean isInitialized = false;
    private MVStore store;

    private static MVStoreFactory instance;

    private MVStoreFactory() {
    }

    public static MVStoreFactory getInstance() {
        if(instance == null) {
            instance = new MVStoreFactory();
        }
        return instance;
    }

    public synchronized void init(String dbPath) {
        if(isInitialized)
            throw new IllegalStateException("MVStore is already initialized");

        store = new MVStore.Builder().fileName(dbPath).compress().open();
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
}
