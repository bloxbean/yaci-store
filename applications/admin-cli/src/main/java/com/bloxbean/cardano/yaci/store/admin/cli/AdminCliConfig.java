package com.bloxbean.cardano.yaci.store.admin.cli;

import com.bloxbean.cardano.yaci.store.admin.cli.common.CommandFailedException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.shell.command.annotation.CommandScan;

@Configuration
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.admin.cli", "com.bloxbean.cardano.yaci.store.dbutils",})
@CommandScan
public class AdminCliConfig {

    /** A command that already printed its failure exits non-zero and prints nothing more. */
    @Bean
    CommandExceptionResolver commandFailedResolver() {
        return e -> e instanceof CommandFailedException
                ? CommandHandlingResult.of(null, CommandFailedException.EXIT_CODE)
                : null;
    }
}
