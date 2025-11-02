package com.bloxbean.cardano.yaci.store.analytics.state;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_export_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportState {

    @EmbeddedId
    private ExportStateId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_status", nullable = false)
    private ExportStatus status;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Convenience constructor
    public ExportState(String tableName, String partitionValue) {
        this.id = new ExportStateId(tableName, partitionValue);
        this.status = ExportStatus.PENDING;
        this.retryCount = 0;
    }
}
