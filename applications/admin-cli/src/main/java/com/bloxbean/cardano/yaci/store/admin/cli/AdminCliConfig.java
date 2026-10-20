package com.bloxbean.cardano.yaci.store.admin.cli;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.NonInteractiveShellRunner;
import org.springframework.shell.core.ShellRunner;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;

@Configuration
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.admin.cli", "com.bloxbean.cardano.yaci.store.dbutils",})
public class AdminCliConfig {

    @Bean(name = "springShellApplicationRunner")
    public AdminCliShellRunner springShellApplicationRunner(ShellRunner shellRunner, CommandParser commandParser,
                                                            CommandRegistry commandRegistry) {
        return new AdminCliShellRunner(shellRunner, new NonInteractiveShellRunner(commandParser, commandRegistry));
    }
}
