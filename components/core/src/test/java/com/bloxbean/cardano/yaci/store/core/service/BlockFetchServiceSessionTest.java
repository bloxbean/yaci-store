package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Only the current sync session may write (#1129, #1328 findings 2 and 4).
 */
class BlockFetchServiceSessionTest {
    private static final String START_HASH = "h100";

    private BlockSync blockSync;
    private CursorService cursorService;
    private ShelleyBlockEventPublisher shelleyPublisher;
    private SyncSessionGuard guard;
    private BlockFetchService blockFetchService;

    @BeforeEach
    void setUp() {
        blockSync = mock(BlockSync.class);
        cursorService = mock(CursorService.class);
        shelleyPublisher = mock(ShelleyBlockEventPublisher.class);
        EraService eraService = mock(EraService.class);
        StoreProperties storeProperties = mock(StoreProperties.class);
        when(storeProperties.getKeepAliveInterval()).thenReturn(60_000);
        when(eraService.getEpochNo(eq(Era.Conway), anyLong())).thenReturn(100);

        blockFetchService = new BlockFetchService(
                mock(ApplicationEventPublisher.class),
                mock(MetricsService.class),
                mock(BlockRangeSync.class),
                blockSync,
                cursorService,
                eraService,
                storeProperties,
                mock(GenesisConfig.class),
                shelleyPublisher,
                mock(ByronBlockEventPublisher.class),
                mock(ErrorStorage.class));
        guard = new SyncSessionGuard();
        blockFetchService.setSessionGuard(guard);

        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 100);
        ReflectionTestUtils.setField(blockFetchService, "previousEra", Era.Conway);
    }

    @AfterEach
    void tearDown() {
        blockFetchService.stop();
    }

    @Test
    void blockFromAnOldSessionIsIgnored() {
        BlockChainDataListener oldSession = startSyncAndGetListener();
        BlockChainDataListener newSession = startSyncAndGetListener();

        Block lateBlock = block(101, "h101", START_HASH);
        oldSession.onBlock(Era.Conway, lateBlock, List.of());
        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(lateBlock), any());

        Block block = block(101, "h101", START_HASH);
        newSession.onBlock(Era.Conway, block, List.of());
        verify(shelleyPublisher).publishBlockEvents(any(), eq(block), any());
    }

    @Test
    void rollbackFromAStoppedSessionIsIgnored() {
        BlockChainDataListener session = startSyncAndGetListener();
        blockFetchService.stop();

        session.onRollback(new Point(90, "h90"));

        verify(cursorService, never()).rollback(anyLong());
    }

    @Test
    void lateWriteFromAnOldSessionDoesNotStopTheCurrentOne() {
        //The old session's thread was inside the block when a restart started a new session. The publisher refuses
        //to commit; that must not be treated as a failure of the current session.
        BlockChainDataListener session = startSyncAndGetListener();
        doAnswer(inv -> {
            guard.newSession(); //a restart happens while this block is being written
            guard.checkCaller();
            return null;
        }).when(shelleyPublisher).publishBlockEvents(any(), any(), any());

        assertThatThrownBy(() -> session.onBlock(Era.Conway, block(101, "h101", START_HASH), List.of()))
                .isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);

        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void anyErrorInAnOldSessionsThreadDoesNotStopTheCurrentOne() {
        //e.g. the shared batch list was reset by the new session while the old thread waited
        BlockChainDataListener session = startSyncAndGetListener();
        doAnswer(inv -> {
            guard.newSession();
            throw new java.util.NoSuchElementException("batch emptied by the new session");
        }).when(shelleyPublisher).publishBlockEvents(any(), any(), any());

        assertThatThrownBy(() -> session.onBlock(Era.Conway, block(101, "h101", START_HASH), List.of()))
                .isInstanceOf(java.util.NoSuchElementException.class);

        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void oldSessionCannotStartAnotherSession() {
        //e.g. an old range session that finishes a long batchDone after a restart already started a new session
        startSyncAndGetListener();
        long oldSession = guard.currentSession();
        guard.newSession();

        guard.enter(oldSession);
        try {
            blockFetchService.startSync(new Point(100, START_HASH), 100L);
        } finally {
            guard.exit();
        }

        verify(blockSync, times(1)).startSync(any(), any());
    }

    private BlockChainDataListener startSyncAndGetListener() {
        clearInvocations(blockSync);
        when(cursorService.getCursor()).thenReturn(Optional.of(
                Cursor.builder().slot(100L).block(100L).blockHash(START_HASH).prevBlockHash("h99").era(Era.Conway).build()));
        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        var captor = ArgumentCaptor.forClass(BlockChainDataListener.class);
        verify(blockSync).startSync(any(), captor.capture());
        return captor.getValue();
    }

    private Block block(long number, String hash, String prevHash) {
        Block block = mock(Block.class, RETURNS_DEEP_STUBS);
        when(block.getHeader().getHeaderBody().getBlockNumber()).thenReturn(number);
        when(block.getHeader().getHeaderBody().getSlot()).thenReturn(number);
        when(block.getHeader().getHeaderBody().getBlockHash()).thenReturn(hash);
        when(block.getHeader().getHeaderBody().getPrevHash()).thenReturn(prevHash);
        when(block.getHeader().getHeaderBody().getIssuerVkey()).thenReturn("a".repeat(64));
        return block;
    }
}
