package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for blockchain rollback events.
 *
 * This exporter tracks rollback events that occurred during chain synchronization.
 * Rollbacks happen when the chain reorganizes due to competing forks.
 *
 * Partitioning: DAILY (date=yyyy-MM-dd based on current_slot)
 * Source: rollback table
 * Output: rollback/date=yyyy-MM-dd/data.parquet
 *
 * Use cases:
 * - Chain stability analysis
 * - Understanding reorganization patterns
 * - Debugging sync issues
 * - Monitoring chain health
 *
 * Note: This table typically has low volume but is important for understanding
 * chain dynamics and potential data consistency issues.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class RollbackExporter extends AbstractTableExporter {

    public RollbackExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "rollback";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    /**
     * Build SQL query for rollback events in the slot range.
     *
     * Uses current_slot for partitioning as it represents when the rollback occurred.
     * JOINs with block table to get block_time for date partitioning.
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT
                r.id,
                r.rollback_to_block_hash,
                r.rollback_to_slot,
                r.current_block_hash,
                r.current_slot,
                r.current_block,
                to_timestamp(b.block_time) as block_time
            FROM source_db.%s.rollback r
            INNER JOIN source_db.%s.block b ON r.current_slot = b.slot
            WHERE r.current_slot >= %d
              AND r.current_slot < %d
            ORDER BY r.current_slot, r.id
            """,
            schema, schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}
