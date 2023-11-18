package com.bloxbean.cardano.yaci.store.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@EnableAdminServer
public class YaciStoreAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(YaciStoreAdminApplication.class, args);
    }
}
