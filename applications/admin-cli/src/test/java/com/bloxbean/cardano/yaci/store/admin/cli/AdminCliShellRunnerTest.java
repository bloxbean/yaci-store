package com.bloxbean.cardano.yaci.store.admin.cli;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.shell.core.ShellRunner;

import static org.mockito.Mockito.*;

class AdminCliShellRunnerTest {
    private final ShellRunner interactive = mock(ShellRunner.class);
    private final ShellRunner command = mock(ShellRunner.class);
    private final AdminCliShellRunner runner = new AdminCliShellRunner(interactive, command);

    @Test
    void runsTheCommandGivenOnTheCommandLine() throws Exception {
        String[] args = {"rollback-data", "--epoch", "450"};

        runner.run(new DefaultApplicationArguments(args));

        verify(command).run(args);
        verifyNoInteractions(interactive);
    }

    @Test
    void quotesArgumentsWithSpaces() throws Exception {
        runner.run(new DefaultApplicationArguments("rollback-data", "--rollback-files", "file:/my dir/r.yml"));

        verify(command).run(new String[]{"rollback-data", "--rollback-files", "\"file:/my dir/r.yml\""});
    }

    @Test
    void opensTheShellWithoutArguments() throws Exception {
        runner.run(new DefaultApplicationArguments());

        verify(interactive).run(new String[0]);
        verifyNoInteractions(command);
    }
}
