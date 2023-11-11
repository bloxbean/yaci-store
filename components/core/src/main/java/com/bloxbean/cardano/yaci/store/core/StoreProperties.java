package com.bloxbean.cardano.yaci.store.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoreProperties { //TODO - replace this with YaciStoreProperties from starter
    private Long eventPublisherId;
    private boolean syncAutoStart;
    private boolean primaryInstance;

    private String cardanoHost;
    private int cardanoPort;
    private long protocolMagic;
    private String n2cNodeSocketPath;
    private String n2cHost;
    private int n2cPort;

    private long shelleyStartSlot;
    private String shelleyStartBlockhash;
    private long shelleyStartBlock;

    private String submitApiUrl;
    private String ogmiosUrl;
    private String mempoolMonitoringEnabled;

    private long syncStartSlot;
    private String syncStartBlockhash;
    private long syncStartByronBlockNumber;

    private long syncStopSlot;
    private String syncStopBlockhash;

    private String byronGenesisFile;
    private String shelleyGenesisFile;

    private int blockDiffToStartSyncProtocol = 2000;

    //Cursor table cleanup properties
    private int cursorNoOfBlocksToKeep = 2160;
    private int cursorCleanupInterval = 3600;

    private int keepAliveInterval = 10000;

    //Only required if the genesis hash can't be fetched
    private String defaultGenesisHash = "Genesis";

    //derived from protocol magic. No need to set
    private boolean mainnet;

    //Executor Configurations
    private int blockProcessingThreads = 15;
    private int eventProcessingThreads = 30;
    private boolean enableParallelProcessing;
    private int blocksBatchSize=100;
    private int blocksPartitionSize=15;
    private boolean useVirtualThreadForBatchProcessing;
    private boolean useVirtualThreadForEventProcessing;
}
