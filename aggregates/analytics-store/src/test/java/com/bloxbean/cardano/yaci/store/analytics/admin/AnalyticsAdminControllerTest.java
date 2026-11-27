package com.bloxbean.cardano.yaci.store.analytics.admin;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.ContinuousSyncScheduler;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.ExportReadinessService;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.PartitionNotReadyException;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.PartitionReadiness;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.UniversalExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsAdminControllerTest {

    private static final String BASE = "/api/v1/analytics/admin";
    private static final LocalDate GENESIS = LocalDate.of(2022, 6, 1);
    private static final LocalDate EXPORT_END = LocalDate.of(2026, 11, 14);

    private final UniversalExportService exportService = mock(UniversalExportService.class);
    private final TableExporterRegistry registry = mock(TableExporterRegistry.class);
    private final ExportStateAdminService adminService = mock(ExportStateAdminService.class);
    private final PartitionRepairService repairService = mock(PartitionRepairService.class);
    private final ExportReadinessService readiness = mock(ExportReadinessService.class);
    private final AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
    private AnalyticsAdminController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        register("transaction", PartitionStrategy.DAILY);
        register("reward", PartitionStrategy.EPOCH);
        when(readiness.getGenesisDate()).thenReturn(GENESIS);
        when(readiness.checkFinal(any())).thenAnswer(i -> {
            PartitionValue partition = i.getArgument(0);
            boolean ready = partition instanceof PartitionValue.DatePartition day
                    ? !day.date().isAfter(EXPORT_END)
                    : ((PartitionValue.EpochPartition) partition).epoch() <= 590;
            return ready ? PartitionReadiness.READY
                    : PartitionReadiness.notFinal(partition.toPathSegment() + " is not final yet");
        });
        controller = new AnalyticsAdminController(exportService, mock(ContinuousSyncScheduler.class), registry,
                adminService, repairService, readiness, properties);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AnalyticsAdminExceptionHandler())
                .build();
    }

    @Test
    void exportingADayThatIsNotFinalIsRefusedWith409() throws Exception {
        mvc.perform(post(BASE + "/export/date/2026-11-16"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("not final")));
        mvc.perform(post(BASE + "/export/table/transaction/range")
                        .param("startDate", "2026-11-01").param("endDate", "2026-11-15"))
                .andExpect(status().isConflict());
        mvc.perform(post(BASE + "/repair/table/transaction/range")
                        .param("startDate", "2026-11-01").param("endDate", "2099-01-01"))
                .andExpect(status().isBadRequest()); // longer than max-range-days
        verifyNoInteractions(exportService);
    }

    @Test
    void aSinglePartitionThatIsNotReadyIsRefusedWith409() throws Exception {
        when(exportService.exportTable("transaction", PartitionValue.ofDate(LocalDate.of(2026, 11, 16))))
                .thenReturn(result(UniversalExportService.Outcome.NOT_READY,
                        PartitionReadiness.notFinal("date=2026-11-16 is not final yet")));

        mvc.perform(post(BASE + "/export/table/transaction/date/2026-11-16"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("date=2026-11-16 is not final yet"));
    }

    @Test
    void aSuccessfulExportIs200AndAFailedOne500() throws Exception {
        when(exportService.exportTable("reward", PartitionValue.ofEpoch(500)))
                .thenReturn(result(UniversalExportService.Outcome.EXPORTED, PartitionReadiness.READY));
        when(exportService.exportTable("reward", PartitionValue.ofEpoch(501)))
                .thenReturn(result(UniversalExportService.Outcome.FAILED, PartitionReadiness.READY));

        mvc.perform(post(BASE + "/export/table/reward/epoch/500")).andExpect(status().isOk());
        mvc.perform(post(BASE + "/export/table/reward/epoch/501")).andExpect(status().isInternalServerError());
    }

    @Test
    void invalidInputIs400() throws Exception {
        mvc.perform(post(BASE + "/export/date/2024-13-45"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("yyyy-MM-dd")));
        mvc.perform(post(BASE + "/export/date/2020-01-01")) // before genesis
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/export/table/reward/epoch/-2147483648")).andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/export/table/reward/epoch/99999999999")).andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/export/table/reward/date/2024-01-01")).andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/export/table/transaction/range")
                        .param("startDate", "2024-02-01").param("endDate", "2024-01-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/export/table/transaction/range")
                        .param("startDate", "2024-01-01").param("endDate", "~"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/export/table/reward/epoch-range")
                        .param("startEpoch", "0").param("endEpoch", String.valueOf(Integer.MAX_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("max-range-epochs")));
        mvc.perform(post(BASE + "/export/table/reward/epoch-range")
                        .param("startEpoch", "10").param("endEpoch", "5"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/export/table/reward/epoch-range").param("startEpoch", "10"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(exportService);
    }

    @Test
    void anEpochThatHasNotStartedIs409() throws Exception {
        mvc.perform(post(BASE + "/export/table/reward/epoch-range")
                        .param("startEpoch", "580").param("endEpoch", "600"))
                .andExpect(status().isConflict());
        verify(exportService, never()).exportEpochRange(anyString(), anyInt(), anyInt());
    }

    @Test
    void anUnknownTableIs404() throws Exception {
        mvc.perform(post(BASE + "/export/table/nope/date/2024-01-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("nope")));
        mvc.perform(post(BASE + "/repair/table/nope/range")
                        .param("startDate", "2024-01-01").param("endDate", "2024-01-02"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aRangeWithinTheLimitsRunsAndReportsItsPartitions() throws Exception {
        when(exportService.exportDateRange("transaction", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3)))
                .thenReturn(new UniversalExportService.RangeExportResult("transaction", 3, 1,
                        List.of("date=2024-01-02"), List.of("date=2024-01-03")));

        mvc.perform(post(BASE + "/export/table/transaction/range")
                        .param("startDate", "2024-01-01").param("endDate", "2024-01-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.totalDays").value(3))
                .andExpect(jsonPath("$.notReady[0]").value("date=2024-01-02"))
                .andExpect(jsonPath("$.failed[0]").value("date=2024-01-03"));
    }

    @Test
    void aRepairOfADayThatIsNotFinalIs409() throws Exception {
        when(repairService.repairDateRange(anyString(), any(), any()))
                .thenThrow(new PartitionNotReadyException(PartitionReadiness.notFinal("date=2026-11-15 is not final yet")));

        mvc.perform(post(BASE + "/repair/table/transaction/range")
                        .param("startDate", "2026-11-01").param("endDate", "2026-11-15"))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyOneManualExportRunsAtATime() {
        when(exportService.exportTable("reward", PartitionValue.ofEpoch(500))).thenAnswer(i -> {
            assertThatThrownBy(() -> controller.exportTableEpoch("reward", 501))
                    .isInstanceOf(AdminApiException.class)
                    .hasMessageContaining("Another manual export");
            return result(UniversalExportService.Outcome.EXPORTED, PartitionReadiness.READY);
        });

        controller.exportTableEpoch("reward", 500);
        verify(exportService, never()).exportTable("reward", PartitionValue.ofEpoch(501));
    }

    @Test
    void resetNormalizesThePartitionAndReports404WhenThereIsNoState() throws Exception {
        when(adminService.resetExportState("transaction", "date=2024-01-15")).thenReturn(true);

        mvc.perform(delete(BASE + "/state/transaction/2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("date=2024-01-15")));
        mvc.perform(delete(BASE + "/state/transaction/date=2024-01-15")).andExpect(status().isOk());

        mvc.perform(delete(BASE + "/state/transaction/2024-10-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("date=2024-10-31")));
        mvc.perform(delete(BASE + "/state/nope/2024-01-15")).andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/state/transaction/450")).andExpect(status().isBadRequest());
        mvc.perform(delete(BASE + "/state/transaction/yesterday")).andExpect(status().isBadRequest());
    }

    @Test
    void rangeResetParsesDatesAndReports404WhenNothingMatched() throws Exception {
        when(adminService.resetDateRange("transaction", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(3);

        mvc.perform(delete(BASE + "/state/transaction/range")
                        .param("startDate", "2024-01-01").param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().string("Reset 3 partition states"));
        mvc.perform(delete(BASE + "/state/transaction/range")
                        .param("startDate", "2025-01-01").param("endDate", "2025-01-31"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/state/transaction/range")
                        .param("startDate", "2024-01-01").param("endDate", "~"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(BASE + "/state/transaction/range")
                        .param("startDate", "2024-02-01").param("endDate", "2024-01-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(BASE + "/state/reward/range")
                        .param("startDate", "2024-01-01").param("endDate", "2024-01-31"))
                .andExpect(status().isBadRequest());
    }

    private void register(String table, PartitionStrategy strategy) {
        TableExporter exporter = mock(TableExporter.class);
        when(exporter.getTableName()).thenReturn(table);
        when(exporter.getPartitionStrategy()).thenReturn(strategy);
        when(registry.hasExporter(table)).thenReturn(true);
        when(registry.getExporter(table)).thenReturn(exporter);
    }

    private static UniversalExportService.PartitionExportResult result(UniversalExportService.Outcome outcome,
                                                                       PartitionReadiness readiness) {
        return new UniversalExportService.PartitionExportResult("t", "p", outcome, readiness);
    }
}
