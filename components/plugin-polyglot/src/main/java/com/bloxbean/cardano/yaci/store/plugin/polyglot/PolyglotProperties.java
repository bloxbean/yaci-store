package com.bloxbean.cardano.yaci.store.plugin.polyglot;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "store.plugins.polyglot", ignoreUnknownFields = true)
public class PolyglotProperties {
    private int poolMaxTotalPerKey = 30;
    private int poolMaxIdlePerKey = 20;
    private int poolMinIdlePerKey = 20;
    private boolean poolTestOnBorrow = true;
    private boolean poolTestOnReturn = true;
}
