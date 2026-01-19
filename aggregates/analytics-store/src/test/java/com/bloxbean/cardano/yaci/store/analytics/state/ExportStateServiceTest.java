package com.bloxbean.cardano.yaci.store.analytics.state;

import com.bloxbean.cardano.yaci.store.analytics.writer.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ExportStateService.class)  // Import the service under test
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class ExportStateServiceTest {

    @Autowired
    private ExportStateService stateService;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testMarkInProgress() {
        // Given
        String tableName = "test_table";
        String partition = "2024-10-30";

        // When
        ExportState state = stateService.markInProgress(tableName, partition);
        entityManager.flush();  // Ensure database writes

        // Then
        assertThat(state.getStatus()).isEqualTo(ExportStatus.IN_PROGRESS);
        assertThat(state.getStartedAt()).isNotNull();
        assertThat(state.getRetryCount()).isEqualTo(0);  // First attempt, not a retry
    }

    @Test
    public void testMarkCompleted() {
        // Given
        String tableName = "test_table";
        String partition = "2024-10-30";
        ExportState state = stateService.markInProgress(tableName, partition);

        ExportResult result = new ExportResult(
            "/path/to/file.parquet", 1000, 5000, 1500
        );

        // When
        stateService.markCompleted(state, result, "abc123");

        // Then
        ExportState completed = stateService.getState(tableName, partition);
        assertThat(completed.getStatus()).isEqualTo(ExportStatus.COMPLETED);
        assertThat(completed.getRowCount()).isEqualTo(1000);
        assertThat(completed.getChecksumSha256()).isEqualTo("abc123");
    }

    @Test
    public void testIdempotency() {
        // Given
        String tableName = "test_table";
        String partition = "2024-10-30";

        // When - First export
        ExportState state1 = stateService.markInProgress(tableName, partition);
        ExportResult result = new ExportResult("/path", 1000, 5000, 1500);
        stateService.markCompleted(state1, result, "abc123");

        // When - Second export attempt
        ExportState existing = stateService.getState(tableName, partition);

        // Then - Should be completed, not attempt re-export
        assertThat(existing.getStatus()).isEqualTo(ExportStatus.COMPLETED);
    }

    @Test
    public void testRetryIncrementOnFailureOnly() {
        // Given
        String tableName = "test_table";
        String partition = "2024-10-30";

        // When - First attempt fails
        ExportState state1 = stateService.markInProgress(tableName, partition);
        assertThat(state1.getRetryCount()).isEqualTo(0);  // Not a retry
        stateService.markFailed(state1, "Connection timeout");
        entityManager.flush();

        // When - Retry after failure
        ExportState state2 = stateService.markInProgress(tableName, partition);
        entityManager.flush();

        // Then - Retry count increments
        assertThat(state2.getRetryCount()).isEqualTo(1);
        assertThat(state2.getStatus()).isEqualTo(ExportStatus.IN_PROGRESS);
    }

    @Test
    public void testStaleFieldsClearedOnRetry() {
        // Given - Completed export with metrics
        String tableName = "test_table";
        String partition = "2024-10-30";
        ExportState state1 = stateService.markInProgress(tableName, partition);
        ExportResult result = new ExportResult("/path/file.parquet", 1000, 5000, 1500);
        stateService.markCompleted(state1, result, "abc123");
        entityManager.flush();

        // When - Mark as failed and retry
        ExportState existing = stateService.getState(tableName, partition);
        stateService.markFailed(existing, "Verification failed");
        entityManager.flush();

        ExportState retryState = stateService.markInProgress(tableName, partition);
        entityManager.flush();

        // Then - Stale fields cleared
        assertThat(retryState.getRowCount()).isNull();
        assertThat(retryState.getFileSizeBytes()).isNull();
        assertThat(retryState.getFilePath()).isNull();
        assertThat(retryState.getChecksumSha256()).isNull();
        assertThat(retryState.getErrorMessage()).isNull();
    }

    @Test
    public void testFailureTimestampTracking() {
        // Given
        String tableName = "test_table";
        String partition = "2024-10-30";
        ExportState state = stateService.markInProgress(tableName, partition);
        entityManager.flush();

        // When - Mark as failed
        stateService.markFailed(state, "Export error");
        entityManager.flush();

        // Then - Failure timestamp and duration tracked
        ExportState failed = stateService.getState(tableName, partition);
        assertThat(failed.getStatus()).isEqualTo(ExportStatus.FAILED);
        assertThat(failed.getCompletedAt()).isNotNull();
        assertThat(failed.getDurationSeconds()).isNotNull();
        assertThat(failed.getErrorMessage()).isEqualTo("Export error");
    }

    @Test
    public void testGetCompletedPartitions() {
        // Given - Multiple partitions with different states
        stateService.markInProgress("test_table", "2024-10-30");
        ExportState state1 = stateService.getState("test_table", "2024-10-30");
        stateService.markCompleted(state1, new ExportResult("/path1", 100, 500, 100), "checksum1");

        stateService.markInProgress("test_table", "2024-10-31");
        ExportState state2 = stateService.getState("test_table", "2024-10-31");
        stateService.markCompleted(state2, new ExportResult("/path2", 200, 600, 150), "checksum2");

        stateService.markInProgress("test_table", "2024-11-01");
        ExportState state3 = stateService.getState("test_table", "2024-11-01");
        stateService.markFailed(state3, "Failed export");

        entityManager.flush();

        // When
        var completedPartitions = stateService.getCompletedPartitions("test_table");

        // Then
        assertThat(completedPartitions).hasSize(2);
        assertThat(completedPartitions).contains("2024-10-30", "2024-10-31");
        assertThat(completedPartitions).doesNotContain("2024-11-01");
    }

    @Test
    public void testFindFailedExports() {
        // Given - Some failed exports with different retry counts
        ExportState state1 = stateService.markInProgress("test_table", "2024-10-30");
        stateService.markFailed(state1, "Error 1");

        ExportState state2 = stateService.markInProgress("test_table", "2024-10-31");
        stateService.markFailed(state2, "Error 2");
        entityManager.flush();

        // Retry once
        ExportState retry1 = stateService.markInProgress("test_table", "2024-10-31");
        stateService.markFailed(retry1, "Error 2 retry");
        entityManager.flush();

        // Retry twice
        ExportState retry2 = stateService.markInProgress("test_table", "2024-10-31");
        stateService.markFailed(retry2, "Error 2 retry 2");
        entityManager.flush();

        // When - Find failed with max 2 retries
        var failedExports = stateService.getFailedExports("test_table", 2);

        // Then - Only partition with retry count < 2 should be returned
        assertThat(failedExports).hasSize(1);
        assertThat(failedExports.get(0).getId().getPartitionValue()).isEqualTo("2024-10-30");
    }

    @Test
    public void testErrorMessageTruncation() {
        // Given - Very long error message
        String tableName = "test_table";
        String partition = "2024-10-30";
        ExportState state = stateService.markInProgress(tableName, partition);

        String longError = "Error: " + "X".repeat(1500);

        // When
        stateService.markFailed(state, longError);
        entityManager.flush();

        // Then - Error message should be truncated
        ExportState failed = stateService.getState(tableName, partition);
        assertThat(failed.getErrorMessage().length()).isEqualTo(1015);  // 1000 + "... (truncated)"
        assertThat(failed.getErrorMessage()).endsWith("... (truncated)");
    }

    @Test
    public void testMultipleRetriesIncrement() {
        // Given
        String tableName = "test_table";
        String partition = "2024-10-30";

        // First attempt
        ExportState state1 = stateService.markInProgress(tableName, partition);
        assertThat(state1.getRetryCount()).isEqualTo(0);
        stateService.markFailed(state1, "Error 1");
        entityManager.flush();

        // Second attempt (first retry)
        ExportState state2 = stateService.markInProgress(tableName, partition);
        assertThat(state2.getRetryCount()).isEqualTo(1);
        stateService.markFailed(state2, "Error 2");
        entityManager.flush();

        // Third attempt (second retry)
        ExportState state3 = stateService.markInProgress(tableName, partition);
        assertThat(state3.getRetryCount()).isEqualTo(2);
        stateService.markFailed(state3, "Error 3");
        entityManager.flush();

        // Fourth attempt (third retry)
        ExportState state4 = stateService.markInProgress(tableName, partition);

        // Then
        assertThat(state4.getRetryCount()).isEqualTo(3);
    }

    @Test
    public void testDurationCalculation() {
        // Given
        String tableName = "test_table";
        String partition = "2024-10-30";
        ExportState state = stateService.markInProgress(tableName, partition);

        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        ExportResult result = new ExportResult("/path", 1000, 5000, 1500);
        stateService.markCompleted(state, result, "checksum");
        entityManager.flush();

        // Then
        ExportState completed = stateService.getState(tableName, partition);
        assertThat(completed.getDurationSeconds()).isNotNull();
        assertThat(completed.getDurationSeconds()).isGreaterThanOrEqualTo(0);
    }
}
