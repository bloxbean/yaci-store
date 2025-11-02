package com.bloxbean.cardano.yaci.store.analytics.writer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an export operation.
 *
 * Contains statistics about the exported data, applicable to both
 * Parquet and DuckLake storage formats.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportResult {
    /**
     * Path to the exported file or directory
     */
    private String filePath;

    /**
     * Number of rows exported
     */
    private long rowCount;

    /**
     * Total size of exported data in bytes
     */
    private long fileSize;

    /**
     * Export duration in milliseconds
     */
    private long durationMs;
}
