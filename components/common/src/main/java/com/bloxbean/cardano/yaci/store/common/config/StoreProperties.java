package com.bloxbean.cardano.yaci.store.common.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoreProperties { //TODO - replace this with YaciStoreProperties from starter
    private Long eventPublisherId;
    private boolean syncAutoStart;
    private boolean readOnlyMode;
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
    private String alonzoGenesisFile;
    private String conwayGenesisFile;

    //For yaci devkit integration
    private boolean devkitNode;

    @Builder.Default
    private int blockDiffToStartSyncProtocol = 2000;

    //Cursor table cleanup properties
    @Builder.Default
    private int cursorNoOfBlocksToKeep = 2160;

    @Builder.Default
    private int cursorCleanupInterval = 3600;

    private int keepAliveInterval = 10000;

    //Only required if the genesis hash can't be fetched
    private String defaultGenesisHash = "Genesis";

    //derived from protocol magic. No need to set
    private boolean mainnet;

    //Executor Configurations
    @Builder.Default
    private int blockProcessingThreads = 15;

    @Builder.Default
    private int eventProcessingThreads = 30;

    private boolean enableParallelProcessing;

    @Builder.Default
    private int blocksBatchSize=100;

    @Builder.Default
    private int blocksPartitionSize=15;
    private boolean useVirtualThreadForBatchProcessing;
    private boolean useVirtualThreadForEventProcessing;

    private int dbBatchSize = 200;
    private boolean dbParallelInsert = true;

    @Builder.Default
    private boolean mvstoreEnabled = false;
    @Builder.Default
    private String mvstorePath = "./.mvstore";

    @Builder.Default
    private int processingThreadsTimeout = 5; //5 min

    @Builder.Default
    private boolean parallelWrite = false;
    @Builder.Default
    private int writeThreadDefaultBatchSize = 1000;
    @Builder.Default
    private int jooqWriteBatchSize = 3000;
    @Builder.Default
    private int writeThreadCount = 5;

    //n2c pool configuration
    @Builder.Default
    private boolean n2cPoolEnabled = false;
    @Builder.Default
    private int n2cPoolMaxTotal = 10;
    @Builder.Default
    private int n2cPoolMinIdle = 2;
    @Builder.Default
    private int n2cPoolMaxIdle = 5;
    @Builder.Default
    private int n2cPoolMaxWaitInMillis = 10000;
}
