package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.exception.BlockParseRuntimeException;
import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import com.bloxbean.cardano.yaci.store.events.ErrorEvent;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Paths that could skip a block without leaving any trace (issue #1308). Blocks are delivered the way
 * yaci 0.4.7's BlockfetchAgent does it: an exception thrown by onBlock is reported back as a parse error.
 */
class BlockFetchServiceBlockSkipTest {
    private static final String ISSUER_VKEY = "a".repeat(64);
    private static final String START_HASH = "h100";

    private ApplicationEventPublisher publisher;
    private BlockSync blockSync;
    private CursorService cursorService;
    private EraService eraService;
    private StoreProperties storeProperties;
    private ShelleyBlockEventPublisher shelleyPublisher;
    private ErrorStorage errorStorage;
    private BlockFetchService blockFetchService;

    @BeforeEach
    void setUp() {
        publisher = mock(ApplicationEventPublisher.class);
        blockSync = mock(BlockSync.class);
        cursorService = mock(CursorService.class);
        eraService = mock(EraService.class);
        storeProperties = mock(StoreProperties.class);
        shelleyPublisher = mock(ShelleyBlockEventPublisher.class);
        errorStorage = mock(ErrorStorage.class);

        when(storeProperties.isEnableParallelProcessing()).thenReturn(true);
        when(storeProperties.isContinueOnParseError()).thenReturn(true);
        when(storeProperties.getKeepAliveInterval()).thenReturn(60_000);
        when(eraService.getEpochNo(eq(Era.Conway), anyLong())).thenReturn(100);

        blockFetchService = new BlockFetchService(
                publisher,
                mock(MetricsService.class),
                mock(BlockRangeSync.class),
                blockSync,
                cursorService,
                eraService,
                storeProperties,
                mock(GenesisConfig.class),
                shelleyPublisher,
                mock(ByronBlockEventPublisher.class),
                errorStorage);

        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 100);
        ReflectionTestUtils.setField(blockFetchService, "previousEra", Era.Conway);
    }

    @AfterEach
    void tearDown() {
        blockFetchService.stop();
    }

    @Test
    void blockArrivingBeforeStartSyncReturnsIsWrittenNotLeftInABatch() {
        Block first = block(101, "h101", START_HASH);
        //BlockSync.startSync waits for the handshake. The node can deliver the first block during that wait.
        doAnswer(inv -> {
            deliver(first);
            return null;
        }).when(blockSync).startSync(any(), any());

        blockFetchService.startSync(new Point(100, START_HASH));

        verify(shelleyPublisher).publishBlockEvents(any(), eq(first), any());
        verify(shelleyPublisher, never()).publishBlockEventsInParallel(any(), any(), any());
    }

    @Test
    void startSyncDropsBlocksLeftInABatchByRangeFetch() {
        blockFetchService.startSync(new Point(100, START_HASH));

        //They are fetched again from the cursor, so flushing them later would write them twice
        verify(shelleyPublisher).reset();
    }

    @Test
    void blockLeftInABatchDuringSyncIsCaughtByTheNextBlock() {
        blockFetchService.startSync(new Point(100, START_HASH));

        //The original startup race: the first block is seen with syncMode still false and goes to a batch
        ReflectionTestUtils.setField(blockFetchService, "syncMode", false);
        deliver(block(101, "h101", START_HASH));
        ReflectionTestUtils.setField(blockFetchService, "syncMode", true);

        Block next = block(102, "h102", "h101");
        deliver(next);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(next), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void databaseFailureBeforeSavingStopsSyncInsteadOfSkippingTheBlock() {
        blockFetchService.startSync(new Point(100, START_HASH));
        when(eraService.checkIfNewEra(any(), any()))
                .thenThrow(new DataAccessResourceFailureException("connection refused"))
                .thenReturn(false);

        Block lost = block(101, "h101", START_HASH);
        Block next = block(102, "h102", "h101");
        deliver(lost);
        deliver(next);

        assertThat(blockFetchService.isError()).isTrue();
        verify(blockSync, atLeastOnce()).stop();
        verify(shelleyPublisher, never()).publishBlockEvents(any(), any(), any());
        verify(errorStorage, never()).save(any());
        verify(publisher, never()).publishEvent(any(ErrorEvent.class));
    }

    @Test
    void blockThatDoesNotBuildOnThePreviousBlockIsNotWritten() {
        blockFetchService.startSync(new Point(100, START_HASH));

        Block linked = block(101, "h101", START_HASH);
        Block afterGap = block(103, "h103", "h102");
        deliver(linked);
        deliver(afterGap);

        verify(shelleyPublisher).publishBlockEvents(any(), eq(linked), any());
        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(afterGap), any());
        assertThat(blockFetchService.isError()).isTrue();
        verify(publisher).publishEvent(any(RequiredSyncRestartEvent.class));
    }

    @Test
    void firstBlockAfterStartMustBuildOnTheStartPoint() {
        blockFetchService.startSync(new Point(100, START_HASH));

        Block afterGap = block(102, "h102", "h101");
        deliver(afterGap);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), any(), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void firstBlockAfterRollbackMustBuildOnTheRollbackPoint() {
        blockFetchService.startSync(new Point(100, START_HASH));
        deliver(block(101, "h101", START_HASH));
        deliver(block(102, "h102", "h101"));

        when(cursorService.getCursor()).thenReturn(Optional.of(cursor(102, "h102")), Optional.of(cursor(101, "h101")));
        blockFetchService.onRollback(new Point(101, "h101"));

        Block fork = block(102, "h102b", "h101");
        deliver(fork);

        verify(shelleyPublisher).publishBlockEvents(any(), eq(fork), any());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void rollbackToABlockThatWasNeverStoredIsNotTrusted() {
        blockFetchService.startSync(new Point(100, START_HASH));
        deliver(block(101, "h101", START_HASH));

        //yaci rolls back to a block it has but the store does not (cursor is still at 101)
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor(101, "h101")));
        blockFetchService.onRollback(new Point(102, "h102"));

        Block afterGap = block(103, "h103", "h102");
        deliver(afterGap);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(afterGap), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void parseErrorSkipsExactlyOneRecordedBlock() {
        blockFetchService.startSync(new Point(100, START_HASH));
        deliver(block(101, "h101", START_HASH));

        blockFetchService.onParsingError(new BlockParseRuntimeException(102L, null, new RuntimeException("bad cbor")));
        verify(errorStorage).save(any(ErrorEvent.class));

        //The skipped block's hash is unknown, so the next block is accepted without the link check
        Block afterSkip = block(103, "h103", "h102");
        deliver(afterSkip);
        verify(shelleyPublisher).publishBlockEvents(any(), eq(afterSkip), any());

        //The check resumes from there
        Block afterGap = block(105, "h105", "h104");
        deliver(afterGap);
        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(afterGap), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void parseErrorThatCannotBeRecordedStopsSync() {
        blockFetchService.startSync(new Point(100, START_HASH));
        doThrow(new DataAccessResourceFailureException("connection refused")).when(errorStorage).save(any());

        assertThatThrownBy(() -> blockFetchService.onParsingError(
                new BlockParseRuntimeException(101L, null, new RuntimeException("bad cbor"))))
                .isInstanceOf(RuntimeException.class);

        assertThat(blockFetchService.isError()).isTrue();
    }

    //Same dispatch as yaci 0.4.7 BlockfetchAgent: an exception from the listener becomes onParsingError
    private void deliver(Block block) {
        try {
            blockFetchService.onBlock(Era.Conway, block, List.of());
        } catch (Exception e) {
            blockFetchService.onParsingError(
                    new BlockParseRuntimeException(block.getHeader().getHeaderBody().getBlockNumber(), null, e));
        }
    }

    private Block block(long number, String hash, String prevHash) {
        Block block = mock(Block.class, RETURNS_DEEP_STUBS);
        when(block.getHeader().getHeaderBody().getBlockNumber()).thenReturn(number);
        when(block.getHeader().getHeaderBody().getSlot()).thenReturn(number);
        when(block.getHeader().getHeaderBody().getBlockHash()).thenReturn(hash);
        when(block.getHeader().getHeaderBody().getPrevHash()).thenReturn(prevHash);
        when(block.getHeader().getHeaderBody().getIssuerVkey()).thenReturn(ISSUER_VKEY);
        return block;
    }

    private Cursor cursor(long number, String hash) {
        return Cursor.builder().slot(number).block(number).blockHash(hash).era(Era.Conway).build();
    }
}
