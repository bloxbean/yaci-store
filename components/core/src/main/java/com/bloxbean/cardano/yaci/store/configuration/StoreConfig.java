package com.bloxbean.cardano.yaci.store.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreConfig {
    @Value("${store.cardano.sync-start-slot:0}")
    private long syncStartSlot;
    @Value("${store.cardano.sync-start-blockhash:null}")
    private String syncStartBlockHash;

    @Value("${store.cardano.sync-stop-slot:0}")
    private long syncStopSlot;
    @Value("${store.cardano.sync-stop-blockhash:null}")
    private String syncStopBlockHash;

    private boolean primaryInstance;

    public long getSyncStartSlot() {
        return syncStartSlot;
    }

    public void setSyncStartSlot(long syncStartSlot) {
        this.syncStartSlot = syncStartSlot;
    }

    public String getSyncStartBlockHash() {
        return syncStartBlockHash;
    }

    public void setSyncStartBlockHash(String syncStartBlockHash) {
        this.syncStartBlockHash = syncStartBlockHash;
    }

    public long getSyncStopSlot() {
        return syncStopSlot;
    }

    public void setSyncStopSlot(long syncStopSlot) {
        this.syncStopSlot = syncStopSlot;
    }

    public String getSyncStopBlockHash() {
        return syncStopBlockHash;
    }

    public void setSyncStopBlockHash(String syncStopBlockHash) {
        this.syncStopBlockHash = syncStopBlockHash;
    }

    public void setPrimaryInstance(boolean flag) {
        this.primaryInstance = flag;
    }

    public boolean getPrimaryInstance() {
        return primaryInstance;
    }
}
