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
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.adapot.storage.PartitionManager;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import org.cardanofoundation.rewards.calculation.domain.EpochCalculationResult;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaPotJobProcessorTest {
    private final AdaPotJobStorage storage = mock(AdaPotJobStorage.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final EraService eraService = mock(EraService.class);
    private final EpochRewardCalculationService epochRewardCalculationService = mock(EpochRewardCalculationService.class);
    private final StakeSnapshotService stakeSnapshotService = mock(StakeSnapshotService.class);
    private final EpochStakeStorageReader epochStakeStorageReader = mock(EpochStakeStorageReader.class);
    private final AdaPotService adaPotService = mock(AdaPotService.class);
    private final DSLContext dsl = mock(DSLContext.class);
    private final AdaPotJobProcessor processor = new AdaPotJobProcessor(
            mock(StoreProperties.class),
            mock(AdaPotProperties.class),
            storage,
            eraService,
            epochRewardCalculationService,
            stakeSnapshotService,
            epochStakeStorageReader,
            mock(DepositSnapshotService.class),
            adaPotService,
            mock(TransactionStorageReader.class),
            publisher,
            mock(PartitionManager.class),
            dsl);

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

    @Test
    void shouldStoreSnapshotTotalAfterTakingStakeSnapshot() throws InterruptedException {
        AdaPotJob job = job(644, 192844802L, 13695759L);
        BigInteger totalStake = new BigInteger("21450116322370497");
        when(storage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, 644))
                .thenReturn(Optional.of(job));
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.of(208));
        when(epochRewardCalculationService.calculateEpochRewards(644))
                .thenReturn(EpochCalculationResult.builder().build());
        when(dsl.dialect()).thenReturn(SQLDialect.H2);
        //Job 644 snapshots epoch 643, which is the active stake of epoch 645
        when(epochStakeStorageReader.getTotalActiveStakeByEpoch(645)).thenReturn(Optional.of(totalStake));

        boolean result = processor.processJob(job);

        assertThat(result).isTrue();
        InOrder inOrder = inOrder(stakeSnapshotService, adaPotService);
        inOrder.verify(stakeSnapshotService).takeStakeSnapshot(643);
        inOrder.verify(adaPotService).updateTotalEpochStake(644, totalStake);
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
