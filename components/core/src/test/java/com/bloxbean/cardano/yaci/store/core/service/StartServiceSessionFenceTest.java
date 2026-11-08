package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StartServiceSessionFenceTest {

    @Test
    void startWaitsForCommitsOfTheStoppedSessionBeforeReadingTheCursor() {
        BlockFetchService blockFetchService = mock(BlockFetchService.class);
        CursorService cursorService = mock(CursorService.class);
        InitService initService = mock(InitService.class);
        when(cursorService.getStartCursor()).thenThrow(new IllegalStateException("stop here"));
        StartService startService = new StartService(mock(ApplicationEventPublisher.class), blockFetchService,
                mock(TipFinderService.class), mock(GenesisBlockFinder.class), cursorService, initService,
                new StoreProperties(), mock(GenesisConfig.class));

        assertThatThrownBy(startService::start).hasMessage("stop here");

        var order = inOrder(blockFetchService, initService, cursorService);
        order.verify(blockFetchService).awaitCommitsOfOldSessions();
        order.verify(initService).init();
        order.verify(cursorService).getStartCursor();
    }

    @Test
    void startFailsWhileACommitOfTheStoppedSessionIsStillRunning() {
        BlockFetchService blockFetchService = mock(BlockFetchService.class);
        CursorService cursorService = mock(CursorService.class);
        doThrow(new IllegalStateException("commit still running")).when(blockFetchService).awaitCommitsOfOldSessions();
        StartService startService = new StartService(mock(ApplicationEventPublisher.class), blockFetchService,
                mock(TipFinderService.class), mock(GenesisBlockFinder.class), cursorService, mock(InitService.class),
                new StoreProperties(), mock(GenesisConfig.class));

        assertThatThrownBy(startService::start).hasMessage("commit still running");

        verify(cursorService, never()).getStartCursor();
    }
}
