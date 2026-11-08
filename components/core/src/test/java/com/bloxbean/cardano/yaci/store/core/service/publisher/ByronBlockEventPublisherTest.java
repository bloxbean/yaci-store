package com.bloxbean.cardano.yaci.store.core.service.publisher;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.service.SyncSessionGuard;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Only the current sync session may commit Byron blocks (#1344 findings 1 and 2).
 */
class ByronBlockEventPublisherTest {
    private ApplicationEventPublisher publisher;
    private CursorService cursorService;
    private SyncSessionGuard guard;
    private ByronBlockEventPublisher byronPublisher;

    @BeforeEach
    void setUp() {
        publisher = mock(ApplicationEventPublisher.class);
        cursorService = mock(CursorService.class);
        ExecutorService blockExecutor = mock(ExecutorService.class);
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(blockExecutor).execute(any());

        StoreProperties storeProperties = StoreProperties.builder()
                .blocksBatchSize(100)
                .blocksPartitionSize(15)
                .processingThreadsTimeout(5)
                .build();
        byronPublisher = new ByronBlockEventPublisher(blockExecutor, publisher, cursorService, storeProperties);
        guard = new SyncSessionGuard();
        byronPublisher.setSessionGuard(guard);
    }

    @Test
    void sessionReplacedDuringCommitEventsDoesNotMoveTheCursor() {
        long session = guard.newSession();
        doAnswer(inv -> guard.newSession()).when(publisher).publishEvent(any(PreCommitEvent.class));

        guard.enter(session);
        try {
            assertThatThrownBy(() -> byronPublisher.publishBlockEvents(eventMetadata(), mock(ByronMainBlock.class), List.of()))
                    .isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            guard.exit();
        }

        verify(cursorService, never()).setCursor(any());
    }

    @Test
    void sessionReplacedDuringCommitEventsOfABatchDoesNotMoveTheCursor() {
        long session = guard.newSession();
        doAnswer(inv -> guard.newSession()).when(publisher).publishEvent(any(PreCommitEvent.class));

        guard.enter(session);
        try {
            byronPublisher.publishBlockEventsInParallel(eventMetadata(), mock(ByronMainBlock.class), List.of());
            assertThatThrownBy(byronPublisher::processBlocksInParallel)
                    .isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            guard.exit();
        }

        verify(cursorService, never()).setCursor(any());
    }

    @Test
    void oldFlushWaitingForTheBatchLockLeavesTheNewBatchAlone() throws Exception {
        long oldSession = guard.newSession();
        List<?> batch = (List<?>) ReflectionTestUtils.getField(byronPublisher, "byronBatchBlockList");

        ShelleyBlockEventPublisherTest.BlockedCall oldFlush;
        synchronized (batch) {
            oldFlush = ShelleyBlockEventPublisherTest.startBlockedOn(guard, oldSession, byronPublisher::processBlocksInParallel);

            long newSession = guard.newSession();
            guard.enter(newSession);
            try {
                byronPublisher.publishBlockEventsInParallel(eventMetadata(), mock(ByronMainBlock.class), List.of());
            } finally {
                guard.exit();
            }
        }
        Throwable oldError = oldFlush.await();

        assertThat(oldError).isNotNull();
        assertThat(batch).hasSize(1);
        verify(cursorService, never()).setCursor(any());
    }

    @Test
    void oldEnqueueWaitingForTheBatchLockDoesNotAddToTheNewBatch() throws Exception {
        long oldSession = guard.newSession();
        List<?> batch = (List<?>) ReflectionTestUtils.getField(byronPublisher, "byronBatchBlockList");

        ShelleyBlockEventPublisherTest.BlockedCall oldEnqueue;
        synchronized (batch) {
            oldEnqueue = ShelleyBlockEventPublisherTest.startBlockedOn(guard, oldSession, () ->
                    byronPublisher.publishBlockEventsInParallel(eventMetadata(), mock(ByronMainBlock.class), List.of()));

            long newSession = guard.newSession();
            guard.enter(newSession);
            try {
                byronPublisher.publishBlockEventsInParallel(eventMetadata(), mock(ByronMainBlock.class), List.of());
            } finally {
                guard.exit();
            }
        }
        Throwable oldError = oldEnqueue.await();

        assertThat(oldError).isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        assertThat(batch).hasSize(1);
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .era(Era.Byron)
                .protocolMagic(1)
                .epochNumber(3)
                .block(5000)
                .blockHash("82af2e80f94c60678b32b5b17da46b12e4095745a6c2a5ae841e0c89434a8b35")
                .prevBlockHash("0080bd77e570d254a6c6bd34917c96a9e99d4e1eee227bb77d276b6483071bcb")
                .slot(65000)
                .build();
    }
}
