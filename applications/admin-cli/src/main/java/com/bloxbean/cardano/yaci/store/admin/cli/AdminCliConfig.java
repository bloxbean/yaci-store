package com.bloxbean.cardano.yaci.store.admin.cli;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.command.annotation.CommandScan;

@Configuration
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.admin.cli", "com.bloxbean.cardano.yaci.store.dbutils",})
@CommandScan
public class AdminCliConfig {
}
