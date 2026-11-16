package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.writer.DuckLakeWriterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DuckLakeMaintenanceServiceTest {

    // 2024-03-01T00:00:00Z
    private static final long MARCH_1 = 1709251200L;

    @TempDir
    Path tempDir;

    private DuckLakeTestCatalog catalog;
    private DuckLakeMaintenanceService maintenance;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(DuckLakeTestCatalog.duckLakeAvailable(), "DuckLake extension not available");
        catalog = new DuckLakeTestCatalog(tempDir);
        maintenance = new DuckLakeMaintenanceService(catalog.dataSource, catalog.helper, catalog.lock,
                catalog.properties);
    }

    @AfterEach
    void tearDown() {
        if (catalog != null) {
            catalog.close();
        }
    }

    @Test
    void replacedFilesStayForTheRetentionAndAreDeletedAfterIt() throws Exception {
        DuckLakeWriterService writer = catalog.writer();
        String day = catalog.path("block", "date=2024-03-01");
        writer.export(blocks(5), day, "block_time");
        List<Path> first = files();
        writer.export(blocks(6), day, "block_time");
        assertThat(files()).hasSize(2);

        // Within the retention: a reader may still use the file list from before the replace
        DuckLakeMaintenanceService.Result kept = maintenance.run(Duration.ofHours(1));
        assertThat(kept.deletedFiles()).isZero();
        assertThat(files()).hasSize(2);

        // After the retention the retired file is deleted; the current one is not
        DuckLakeMaintenanceService.Result cleaned = maintenance.run(Duration.ZERO);
        assertThat(cleaned.expiredSnapshots()).isPositive();
        assertThat(cleaned.deletedFiles()).isEqualTo(1);
        assertThat(files()).hasSize(1).doesNotContainAnyElementsOf(first);
        assertThat(catalog.count("SELECT COUNT(*) FROM block")).isEqualTo(6);
        assertThat(writer.verifyExport(day, 6)).isTrue();
    }

    @Test
    void scheduledRunIsSkippedWhenDisabled() throws Exception {
        catalog.properties.getDucklake().getMaintenance().setEnabled(false);
        catalog.properties.getDucklake().getMaintenance().setRetentionHours(1);
        DuckLakeWriterService writer = catalog.writer();
        String day = catalog.path("block", "date=2024-03-01");
        writer.export(blocks(5), day, "block_time");
        writer.export(blocks(6), day, "block_time");

        maintenance.scheduledMaintenance();

        assertThat(files()).hasSize(2);
    }

    private List<Path> files() throws Exception {
        try (Stream<Path> files = Files.walk(Path.of(catalog.exportPath, "main", "block"))) {
            return files.filter(f -> f.toString().endsWith(".parquet")).sorted().toList();
        }
    }

    private static String blocks(int count) {
        return "SELECT range AS slot, to_timestamp(" + MARCH_1 + " + range) AS block_time FROM range(" + count + ")";
    }
}
