package com.bloxbean.cardano.yaci.store.starter.mcp.server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class McpServerStoreAutoConfigProperties {
    private McpServer account = new McpServer();

    @Getter
    @Setter
    public static final class McpServer {
        private boolean enabled = false;
    }

}
