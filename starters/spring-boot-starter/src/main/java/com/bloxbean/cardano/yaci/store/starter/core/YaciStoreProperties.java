package com.bloxbean.cardano.yaci.store.starter.core;

import com.bloxbean.cardano.yaci.core.common.Constants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class YaciStoreProperties {

    private Cardano cardano = new Cardano();
    private int eventPublisherId = 1;
    private boolean syncAutoStart = true;

    public Cardano getCardano() {
        return cardano;
    }

    public void setCardano(Cardano cardano) {
        this.cardano = cardano;
    }

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

        private long syncStartSlot;
        private String syncStartBlockhash;
        private String syncStopBlockhash;
    }

}
