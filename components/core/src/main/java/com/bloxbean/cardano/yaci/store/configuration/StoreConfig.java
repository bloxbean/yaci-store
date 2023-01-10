package com.bloxbean.cardano.yaci.store.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreConfig {
    @Value("${cardano.sync_start_slot:0}")
    private long syncStartSlot;
    @Value("${cardano.sync_start_blockhash:null}")
    private String syncStartBlockHash;

    @Value("${cardano.sync_stop_slot:0}")
    private long syncStopSlot;
    @Value("${cardano.sync_stop_blockhash:null}")
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
