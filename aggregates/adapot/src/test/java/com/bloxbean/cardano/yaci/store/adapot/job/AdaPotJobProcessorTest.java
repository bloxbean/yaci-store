package com.bloxbean.cardano.yaci.store.adapot.job;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.adapot.service.EpochRewardCalculationService;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.DepositSnapshotService;
import com.bloxbean.cardano.yaci.store.adapot.snapshot.StakeSnapshotService;
import com.bloxbean.cardano.yaci.store.adapot.storage.PartitionManager;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaPotJobProcessorTest {
    private final AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final AdaPotJobProcessor processor = new AdaPotJobProcessor(
            mock(StoreProperties.class),
            mock(AdaPotProperties.class),
            storage,
            mock(EraService.class),
            mock(EpochRewardCalculationService.class),
            mock(StakeSnapshotService.class),
            mock(DepositSnapshotService.class),
            mock(AdaPotService.class),
            mock(TransactionStorageReader.class),
            publisher,
            mock(PartitionManager.class),
            mock(DSLContext.class));

    @Test
    void shouldSkipJobCancelledByRollback() throws InterruptedException {
        AdaPotJob job = job(644, 192844802L, 13695759L);

        boolean result = processor.processJob(job, () -> true);

        assertThat(result).isTrue();
        verify(storage, never()).save(job);
        verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSkipJobReplacedByCanonicalTransition() throws InterruptedException {
        AdaPotJob staleJob = job(644, 192844802L, 13695759L, "orphan-hash");
        AdaPotJob canonicalJob = job(644, 192845302L, 13695759L, "canonical-hash");
        when(storage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, 644))
                .thenReturn(Optional.of(canonicalJob));

        boolean result = processor.processJob(staleJob);

        assertThat(result).isTrue();
        verify(storage, never()).save(staleJob);
        verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSkipJobReplacedAtSameSlotAndBlockByDifferentHash() throws InterruptedException {
        AdaPotJob staleJob = job(644, 192844802L, 13695759L, "orphan-hash");
        AdaPotJob canonicalJob = job(644, 192844802L, 13695759L, "canonical-hash");
        when(storage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, 644))
                .thenReturn(Optional.of(canonicalJob));

        boolean result = processor.processJob(staleJob);

        assertThat(result).isTrue();
        verify(storage, never()).save(staleJob);
        verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldStopRunningJobWhenRollbackCancelsIt() throws InterruptedException {
        AdaPotJob job = job(644, 192844802L, 13695759L);
        when(storage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, 644))
                .thenReturn(Optional.of(job));
        AtomicInteger cancellationChecks = new AtomicInteger();

        boolean result = processor.processJob(job, () -> cancellationChecks.incrementAndGet() > 1);

        assertThat(result).isTrue();
        verify(storage).save(job);
        verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private AdaPotJob job(int epoch, long slot, long block) {
        return job(epoch, slot, block, null);
    }

    private AdaPotJob job(int epoch, long slot, long block, String blockHash) {
        return AdaPotJob.builder()
                .epoch(epoch)
                .slot(slot)
                .block(block)
                .blockHash(blockHash)
                .type(AdaPotJobType.REWARD_CALC)
                .status(AdaPotJobStatus.NOT_STARTED)
                .build();
    }
}
