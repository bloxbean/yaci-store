package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CostModelExporterTest {
    private static final LocalDate DAY = LocalDate.of(2026, 6, 18);
    private static final long DAY_END = DAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

    private final EraService eraService = mock(EraService.class);
    private final AdaPotJobStorage jobs = mock(AdaPotJobStorage.class);
    private CostModelExporter exporter;

    @BeforeEach
    void setUp() {
        // One slot per second from time 0, so a day's end slot is its end time.
        when(eraService.slotFromTime(anyLong())).thenAnswer(i -> i.getArgument(0));
        when(eraService.getFirstNonByronSlot()).thenReturn(1000L);
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.of(1));
        when(eraService.getEpochNo(Era.Shelley, DAY_END - 1)).thenReturn(638);
        exporter = new CostModelExporter(mock(StorageWriter.class), mock(ExportStateService.class), eraService,
                new AnalyticsStoreProperties(), jobs);
    }

    @Test
    void waitsForTheAdaPotJobOfTheDaysLastEpoch() {
        job(AdaPotJobStatus.STARTED);
        assertThat(exporter.preExportValidation(PartitionValue.ofDate(DAY))).isFalse();

        job(AdaPotJobStatus.COMPLETED);
        assertThat(exporter.preExportValidation(PartitionValue.ofDate(DAY))).isTrue();
    }

    @Test
    void doesNotWaitForByronDays() {
        when(eraService.getFirstNonByronSlot()).thenReturn(DAY_END);

        assertThat(exporter.preExportValidation(PartitionValue.ofDate(DAY))).isTrue();
    }

    @Test
    void doesNotWaitWhenAdaPotIsDisabled() {
        CostModelExporter noAdaPot = new CostModelExporter(mock(StorageWriter.class), mock(ExportStateService.class),
                eraService, new AnalyticsStoreProperties(), new NoOpAdaPotJobStorage());

        assertThat(noAdaPot.preExportValidation(PartitionValue.ofDate(DAY))).isTrue();
    }

    private void job(AdaPotJobStatus status) {
        AdaPotJob job = new AdaPotJob();
        job.setEpoch(638);
        job.setType(AdaPotJobType.REWARD_CALC);
        job.setStatus(status);
        when(jobs.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, 638)).thenReturn(Optional.of(job));
    }
}
