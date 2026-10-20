package com.bloxbean.cardano.yaci.store.admin.cli;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.admin.cli", "com.bloxbean.cardano.yaci.store.dbutils",})
public class AdminCliConfig {
}
