package com.bloxbean.cardano.yaci.indexer;

import com.bloxbean.cardano.yaci.core.helpers.TipFinder;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
//import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class YaciIndexerConfiguration {
    @Value("${cardano.host}")
    private String host;
    @Value("${cardano.port}")
    private int port;
    @Value("${cardano.protocol_magic}")
    private long protocolMagic;
//    @Value("${cardano.known_slot}")
//    private long knownSlot;
//    @Value("${cardano.known_block_hash}")
//    private String knownBlockHash;

//    private TipFinder tipFinder;
//    private BlockRangeSync blockRangeSync;
//    private BlockSync blockSync;

    @Bean
    public TipFinder tipFinder() {
        TipFinder tipFinder = new TipFinder(host, port, Point.ORIGIN, protocolMagic);
        tipFinder.start();
        return tipFinder;
    }

    @Bean
    @Scope("prototype")
    public BlockRangeSync blockRangeSync() {
        System.out.println("Creating new blockFetcher >>>");
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

//    @Bean
//    JedisConnectionFactory jedisConnectionFactory() {
//        return new JedisConnectionFactory();
//    }

////    @Bean
//    public LettuceConnectionFactory redisConnectionFactory() {
//        LettuceConnectionFactory lcf = new LettuceConnectionFactory();
////        lcf.setHostName("your_host_name_or_ip");
////        lcf.setPort(6379);
//        lcf.afterPropertiesSet();
//        return lcf;
//    }
//
////    @Bean
//    public RedisTemplate<String, Object> redisTemplate() {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(redisConnectionFactory());
//        return template;
//    }
}
