package com.bloxbean.cardano.yaci.indexer;

import com.bloxbean.cardano.yaci.core.helpers.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.core.helpers.TipFinder;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@Slf4j
public class YaciIndexerConfiguration {
    @Value("${cardano.host}")
    private String host;
    @Value("${cardano.port}")
    private int port;
    @Value("${cardano.protocol.magic}")
    private long protocolMagic;
    @Value("${cardano.node.socket.path:''}")
    private String nodeSocketPath;

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
    @ConditionalOnProperty(prefix = "cardano", name = "node.socket.path")
    public LocalStateQueryClient localStateQueryClient() {
        log.info("LocalStateQueryClient ---> Configured");
        return new LocalStateQueryClient(nodeSocketPath, protocolMagic);
    }

}
