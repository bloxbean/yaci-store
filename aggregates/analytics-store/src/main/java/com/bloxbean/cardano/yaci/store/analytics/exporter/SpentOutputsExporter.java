package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Exporter for spent outputs (append-only log).
 *
 * This exporter creates an append-only log of when outputs are spent:
 * - Partitioned by spent date (when the output was consumed)
 * - Links back to transaction_outputs via (tx_hash, output_index)
 * - Enables efficient "unspent UTXO" queries via LEFT JOIN / NOT EXISTS
 *
 * Partitioning: DAILY (date=yyyy-MM-dd) - BY SPENT DATE
 * Source: tx_input table
 * Output: spent_outputs/date=2024-01-15/data.parquet
 *
 * NOTE: Partitioned by SPENT date (spent_at_slot), not creation date.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class SpentOutputsExporter extends AbstractTableExporter {

    public SpentOutputsExporter(
            StorageWriter storageWriter,
            ExportStateService stateService,
            EraService eraService,
            AnalyticsStoreProperties properties) {
        super(storageWriter, stateService, eraService, properties);
    }

    @Override
    public String getTableName() {
        return "tx_input";
    }

    @Override
    public PartitionStrategy getPartitionStrategy() {
        return PartitionStrategy.DAILY;
    }

    @Override
    public String getPartitionColumn() {
        // Spent outputs are partitioned by when they were spent, not created
        return "spent_block_time";
    }

    /**
     * Build SQL query for spent outputs.
     *
     * Filters by spent_at_slot range (indexed column for performance).
     * NOTE: Partitioned by SPENT date, not creation date.
     */
    @Override
    protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
        String schema = getSourceSchema();
        return String.format("""
            SELECT
                ti.tx_hash,
                ti.output_index,
                ti.spent_tx_hash,
                ti.spent_at_slot,
                ti.spent_at_block,
                ti.spent_at_block_hash,
                to_timestamp(ti.spent_block_time) as spent_block_time,
                ti.spent_epoch
            FROM source_db.%s.tx_input ti
            WHERE ti.spent_at_slot >= %d
              AND ti.spent_at_slot < %d
            ORDER BY ti.spent_at_slot, ti.spent_tx_hash
            """,
            schema,
            slotRange.startSlot(),
            slotRange.endSlot()
        );
    }
}
