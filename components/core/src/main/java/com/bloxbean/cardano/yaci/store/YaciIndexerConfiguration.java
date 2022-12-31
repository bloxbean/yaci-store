package com.bloxbean.cardano.yaci.store;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableScheduling
@Slf4j
public class YaciIndexerConfiguration {
    @Value("${cardano.host}")
    private String host;
    @Value("${cardano.port}")
    private int port;
    @Value("${cardano.protocol.magic}")
    private long protocolMagic;
    @Value("${cardano.n2c.node.socket.path:''}")
    private String n2cNodeSocketPath;
    @Value("${cardano.n2c.host:''}")
    private String n2cNodeSocketHost;
    @Value("${cardano.n2c.port:0}")
    private int n2cNodeSocketPort;

    @Bean
    public TipFinder tipFinder() {
        TipFinder tipFinder = new TipFinder(host, port, Point.ORIGIN, protocolMagic);
        tipFinder.start();
        return tipFinder;
    }

    @Bean
    @Scope("prototype")
    public BlockRangeSync blockRangeSync() {
        log.info("Creating BlockRangeSync to fetch blocks");
        BlockRangeSync blockRangeSync = new BlockRangeSync(host, port, protocolMagic);
        return blockRangeSync;
    }

    @Bean
    public BlockSync blockSync() {
        BlockSync blockSync = new BlockSync(host, port, protocolMagic, Point.ORIGIN);
        return blockSync;
    }

    @Bean
    public GenesisBlockFinder genesisBlockFinder() {
        GenesisBlockFinder genesisBlockFinder = new GenesisBlockFinder(host, port, protocolMagic);
        return genesisBlockFinder;
    }

    @Bean
    @ConditionalOnProperty(prefix = "cardano", name = "n2c.node.socket.path")
    @Primary
    public LocalClientProvider localClientProviderNodeSocketPath() {
        log.info("LocalStateQueryClient ---> Configured --> " + n2cNodeSocketPath);
        return new LocalClientProvider(n2cNodeSocketPath, protocolMagic);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cardano", name = "n2c.host")
    public LocalClientProvider localClientProviderNodeSocketPort() {
        if (n2cNodeSocketPort == 0)
            throw new IllegalArgumentException("Invalid cardano.n2c.port " + n2cNodeSocketPort);
        log.info("LocalStateQueryClient ---> Configured (n2c host/port)--> " + n2cNodeSocketHost + ", " + n2cNodeSocketPort);
        return new LocalClientProvider(n2cNodeSocketHost, n2cNodeSocketPort);

    }

}
