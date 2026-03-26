package com.bloxbean.cardano.yaci.store.analytics.state;

import com.bloxbean.cardano.yaci.store.analytics.writer.ExportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for stale IN_PROGRESS export recovery and shutdown cleanup.
 * Tests the new resetStaleInProgressExports() and resetAllInProgressExports() methods
 * in ExportStateService.
 */
@DataJpaTest
@Import(ExportStateService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class StaleExportRecoveryTest {

    @Autowired
    private ExportStateService stateService;

    @Autowired
    private ExportStateRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testResetStaleInProgressExports_resetsOldExports() {
        // Given - An IN_PROGRESS export that started 2 hours ago (stale)
        ExportState staleState = stateService.markInProgress("block", "2024-10-30");
        staleState.setStartedAt(LocalDateTime.now().minusHours(2));
        repository.save(staleState);
        entityManager.flush();

        // When - Reset stale exports with 60 min timeout
        int resetCount = stateService.resetStaleInProgressExports(60);

        // Then - The stale export should be reset to FAILED
        assertThat(resetCount).isEqualTo(1);

        ExportState result = stateService.getState("block", "2024-10-30");
        assertThat(result.getStatus()).isEqualTo(ExportStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("stale IN_PROGRESS");
        assertThat(result.getErrorMessage()).contains("60 min timeout");
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getDurationSeconds()).isNotNull();
    }

    @Test
    public void testResetStaleInProgressExports_doesNotResetRecentExports() {
        // Given - An IN_PROGRESS export that started 5 minutes ago (not stale)
        ExportState recentState = stateService.markInProgress("block", "2024-10-30");
        recentState.setStartedAt(LocalDateTime.now().minusMinutes(5));
        repository.save(recentState);
        entityManager.flush();

        // When - Reset stale exports with 60 min timeout
        int resetCount = stateService.resetStaleInProgressExports(60);

        // Then - The recent export should NOT be reset
        assertThat(resetCount).isEqualTo(0);

        ExportState result = stateService.getState("block", "2024-10-30");
        assertThat(result.getStatus()).isEqualTo(ExportStatus.IN_PROGRESS);
    }

    @Test
    public void testResetStaleInProgressExports_doesNotAffectCompleted() {
        // Given - A completed export
        ExportState state = stateService.markInProgress("block", "2024-10-30");
        stateService.markCompleted(state, new ExportResult("/path", 100, 500, 100), "checksum1");
        entityManager.flush();

        // When - Reset stale exports
        int resetCount = stateService.resetStaleInProgressExports(60);

        // Then - Completed export should not be affected
        assertThat(resetCount).isEqualTo(0);

        ExportState result = stateService.getState("block", "2024-10-30");
        assertThat(result.getStatus()).isEqualTo(ExportStatus.COMPLETED);
    }

    @Test
    public void testResetStaleInProgressExports_doesNotAffectFailed() {
        // Given - A failed export
        ExportState state = stateService.markInProgress("block", "2024-10-30");
        stateService.markFailed(state, "Some error");
        entityManager.flush();

        // When - Reset stale exports
        int resetCount = stateService.resetStaleInProgressExports(60);

        // Then - Failed export should not be affected
        assertThat(resetCount).isEqualTo(0);

        ExportState result = stateService.getState("block", "2024-10-30");
        assertThat(result.getStatus()).isEqualTo(ExportStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Some error");
    }

    @Test
    public void testResetStaleInProgressExports_multipleStaleExports() {
        // Given - Multiple stale IN_PROGRESS exports across different tables
        ExportState stale1 = stateService.markInProgress("block", "2024-10-30");
        stale1.setStartedAt(LocalDateTime.now().minusHours(3));
        repository.save(stale1);

        ExportState stale2 = stateService.markInProgress("transaction", "2024-10-30");
        stale2.setStartedAt(LocalDateTime.now().minusHours(2));
        repository.save(stale2);

        // A recent (non-stale) export
        ExportState recent = stateService.markInProgress("reward", "epoch=450");
        recent.setStartedAt(LocalDateTime.now().minusMinutes(10));
        repository.save(recent);

        entityManager.flush();

        // When - Reset stale exports with 60 min timeout
        int resetCount = stateService.resetStaleInProgressExports(60);

        // Then - Only the 2 stale exports should be reset
        assertThat(resetCount).isEqualTo(2);

        assertThat(stateService.getState("block", "2024-10-30").getStatus())
                .isEqualTo(ExportStatus.FAILED);
        assertThat(stateService.getState("transaction", "2024-10-30").getStatus())
                .isEqualTo(ExportStatus.FAILED);
        assertThat(stateService.getState("reward", "epoch=450").getStatus())
                .isEqualTo(ExportStatus.IN_PROGRESS);
    }

    @Test
    public void testResetStaleInProgressExports_returnsZeroWhenNoneStale() {
        // Given - No IN_PROGRESS exports at all
        ExportState state = stateService.markInProgress("block", "2024-10-30");
        stateService.markCompleted(state, new ExportResult("/path", 100, 500, 100), "checksum");
        entityManager.flush();

        // When
        int resetCount = stateService.resetStaleInProgressExports(60);

        // Then
        assertThat(resetCount).isEqualTo(0);
    }

    @Test
    public void testResetAllInProgressExports_resetsAll() {
        // Given - Multiple IN_PROGRESS exports (both recent and stale)
        ExportState state1 = stateService.markInProgress("block", "2024-10-30");
        ExportState state2 = stateService.markInProgress("transaction", "2024-10-30");
        ExportState state3 = stateService.markInProgress("reward", "epoch=450");
        entityManager.flush();

        // When - Reset all (used during shutdown)
        int resetCount = stateService.resetAllInProgressExports();

        // Then - All should be reset to FAILED
        assertThat(resetCount).isEqualTo(3);

        assertThat(stateService.getState("block", "2024-10-30").getStatus())
                .isEqualTo(ExportStatus.FAILED);
        assertThat(stateService.getState("transaction", "2024-10-30").getStatus())
                .isEqualTo(ExportStatus.FAILED);
        assertThat(stateService.getState("reward", "epoch=450").getStatus())
                .isEqualTo(ExportStatus.FAILED);

        // Verify error messages indicate shutdown
        assertThat(stateService.getState("block", "2024-10-30").getErrorMessage())
                .contains("application shutdown");
    }

    @Test
    public void testResetAllInProgressExports_doesNotAffectOtherStatuses() {
        // Given - Mix of statuses
        ExportState completed = stateService.markInProgress("block", "2024-10-29");
        stateService.markCompleted(completed, new ExportResult("/path", 100, 500, 100), "checksum");

        ExportState failed = stateService.markInProgress("block", "2024-10-28");
        stateService.markFailed(failed, "Error");

        ExportState inProgress = stateService.markInProgress("block", "2024-10-30");
        entityManager.flush();

        // When
        int resetCount = stateService.resetAllInProgressExports();

        // Then - Only the IN_PROGRESS export should be reset
        assertThat(resetCount).isEqualTo(1);

        assertThat(stateService.getState("block", "2024-10-29").getStatus())
                .isEqualTo(ExportStatus.COMPLETED);
        assertThat(stateService.getState("block", "2024-10-28").getStatus())
                .isEqualTo(ExportStatus.FAILED);
        assertThat(stateService.getState("block", "2024-10-30").getStatus())
                .isEqualTo(ExportStatus.FAILED);
    }

    @Test
    public void testResetAllInProgressExports_returnsZeroWhenNone() {
        // Given - No IN_PROGRESS exports
        ExportState state = stateService.markInProgress("block", "2024-10-30");
        stateService.markCompleted(state, new ExportResult("/path", 100, 500, 100), "checksum");
        entityManager.flush();

        // When
        int resetCount = stateService.resetAllInProgressExports();

        // Then
        assertThat(resetCount).isEqualTo(0);
    }

    @Test
    public void testStaleResetAllowsRetry() {
        // Given - A stale export is reset, then retried successfully
        ExportState stale = stateService.markInProgress("block", "2024-10-30");
        stale.setStartedAt(LocalDateTime.now().minusHours(2));
        repository.save(stale);
        entityManager.flush();

        // Reset stale
        stateService.resetStaleInProgressExports(60);
        entityManager.flush();

        // Verify it's FAILED now
        ExportState afterReset = stateService.getState("block", "2024-10-30");
        assertThat(afterReset.getStatus()).isEqualTo(ExportStatus.FAILED);

        // When - Retry the export
        ExportState retryState = stateService.markInProgress("block", "2024-10-30");
        entityManager.flush();

        // Then - Retry count should increment and status should be IN_PROGRESS
        assertThat(retryState.getStatus()).isEqualTo(ExportStatus.IN_PROGRESS);
        assertThat(retryState.getRetryCount()).isEqualTo(1);

        // Complete the retry
        stateService.markCompleted(retryState, new ExportResult("/path", 100, 500, 100), "checksum");
        entityManager.flush();

        ExportState finalState = stateService.getState("block", "2024-10-30");
        assertThat(finalState.getStatus()).isEqualTo(ExportStatus.COMPLETED);
    }

    @Test
    public void testDurationCalculatedOnStaleReset() {
        // Given - A stale export started 90 minutes ago
        ExportState stale = stateService.markInProgress("block", "2024-10-30");
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(90);
        stale.setStartedAt(startTime);
        repository.save(stale);
        entityManager.flush();

        // When
        stateService.resetStaleInProgressExports(60);
        entityManager.flush();

        // Then - Duration should be calculated from the original start time
        ExportState result = stateService.getState("block", "2024-10-30");
        assertThat(result.getDurationSeconds()).isNotNull();
        // Duration should be at least 90 minutes = 5400 seconds (with some tolerance)
        assertThat(result.getDurationSeconds()).isGreaterThanOrEqualTo(5399);
    }
}
