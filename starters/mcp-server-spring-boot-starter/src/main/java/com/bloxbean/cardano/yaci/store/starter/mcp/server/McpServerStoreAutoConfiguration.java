package com.bloxbean.cardano.yaci.store.starter.mcp.server;

import com.bloxbean.cardano.yaci.store.mcp.server.StoreMcpServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(McpServerStoreAutoConfigProperties.class)
@Import({StoreMcpServerConfig.class})
@Slf4j
public class McpServerStoreAutoConfiguration {

    @Autowired
    McpServerStoreAutoConfigProperties properties;

}
