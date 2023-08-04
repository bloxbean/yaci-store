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
    private long eventPublisherId = 1;
    private boolean syncAutoStart = true;
    private String utxoClientUrl;

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
    }

    @Getter
    @Setter
    public static final class Core {
        private boolean enabled = true;
    }
}
