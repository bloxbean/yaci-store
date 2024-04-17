package com.bloxbean.cardano.yaci.store.starter.core;

import com.bloxbean.cardano.yaci.core.common.Constants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class YaciStoreProperties {
    private Core core = new Core();
    private Cardano cardano = new Cardano();
    private Executor executor = new Executor();
    private Db db = new Db();
    private long eventPublisherId = 1;
    private boolean syncAutoStart = true;
    private String utxoClientUrl;
    private boolean mvstoreEnabled = false;
    private String mvstorePath = "./.mvstore";

    @Getter
    @Setter
    public static final class Cardano {
        private String host = Constants.MAINNET_IOHK_RELAY_ADDR;
        private int port = Constants.MAINNET_IOHK_RELAY_PORT;
        private long protocolMagic = Constants.MAINNET_PROTOCOL_MAGIC;
        private String n2cNodeSocketPath;
        private String n2cHost;
        private int n2cPort;
        private String submitApiUrl;
        private String ogmiosUrl;
        private String mempoolMonitoringEnabled;

        private long shelleyStartSlot;
        private String shelleyStartBlockhash;
        private long shelleyStartBlock;

        private long syncStartSlot;
        private String syncStartBlockhash;
        private long syncStartByronBlockNumber;

        private long syncStopSlot;
        private String syncStopBlockhash;

        private String byronGenesisFile;
        private String shelleyGenesisFile;
        private String alonzoGenesisFile;
        private String conwayGenesisFile;

        private int blockDiffToStartSyncProtocol = 2000;

        private int cursorNoOfBlocksToKeep = 2160;
        private int cursorCleanupInterval = 3600;
        private int keepAliveInterval = 10000;

        //This is only required when the genesis hash can't be fetched from the network.
        // In that case, the default genesis hash will be used
        private String defaultGenesisHash = "Genesis";
    }

    @Getter
    @Setter
    public static final class Core {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static final class Executor {
        private boolean enableParallelProcessing;
        private int blockProcessingThreads = 15;
        private int eventProcessingThreads = 30;
        private int blocksBatchSize=100;
        private int blocksPartitionSize=10;
        private boolean useVirtualThreadForBatchProcessing;
        private boolean useVirtualThreadForEventProcessing;
        /**
         * Timeout in seconds for processing threads
         */
        private int processingThreadsTimeout = 5;
    }

    @Getter
    @Setter
    public static final class Db {
        private int batchSize = 200;
        private boolean parallelInsert = true;

        //parallel write & batch size settings
        private boolean parallelWrite = false;
        private int writeThreadDefaultBatchSize = 1000;
        private int jooqWriteBatchSize = 3000;
        private int writeThreadCount = 5;
    }
}
