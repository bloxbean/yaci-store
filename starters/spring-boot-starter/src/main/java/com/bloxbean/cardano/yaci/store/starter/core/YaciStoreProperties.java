package com.bloxbean.cardano.yaci.store.starter.core;

import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.config.ScriptRef;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private boolean readOnlyMode = false;
    private String utxoClientUrl;
    private boolean mvstoreEnabled = false;
    private String mvstorePath = "./.mvstore";
    private boolean continueOnParseError = false;

    private Plugins plugins = new Plugins();
    private Metrics metrics = new Metrics();

    @Getter
    @Setter
    public static final class Cardano {
        private String host;
        private int port;
        private long protocolMagic;
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

        private boolean devkitNode;

        private int blockDiffToStartSyncProtocol = 2000;

        private int cursorNoOfBlocksToKeep = 2160;
        private int cursorCleanupInterval = 3600;
        private int keepAliveInterval = 10000;

        //This is only required when the genesis hash can't be fetched from the network.
        // In that case, the default genesis hash will be used
        private String defaultGenesisHash = "Genesis";

        //n2c pool configuration
        private boolean n2cPoolEnabled = false;
        private int n2cPoolMaxTotal = 10;
        private int n2cPoolMinIdle = 2;
        private int n2cPoolMaxIdle = 5;
        private int n2cPoolMaxWaitInMillis = 10000;
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

    @Getter
    @Setter
    public static final class Plugins {
        private boolean enabled = true;
        private boolean exitOnError = true;

        private List<Class> variableProviders = new ArrayList<>();
        private List<ScriptRef> scripts = new ArrayList<>();
        private Map<String, PluginDef> init = new HashMap<>();

        private Map<String, List<PluginDef>> filters = new HashMap<>();
        private Map<String, List<PluginDef>> preActions = new HashMap<>();
        private Map<String, List<PluginDef>> postActions = new HashMap<>();
        private Map<String, List<PluginDef>> eventHandlers = new HashMap<>();

        private PythonSettings python = new PythonSettings();
        private FileSettings files = new FileSettings();
    }

    @Getter
    @Setter
    public static final class PythonSettings {
        private String venv;
    }

    @Getter
    @Setter
    public static final class Metrics {
        boolean enabled = true;
    }

    @Getter
    @Setter
    public static final class FileSettings {
        private String rootPath = "./plugins/files";
        private boolean enableLocks = true;
    }
}
