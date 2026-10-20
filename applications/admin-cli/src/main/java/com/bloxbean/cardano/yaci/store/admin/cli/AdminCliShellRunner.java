package com.bloxbean.cardano.yaci.store.admin.cli;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.shell.core.ShellRunner;

/**
 * Runs a command given on the command line ({@code admin-cli rollback-data --epoch 450}) and exits, or opens the
 * interactive shell when there are no arguments, as Spring Shell 3 did. Spring Shell 4 picks one runner from
 * {@code spring.shell.interactive.enabled} and ignores the arguments in interactive mode.
 * Registered as {@code springShellApplicationRunner}, which replaces Spring Shell's own runner.
 */
public class AdminCliShellRunner implements ApplicationRunner {
    private final ShellRunner interactiveRunner;
    private final ShellRunner commandRunner;

    public AdminCliShellRunner(ShellRunner interactiveRunner, ShellRunner commandRunner) {
        this.interactiveRunner = interactiveRunner;
        this.commandRunner = commandRunner;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String[] sourceArgs = args.getSourceArgs();
        if (sourceArgs.length > 0)
            commandRunner.run(sourceArgs);
        else
            interactiveRunner.run(sourceArgs);
    }
}
