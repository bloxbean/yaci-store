package com.bloxbean.cardano.yaci.store.analytics.writer;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DuckLakeWriterServiceVerifyExportTest {

    @TempDir
    Path tempDir;

    private DuckLakeWriterService writerService;

    @BeforeEach
    void setUp() {
        AnalyticsStoreProperties properties = mock(AnalyticsStoreProperties.class);
        when(properties.getExportPath()).thenReturn(tempDir.toString());

        // Use ReflectionTestUtils to bypass constructor (which requires DuckDB dependencies)
        writerService = mock(DuckLakeWriterService.class);
        ReflectionTestUtils.setField(writerService, "properties", properties);

        // Call real verifyExport method
        org.mockito.Mockito.doCallRealMethod().when(writerService).verifyExport(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldReturnTrueWhenParquetFilesExist() throws IOException {
        Path duckLakeDir = tempDir.resolve("main/delegation/date=2023-05-31");
        Files.createDirectories(duckLakeDir);
        Files.createFile(duckLakeDir.resolve("ducklake-abc123.parquet"));

        String outputPath = tempDir.resolve("delegation/date=2023-05-31/data.parquet").toString();

        assertThat(writerService.verifyExport(outputPath, 100)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenDirectoryDoesNotExist() {
        String outputPath = tempDir.resolve("delegation/date=2023-05-31/data.parquet").toString();

        assertThat(writerService.verifyExport(outputPath, 100)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenDirectoryExistsButEmpty() throws IOException {
        Path duckLakeDir = tempDir.resolve("main/delegation/date=2023-05-31");
        Files.createDirectories(duckLakeDir);

        String outputPath = tempDir.resolve("delegation/date=2023-05-31/data.parquet").toString();

        assertThat(writerService.verifyExport(outputPath, 100)).isFalse();
    }

    @Test
    void shouldReturnTrueWhenExpectedRowCountIsZero() {
        String outputPath = tempDir.resolve("delegation/date=2023-05-31/data.parquet").toString();

        assertThat(writerService.verifyExport(outputPath, 0)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenDirectoryHasNonParquetFilesOnly() throws IOException {
        Path duckLakeDir = tempDir.resolve("main/delegation/date=2023-05-31");
        Files.createDirectories(duckLakeDir);
        Files.createFile(duckLakeDir.resolve("some-temp-file.tmp"));

        String outputPath = tempDir.resolve("delegation/date=2023-05-31/data.parquet").toString();

        assertThat(writerService.verifyExport(outputPath, 100)).isFalse();
    }
}
