package com.bloxbean.cardano.yaci.store.admin.cli.common;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringBootExceptionReporter;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Spring Shell ends a non-interactive run with a non-zero exit code by throwing an exception that
 * carries it. The command has already printed its failure, so report that exception as handled and
 * keep Spring Boot from logging it as "Application run failed" with a stack trace. Every other
 * startup failure is still logged.
 */
public class ExitCodeExceptionReporter implements SpringBootExceptionReporter {

    public ExitCodeExceptionReporter(ConfigurableApplicationContext context) {
    }

    @Override
    public boolean reportException(Throwable failure) {
        return failure instanceof ExitCodeGenerator
                && failure.getClass().getName().startsWith("org.springframework.shell.");
    }
}
