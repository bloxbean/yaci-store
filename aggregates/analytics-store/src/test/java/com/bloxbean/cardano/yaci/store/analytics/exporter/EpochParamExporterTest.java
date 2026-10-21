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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EpochParamExporterTest {
    private final EraService eraService = mock(EraService.class);
    private final AdaPotJobStorage jobs = mock(AdaPotJobStorage.class);
    private EpochParamExporter exporter;

    @BeforeEach
    void setUp() {
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.of(208));
        when(eraService.getEraForEpoch(400)).thenReturn(Era.Babbage);
        when(eraService.getEraForEpoch(600)).thenReturn(Era.Conway);
        exporter = exporter(jobs);
    }

    @Test
    void waitsForTheAdaPotJobInEveryNonByronEra() {
        for (int epoch : new int[]{400, 600}) {
            job(epoch, AdaPotJobStatus.STARTED);
            assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(epoch))).isFalse();

            job(epoch, AdaPotJobStatus.COMPLETED);
            assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(epoch))).isTrue();
        }
    }

    @Test
    void byronEpochsHaveNoJobToWaitFor() {
        assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(10))).isTrue();
    }

    @Test
    void withoutAdaPotOnlyPreConwayEpochsAreExported() {
        EpochParamExporter noAdaPot = exporter(new NoOpAdaPotJobStorage());

        assertThat(noAdaPot.preExportValidation(PartitionValue.ofEpoch(400))).isTrue();
        assertThat(noAdaPot.preExportValidation(PartitionValue.ofEpoch(600))).isFalse();
    }

    private EpochParamExporter exporter(AdaPotJobStorage storage) {
        return new EpochParamExporter(mock(StorageWriter.class), mock(ExportStateService.class), eraService,
                new AnalyticsStoreProperties(), storage);
    }

    private void job(int epoch, AdaPotJobStatus status) {
        AdaPotJob job = new AdaPotJob();
        job.setEpoch(epoch);
        job.setType(AdaPotJobType.REWARD_CALC);
        job.setStatus(status);
        when(jobs.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, epoch)).thenReturn(Optional.of(job));
    }
}
