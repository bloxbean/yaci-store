package com.bloxbean.cardano.yaci.store.starter.utxo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class UtxoStoreProperties {
    private Utxo utxo;

    @Getter
    @Setter
    public static final class Utxo {
       private boolean enabled = true;
       private boolean apiEnabled = true;
    }

}
