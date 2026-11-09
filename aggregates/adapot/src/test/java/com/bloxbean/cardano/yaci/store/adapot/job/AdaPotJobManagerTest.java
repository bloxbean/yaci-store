package com.bloxbean.cardano.yaci.store.adapot.job;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdaPotJobManagerTest {

    private static AdaPotJobManager managerWithoutDelays(AdaPotJobProcessor processor) {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        var manager = new AdaPotJobManager(storage, processor, false);
        manager.initialDelayMillis = 0;
        manager.retryBaseDelayMillis = 0;
        manager.retryMaxDelayMillis = 0;
        return manager;
    }

    private static AdaPotJob job(int epoch) {
        return AdaPotJob.builder().epoch(epoch).slot(192844802L).block(13695759L).blockHash("hash").build();
    }

    @Test
    void shouldRetrySameJobWhenProcessingThrows() throws Exception {
        //Database down: saving the failure state throws out of processJob
        AdaPotJobProcessor processor = mock(AdaPotJobProcessor.class);
        AdaPotJob job = job(36);
        when(processor.processJob(eq(job), any(BooleanSupplier.class)))
                .thenThrow(new RuntimeException("Unable to acquire JDBC Connection"))
                .thenThrow(new RuntimeException("Unable to acquire JDBC Connection"))
                .thenReturn(true);
        var manager = managerWithoutDelays(processor);

        assertThat(manager.runUntilDone(job)).isTrue();

        verify(processor, times(3)).processJob(eq(job), any(BooleanSupplier.class));
    }

    @Test
    void shouldRetrySameJobWhenProcessingFailsInsteadOfStopping() throws Exception {
        //Retry count exceeded inside the processor: the job must not be dropped and the loop must not exit
        AdaPotJobProcessor processor = mock(AdaPotJobProcessor.class);
        AdaPotJob job = job(37);
        when(processor.processJob(eq(job), any(BooleanSupplier.class)))
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);
        var manager = managerWithoutDelays(processor);

        assertThat(manager.runUntilDone(job)).isTrue();

        verify(processor, times(3)).processJob(eq(job), any(BooleanSupplier.class));
    }

    @Test
    void shouldStopRetryingWhenRollbackCancelsTheJob() throws Exception {
        AdaPotJobProcessor processor = mock(AdaPotJobProcessor.class);
        AdaPotJob job = job(644);
        var manager = managerWithoutDelays(processor);
        doAnswer(invocation -> {
            manager.handleRollback(RollbackEvent.builder()
                    .rollbackTo(new Point(192844727L, "rollback-hash"))
                    .build());
            return false;
        }).when(processor).processJob(eq(job), any(BooleanSupplier.class));

        assertThat(manager.runUntilDone(job)).isTrue();

        verify(processor, times(1)).processJob(eq(job), any(BooleanSupplier.class));
        assertThat(manager.isCancelled(job)).isFalse();
    }

    @Test
    void shouldBackOffUpToTheMaximumDelay() {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        var manager = new AdaPotJobManager(storage, mock(AdaPotJobProcessor.class), false);

        assertThat(manager.retryDelayMillis(1)).isEqualTo(10_000);
        assertThat(manager.retryDelayMillis(2)).isEqualTo(20_000);
        assertThat(manager.retryDelayMillis(8)).isEqualTo(1_280_000);
        assertThat(manager.retryDelayMillis(9)).isEqualTo(1_800_000);
        assertThat(manager.retryDelayMillis(1000)).isEqualTo(1_800_000);
    }

    @Test
    void shouldRetrySameJobWhenProcessingThrowsAnError() throws Exception {
        AdaPotJobProcessor processor = mock(AdaPotJobProcessor.class);
        AdaPotJob job = job(40);
        when(processor.processJob(eq(job), any(BooleanSupplier.class)))
                .thenThrow(new StackOverflowError())
                .thenReturn(true);
        var manager = managerWithoutDelays(processor);

        assertThat(manager.runUntilDone(job)).isTrue();

        verify(processor, times(2)).processJob(eq(job), any(BooleanSupplier.class));
    }

    @Test
    void shouldStopRetryingWhenRollbackCancelsTheJobWhileItThrows() throws Exception {
        AdaPotJobProcessor processor = mock(AdaPotJobProcessor.class);
        AdaPotJob job = job(644);
        var manager = managerWithoutDelays(processor);
        doAnswer(invocation -> {
            manager.handleRollback(RollbackEvent.builder()
                    .rollbackTo(new Point(192844727L, "rollback-hash"))
                    .build());
            throw new RuntimeException("Unable to acquire JDBC Connection");
        }).when(processor).processJob(eq(job), any(BooleanSupplier.class));

        assertThat(manager.runUntilDone(job)).isTrue();

        verify(processor, times(1)).processJob(eq(job), any(BooleanSupplier.class));
    }

    @Test
    void shouldEndTheBackoffEarlyWhenRollbackCancelsTheJob() throws Exception {
        AdaPotJobProcessor processor = mock(AdaPotJobProcessor.class);
        AdaPotJob job = job(644);
        CountDownLatch attempted = new CountDownLatch(1);
        when(processor.processJob(eq(job), any(BooleanSupplier.class))).thenAnswer(invocation -> {
            attempted.countDown();
            return false;
        });
        var manager = managerWithoutDelays(processor);
        manager.retryBaseDelayMillis = 600_000;
        manager.retryMaxDelayMillis = 600_000;

        //Roll back once the first attempt has failed and the 10 min backoff has started
        var rollback = Thread.startVirtualThread(() -> {
            try {
                if (!attempted.await(10, TimeUnit.SECONDS))
                    return;
                Thread.sleep(200);
            } catch (InterruptedException e) {
                return;
            }
            manager.handleRollback(RollbackEvent.builder()
                    .rollbackTo(new Point(192844727L, "rollback-hash"))
                    .build());
        });

        try {
            assertTimeoutPreemptively(Duration.ofSeconds(10), () -> assertThat(manager.runUntilDone(job)).isTrue());
            verify(processor, times(1)).processJob(eq(job), any(BooleanSupplier.class));
        } finally {
            rollback.interrupt();
            rollback.join();
        }
    }

    @Test
    void shouldEndTheJobThreadOnStop() throws Exception {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        AdaPotJobProcessor processor = mock(AdaPotJobProcessor.class);
        var manager = new AdaPotJobManager(storage, processor, true);
        Thread thread = (Thread) ReflectionTestUtils.getField(manager, "processorThread");

        manager.stop();
        thread.join(5000);

        assertThat(thread.isAlive()).isFalse();
        manager.triggerRewardCalcJob(42, 1L, 1L, "hash");
        verify(processor, org.mockito.Mockito.after(300).never()).processJob(any(AdaPotJob.class), any(BooleanSupplier.class));
    }

    @Test
    void shouldStopWhenInterrupted() throws Exception {
        AdaPotJobProcessor processor = mock(AdaPotJobProcessor.class);
        AdaPotJob job = job(41);
        when(processor.processJob(eq(job), any(BooleanSupplier.class))).thenThrow(new InterruptedException());
        var manager = managerWithoutDelays(processor);

        assertThat(manager.runUntilDone(job)).isFalse();
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void shouldNotStartTheNextEpochBeforeTheFailedOneCompletes() throws Exception {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        AdaPotJobProcessor processor = mock(AdaPotJobProcessor.class);
        List<Integer> calls = new CopyOnWriteArrayList<>();
        CountDownLatch nextEpochDone = new CountDownLatch(1);
        AtomicInteger epoch36Attempts = new AtomicInteger();
        when(processor.processJob(any(AdaPotJob.class), any(BooleanSupplier.class))).thenAnswer(invocation -> {
            AdaPotJob job = invocation.getArgument(0);
            calls.add(job.getEpoch());
            if (job.getEpoch() == 36)
                return epoch36Attempts.incrementAndGet() >= 3;
            nextEpochDone.countDown();
            return true;
        });

        //Starts the real job thread
        var manager = new AdaPotJobManager(storage, processor, true);
        try {
            manager.initialDelayMillis = 0;
            manager.retryBaseDelayMillis = 1;
            manager.retryMaxDelayMillis = 1;
            manager.triggerRewardCalcJob(36, 13910414L, 331235L, "hash-36");
            manager.triggerRewardCalcJob(37, 14342434L, 352288L, "hash-37");

            assertThat(nextEpochDone.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(calls).containsExactly(36, 36, 36, 37);
        } finally {
            manager.stop();
        }
    }

    @Test
    void shouldRemoveQueuedJobCreatedAfterRollbackPoint() {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        var manager = new AdaPotJobManager(storage, mock(AdaPotJobProcessor.class), false);

        manager.triggerRewardCalcJob(644, 192844802L, 13695759L);
        assertThat(manager.queuedJobCount()).isOne();

        manager.handleRollback(RollbackEvent.builder()
                .rollbackTo(new Point(192844727L, "rollback-hash"))
                .build());

        assertThat(manager.queuedJobCount()).isZero();
    }

    @Test
    void shouldKeepQueuedJobAtOrBeforeRollbackPoint() {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        var manager = new AdaPotJobManager(storage, mock(AdaPotJobProcessor.class), false);

        manager.triggerRewardCalcJob(644, 192844727L, 13695758L);
        manager.handleRollback(RollbackEvent.builder()
                .rollbackTo(new Point(192844727L, "rollback-hash"))
                .build());

        assertThat(manager.queuedJobCount()).isOne();
    }

    @Test
    void shouldMarkRunningJobAsCancelledAfterRollback() {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        var manager = new AdaPotJobManager(storage, mock(AdaPotJobProcessor.class), false);
        var runningJob = AdaPotJob.builder()
                .epoch(644)
                .slot(192844802L)
                .block(13695759L)
                .blockHash("orphan-hash")
                .build();
        ReflectionTestUtils.invokeMethod(manager, "activateJob", runningJob);

        manager.handleRollback(RollbackEvent.builder()
                .rollbackTo(new Point(192844727L, "rollback-hash"))
                .build());

        assertThat(manager.isCancelled(runningJob)).isTrue();
    }

    @Test
    void shouldUseBlockHashToDistinguishJobsAtSameSlotAndBlock() {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        var manager = new AdaPotJobManager(storage, mock(AdaPotJobProcessor.class), false);
        var orphanJob = AdaPotJob.builder()
                .epoch(644)
                .slot(192844802L)
                .block(13695759L)
                .blockHash("orphan-hash")
                .build();
        var canonicalJob = AdaPotJob.builder()
                .epoch(644)
                .slot(192844802L)
                .block(13695759L)
                .blockHash("canonical-hash")
                .build();
        ReflectionTestUtils.invokeMethod(manager, "activateJob", orphanJob);

        manager.handleRollback(RollbackEvent.builder()
                .rollbackTo(new Point(192844727L, "rollback-hash"))
                .build());

        assertThat(manager.isCancelled(orphanJob)).isTrue();
        assertThat(manager.isCancelled(canonicalJob)).isFalse();
    }

    @Test
    void shouldNotCarryCancellationToAReplayedIdenticalJob() {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        var manager = new AdaPotJobManager(storage, mock(AdaPotJobProcessor.class), false);
        var firstExecution = AdaPotJob.builder()
                .epoch(644)
                .slot(192844802L)
                .block(13695759L)
                .blockHash("same-hash")
                .build();
        var replayedExecution = AdaPotJob.builder()
                .epoch(644)
                .slot(192844802L)
                .block(13695759L)
                .blockHash("same-hash")
                .build();
        ReflectionTestUtils.invokeMethod(manager, "activateJob", firstExecution);
        manager.handleRollback(RollbackEvent.builder()
                .rollbackTo(new Point(192844727L, "rollback-hash"))
                .build());
        assertThat(manager.isCancelled(firstExecution)).isTrue();

        ReflectionTestUtils.invokeMethod(manager, "activateJob", replayedExecution);

        assertThat(manager.isCancelled(replayedExecution)).isFalse();
    }

    @Test
    void shouldPersistTransitionBlockHashWhenJobIsTriggered() {
        AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
        when(storage.getJobsByTypeAndStatus(any(), any())).thenReturn(List.of());
        var manager = new AdaPotJobManager(storage, mock(AdaPotJobProcessor.class), false);

        manager.triggerRewardCalcJob(644, 192844802L, 13695759L, "transition-hash");

        verify(storage).save(org.mockito.ArgumentMatchers.argThat(job ->
                "transition-hash".equals(job.getBlockHash())));
    }
}
