package com.bloxbean.cardano.yaci.store.adapot.job;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(manager.retryDelayMillis(5)).isEqualTo(160_000);
        assertThat(manager.retryDelayMillis(6)).isEqualTo(300_000);
        assertThat(manager.retryDelayMillis(100)).isEqualTo(300_000);
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
