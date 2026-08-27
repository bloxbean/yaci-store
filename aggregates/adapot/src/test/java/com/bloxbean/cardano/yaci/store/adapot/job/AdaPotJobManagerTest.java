package com.bloxbean.cardano.yaci.store.adapot.job;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdaPotJobManagerTest {

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
