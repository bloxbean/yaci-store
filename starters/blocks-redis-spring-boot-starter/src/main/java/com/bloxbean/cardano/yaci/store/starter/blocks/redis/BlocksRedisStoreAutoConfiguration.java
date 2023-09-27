package com.bloxbean.cardano.yaci.store.starter.blocks.redis;

import com.bloxbean.cardano.yaci.store.blocks.redis.BlocksRedisStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(BlocksRedisStoreProperties.class)
@Import(BlocksRedisStoreConfiguration.class)
@Slf4j
public class BlocksRedisStoreAutoConfiguration {

    @Autowired
    BlocksRedisStoreProperties properties;
}
